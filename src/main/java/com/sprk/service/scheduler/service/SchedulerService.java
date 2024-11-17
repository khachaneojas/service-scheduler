package com.sprk.service.scheduler.service;

import com.sprk.commons.dao.JobProxy;
import com.sprk.commons.dto.APIResponse;
import com.sprk.commons.entity.mq.JobModel;
import com.sprk.commons.entity.mq.RegistryModel;
import com.sprk.commons.entity.mq.tag.JobStatus;
import com.sprk.commons.entity.mq.tag.JobType;
import com.sprk.commons.entity.mq.tag.ScheduleType;
import com.sprk.commons.entity.primary.common.FileDataModel;
import com.sprk.commons.entity.primary.common.OrganizationModel;
import com.sprk.commons.entity.primary.enquiry.EnquiryArchiveModel;
import com.sprk.commons.entity.primary.enquiry.EnquiryModel;
import com.sprk.commons.entity.primary.user.HolidayModel;
import com.sprk.commons.entity.primary.user.RequestModel;
import com.sprk.commons.entity.primary.user.UserModel;
import com.sprk.commons.exception.InvalidDataException;
import com.sprk.commons.exception.ResourceNotFoundException;
import com.sprk.service.scheduler.properties.amqp.AMQPConfigProperties;
import com.sprk.service.scheduler.dao.JPAProxy;
import com.sprk.service.scheduler.dto.payload.JobRequest;
import com.sprk.service.scheduler.repository.mq.JobRepository;
import com.sprk.service.scheduler.repository.primary.*;
import com.sprk.service.scheduler.tag.DeviceAddressType;
import com.sprk.service.scheduler.util.DeviceIdentityWizard;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {

    private final JPAProxy jpaProxy;
    private final JobProxy jobProxy;
    private final JobProcessor jobProcessor;

    private final FileDataRepository fileDataRepository;
    private final UserRepository userRepository;
    private final EmployeeRequestRepository employeeRequestRepository;
    private final HolidayRepository holidayRepository;
    private final OrganizationRepository organizationRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryArchiveRepository enquiryArchiveRepository;
    private final JobRepository jobRepository;

    private final AtomicBoolean isJobProcessing = new AtomicBoolean(false);
    private final AtomicBoolean isInstanceSyncing = new AtomicBoolean(false);


    // TESTING
    private final AMQPConfigProperties amqpConfigProperties;
    private final RabbitTemplate rabbitTemplate;




    /**
     * Inserts unique identifiers (UIDs) for items that are missing them. The method generates UIDs
     * using the provided UID generator function and ensures that the generated UIDs are unique
     * across all items.
     *
     * @param findAll         Supplier to retrieve all items from the data source.
     * @param saveAll         Consumer to save the updated list of items back to the data source.
     * @param isUIDNotBlank   Predicate to check if an item's UID is not blank.
     * @param getUidFunction  Function to retrieve the UID from an item.
     * @param setUidConsumer  BiConsumer to set the UID for an item.
     * @param uidGenerator    Function to generate a UID for an item.
     * @param <T>             The type of the items.
     */
    public <T> void insertMissingUID(
            Supplier<List<T>> findAll,
            Consumer<List<T>> saveAll,
            Predicate<T> isUIDNotBlank,
            Function<T, String> getUidFunction,
            BiConsumer<T, String> setUidConsumer,
            Function<T, String> uidGenerator
    ) {
        // Retrieve all items from the data source
        List<T> allItems = findAll.get();

        // Filter items that are missing UIDs
        List<T> itemsWithoutUID = allItems.stream()
                .filter(isUIDNotBlank.negate())
                .collect(Collectors.toCollection(ArrayList::new));

        // If there are items without UIDs, process them
        if (!itemsWithoutUID.isEmpty()) {
            // Collect existing UIDs from items that already have them
            HashSet<String> existingUid = allItems.stream()
                    .filter(isUIDNotBlank)
                    .map(getUidFunction)
                    .collect(Collectors.toCollection(HashSet::new));

            // Iterate over each item without a UID
            for (T item : itemsWithoutUID) {
                String uid;
                boolean exists;
                int attempts = 0;

                // Generate a new UID and check for uniqueness, retrying up to 100 times if necessary
                do {
                    uid = uidGenerator.apply(item);
                    exists = existingUid.contains(uid);
                    attempts++;
                } while (exists && attempts <= 100);

                // If a unique UID is found, set it on the item and add it to the set of existing UIDs
                if (!exists) {
                    setUidConsumer.accept(item, uid);
                    existingUid.add(uid);
                } else {
                    // If no unique UID can be found after 100 attempts, throw an exception
                    throw new InvalidDataException("Failed to add unique identifier. Try again");
                }
            }
            // Save the updated list of items back to the data source
            saveAll.accept(itemsWithoutUID);
        }
    }



    /**
     * Inserts unique identifiers (UIDs) for files that are missing them. The method generates UIDs
     * using a specific pattern that includes a prefix based on the creation timestamp of the file and a UUID suffix.
     */
    public void insertMissingUIDInFiles() {
        insertMissingUID(
                // Supplier to retrieve all FileDataModel items from the repository
                fileDataRepository::findAll,
                // Consumer to save the updated list of FileDataModel items back to the repository
                fileDataRepository::saveAll,
                // Predicate to check if a FileDataModel's UID is not blank
                model -> StringUtils.isNotBlank(model.getFileUid()),
                // Function to retrieve the UID from a FileDataModel
                FileDataModel::getFileUid,
                // BiConsumer to set the UID for a FileDataModel
                FileDataModel::setFileUid,
                model -> {
                    // Extract the creation timestamp of the file and format it
                    LocalDateTime createdAt = LocalDateTime.ofInstant(model.getCreatedAt(), ZoneOffset.UTC);
                    String prefix = "F" + DateTimeFormatter.ofPattern("yyMMddHHmm").format(createdAt);
                    // Generate a random UUID suffix
                    String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6);
                    // Combine the prefix and suffix to create the UID
                    return prefix + uuid;
                }
        );
    }



    /**
     * Inserts missing UIDs in enquiry data models.
     */
    public void insertMissingUIDInEnquiries() {
        insertMissingUID(
                enquiryRepository::findAll,
                enquiryRepository::saveAll,
                model -> StringUtils.isNotBlank(model.getEnquiryUid()),
                EnquiryModel::getEnquiryUid,
                EnquiryModel::setEnquiryUid,
                model -> {
                    LocalDateTime createdAt = LocalDateTime.ofInstant(model.getCreatedAt(), ZoneOffset.UTC);
                    String prefix = "EQ" + DateTimeFormatter.ofPattern("yyMMdd").format(createdAt);
                    String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 7);
                    return prefix + uuid;
                }
        );
    }



    /**
     * Capitalizes the first, middle, and last names of all employee requests.
     * Retrieves all employee requests from the repository, capitalizes the names,
     * and saves the updated requests back to the repository.
     */
    public void capitalizeNamesInEmployeeRequests() {

        // Retrieve all employee requests from the repository
        ArrayList<RequestModel> employeeRequests = employeeRequestRepository.findAll()
                .stream()
                .map(model -> {
                    // Capitalize the first name
                    model.setFirstname(
                            StringUtils.capitalize(model.getFirstname())
                    );
                    // Capitalize the middle name
                    model.setMiddlename(
                            StringUtils.capitalize(model.getMiddlename())
                    );
                    // Capitalize the last name
                    model.setLastname(
                            StringUtils.capitalize(model.getLastname())
                    );
                    return model;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Save all the updated employee requests back to the repository
        employeeRequestRepository.saveAll(employeeRequests);
    }



    /**
     * Capitalizes the first, middle, and last names of all users.
     * Retrieves all user records from the repository, capitalizes the names,
     * and saves the updated records back to the repository.
     */
    public void capitalizeNamesInUsers() {

        // Retrieve all users from the repository
        ArrayList<UserModel> employeeRequests = userRepository.findAll()
                .stream()
                .map(model -> {
                    // Capitalize the first name
                    model.setFirstname(
                            StringUtils.capitalize(model.getFirstname())
                    );
                    // Capitalize the middle name
                    model.setMiddlename(
                            StringUtils.capitalize(model.getMiddlename())
                    );
                    // Capitalize the last name
                    model.setLastname(
                            StringUtils.capitalize(model.getLastname())
                    );

                    return model;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Save all the updated user records back to the repository
        userRepository.saveAll(employeeRequests);
    }



    /**
     * Capitalizes the parent, referral, and student names in all enquiry records.
     * Retrieves all enquiries from the repository, capitalizes the specified names,
     * and saves the updated records back to the repository.
     */
    public void capitalizeNamesInEnquiries() {
        // Retrieve all enquiries from the repository
        ArrayList<EnquiryModel> enquiries = enquiryRepository.findAll()
                .stream()
                .map(model -> {
                    // Capitalize the parent's first, middle, and last names
                    model.setParentFirstname(
                            StringUtils.capitalize(model.getParentFirstname())
                    );
                    model.setParentMiddlename(
                            StringUtils.capitalize(model.getParentMiddlename())
                    );
                    model.setParentLastname(
                            StringUtils.capitalize(model.getParentLastname())
                    );

                    // Capitalize the referral's first, middle, and last names
                    model.setReferralFirstname(
                            StringUtils.capitalize(model.getReferralFirstname())
                    );
                    model.setReferralMiddlename(
                            StringUtils.capitalize(model.getReferralMiddlename())
                    );
                    model.setReferralLastname(
                            StringUtils.capitalize(model.getReferralLastname())
                    );

                    // Capitalize the student's first, middle, and last names
                    model.setStudentFirstname(
                            StringUtils.capitalize(model.getStudentFirstname())
                    );
                    model.setStudentMiddlename(
                            StringUtils.capitalize(model.getStudentMiddlename())
                    );
                    model.setStudentLastname(
                            StringUtils.capitalize(model.getStudentLastname())
                    );
                    return model;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Save all the updated enquiry records back to the repository
        enquiryRepository.saveAll(enquiries);
    }



    /**
     * Capitalizes the parent, referral, and student names in all archived enquiry records.
     * Retrieves all archived enquiries from the repository, capitalizes the specified names,
     * and saves the updated records back to the repository.
     */
    public void capitalizeNamesInArchivedEnquiries() {
        // Retrieve all archived enquiries from the repository
        ArrayList<EnquiryArchiveModel> archivedEnquiries = enquiryArchiveRepository.findAll()
                .stream()
                .map(model -> {
                    // Capitalize the parent's first, middle, and last names
                    model.setParentFirstname(
                            StringUtils.capitalize(model.getParentFirstname())
                    );
                    model.setParentMiddlename(
                            StringUtils.capitalize(model.getParentMiddlename())
                    );
                    model.setParentLastname(
                            StringUtils.capitalize(model.getParentLastname())
                    );

                    // Capitalize the referral's first, middle, and last names
                    model.setReferralFirstname(
                            StringUtils.capitalize(model.getReferralFirstname())
                    );
                    model.setReferralMiddlename(
                            StringUtils.capitalize(model.getReferralMiddlename())
                    );
                    model.setReferralLastname(
                            StringUtils.capitalize(model.getReferralLastname())
                    );

                    // Capitalize the student's first, middle, and last names
                    model.setStudentFirstname(
                            StringUtils.capitalize(model.getStudentFirstname())
                    );
                    model.setStudentMiddlename(
                            StringUtils.capitalize(model.getStudentMiddlename())
                    );
                    model.setStudentLastname(
                            StringUtils.capitalize(model.getStudentLastname())
                    );
                    return model;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Save all the updated archived enquiry records back to the repository
        enquiryArchiveRepository.saveAll(archivedEnquiries);
    }



    /**
     * Inserts a list of holidays into the repository from a given map of holidays.
     * Each entry in the map represents a holiday with its name and start and end dates.
     *
     * @param holidaysMap a map where the key is the holiday name, and the value is an
     *                    ImmutablePair containing the start and end dates of the holiday.
     */
    public void insertHolidays(HashMap<String, ImmutablePair<String, String>> holidaysMap) {
        // Check if the holidaysMap is null or empty, and return early if so
        if (null == holidaysMap || holidaysMap.isEmpty()) {
            return;
        }

        // Convert the entries of the map into a list of HolidayModel objects
        ArrayList<HolidayModel> holidays = holidaysMap.entrySet()
                .stream()
                .map(entry -> {

                    // Extract the start and end dates from the ImmutablePair
                    ImmutablePair<String, String> value = entry.getValue();

                    // Build a HolidayModel instance for each map entry
                    return HolidayModel.builder()
                            .holidayUid(null)   // UID is initially null and will be set by the database
                            .holidayName(entry.getKey())    // Set the holiday name
                            .holidayStart(Instant.parse(value.left))    // Parse and set the start date
                            .holidayEnd(Instant.parse(value.right))     // Parse and set the end date
                            .build();
                })
                .collect(Collectors.toCollection(ArrayList::new));  // Collect the results into an ArrayList

        // Save all the HolidayModel instances into the holidayRepository
        holidayRepository.saveAll(holidays);
    }



    /**
     * Inserts a new organization into the repository with the specified details.
     *
     * @param orgCode               The code identifying the organization.
     * @param orgAddress            The address of the organization.
     * @param orgTimeZone           The time zone of the organization.
     * @param maxFileUploadLimit   The maximum file upload limit for the organization.
     * @param certificateReleaseInDays The number of days after which certificates are released.
     */
    public void insertOrganization(String orgCode, String orgAddress, String orgTimeZone, Long maxFileUploadLimit, Integer certificateReleaseInDays) {
        // Create a new instance of OrganizationModel using the provided details
        OrganizationModel model = OrganizationModel.builder()
                .code(orgCode)      // Set the organization code
                .address(orgAddress)    // Set the organization address
                .zone(orgTimeZone)      // Set the time zone
                .maxFileUploadLimit(maxFileUploadLimit)     // Set the maximum file upload limit
                .certificateReleaseInDays(certificateReleaseInDays)     // Set the certificate release delay in days
                .build();

        // Save the newly created OrganizationModel instance to the repository
        organizationRepository.save(model);
    }





    /**
     * Initializes the application.
     * This method is annotated with @PostConstruct and is transactional with serializable isolation level and required propagation.
     * It schedules the autoRegistrySchedule() method and adds a job in the database if no jobs exist.
     */
    @PostConstruct
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public void init() {
//        autoSyncInstanceInRegistry();
//        rabbitTemplate.convertAndSend(
//                amqpConfigProperties.getExchange(),
//                amqpConfigProperties.getRoutingKey().getCertificateReleaser(),
//                443L
//        );

        if (jpaProxy.getAllJobCountByType(JobType.STUDENT_PAYMENT_DUE_MAIL) < 1) {
            Instant instant10AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(4, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Payment Due Email Reminder.",
                    "Reminds students for upcoming due date (10 Feb) with pattern of (30-31, 1, 3, 5, 7, 8, 9, 10)",
                    null,
                    JobType.STUDENT_PAYMENT_DUE_MAIL,
                    ScheduleType.EVERYDAY,
                    instant10AMAtUTC,
                    null,
                    null
            );
        }

        if (jpaProxy.getAllJobCountByType(JobType.DELETE_UNPAID_BOOKINGS) < 1) {
            Instant instant9AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(3, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Delete Bookings with No Payments.",
                    "Remove all bookings that do not have any associated payments.",
                    null,
                    JobType.DELETE_UNPAID_BOOKINGS,
                    ScheduleType.EVERYDAY,
                    instant9AMAtUTC,
                    null,
                    null
            );
        }

        if (jpaProxy.getAllJobCountByType(JobType.UPDATE_EXPIRY_STATUS) < 1) {
            Instant instant3AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(21, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Update booking Status to EXPIRED.",
                    "Update the booking status to EXPIRED for all bookings that have reached their expiry date.",
                    null,
                    JobType.UPDATE_EXPIRY_STATUS,
                    ScheduleType.EVERYDAY,
                    instant3AMAtUTC,
                    null,
                    null
            );

        }

        if (jpaProxy.getAllJobCountByType(JobType.EXPIRY_REMINDER_MAIL) < 1) {
            Instant instant2AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(20, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Send booking expiry reminder mail.",
                    "Send booking expiry reminder mail to all students whose bookings are set to expire within the next 10 days.",
                    null,
                    JobType.EXPIRY_REMINDER_MAIL,
                    ScheduleType.EVERYDAY,
                    instant2AMAtUTC,
                    null,
                    null
            );
        }

        if (jpaProxy.getAllJobCountByType(JobType.UPDATE_STUDENT_STATUS) < 1) {
            Instant instant4AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(22, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Update student status to FINANCIAL DROPOUT.",
                    "Update the student status to FINANCIAL DROPOUT if the student fails to pay the installment within 60 days.",
                    null,
                    JobType.UPDATE_STUDENT_STATUS,
                    ScheduleType.EVERYDAY,
                    instant4AMAtUTC,
                    null,
                    null
            );
        }


//         Sync Certificate data from portal to website db
        if (jpaProxy.getAllJobCountByType(JobType.WEBSITE_DATA_TRANSFER) < 1) {
            Instant instant7AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(1, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Sync certificate data from portal to website database",
                    "Add or update a newly published or existing certificate",
                    null,
                    JobType.WEBSITE_DATA_TRANSFER,
                    ScheduleType.EVERYDAY,
                    instant7AMAtUTC,
                    null,
                    null
            );
        }



        if (jpaProxy.getAllJobCountByType(JobType.NOTIFY_BOOKING_START) < 1) {
            Instant instant8AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(2, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Notify user 5 days prior of estimated start date of booking.",
                    "The associated user of the booking will be reminded 5 days in advance of estimated start date.",
                    null,
                    JobType.NOTIFY_BOOKING_START,
                    ScheduleType.EVERYDAY,
                    instant8AMAtUTC,
                    null,
                    null
            );
        }




        if (jpaProxy.getAllJobCountByType(JobType.RELEASE_CERTIFICATES) < 1) {
            Instant instant6AMAtUTC = LocalDateTime
                    .of(LocalDate.now(), LocalTime.of(0, 30))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
            jobProxy.addJobInDB(
                    "Release Certificates.",
                    "Release all the certificates that are set to be issued.",
                    null,
                    JobType.RELEASE_CERTIFICATES,
                    ScheduleType.EVERYDAY,
                    instant6AMAtUTC,
                    null,
                    null
            );
        }

    }


//    /**
//     * Automatically updates the registry at a fixed delay interval.
//     * This method is scheduled to run every 30 seconds.
//     */
    /*
    @Scheduled(fixedDelay = 10_000)
    public void autoSyncInstanceInRegistry() {
        if (isInstanceSyncing.compareAndSet(false, true)) {
            try {
//                log.info("Resolving registry via schedule.");
                syncInstanceInRegistry();
            } finally {
                isInstanceSyncing.set(false);
            }
        }
    }
     */

    /**
     * Automatically adds jobs to RabbitMQ for processing at a fixed delay interval.
     * This method is scheduled to run every 60 seconds with an initial delay of 60 seconds.
     */
    @Scheduled(fixedDelay = 90_000, initialDelay = 60_000)
    public void autoPublishJobsInQueue() {
        if (isJobProcessing.compareAndSet(false, true)) {
            try {
//                log.info("Resolving job-queue via schedule.");
                publishJobsInQueue();
            } finally {
                isJobProcessing.set(false);
            }
        }
    }



    /**
     * Adds a job to the database based on the provided request parameters.
     * This method is transactional with serializable isolation level and required propagation.
     *
     * @param request The job request containing details like name, type, schedule, etc.
     * @return An APIResponse indicating the success or failure of adding the job.
     * @throws InvalidDataException If any required parameter is missing or invalid.
     */
    public APIResponse<String> addJobInDB(@Nonnull JobRequest request) {
        StringBuilder errorBuilder = new StringBuilder("Cannot proceed without specifying ");
        jobProxy.addJobInDB(
                Optional.ofNullable(request.getName()).orElseThrow(() -> new InvalidDataException(errorBuilder.append("job-name.").toString())),
                request.getDescription(),
                request.getJson(),
                Optional.ofNullable(request.getJob_type()).orElseThrow(() -> new InvalidDataException(errorBuilder.append("job-type.").toString())),
                Optional.ofNullable(request.getSchedule_type()).orElseThrow(() -> new InvalidDataException(errorBuilder.append("schedule-type.").toString())),
                Optional.ofNullable(request.getTime()).orElseThrow(() -> new InvalidDataException(errorBuilder.append("time.").toString())),
                null,
                null
        );

        return APIResponse
                .<String>builder()
                .message("Added Job in the DB. [" + request.getJob_type() + "]")
                .build();
    }



    /**
     * Updates the heartbeat in the registry.
     * This method is annotated with @Transactional to ensure serializable isolation level and required propagation.
     */
    private void syncInstanceInRegistry() {
        RegistryModel instance = jobProcessor.getInstance();
        instance.setLastUpdateReceived(Instant.now());
        jpaProxy.saveRegistry(instance);
    }



    /**
     * Adds jobs to RabbitMQ for processing.
     * This method retrieves jobs from the database and adds them to the RabbitMQ queue based on their status and schedule.
     * It is transactional with serializable isolation level and required propagation.
     */
    private void publishJobsInQueue() {
        jpaProxy.getAllJobs()
                .stream()
                .filter(model -> !Arrays.asList(JobStatus.RUNNING, JobStatus.NO_INSTANCE).contains(model.getStatus()))
                .filter(model -> !JobStatus.FAILED.equals(model.getStatus())
                        || jobProcessor.isJobAttemptsNotExceeded(model.getAttempts())
                        || jobProcessor.isJobScheduleIsEverydayAndPastLastRanAtOrNull(model::getScheduleType, model::getLastRanAt))
                .filter(jobProcessor::isJobReadyToRun)
                .map(jobProcessor::enqueueJob)
                .filter(Objects::nonNull)
                .forEach(job -> log.info("Added Job ({}) in the queue. [{}]", job.getLeft(), job.getRight()));
    }



    /**
     * Retrieves the most recent instance from the registry.
     * This method is transactional with read committed isolation level and required propagation.
     * It retrieves the most recent instance from the registry repository.
     * If no instances are found, it throws a ResourceNotFoundException.
     * @return The most recent instance from the registry.
     * @throws ResourceNotFoundException if no instances are found in the registry.
     */
    private RegistryModel getMostRecentInstance() {
        // Retrieve the most recent instance from the registry repository.
        return Optional
                .ofNullable(jpaProxy.getMostRecentInstance())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElseThrow(() ->
                        new ResourceNotFoundException("No instances are running.")
                );
    }



    /**
     * Retrieves the count of instances with the same MAC address.
     * This method is transactional with read committed isolation level and required propagation.
     * It retrieves the count of instances from the registry repository with the specified MAC address.
     * @param macAddress The MAC address to search for.
     * @return The count of instances with the same MAC address.
     */
    private int getInstanceCountForSameMacAddress(String macAddress) {
        // Retrieve the count of instances from the registry repository with the specified MAC address.
        // If the MAC address is null, return -1.
        return Optional
                .of(macAddress)
                .map(jpaProxy::getMACCount)
                .orElse(-1);
    }

//    @Scheduled(fixedRate = 60000) // Run every minute
//    public void scheduleDataTransfer() {
//        // Create a message with details
//        String message = "Transfer Data";
//        rabbitTemplate.convertAndSend("dataTransferQueue", message);
//    }


}
