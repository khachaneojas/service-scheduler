package com.sprk.service.scheduler.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.sprk.commons.dao.JobProxy;
import com.sprk.commons.dto.APIResponse;
import com.sprk.commons.dto.amqp.AddStartDateForCertificationStatusDTO;
import com.sprk.commons.entity.mq.JobModel;
import com.sprk.commons.entity.mq.tag.JobStatus;
import com.sprk.commons.entity.mq.tag.JobType;
import com.sprk.commons.entity.mq.tag.ScheduleType;
import com.sprk.commons.entity.primary.batch.mapping.BatchStudentMapping;
import com.sprk.commons.entity.primary.common.OrganizationModel;
import com.sprk.commons.entity.primary.course.CourseGroupModel;
import com.sprk.commons.entity.primary.course.CourseModel;
import com.sprk.commons.entity.primary.enquiry.EnquiryModel;
import com.sprk.commons.entity.primary.examination.CertificateModel;
import com.sprk.commons.entity.primary.examination.tag.AssessmentType;
import com.sprk.commons.entity.primary.examination.tag.CertificateStatus;
import com.sprk.commons.entity.primary.examination.tag.FinalExamStatus;
import com.sprk.commons.entity.primary.student.BookingModel;
import com.sprk.commons.entity.primary.student.StudentModel;
import com.sprk.commons.entity.primary.student.mapping.BookingCertificateStatusMapping;
import com.sprk.commons.entity.primary.student.mapping.BookingCourseCertificateStatusMapping;
import com.sprk.commons.entity.primary.student.mapping.BookingCourseGroupMapping;
import com.sprk.commons.entity.primary.student.mapping.BookingCourseMapping;
import com.sprk.commons.entity.primary.student.tag.*;
import com.sprk.commons.entity.primary.user.NotificationModel;
import com.sprk.commons.entity.primary.user.UserModel;
import com.sprk.commons.entity.primary.user.mapping.NotificationUserMapping;
import com.sprk.commons.entity.website.CertificateWModel;
import com.sprk.commons.exception.InvalidDataException;
import com.sprk.commons.exception.ResourceNotFoundException;
import com.sprk.commons.experimental.ImplProvider;
import com.sprk.service.scheduler.dao.JPAProxy;
import com.sprk.commons.dto.amqp.ExamStatusChangeDTO;
import com.sprk.service.scheduler.dto.procedure.SPGetAttendanceAllStudentsForBatch;
import com.sprk.service.scheduler.repository.primary.*;
import com.sprk.service.scheduler.repository.website.CertificateWRepository;
import com.sprk.service.scheduler.util.EMailTemplates;
import com.sprk.service.scheduler.util.JsonConverter;
import com.sprk.service.scheduler.util.MailerWizard;

import com.sprk.service.scheduler.util.TextWizard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.sprk.commons.dto.amqp.EmailTemplateDTO;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@Slf4j
@RequiredArgsConstructor
public class JobExecutor {

    private static final Float ATTENDANCE_CRITERIA = 70f;
    private static final String DEFAULT_JOB_LOGGER_MESSAGE = "Executing job ({}) --- {}";
    private final RestTemplate loadBalancedRestTemplate;
    private final JPAProxy jpaProxy;
    private final JobProxy jobProxy;
    private final MailerWizard mailer;
    private final TextWizard textHelper;
    private final TaskExecutor taskExecutor;
    private final JsonConverter jsonConverter;
    private final EMailTemplates eMailTemplates;
    private final BookingRepository bookingRepository;
    private final StudentRepository studentRepository;
    private final CertificateRepository certificateRepository;
    private final StudentFinalExamRepository studentFinalExamRepository;
    private final BookingCourseGroupMappingRepository bookingCourseGroupMappingRepository;
    private final BookingCertificateStatusMappingRepository certificateStatusMappingRepository;
    private final BookingCourseCertificateStatusMappingRepository courseCertificateStatusMappingRepository;
    private final CertificateWRepository certificateWRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationUserRepository notificationUserRepository;


    @Value("${app.email.certificate.download}")
    private String downloadCertificateLink;


    private static void info(Long jobId, JobStatus status) {
        log.info(DEFAULT_JOB_LOGGER_MESSAGE, jobId, status);
    }

    private static void info(Long jobId, String message) {
        log.info(DEFAULT_JOB_LOGGER_MESSAGE + " --- {}", jobId, JobStatus.RUNNING, message);
    }

    private static void error(Long jobId, String message) {
        log.error(DEFAULT_JOB_LOGGER_MESSAGE, jobId, message);
    }


    /**
     * Handles the execution of standard jobs based on the job type.
     * This method is triggered by messages from a RabbitMQ queue and processes various job types
     * by making appropriate HTTP calls. It is transactional with serializable isolation to ensure
     * consistency in job execution.
     * @param jobId The ID of the job to be executed.
     */
    @RabbitListener(queues = "#{standardQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeStandardJobs(Long jobId) {
        executor(jobId, (job) -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            switch (job.getJobType()) {
                case STUDENT_PAYMENT_DUE_MAIL -> {
                    // Fetch email templates from the student service.
                    ResponseEntity<List<EmailTemplateDTO>> response = loadBalancedRestTemplate.exchange(
                            "http://STUDENTSERVICE/api/student/template/18oq884aomeyqmns6pqe7okusqmt0dm92xvr1o30zydhuepm1d",
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            new ParameterizedTypeReference<List<EmailTemplateDTO>>() {}
                    );
                    // Process the retrieved email templates.
                    List<EmailTemplateDTO> templates = Objects.requireNonNullElseGet(response.getBody(), List::of);
                    if (templates.isEmpty()) {
                        info(job.getId(), "No email templates to can be found.");
                    } else {
                        String emails = templates
                                .stream()
                                .map(EmailTemplateDTO::getRecipient)
                                .collect(Collectors.joining(", "));
                        jobProxy.addJobInDB(
                                "Sending reminders to students for upcoming payment due date (10 Feb) with pattern of (30-31, 1, 3, 5, 7, 8, 9, 10)",
                                "Scheduled emails to be sent to " + emails,
                                jsonConverter.convertListToJsonString(templates),
                                JobType.EMAIL,
                                ScheduleType.ONCE,
                                Instant.now(),
                                null,
                                null
                        );
                        info(job.getId(), "Added Email Job in DB. (" + emails + ")");
                    }
                }

                case EXAM_STATUS_CHANGE -> {
                    // Convert JSON data to ExamStatusChangeDTO and post it to the examination service.
                    ExamStatusChangeDTO payload = Optional.ofNullable(job.getJsonData())
                            .map(jsonString -> {
                                try {
                                    return jsonConverter.convert(jsonString, ExamStatusChangeDTO.class);
                                } catch (IOException exception) {
                                    return null;
                                }
                            })
                            .orElseThrow();
                    loadBalancedRestTemplate.postForObject(
                            "http://EXAMINATIONSERVICE/api/exam/template/0zVCGGErL4KMuX9xF6C9aJYM1IN2F3byGNMKZ3O5apgAbyjsCN",
                            payload,
                            Void.class
                    );
                    info(job.getId(), "Made REST Call for EXAM_STATUS_CHANGE.");
                }

                case DELETE_UNPAID_BOOKINGS -> {
                    // Make a DELETE request to remove unpaid bookings.
                    loadBalancedRestTemplate.delete("http://STUDENTSERVICE/api/student/template/KWwhRh136YUCdQQt6E0DxODcWj7k0b");
                    info(job.getId(), "Made REST Call for DELETE_UNPAID_BOOKINGS.");
                }

                default -> throw new IllegalStateException("Unexpected value: " + job.getJobType());
            }
        });
    }




    /**
     * Processes and executes mailer jobs based on the provided job ID.
     *
     * This method listens for messages on the RabbitMQ queue associated with mailer jobs.
     * It processes the job by converting JSON data into a list of email templates and then sends
     * emails using those templates. It is transactional with serializable isolation to ensure
     * consistent execution of mailer jobs.
     *
     * @param jobId The ID of the job to be executed.
     */
    @RabbitListener(queues = "#{mailerQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeMailerJobs(Long jobId) {
        // Execute the job based on its ID.
        executor(jobId, (job) -> {
            // Convert the JSON data of the job into a list of EmailTemplateDTO objects.
            List<EmailTemplateDTO> templates = Optional.ofNullable(job.getJsonData())
                    .map(jsonString -> {
                        try {
                            return jsonConverter.convertToList(jsonString, EmailTemplateDTO.class);
                        } catch (IOException exception) {
                            // Handle JSON conversion errors.
                            return null;
                        }
                    })
                    .orElseThrow();

            // Send emails based on the extracted templates.
            sendMails(templates, job.getId());
        });
    }



    /**
     * RabbitMQ Listener method that listens to messages from the websiteDataTransferQueue.
     * This method processes certificates, compares existing and new certificate data,
     * and updates or saves the certificates as required.
     * The process is transactional, ensuring data consistency using SERIALIZABLE isolation level.
     *
     * @param jobId The job ID passed through RabbitMQ, representing the task to be executed.
     */
    @RabbitListener(queues = "#{websiteDataTransferQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void websiteDataTransfer(Long jobId) {

        // Executor to handle the task for the given jobId
        executor(jobId, (job) -> {

            // Fetch all existing CertificateWModel entities from the database.
            List<CertificateWModel> existingCertificateWModel = certificateWRepository.findAll();

            // Map of certificate UIDs to their current status for quick lookup.
            Map<String, CertificateStatus> certificateWStatusMap = existingCertificateWModel.stream()
                    .collect(Collectors.toMap(
                            CertificateWModel::getCertificateUid,
                            CertificateWModel::getCertificateStatus,
                            (existing, replacement) -> existing     // In case of duplicates, keep the existing entry.
                    ));

            // Map of certificate UIDs to CertificateWModel objects for quick lookup and updates.
            Map<String, CertificateWModel> certificateModelMap = existingCertificateWModel.stream()
                    .collect(Collectors.toMap(
                            CertificateWModel::getCertificateUid,
                            certificate -> certificate,
                            (existing, replacement) -> existing
                    ));

            // List to store certificates that need to be updated.
            List<CertificateWModel> certificateWToBeUpdated = new ArrayList<>();

            // Fetch all existing CertificateModel entities (representing new certificate data).
            List<CertificateModel> existingCertificate = certificateRepository.findAll();

            // Filter, map, and collect certificates to be saved to CertificateWModel format.
            List<CertificateWModel> certificateWToBeSaved = existingCertificate.stream()
                    .filter(certificate -> {
                        // Get the current certificate status from the existing CertificateWModel data.
                        CertificateStatus certificateStatus = certificateWStatusMap.get(certificate.getCertificateUid());
                        // If the certificate status has not changed, it doesn't need to be saved or updated.
                        if (null != certificateStatus && certificateStatus.equals(certificate.getCertificateStatus())) {
                            return false;
                        } else if (null != certificateStatus && !certificateStatus.equals(certificate.getCertificateStatus())) {
                            // If the status has changed, add it to the update list.
                            CertificateWModel certificateToUpdate = certificateModelMap.get(certificate.getCertificateUid());
                            if (null != certificateToUpdate) {
                                certificateToUpdate.setCertificateStatus(certificate.getCertificateStatus());
                                certificateWToBeUpdated.add(certificateToUpdate);
                            }
                            return false;
                        } else {
                            // If the certificate is new, mark it for saving.
                            return true;
                        }
                    })
                    .map(cert -> {
                        // Retrieve course group information for the certificate.
                        CourseGroupModel courseGroupModel = cert.getCourseGroup();

                        // Determine if the certificate is a faculty certificate.
                        boolean isFacultyCertificate = AssessmentType.SKILL_CLEARANCE.equals(cert.getAssessmentType());

                        // If it's not a faculty certificate, retrieve additional status mapping info.
                        BookingCertificateStatusMapping statusMapping = isFacultyCertificate ? null : certificateStatusMappingRepository
                                .findReadyAt(cert.getCertificateUid());

                        // Set the course duration based on the status mapping (or -1 for faculty certificates).
                        int duration = isFacultyCertificate ? -1 : statusMapping.getCourseDuration();

                        // Convert the certificate model to CertificateWModel for saving.
                        return CertificateWModel.builder()
                                .certificateUid(cert.getCertificateUid())
                                .assessmentType(cert.getAssessmentType())
                                .certificateStatus(cert.getCertificateStatus())
                                .courseDuration(duration)
                                .issuedToName(
                                        textHelper.buildFullName(
                                                cert.getIssuedToFirstName(),
                                                cert.getIssuedToMiddleName(),
                                                cert.getIssuedToLastName()
                                        )
                                )
                                .courseGroup(courseGroupModel.getCourseGroupName())
                                .year(DateTimeFormatter.ofPattern("yyyy").withZone(ZoneId.of("UTC")).format(cert.getCreatedAt()))
                                .grade(isFacultyCertificate ? null : statusMapping.getGrade())
                                .start(isFacultyCertificate ? null : statusMapping.getCertificateStartAt())
                                .end(isFacultyCertificate ? null : statusMapping.getCertificateReadyAt())
                                .build();
                    }).collect(Collectors.toCollection(ArrayList::new));

            // If there are certificates to be updated, save them to the database.
            if (!certificateWToBeUpdated.isEmpty()) {
                List<CertificateWModel> updatedCertificateW = certificateWRepository.saveAll(certificateWToBeUpdated);

                // Log the UIDs of the updated certificates.
                String updatedCertificateID = updatedCertificateW.stream()
                        .map(CertificateWModel::getCertificateUid)
                        .collect(Collectors.joining(", "));

                log.info("Certificate updated sucessfully ("+updatedCertificateID+").");
            }

            // If there are certificates to be saved, save them to the database.
            if (!certificateWToBeSaved.isEmpty()) {
                List<CertificateWModel> savedCertificateW = certificateWRepository.saveAll(certificateWToBeSaved);

                // Log the UIDs of the newly saved certificates.
                String savedCertificateID = savedCertificateW.stream()
                        .map(CertificateWModel::getCertificateUid)
                        .collect(Collectors.joining(", "));

                log.info("Certificate saved sucessfully ("+savedCertificateID+").");
            }

        });

    }




    /**
     * Processes and executes release criteria marker jobs based on the provided job ID.
     *
     * This method listens for messages on the RabbitMQ queue associated with release criteria marker jobs.
     * It processes the job based on its type and performs various operations such as updating clearance statuses,
     * computing grades, and handling certification processes. It is transactional with serializable isolation
     * to ensure consistent execution and integrity of release criteria marker jobs.
     *
     * @param jobId The ID of the job to be executed.
     */
    @RabbitListener(queues = "#{releaseCriteriaMarkerQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeReleaseCriteriaMarkerJobs(Long jobId) {
        // Execute the job based on its ID.
        executor(jobId, (job) -> {
            JobType jobType = job.getJobType();
            switch (jobType) {
                // Handling MARK_ELIGIBILITY_ACADEMICS_THEORY and MARK_ELIGIBILITY_ACADEMICS_PROJECT jobs
                case MARK_ELIGIBILITY_ACADEMICS_THEORY,
                     MARK_ELIGIBILITY_ACADEMICS_PROJECT ->
                {
                    // ACADEMICS
                    boolean isAcademicsTheory = JobType.MARK_ELIGIBILITY_ACADEMICS_THEORY.equals(jobType);
                    boolean isAcademicsProject = JobType.MARK_ELIGIBILITY_ACADEMICS_PROJECT.equals(jobType);

                    // Fetch and filter course certificate status mappings based on the job type
                    ArrayList<BookingCourseCertificateStatusMapping> certificateCourseStatusMappings = studentFinalExamRepository
                            .findAllById(jsonConverter.convertToList(job.getJsonData(), Long.class))
                            .stream()
                            .filter(mapping -> FinalExamStatus.CLEARED.equals(mapping.getExamStatus()))
                            .flatMap(mapping -> isAcademicsTheory ? mapping.getTheoryCourseCertificateMappings().stream() : mapping.getProjectCourseCertificateMappings().stream())
                            .collect(Collectors.toCollection(ArrayList::new));

                    // Log the number of mappings found
                    // TODO LOG
                    log.info("mapping found {}", certificateCourseStatusMappings.size());

                    // Update clearance status based on the job type
                    certificateCourseStatusMappings.forEach(courseCertificateMapping -> {
                        if (isAcademicsTheory) {
                            courseCertificateMapping.setTheoryClearanceStatus(ClearanceStatus.CLEARED);
                        } else {
                            courseCertificateMapping.setProjectClearanceStatus(ClearanceStatus.CLEARED);
                        }
                    });

                    List<BookingCertificateStatusMapping> certificateStatusMappings = certificateCourseStatusMappings
                            .stream()
                            .map(BookingCourseCertificateStatusMapping::getBookingCertificateStatusMapping)
                            .distinct()
                            .collect(Collectors.toCollection(ArrayList::new));

                    if (!certificateStatusMappings.isEmpty()) {
                        certificateStatusMappings.forEach(mapping -> {
                            if (Arrays.asList(CertificateReleaseStatus.PENDING, CertificateReleaseStatus.TO_REVIEW).contains(mapping.getCertificateReleaseStatus())) {
                                List<BookingCourseCertificateStatusMapping> courseCertificateMappings = mapping.getCourseCertificateStatusMappings();
                                boolean isTheoryCleared = courseCertificateMappings.stream().allMatch(courseCertificateMapping -> ClearanceStatus.CLEARED.equals(courseCertificateMapping.getTheoryClearanceStatus()));
                                boolean isProjectCleared = courseCertificateMappings.stream().allMatch(courseCertificateMapping -> ClearanceStatus.CLEARED.equals(courseCertificateMapping.getProjectClearanceStatus()));
                                mapping.setTheoryClearanceStatus(isTheoryCleared ? ClearanceStatus.CLEARED : ClearanceStatus.PENDING);
                                mapping.setProjectClearanceStatus(isProjectCleared ? ClearanceStatus.CLEARED : ClearanceStatus.PENDING);

                                // Log the inner mappings and clearance statuses
                                // TODO LOG
                                log.info("inner mapping found {}, PROJECT {}, THEORY {}", courseCertificateMappings.size(), isProjectCleared, isTheoryCleared);

                                if (isTheoryCleared && isProjectCleared) {
                                    mapping.setAcademicsClearedAt(Instant.now());
                                }

                                if (isProjectCleared) {
                                    computeCertificationGrade(
                                            courseCertificateMappings::stream,
                                            mapping::setGrade,
                                            mapping::setTotalMarks,
                                            mapping::setObtainedMarks,
                                            mapping::getTotalMarks,
                                            mapping::getObtainedMarks
                                    );
                                }

                                List<ClearanceStatus> clearanceStatuses = Arrays.asList(
                                        mapping.getTheoryClearanceStatus(),
                                        mapping.getProjectClearanceStatus(),
                                        mapping.getAttendanceClearanceStatus(),
                                        mapping.getFinanceClearanceStatus()
                                );

                                if (clearanceStatuses.stream().allMatch(ClearanceStatus.CLEARED::equals)) {
                                    processIfAllChecked(mapping::getCertificateReleaseStatus, mapping::setCertificateReleaseStatus, mapping::setCertificateReadyAt, mapping::setToBeReleasedAt, mapping::getCertificateStartAt, mapping::setCertificateStartAt, mapping.getBookingCourseGroupMapping());
                                } else if (clearanceStatuses.stream().anyMatch(ClearanceStatus.CLEARED::equals)) {
                                    mapping.setCertificateReleaseStatus(CertificateReleaseStatus.TO_REVIEW);
                                }
                            }
                        });

                        certificateStatusMappingRepository.saveAll(certificateStatusMappings);
                    }

                }

                // Handling MARK_ELIGIBILITY_ATTENDANCE jobs
                // ATTENDANCE
                case MARK_ELIGIBILITY_ATTENDANCE ->
                {
                    List<SPGetAttendanceAllStudentsForBatch> spResponse = jpaProxy.getSPGetAttendanceAllStudentsForBatch(
                            jsonConverter.convert(job.getJsonData(), String.class)
                    );
                    ArrayList<BookingCourseCertificateStatusMapping> courseCertificateStatusMappings = spResponse
                            .stream()
                            .filter(obj -> null != obj.getBookingCourseGroupMappingId() && null != obj.getCourseGroupId())
                            .filter(obj -> {
                                Float overallPercentage = computePercentage(obj.getAttendedModules(), obj.getTotalModules());
                                return overallPercentage.compareTo(ATTENDANCE_CRITERIA) >= 0;
                            })
                            .map(obj -> courseCertificateStatusMappingRepository.findByBookingCourseGroupMappingIdAndCourseId(obj.getBookingCourseGroupMappingId(), obj.getCourseId()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(ArrayList::new));

                    if (!courseCertificateStatusMappings.isEmpty()) {
                        courseCertificateStatusMappings.forEach(mapping -> {
                            mapping.setAttendanceClearanceStatus(ClearanceStatus.CLEARED);
                        });

                        List<BookingCertificateStatusMapping> certificateStatusMappings = courseCertificateStatusMappings
                                .stream()
                                .map(BookingCourseCertificateStatusMapping::getBookingCertificateStatusMapping)
                                .distinct()
                                .collect(Collectors.toCollection(ArrayList::new));

                        if (!certificateStatusMappings.isEmpty()) {
                            certificateStatusMappings.forEach(mapping -> {
                                if (Arrays.asList(CertificateReleaseStatus.PENDING, CertificateReleaseStatus.TO_REVIEW).contains(mapping.getCertificateReleaseStatus())) {
                                    List<BookingCourseCertificateStatusMapping> courseCertificateMappings = mapping.getCourseCertificateStatusMappings();
                                    boolean isAttendanceCleared = courseCertificateMappings.stream().allMatch(courseCertificateMapping -> ClearanceStatus.CLEARED.equals(courseCertificateMapping.getAttendanceClearanceStatus()));
                                    mapping.setAttendanceClearanceStatus(isAttendanceCleared ? ClearanceStatus.CLEARED : ClearanceStatus.PENDING);

                                    List<ClearanceStatus> clearanceStatuses = Arrays.asList(
                                            mapping.getTheoryClearanceStatus(),
                                            mapping.getProjectClearanceStatus(),
                                            mapping.getAttendanceClearanceStatus(),
                                            mapping.getFinanceClearanceStatus()
                                    );

                                    if (clearanceStatuses.stream().allMatch(ClearanceStatus.CLEARED::equals)) {
                                        processIfAllChecked(mapping::getCertificateReleaseStatus, mapping::setCertificateReleaseStatus, mapping::setCertificateReadyAt, mapping::setToBeReleasedAt, mapping::getCertificateStartAt, mapping::setCertificateStartAt, mapping.getBookingCourseGroupMapping());
                                    } else if (ClearanceStatus.CLEARED.equals(mapping.getTheoryClearanceStatus())) {
                                        mapping.setCertificateReleaseStatus(CertificateReleaseStatus.TO_REVIEW);
                                    }

//                                    else if (clearanceStatuses.stream().anyMatch(ClearanceStatus.CLEARED::equals)) { TODO
                                }
                            });

                            certificateStatusMappingRepository.saveAll(certificateStatusMappings);
                        }
                    }
                }

                // Handling MARK_ELIGIBILITY_ADD_START_DATE jobs
                // ADD_START_DATE
                case MARK_ELIGIBILITY_ADD_START_DATE -> {
                    AddStartDateForCertificationStatusDTO data = jsonConverter.convert(job.getJsonData(), AddStartDateForCertificationStatusDTO.class);
                    List<BookingCertificateStatusMapping> certificateStatusMappings = certificateStatusMappingRepository.findAllMappingsWithoutStartDate(data.getStudents(), data.getCourse());
                    certificateStatusMappings.forEach(mapping -> {
                        if (null == mapping.getCertificateStartAt()) {
                            mapping.setCertificateStartAt(Instant.now());
                        }
                    });
                    certificateStatusMappingRepository.saveAll(certificateStatusMappings);
                }

                // Handling MARK_ELIGIBILITY_FINANCE jobs
                // FINANCE
                case MARK_ELIGIBILITY_FINANCE -> {
                    List<BookingCertificateStatusMapping> mappings = certificateStatusMappingRepository.findByBookingCourseGroupMappingBookingCourseGroupIdIn(jsonConverter.convertToList(job.getJsonData(), Long.class));
                    for (BookingCertificateStatusMapping mapping : mappings) {
                        if (null != mapping) {
                            List<ClearanceStatus> clearanceStatuses = Arrays.asList(
                                    mapping.getTheoryClearanceStatus(),
                                    mapping.getProjectClearanceStatus(),
                                    mapping.getAttendanceClearanceStatus(),
                                    mapping.getFinanceClearanceStatus()
                            );

                            if (clearanceStatuses.stream().allMatch(ClearanceStatus.CLEARED::equals)) {
                                processIfAllChecked(mapping::getCertificateReleaseStatus, mapping::setCertificateReleaseStatus, mapping::setCertificateReadyAt, mapping::setToBeReleasedAt, mapping::getCertificateStartAt, mapping::setCertificateStartAt, mapping.getBookingCourseGroupMapping());
                            }
                        }
                    } // FOR
                }
            }


        });
    }



    /**
     * RabbitMQ listener method that processes the student status update job for students who meet specific conditions.
     * This method identifies students whose bookings are either ON_GOING or EXPIRED, with PENDING clearance status
     * and who have certain student statuses (PASSED_OUT, FINANCIAL_DROPOUT) and updates their status to FINANCIAL_DROPOUT.
     * The process is transactional to ensure data integrity and consistency.
     *
     * @param jobId The job ID passed through RabbitMQ, representing the task to be executed.
     */
    @RabbitListener(queues = "#{updateStudentStatusQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeUpdateStudentStatusJob(Long jobId){

        // Execute the task within the context of the given jobId
        executor(jobId, (job) -> {

            // Fetch the organization model with ID 1 (assumed to be the primary organization)
            Optional<OrganizationModel> organization = organizationRepository.findById(1L);

            // Retrieve all bookings that match the conditions:
            // BookingStatus: ON_GOING or EXPIRED, ClearanceStatus: PENDING,
            // StudentStatus: PASSED_OUT or FINANCIAL_DROPOUT,
            // and date conditions related to the financial validity of the organization.
            List<BookingModel> bookings = bookingRepository.findBookingsByConditions(Arrays.asList(BookingStatus.ON_GOING, BookingStatus.EXPIRED), ClearanceStatus.PENDING, Arrays.asList(StudentStatus.PASSED_OUT, StudentStatus.FINANCIAL_DROPOUT), getCurrentUtcDate(), organization.get().getFinancialValidity());

            // Build a map of students (studentUid -> StudentModel) from the fetched bookings.
            // This ensures no duplicate students are processed.
            HashMap<String, StudentModel> studentMap = bookings.stream()
                    .collect(Collectors.toMap(
                            booking -> booking.getStudent().getStudentUid(),
                            BookingModel::getStudent,
                            (existing, replacement) -> existing,
                            HashMap::new
                    ));

            // If the student map is not empty, proceed to update the student statuses.
            if(!studentMap.isEmpty()) {
                // Extract the student UIDs and convert the map values to a list of StudentModel objects.
                Set<String> studentIdSet = studentMap.keySet();
                List<StudentModel> studentList = new ArrayList<>(studentMap.values());

                // Update each student's status to FINANCIAL_DROPOUT.
                for (StudentModel student : studentList) {
                    student.setStudentStatus(StudentStatus.FINANCIAL_DROPOUT);
                }

                // Save all updated student records to the database.
                studentRepository.saveAll(studentList);

                // Log the UIDs of the students whose statuses were updated.
                String studentIds = studentIdSet.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));

                log.info("Status of students (" + studentIds + ") updated as FINANCIAL DROPOUT at " + Instant.now());
            }

        });
    }



    /**
     * RabbitMQ listener method that processes the job for sending booking expiry reminder emails.
     * This method identifies bookings that are about to expire, sends email reminders to students,
     * and triggers notifications for the users who booked the students.
     * The process is transactional to ensure data integrity and consistency.
     *
     * @param jobId The job ID passed through RabbitMQ, representing the task to be executed.
     */
    @RabbitListener(queues = "#{expiryReminderMailQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeBookingExpiryReminderMailJob(Long jobId){

        // Execute the task within the context of the given jobId
        executor(jobId, (job) -> {

            // Fetch all bookings that are about to expire, based on the following conditions:
            // BookingStatus: ON_GOING, StudentStatus: PASSED_OUT, estimated expiry within 9 days.
            List<BookingModel> aboutToExpireBookings = bookingRepository.findBookingsAboutToExpire(BookingStatus.ON_GOING, StudentStatus.PASSED_OUT, getCurrentUtcDate(),9);

            // Map to store user IDs and their associated booking UIDs
            Map<Long, List<String>> notificationMap = new HashMap<>();
            // Map to store user details, ensuring no duplicate UserModel objects are stored
            Map<Long, UserModel> notificationUserMap = new HashMap<>();
            // Map to store booking details by booking UID
            Map<String, BookingModel> bookingMap = new HashMap<>();

            // Stream through the bookings about to expire, creating email templates for each booking
            List<EmailTemplateDTO> emailTemplateDTOS = aboutToExpireBookings.stream()
                    .map(booking -> {
                        // Retrieve the student and organization details for the booking
                        StudentModel student = booking.getStudent();
                        OrganizationModel organization = student.getOrganization();
                        // Convert the organization's time zone to ZoneId
                        ZoneId zone = convertStringToZoneId(organization.getZone());

                        // Retrieve details of the user who made the booking
                        UserModel bookedBy = booking.getBookedBy();
                        Long bookedByUserID = bookedBy.getUserPid();

                        // Format the estimated expiry date of the booking
                        Instant bookingExpiryDate = booking.getEstimatedExpirationDate();
                        String formattedExpiryDate = formatInstantDate(bookingExpiryDate, zone);

                        // Retrieve student details from the enquiry
                        EnquiryModel enquiry = student.getEnquiry();
                        String studentFullName = textHelper.buildFullName(enquiry.getStudentFirstname(), enquiry.getStudentMiddlename(), enquiry.getStudentLastname());
                        String bookingId = booking.getBookingUid();
                        String studentEmail = enquiry.getPrimaryEmail();

                        // Add the booking UID to the notification map for the user who made the booking
                        notificationMap.computeIfAbsent(bookedByUserID, k -> new ArrayList<>()).add(bookingId);

                        // Add the UserModel to the notification map, only if it is not already present
                        notificationUserMap.putIfAbsent(bookedByUserID, bookedBy);

                        // Store the booking details by booking UID
                        bookingMap.putIfAbsent(bookingId, booking);

                        // Build and return the email template for the expiring booking
                        return eMailTemplates.getCourseBookingExpiringEmail(studentFullName, bookingId, formattedExpiryDate, studentEmail);

                    }).collect(Collectors.toCollection(ArrayList::new));

            // If there are any email templates to send
            if(!emailTemplateDTOS.isEmpty()){

                // If there are any user notifications, send them
                if(!notificationUserMap.isEmpty())
                    sendUserNotificationForAboutToExpireBookings(notificationMap, notificationUserMap, bookingMap);

                // Send the expiry reminder emails
                sendMails(emailTemplateDTOS, jobId);
            }
        });

    }




    /**
     * RabbitMQ listener method that processes the job for updating booking statuses to 'EXPIRED'.
     * The method identifies bookings that are still ongoing but have reached their expiration date.
     * It also sends notification emails to students and users (booked by) regarding the expired bookings.
     * The process is transactional to ensure data consistency.
     *
     * @param jobId The job ID passed through RabbitMQ, representing the task to be executed.
     */
    @RabbitListener(queues = "#{updateExpiryStatusQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeUpdateBookingStatusToExpireJob(Long jobId){

        // Execute the task within the context of the given jobId
        executor(jobId, (job) -> {

            // Retrieve bookings that are still ongoing and have now expired
            List<BookingModel> bookingModels = bookingRepository.findDistinctBookingsByBookingStatus(Instant.now(), BookingStatus.ON_GOING);

            // Lists to store entities to be updated or used for notifications/emails
            List<BookingCourseGroupMapping> bookingCourseGroupMappingsToBeExpired = new ArrayList<>();
            List<EmailTemplateDTO> emailTemplateDTOS = new ArrayList<>();

            // Maps to manage notifications and emails for users and bookings
            Map<Long, List<String>> notificationMap = new HashMap<>();
            Map<Long, UserModel> notificationUserMap = new HashMap<>();
            Map<String, BookingModel> bookingMap = new HashMap<>();

            // Iterate through each booking and check if it needs to be marked as expired
            for(BookingModel bookingModel : bookingModels){

                // Retrieve student and organization information
                StudentModel student = bookingModel.getStudent();
                OrganizationModel organization = student.getOrganization();
                ZoneId zone = convertStringToZoneId(organization.getZone());

                // Retrieve the user who made the booking
                UserModel bookedBy = bookingModel.getBookedBy();
                Long bookedByUserID = bookedBy.getUserPid();

                // Build the student's full name and get booking ID
                EnquiryModel enquiry = student.getEnquiry();
                String studentFullName = textHelper.buildFullName(enquiry.getStudentFirstname(), enquiry.getStudentMiddlename(), enquiry.getStudentLastname());
                String bookingId = bookingModel.getBookingUid();

                // Format the estimated expiration date for the booking
                Instant bookingExpiryDate = bookingModel.getEstimatedExpirationDate();
                String formattedExpiryDate = formatInstantDate(bookingExpiryDate, zone);
                String studentEmail = enquiry.getPrimaryEmail();

                List<BookingCourseGroupMapping> bookingCourseGroupMappings = bookingModel.getCourseGroupMappings();

                // Get active course group mappings that are not yet marked as 'RBC' (removed by course)
                List<BookingCourseGroupMapping> activeCourseGroupMappings = bookingCourseGroupMappings.stream()
                        .filter(bookingCourseGroupMapping -> !BookingStatus.RBC.equals(bookingCourseGroupMapping.getStatus()))
                        .collect(Collectors.toCollection(ArrayList::new));

                // Get certificate status mappings from active course group mappings
                List<BookingCertificateStatusMapping> activeCertificateStatusMapping = activeCourseGroupMappings.stream()
                        .map(BookingCourseGroupMapping::getCertificateStatusMapping)
                        .collect(Collectors.toCollection(ArrayList::new));

                // Check if any active certificates are still pending or to be reviewed
                HashSet<CertificateReleaseStatus> eligibleStatus = new HashSet<>(Set.of(CertificateReleaseStatus.PENDING, CertificateReleaseStatus.TO_REVIEW));
                if(activeCertificateStatusMapping.stream().anyMatch(mapping -> eligibleStatus.contains(mapping.getCertificateReleaseStatus()))) {

                    // Retrieve course mappings from active course groups
                    List<BookingCourseMapping> bookingCourseMappings = activeCourseGroupMappings.stream()
                            .flatMap(mapping -> mapping.getCourses().stream())
                            .collect(Collectors.toCollection(ArrayList::new));

                    // Ensure none of the courses in the group are paused
                    if (bookingCourseMappings.stream().noneMatch(mapping -> BookingCourseStatus.PAUSED.equals(mapping.getBookingCourseStatus()))){

                        // Mark all active course group mappings as 'EXPIRED'
                        for (BookingCourseGroupMapping courseGroupMapping : activeCourseGroupMappings) {
                            courseGroupMapping.setStatus(BookingStatus.EXPIRED);
                        }
                        bookingCourseGroupMappingsToBeExpired.addAll(activeCourseGroupMappings);

                        // Prepare an email template for the expired booking
                        emailTemplateDTOS.add(eMailTemplates.getCourseExpiredEmail(
                                studentFullName,
                                bookingId,
                                formattedExpiryDate,
                                studentEmail
                        ));

                        // Add the booking ID to the notification map for the user
                        notificationMap.computeIfAbsent(bookedByUserID, k -> new ArrayList<>()).add(bookingId);

                        // Add the user information if not already present in the notification map
                        notificationUserMap.putIfAbsent(bookedByUserID, bookedBy);

                        // Store the booking details in the booking map
                        bookingMap.putIfAbsent(bookingId, bookingModel);

                        // Log the update of booking status
                        log.info("Booking status of "+bookingId+" updated to expired at "+Instant.now());
                    }
                }
            }

            // If any course group mappings were marked for expiration, save them
            if(!bookingCourseGroupMappingsToBeExpired.isEmpty()){
                bookingCourseGroupMappingRepository.saveAll(bookingCourseGroupMappingsToBeExpired);

                // If there are users to notify, send notifications
                if(!notificationUserMap.isEmpty())
                    sendUserNotificationForExpiredBookings(notificationMap, notificationUserMap, bookingMap);

                // If there are emails to be sent, send the emails
                if(!emailTemplateDTOS.isEmpty()){
                    sendMails(emailTemplateDTOS, jobId);
                }
            }

        });
    }



    /**
     * RabbitMQ listener method that processes the job for notifying users of bookings that are about to start.
     * The method retrieves bookings that are starting within a specified period and sends notifications to the users
     * who made the bookings. The process is transactional to ensure data consistency.
     *
     * @param jobId The job ID passed through RabbitMQ, representing the task to be executed.
     */
    @RabbitListener(queues = "#{notifyBookingStartQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeNotifyBookingStartJob(Long jobId){

        // Execute the task within the context of the given jobId
        executor(jobId, (job) -> {

            // Retrieve bookings that have a start date within 4 days of the current UTC date and are ongoing
            List<BookingModel> bookings = bookingRepository.findBookingsByStartDate(BookingStatus.ON_GOING, getCurrentUtcDate(), 4);

            // Collect the bookings into a map with booking ID as the key and BookingModel as the value
            Map<Long, BookingModel> bookingsMap = bookings.stream()
                    .collect(Collectors.toMap(
                            BookingModel::getBookingId,
                            booking -> booking,
                            (existing, replacement) -> existing
                    ));

            // Check if there are any bookings to process
            if(!bookingsMap.isEmpty()){

                // Maps to manage notifications and emails for users and bookings
                Map<Long, List<String>> notificationMap = new HashMap<>();
                Map<Long, UserModel> notificationUserMap = new HashMap<>();
                Map<String, BookingModel> bookingMap = new HashMap<>();

                // Iterate through each booking and gather notification details
                for (BookingModel booking : bookingsMap.values()){

                    // Retrieve the user who made the booking
                    UserModel bookedBy = booking.getBookedBy();
                    Long bookedByUserID = bookedBy.getUserPid();
                    String bookingId = booking.getBookingUid();

                    // Add the booking ID to the notification map for the user
                    notificationMap.computeIfAbsent(bookedByUserID, k -> new ArrayList<>()).add(bookingId);

                    // Add the user information if not already present in the notification map
                    notificationUserMap.putIfAbsent(bookedByUserID, bookedBy);

                    // Store the booking details in the booking map
                    bookingMap.putIfAbsent(bookingId, booking);
                }

                // If there are users to notify, send notifications for the bookings about to start
                if(!notificationUserMap.isEmpty())
                    sendUserNotificationForBookingStart(notificationMap, notificationUserMap, bookingMap);
            }
        });

    }


    /**
     * Sends notifications to users regarding the start of their bookings.
     * This method processes the notification data, creates and saves notifications, and
     * associates each notification with the corresponding user.
     *
     * @param notificationMap A map where the key is the user ID, and the value is a list of booking IDs that the user needs to be notified about.
     * @param notificationUserMap A map where the key is the user ID, and the value is the corresponding UserModel of the user to be notified.
     * @param bookingMap A map where the key is the booking ID, and the value is the corresponding BookingModel, used to get booking details.
     */
    public void sendUserNotificationForBookingStart(
            Map<Long, List<String>> notificationMap,
            Map<Long, UserModel> notificationUserMap,
            Map<String, BookingModel> bookingMap
    ){
        // A map that will hold the notifications to be sent for each user
        Map<Long, List<NotificationModel>> notificationsMap = new HashMap<>();

        // Loop through each user who needs notifications
        for(Map.Entry<Long, UserModel> userEntry : notificationUserMap.entrySet()){

            Long userId = userEntry.getKey();   // The ID of the user to notify
            List<String> bookingIds = notificationMap.get(userId);     // The list of booking IDs for this user

            // If there are bookings to notify the user about, create notification models
            if(null != bookingIds && !bookingIds.isEmpty()){

                List<NotificationModel> notificationModels = new ArrayList<>();     // List of notifications for this user

                // Loop through each booking ID and create a notification
                for(String bookingId : bookingIds){

                    BookingModel booking = bookingMap.get(bookingId);               // Retrieve the booking details
                    StudentModel student = booking.getStudent();                    // Get the student associated with the booking
                    OrganizationModel organization = student.getOrganization();     // Get the organization details
                    ZoneId zone = convertStringToZoneId(organization.getZone());    // Convert the organization's zone to ZoneId

                    Instant bookingStartDate = booking.getBookingStartDate();       // Retrieve the start date of the booking
                    String formattedStartDate = formatInstantDate(bookingStartDate, zone);  // Format the start date to the user's timezone

                    // Create a notification model for the booking
                    notificationModels.add(
                            NotificationModel.builder()
                                    .referenceId(bookingId)     // The booking ID as a reference
                                    .message("Reminder: Booking ("+bookingId+") estimated to commence on "+formattedStartDate)  // Notification message
                                    .view("BOOKINGS")     // The view or section of the application where the notification will be displayed
                                    .build()
                    );
                }
                // Map the user ID to the list of notifications for later processing
                notificationsMap.put(userId, notificationModels);
            }
        }

        // Loop through the notifications created for each user
        for(Map.Entry<Long, List<NotificationModel>> userNotificationEntry : notificationsMap.entrySet()){

            Long userId = userNotificationEntry.getKey();   // The ID of the user
            UserModel user = notificationUserMap.get(userId);   // Retrieve the UserModel for this user
            List<NotificationModel> notifications = userNotificationEntry.getValue();   // Retrieve the list of notifications for this user

            // Save the notifications to the database
            List<NotificationModel> savedNotifications = notificationRepository.saveAll(notifications);

            // Create a list to hold the mappings between the user and the saved notifications
            List<NotificationUserMapping> notificationUserMappings = new ArrayList<>();

            // Loop through the saved notifications and create mappings between the user and the notification
            for(NotificationModel notification : savedNotifications){
                notificationUserMappings.add(
                        NotificationUserMapping.builder()
                                .user(user)     // Associate the user with the notification
                                .notification(notification)     // Associate the notification with the user
                                .build()
                );
            }

            // Save the user-notification mappings to the database
            notificationUserRepository.saveAll(notificationUserMappings);
        }

    }




    /**
     * Sends user notifications for expired bookings.
     *
     * This method generates notifications for each user whose bookings have expired. It processes a
     * notification map that links users to their respective expired booking IDs, formats the expiry
     * date based on the user's organization time zone, and then saves both the notifications and
     * user-notification mappings.
     *
     * @param notificationMap       A map containing user IDs as keys and a list of expired booking IDs as values.
     * @param notificationUserMap   A map containing user IDs as keys and their corresponding UserModel objects as values.
     * @param bookingMap            A map containing booking IDs as keys and the corresponding BookingModel objects as values.
     */
    public void sendUserNotificationForExpiredBookings(
            Map<Long, List<String>> notificationMap,
            Map<Long, UserModel> notificationUserMap,
            Map<String, BookingModel> bookingMap
    ){
        // Map to store notifications for each user (keyed by userId)
        Map<Long, List<NotificationModel>> notificationsMap = new HashMap<>();

        // Iterate over all users from the notificationUserMap
        for(Map.Entry<Long, UserModel> userEntry : notificationUserMap.entrySet()){

            Long userId = userEntry.getKey();       // Get user ID
            List<String> bookingIds = notificationMap.get(userId);   // Get expired booking IDs

            // Check if the user has any expired bookings
            if(null != bookingIds && !bookingIds.isEmpty()){
                // List to store generated notifications
                List<NotificationModel> notificationModels = new ArrayList<>();

                // Iterate over all booking IDs for the user
                for(String bookingId : bookingIds){

                    BookingModel booking = bookingMap.get(bookingId);   // Fetch the booking model
                    StudentModel student = booking.getStudent();        // Get the student associated with the booking
                    OrganizationModel organization = student.getOrganization();     // Get the student's organization
                    ZoneId zone = convertStringToZoneId(organization.getZone());    // Convert organization zone to ZoneId

                    // Get the booking's expiration date and format it based on the user's time zone
                    Instant bookingExpiryDate = booking.getEstimatedExpirationDate();
                    String formattedExpiryDate = formatInstantDate(bookingExpiryDate, zone);

                    // Create a notification model for the expired booking
                    notificationModels.add(
                            NotificationModel.builder()
                                    .referenceId(bookingId)
                                    .message("Booking ("+bookingId+") has expired on "+formattedExpiryDate)
                                    .view("BOOKINGS")
                                    .build()
                    );
                }
                // Store the list of notifications for the current user
                notificationsMap.put(userId, notificationModels);
            }
        }

        // Iterate over the map of users and their generated notifications
        for(Map.Entry<Long, List<NotificationModel>> userNotificationEntry : notificationsMap.entrySet()){

            Long userId = userNotificationEntry.getKey();   // Get user ID
            UserModel user = notificationUserMap.get(userId);   // Get the corresponding user model
            List<NotificationModel> notifications = userNotificationEntry.getValue();   // Get the list of notifications for the user

            // Save all notifications to the repository
            List<NotificationModel> savedNotifications = notificationRepository.saveAll(notifications);

            // Create a list of user-notification mappings to store which user received which notifications
            List<NotificationUserMapping> notificationUserMappings = new ArrayList<>();

            for(NotificationModel notification : savedNotifications){
                notificationUserMappings.add(
                        NotificationUserMapping.builder()
                                .user(user)     // Associate the user with the notification
                                .notification(notification)
                                .build()
                );
            }
            // Save all user-notification mappings to the repository
            notificationUserRepository.saveAll(notificationUserMappings);
        }
    }



    /**
     * Sends notifications to users regarding the expiration of their bookings.
     * This method processes the notification data, creates and saves notifications, and
     * associates each notification with the corresponding user.
     *
     * @param notificationMap A map where the key is the user ID, and the value is a list of booking IDs that the user needs to be notified about.
     * @param notificationUserMap A map where the key is the user ID, and the value is the corresponding UserModel of the user to be notified.
     * @param bookingMap A map where the key is the booking ID, and the value is the corresponding BookingModel, used to get booking details.
     */
    public void sendUserNotificationForAboutToExpireBookings(
            Map<Long, List<String>> notificationMap,
            Map<Long, UserModel> notificationUserMap,
            Map<String, BookingModel> bookingMap
    ){
        // A map that will hold the notifications to be sent for each user
        Map<Long, List<NotificationModel>> notificationsMap = new HashMap<>();

        // Loop through each user who needs notifications
        for(Map.Entry<Long, UserModel> userEntry : notificationUserMap.entrySet()){

            Long userId = userEntry.getKey();   // The ID of the user to notify
            List<String> bookingIds = notificationMap.get(userId);    // The list of booking IDs for this user

            // If there are bookings to notify the user about, create notification models
            if(null != bookingIds && !bookingIds.isEmpty()){

                List<NotificationModel> notificationModels = new ArrayList<>();     // List of notifications for this user

                // Loop through each booking ID and create a notification
                for(String bookingId : bookingIds){

                    BookingModel booking = bookingMap.get(bookingId);   // Retrieve the booking details
                    StudentModel student = booking.getStudent();    // Get the student associated with the booking
                    OrganizationModel organization = student.getOrganization();     // Get the organization details
                    ZoneId zone = convertStringToZoneId(organization.getZone());    // Convert the organization's zone to ZoneId

                    Instant bookingExpiryDate = booking.getEstimatedExpirationDate();   // Retrieve the expiry date of the booking
                    String formattedExpiryDate = formatInstantDate(bookingExpiryDate, zone);    // Format the expiry date to the user's timezone

                    // Create a notification model for the expired booking
                    notificationModels.add(
                            NotificationModel.builder()
                                    .referenceId(bookingId)      // The booking ID as a reference
                                    .message("Booking ("+bookingId+") is about to expire on "+formattedExpiryDate)  // Notification message
                                    .view("BOOKINGS")   // The view or section of the application where the notification will be displayed
                                    .build()
                    );
                }
                // Map the user ID to the list of notifications for later processing
                notificationsMap.put(userId, notificationModels);
            }
        }

        // Loop through the notifications created for each user
        for(Map.Entry<Long, List<NotificationModel>> userNotificationEntry : notificationsMap.entrySet()){

            Long userId = userNotificationEntry.getKey();   // The ID of the user
            UserModel user = notificationUserMap.get(userId);   // Retrieve the UserModel for this user
            List<NotificationModel> notifications = userNotificationEntry.getValue();   // Retrieve the list of notifications for this user

            // Save the notifications to the database
            List<NotificationModel> savedNotifications = notificationRepository.saveAll(notifications);
            // Create a list to hold the mappings between the user and the saved notifications
            List<NotificationUserMapping> notificationUserMappings = new ArrayList<>();

            // Loop through the saved notifications and create mappings between the user and the notification
            for(NotificationModel notification : savedNotifications){
                notificationUserMappings.add(
                        NotificationUserMapping.builder()
                                .user(user)     // Associate the user with the notification
                                .notification(notification)     // Associate the notification with the user
                                .build()
                );
            }
            // Save the user-notification mappings to the database
            notificationUserRepository.saveAll(notificationUserMappings);
        }
    }




    /**
     * Processes the release status and start date of a booking course group mapping if all clearance criteria are met.
     *
     * This method is transactional with mandatory propagation, meaning it must be executed within an existing transaction.
     * It updates the release status and timestamps based on the provided criteria and conditions.
     *
     * @param releaseStatusSupplier A supplier for the current release status.
     * @param releaseStatusConsumer A consumer to update the release status.
     * @param readyAtConsumer A consumer to set the ready timestamp.
     * @param toBeReleasedAt A consumer to set the to-be-released timestamp.
     * @param startAtGetter A supplier to get the start date of the booking.
     * @param startAtSetter A consumer to set the start date of the booking.
     * @param mapping The booking course group mapping to process.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.MANDATORY)
    private void processIfAllChecked(
            Supplier<CertificateReleaseStatus> releaseStatusSupplier,
            Consumer<CertificateReleaseStatus> releaseStatusConsumer,
            Consumer<Instant> readyAtConsumer,
            Consumer<Instant> toBeReleasedAt,
            Supplier<Instant> startAtGetter,
            Consumer<Instant> startAtSetter,
            BookingCourseGroupMapping mapping
    ) {
        // Check if the current release status is not RELEASED
        if (!CertificateReleaseStatus.RELEASED.equals(releaseStatusSupplier.get())) {
            // Update release status to READY and set the ready timestamp to the current time
            releaseStatusConsumer.accept(CertificateReleaseStatus.READY);
            readyAtConsumer.accept(Instant.now());
        }

        // Retrieve the booking model associated with the mapping
        BookingModel booking = mapping.getBooking();
        Long bookingId = booking.getBookingId();
        StudentModel student = booking.getStudent();

        // Check if the start date is not already set
        if (null == startAtGetter.get()) {
            // Collect the course IDs from the course group mappings
            Set<Long> courseIds = streamCourses(booking.getCourseGroupMappings()).map(CourseModel::getCourseId).collect(Collectors.toSet()); // CHANGE
            // Find the batch student mapping with the matching course and booking ID
            startAtSetter.accept(student.getBatches()
                    .stream()
                    .filter(batchStudentMapping -> courseIds.contains(batchStudentMapping.getBatch().getCourse().getCourseId()))
                    .filter(batchStudentMapping -> batchStudentMapping.getBookingModelList().stream().anyMatch(bookingModel -> bookingModel.getBookingId().equals(bookingId))) // CHANGE
                    .findFirst()
                    .map(BatchStudentMapping::getAddedAt)
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not determine the start-date of the student (" + student.getStudentUid() + ") for booking (" + booking.getBookingUid() + ")"
                    ))
            );
        }
    }



    /**
     * Calculates the certification grade based on the overall percentage.
     * The grade is assigned according to predefined percentage ranges.
     *
     * @param overallPercentage The overall percentage for which the grade is to be calculated.
     * @return The certification grade corresponding to the given percentage.
     */
    private static String calculateCertificationGrade(float overallPercentage) {
        /*
            Grade scale:
            91 - 100   O+
            81 - 90    A+
            71 - 80    A
            61 - 70    B+
            51 - 60    B
            41 and below  C
         */

        if (overallPercentage >= 91 && overallPercentage <= 100) {
            return "O+";     // Outstanding Plus
        } else if (overallPercentage >= 81 && overallPercentage <= 90) {
            return "A+";    // Excellent Plus
        } else if (overallPercentage >= 71 && overallPercentage <= 80) {
            return "A";     // Excellent
        } else if (overallPercentage >= 61 && overallPercentage <= 70) {
            return "B+";    // Good Plus
        } else if (overallPercentage >= 51 && overallPercentage <= 60) {
            return "B";     // Good
        } else {
            return "C";     // Pass
        }
    }



    /**
     * Computes the percentage based on obtained marks and total marks.
     *
     * @param obtained The marks obtained.
     * @param total The total possible marks.
     * @return The percentage of obtained marks.
     * @throws IllegalArgumentException If the total marks are zero to prevent division by zero.
     */
    public static float computePercentage(float obtained, float total) {
        if (total == 0) {
            throw new IllegalArgumentException("Value for total cannot be zero.");
        }

        return (obtained / total) * 100;
    }



    /**
     * Computes and updates the certification grade based on the provided mappings.
     *
     * This method calculates the total obtained marks and total possible marks from a stream of
     * `BookingCourseCertificateStatusMapping` objects. It then computes the percentage and determines
     * the corresponding grade. If the computed grade is higher than the existing one, it updates
     * the total marks, obtained marks, and the grade.
     *
     * @param mappings A supplier providing a stream of `BookingCourseCertificateStatusMapping` objects.
     * @param gradeSetter A consumer to set the computed grade.
     * @param totalSetter A consumer to set the total marks.
     * @param obtainedSetter A consumer to set the obtained marks.
     * @param totalGetter A supplier to get the existing total marks.
     * @param obtainedGetter A supplier to get the existing obtained marks.
     */
    private void computeCertificationGrade(
            Supplier<Stream<BookingCourseCertificateStatusMapping>> mappings,
            Consumer<String> gradeSetter,
            Consumer<Float> totalSetter,
            Consumer<Float> obtainedSetter,
            Supplier<Float> totalGetter,
            Supplier<Float> obtainedGetter
    ) {
        // Collect all obtained and total marks from the mappings
        ArrayList<ImmutablePair<Float, Float>> marks = mappings.get()
                .map(BookingCourseCertificateStatusMapping::getProjectFinalExam)
                .filter(obj -> FinalExamStatus.CLEARED.equals(obj.getExamStatus()))
                .map(obj -> ImmutablePair.of(obj.getLatestObtainedMarks(), obj.getLatestExamTotalMarks()))
                .collect(Collectors.toCollection(ArrayList::new));

        // If marks are present, compute the total obtained and total possible marks
        if (!marks.isEmpty()) {
            ImmutablePair<Float, Float> totals = marks.stream()
                    .reduce(new ImmutablePair<>(0.0f, 0.0f), (first, second) -> new ImmutablePair<>(first.getLeft() + second.getLeft(), first.getRight() + second.getRight()));
            float totalObtainedMarks = totals.getLeft();
            float totalTotalMarks = totals.getRight();

            if (totalTotalMarks > 0) {
                // Determine if this is the first computation or if we need to compare with existing values
                boolean isComputingFirstTime = null == totalGetter.get() || null == obtainedGetter.get();
                float existingOverallPercentage = isComputingFirstTime ? 0f : computePercentage(obtainedGetter.get(), totalGetter.get());
                float newlyOverallPercentage = computePercentage(totalObtainedMarks, totalTotalMarks);

                // Update the grade if the newly computed percentage is higher
                if (Float.compare(existingOverallPercentage, newlyOverallPercentage) < 0) {
                    String grade = calculateCertificationGrade(newlyOverallPercentage);
                    obtainedSetter.accept(totalObtainedMarks);
                    totalSetter.accept(totalTotalMarks);
                    gradeSetter.accept(grade);
                }
            }
        }
    }



    /**
     * Processes certificate releasing jobs. This method is triggered by messages from the RabbitMQ queue
     * specified by `certificateReleaserQueue`. It performs various tasks related to certificate issuance
     * including fetching data, processing certificate statuses, updating repositories, and scheduling email jobs.
     *
     * @param jobId The ID of the job to process.
     */
    @RabbitListener(queues = "#{certificateReleaserQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeCertificateReleaserJobs(Long jobId) {
        executor(jobId, (job) -> {
            // Initialize lists and maps to track various entities and email templates
            ArrayList<BookingModel> bookings = new ArrayList<>();
            ArrayList<Long> students = new ArrayList<>();
            ArrayList<BookingCourseGroupMapping> courseGroupMappings = new ArrayList<>();
            ArrayList<BookingCertificateStatusMapping> certificateMappings = new ArrayList<>();
            HashMap<BookingCertificateStatusMapping, CertificateModel> certificates = new HashMap<>();
            ArrayList<EmailTemplateDTO> onHoldTemplates = new ArrayList<>();
            ArrayList<EmailTemplateDTO> confirmationTemplates = new ArrayList<>();
            ArrayList<EmailTemplateDTO> releasedTemplates = new ArrayList<>();

            // Fetch all releasable booking-course-group mappings that are ongoing
            List<ImmutableTriple<String, String, String>> mappings = bookingCourseGroupMappingRepository
                    .findAllByReleasableTrue(BookingStatus.ON_GOING)
                    .stream()
                    .collect(Collectors.groupingBy(
                            BookingCourseGroupMapping::getBooking,
                            Collectors.mapping(BookingCourseGroupMapping::getCertificateStatusMapping, Collectors.toList())
                    ))
                    .entrySet()
                    .stream()
                    // Filter out empty or blank mappings
                    .filter(entry -> textHelper.isNonBlank(entry.getValue()))
                    .filter(entry -> {
                        BookingModel bookingKey = entry.getKey();
                        List<BookingCertificateStatusMapping> certificateStatusMappingsValues = entry.getValue();
                        int totalCertificateMappings = certificateStatusMappingsValues.size();

                        // Check clearance statuses for each certificate status mapping
                        for (BookingCertificateStatusMapping statusMapping : certificateStatusMappingsValues) {
                            List<ClearanceStatus> clearanceStatuses = Arrays.asList(
                                    statusMapping.getTheoryClearanceStatus(),
                                    statusMapping.getProjectClearanceStatus(),
                                    statusMapping.getAttendanceClearanceStatus(),
                                    statusMapping.getFinanceClearanceStatus()
                            );

                            // Update status and add to hold list if not all statuses are CLEARED
                            if (!clearanceStatuses.stream().allMatch(ClearanceStatus.CLEARED::equals)) {
                                totalCertificateMappings--;
                                statusMapping.setCertificateReleaseStatus(CertificateReleaseStatus.TO_REVIEW);
                                certificateMappings.add(statusMapping);

                                if (bookingKey.isReleasable()) {
                                    BookingCourseGroupMapping bookingCourseGroupMapping = statusMapping.getBookingCourseGroupMapping();
                                    BookingModel booking = bookingCourseGroupMapping.getBooking();
                                    StudentModel student = booking.getStudent();
                                    EnquiryModel enquiry = student.getEnquiry();
                                    String studentName = textHelper.concatenateStrings(
                                            " ",
                                            enquiry.getStudentFirstname(),
                                            enquiry.getStudentMiddlename(),
                                            enquiry.getStudentLastname()
                                    );

                                    // Add email template for certificate on hold
                                    onHoldTemplates.add(eMailTemplates.getCertificationOnHoldTemplate(
                                            enquiry.getPrimaryEmail(),
                                            studentName,
                                            bookingCourseGroupMapping.getCourseGroup().getCourseGroupName(),
                                            DateTimeFormatter.ofPattern("dd-MMM-yy").withZone(ZoneId.of("UTC")).format(statusMapping.getToBeReleasedAt())
                                    ));
                                }
                            }
                        }

                        bookingKey.setReleasable(totalCertificateMappings == certificateStatusMappingsValues.size());
                        bookings.add(bookingKey);
                        return bookingKey.isReleasable();
                    })
                    .map(entry -> {
                        BookingModel bookingKey = entry.getKey();
                        List<BookingCertificateStatusMapping> certificateStatusMappingsValues = entry.getValue();
                        // Set release date if not already set and prepare confirmation email template
                        if (certificateStatusMappingsValues.stream().anyMatch(statusMapping -> null == statusMapping.getToBeReleasedAt())) {
                            StudentModel student = bookingKey.getStudent();
                            EnquiryModel enquiry = student.getEnquiry();
                            OrganizationModel organization = enquiry.getOrganization();

                            Instant supposeToBeReleased = Instant.now().plus(Duration.ofDays(organization.getCertificateReleaseInDays()));
                            String studentName = textHelper.concatenateStrings(
                                    " ",
                                    enquiry.getStudentFirstname(),
                                    enquiry.getStudentMiddlename(),
                                    enquiry.getStudentLastname()
                            );

                            certificateStatusMappingsValues.forEach(statusMapping -> {
                                if (null == statusMapping.getToBeReleasedAt()) {
                                    CourseGroupModel courseGroup = statusMapping.getBookingCourseGroupMapping().getCourseGroup();
                                    confirmationTemplates.add(eMailTemplates.getCertificateVerificationTemplate(
                                            studentName,
                                            enquiry.getPrimaryEmail(),
                                            courseGroup.getCourseGroupName(),
                                            student.getStudentUid(),
                                            organization.getCertificateReleaseInDays()
                                    ));
                                    // Set the date when the certificate should be released
                                    statusMapping.setToBeReleasedAt(supposeToBeReleased);
                                }
                            });

                            entry.setValue(certificateStatusMappingsValues);
                        }

                        return entry;
                    })
                    .filter(entry -> {
                        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                        // Filter out mappings where release date is after the current time
                        return entry.getValue().stream().noneMatch(statusMapping -> {
                            LocalDateTime supposeToBeReleased = statusMapping.getToBeReleasedAt().atZone(ZoneId.of("UTC")).toLocalDate().atStartOfDay();
                            return supposeToBeReleased.isAfter(now);
                        });
                    })
                    .flatMap(entry -> entry.getValue()
                            .stream()
                            .map(statusMapping -> {
                                // Create and save certificate model for each status mapping
                                BookingCourseGroupMapping bookingCourseGroupMapping = statusMapping.getBookingCourseGroupMapping();
                                CourseGroupModel courseGroup = bookingCourseGroupMapping.getCourseGroup();
                                BookingModel booking = bookingCourseGroupMapping.getBooking();
                                StudentModel student = booking.getStudent();
                                EnquiryModel enquiry = student.getEnquiry();

                                CertificateModel certificate = CertificateModel.builder()
                                        .certificateUid(generateCertificateUID())
                                        .student(student)
                                        .issuedToFirstName(enquiry.getStudentFirstname())
                                        .issuedToMiddleName(enquiry.getStudentMiddlename())
                                        .issuedToLastName(enquiry.getStudentLastname())
                                        .certificateStatus(CertificateStatus.ACTIVE)
                                        .assessmentType(AssessmentType.FINAL)
                                        .courseGroup(courseGroup)
                                        .certificateStatusMapping(statusMapping)
                                        .downloadCount(0)
                                        .build();
                                certificates.put(statusMapping, certificate);

                                // Update status mappings and booking-course-group mappings
                                statusMapping.setCertificateReleaseStatus(CertificateReleaseStatus.RELEASED);
                                certificateMappings.add(statusMapping);

                                bookingCourseGroupMapping.setStatus(BookingStatus.COMPLETED);
                                courseGroupMappings.add(bookingCourseGroupMapping);
                                students.add(student.getStudentId());

                                // Prepare email templates for released certificates
                                String studentName = textHelper.concatenateStrings(
                                        " ",
                                        enquiry.getStudentFirstname(),
                                        enquiry.getStudentMiddlename(),
                                        enquiry.getStudentLastname()
                                );
                                String courseGroupName = courseGroup.getCourseGroupName();
                                String candidateEmail = enquiry.getPrimaryEmail();

                                Objects.requireNonNull(downloadCertificateLink);
                                String downloadLink = downloadCertificateLink + statusMapping.getBookingCertificateStatusUid();
                                releasedTemplates.add(eMailTemplates.getDownloadCertificateTemplate(
                                        studentName,
                                        candidateEmail,
                                        courseGroupName,
                                        downloadLink
                                ));

                                return ImmutableTriple.of(courseGroupName, studentName, candidateEmail);
                            })
                    )
                    .toList();

            // Save updated bookings to the repository
            if (!bookings.isEmpty()) {
                bookingRepository.saveAll(bookings);
            }

            // Save updated course group mappings to the repository
            if (!courseGroupMappings.isEmpty()) {
                bookingCourseGroupMappingRepository.saveAll(courseGroupMappings);
            }

            // Save newly created certificates to the repository
            if (!certificates.isEmpty()) {
                for (BookingCertificateStatusMapping statusMapping : certificateMappings) {
                    if (certificates.containsKey(statusMapping)) {
                        statusMapping.setCertificate(
                                certificateRepository.save(certificates.get(statusMapping))
                        );
                    }
                }
            }

            // Save updated certificate status mappings to the repository
            if (!certificateMappings.isEmpty()) {
                certificateStatusMappingRepository.saveAll(certificateMappings);
            }

            // Update student statuses to PASSED_OUT if all bookings are completed
            if (!students.isEmpty()) {
                List<StudentModel> passedOutStudents = studentRepository
                        .findAllById(students)
                        .stream()
                        .filter(model -> model.getBookings()
                                .stream()
                                .allMatch(booking -> booking.getCourseGroupMappings()
                                        .stream()
                                        .noneMatch(groupMapping -> BookingStatus.ON_GOING.equals(groupMapping.getStatus()))
                                ))
                        .map(model -> {
                            model.setStudentStatus(StudentStatus.PASSED_OUT);
                            return model;
                        })
                        .collect(Collectors.toCollection(ArrayList::new));
                if (!passedOutStudents.isEmpty()) {
                    studentRepository.saveAll(passedOutStudents);
                }
            }

            // Add jobs to send email notifications
            if (!confirmationTemplates.isEmpty()) {
                jobProxy.addJobInDB(
                        "Email to student confirmation before releasing certificate.",
                        ("Send email to " + onHoldTemplates.stream()
                                .map(EmailTemplateDTO::getRecipient)
                                .collect(Collectors.joining(", "))
                        ),
                        jsonConverter.getJsonStringFromList(confirmationTemplates),
                        JobType.EMAIL,
                        ScheduleType.ONCE,
                        Instant.now(),
                        null,
                        null
                );
            }

            if (!onHoldTemplates.isEmpty()) {
                jobProxy.addJobInDB(
                        "Email to the students regarding certificate release has been put on hold.",
                        ("Send email to " + onHoldTemplates.stream()
                                .map(EmailTemplateDTO::getRecipient)
                                .collect(Collectors.joining(", "))
                        ),
                        jsonConverter.getJsonStringFromList(onHoldTemplates),
                        JobType.EMAIL,
                        ScheduleType.ONCE,
                        Instant.now(),
                        null,
                        null
                );
            }

            if (!releasedTemplates.isEmpty()) {
                jobProxy.addJobInDB(
                        "Email to the students regarding the release of their certificate according to the specified schedule.",
                        ("Send email to " + releasedTemplates.stream()
                                .map(EmailTemplateDTO::getRecipient)
                                .collect(Collectors.joining(", "))
                        ),
                        jsonConverter.getJsonStringFromList(releasedTemplates),
                        JobType.EMAIL,
                        ScheduleType.ONCE,
                        Instant.now(),
                        null,
                        null
                );
            }

            // Log the certificate issuance details
            mappings.forEach(tripleValues -> log.info(
                    "Certificate for {} has been issued to {} on {} at {}.",
                    tripleValues.getLeft(),
                    tripleValues.getMiddle(),
                    tripleValues.getRight(),
                    Instant.now()
            ));
        });
    }



    /**
     * Generates a unique certificate UID with a specified prefix. The method ensures the UID is unique
     * by checking against the database and retries up to 100 times if necessary.
     *
     * @return A unique certificate UID.
     * @throws InvalidDataException If a unique UID cannot be generated after 100 attempts.
     */
    public String generateCertificateUID() {
        // Generate the prefix
        String prefix = "CR";
        String certificateUid;
        boolean existsInDatabase;
        int attempts = 0;
        do {
            // Generate a random UUID suffix and combine it with the prefix to create the UID
            String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16).toUpperCase();
            certificateUid = prefix + uuid;
            // Check if the generated UID already exists in the database
            existsInDatabase = certificateRepository.existsByCertificateUid(certificateUid);
            attempts++;
        } while (existsInDatabase && attempts <= 100);

        // If the generated UID still exists after 100 attempts, throw an exception
        if (existsInDatabase)
            throw new InvalidDataException("Failed to execute current task.Try again");

        // Return the unique certificate UID
        return certificateUid;
    }




    /**
     * Sends emails asynchronously using a list of email templates. If any email fails to send,
     * the failure details are logged, and a job is added to the database to handle the failed emails.
     *
     * @param templates A list of {@link EmailTemplateDTO} containing the email details to be sent.
     * @param jobId The job ID for logging and tracking purposes.
     * @throws JsonProcessingException If there is an error processing the JSON for the failed emails.
     */
    public void sendMails(List<EmailTemplateDTO> templates, Long jobId) throws JsonProcessingException {
        // Concurrent map to store failed email templates and their failure reasons
        ConcurrentHashMap<EmailTemplateDTO, String> failedTemplates = new ConcurrentHashMap<>();

        // Asynchronously send each email template using CompletableFuture
        List<CompletableFuture<Void>> future = templates.stream().map(template -> CompletableFuture.runAsync(() -> {
            try {
                if (mailer.sendMail(template.getRecipient(), template.getSubject(), template.getMessageBody(), template.isHtml(), template.getAttachmentPath())) {
                    info(jobId, "Sent mail to " + template.getRecipient() + " for " + template.getSubject());
                } else {
                    error(jobId, "Failed to send mail " + template.getRecipient() + " for " + template.getSubject());
                }
            } catch (Exception e) {
                failedTemplates.put(template, e.getMessage());
            }
        }, taskExecutor)).toList();

        // Wait for all asynchronous tasks to complete
        CompletableFuture.allOf(future.toArray(new CompletableFuture[0])).join();

        // If there are any failed email templates, log the failures and add a job to handle them
        if (!failedTemplates.isEmpty()) {
            failedTemplates.forEach((template, cause) -> error(jobId, "Task was rejected [" + cause + "] mail to (" + template.getRecipient() + ") for [" + template.getSubject() + "]"));
            jobProxy.addJobInDB(
                    "Failed Emails",
                    failedTemplates.keySet().stream().map(EmailTemplateDTO::getRecipient).collect(Collectors.joining(", ")),
                    jsonConverter.convertMapToJsonString(failedTemplates),
                    JobType.FAILED_TEMPLATES,
                    ScheduleType.ONCE,
                    Instant.now(),
                    null,
                    null
            );
        }
    }



    /**
     * Executes a job using the provided implementation provider. The job status is updated to RUNNING
     * before execution, and upon completion, the status is updated to either SUCCESS or FAILED based
     * on the outcome. The job is then saved to the database.
     *
     * @param jobId The ID of the job to be executed.
     * @param implProvider The implementation provider that defines the job execution logic.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executor(Long jobId, ImplProvider<JobModel> implProvider) {
        // Retrieve the job from the database using the provided jobId
        JobModel job = jpaProxy.getJobById(jobId).orElse(null);

        // If the job is not found, log an error and return
        if (null ==  job) {
            error(jobId, "Job is missing while executing.");
            return;
        }

        // Log the job status as RUNNING
        info(jobId, JobStatus.RUNNING);
        try {
            // Execute the job using the provided implementation
            implProvider.execute(job);// EXECUTION
            // Update the job status to SUCCESS upon successful execution
            job.setStatus(JobStatus.SUCCESS);
            info(job.getId(), JobStatus.SUCCESS);
        } catch (Exception exception) {
            // Update the job status to FAILED and increment the attempt count upon failure
            job.setStatus(JobStatus.FAILED);
            job.setAttempts(job.getAttempts() + 1);
            // Log the failure details, including the stack trace of the exception
            error(job.getId(), JobStatus.FAILED.name() + "\n" + ExceptionUtils.getStackTrace(exception));
        } finally {
            // Save the job back to the database with the updated status and attempt count
            jpaProxy.saveJob(job);
        }
    }





    // UTILS
    Predicate<String> predicateIsRegistration = courseName -> courseName.equalsIgnoreCase("REGISTRATION");

    private boolean isAnyRegistration(List<BookingCourseGroupMapping> mapping) {
        return streamCourseNames(mapping).anyMatch(predicateIsRegistration);
    }

    private boolean isAnyRegistration(BookingCourseGroupMapping mapping) {
        return streamCourseNames(mapping).anyMatch(predicateIsRegistration);
    }

    private boolean isAllRegistration(List<BookingCourseGroupMapping> mapping) {
        return streamCourseNames(mapping).anyMatch(predicateIsRegistration);
    }

    private Stream<String> streamCourseNames(List<BookingCourseGroupMapping> mapping) {
        return streamCourses(mapping).map(CourseModel::getCourseName);
    }

    private Stream<String> streamCourseNames(BookingCourseGroupMapping mapping) {
        return streamCourses(mapping).map(CourseModel::getCourseName);
    }

    private Stream<CourseModel> streamCourses(List<BookingCourseGroupMapping> mapping) {
        return mapping.stream()
                .map(BookingCourseGroupMapping::getCourses)
                .flatMap(List::stream)
                .map(BookingCourseMapping::getCourse);
    }

    private Stream<CourseModel> streamCourses(BookingCourseGroupMapping mapping) {
        return mapping.getCourses()
                .stream()
                .map(BookingCourseMapping::getCourse);
    }


    private static String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }



    public static String formatInstantDate(Instant instant, ZoneId zone) {
        // Convert Instant to ZonedDateTime using the given ZoneId
        ZonedDateTime zonedDateTime = instant.atZone(zone);

        // Define the desired date format with day suffix, month name, and year
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d'th' MMMM yyyy", Locale.ENGLISH);

        // Handle the day suffix for the date
        String daySuffix = getDaySuffix(zonedDateTime.getDayOfMonth());

        return zonedDateTime.format(formatter).replace("th", daySuffix);
    }


    public ZoneId convertStringToZoneId(String id) {
        try {
            return ZoneId.of(id);
        } catch (Exception e) {
            throw new InvalidDataException("Invalid time zone");
        }
    }

    public static LocalDate getCurrentUtcDate() {
        // Get the current date-time in UTC
        ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        // Extract the LocalDate from the ZonedDateTime
        return utcDateTime.toLocalDate();
    }


}



/*
    1. START_DATE - [Batch - 1st Session (Mon)] :
    Publisher -> BATCH_UID (MARK_ELIGIBILITY_ADD_START_DATE)
    Consumer -> {
        a) SP -> Map<BOO_COU_GRO_MAPPING_ID, START_DATE>
        b) find all `BookingCertificateStatusMapping` with BOO_COU_GRO_MAPPING_ID
        c) Update START_AT only if it's NULL.
    }



    2. ATTENDANCE - [Batch - Mark As Complete (Mon)] :
    Publisher -> BATCH_UID (MARK_ELIGIBILITY_ATTENDANCE)
    Consumer -> {
        a) SP -> Map<BOO_COU_GRO_MAPPING_ID, COU_ID> (filter 70%)
        b) find all `BookingCertificateStatusMapping` with BOO_COU_GRO_MAPPING_ID
        c) Iterate `BookingCourseCertificateStatusMapping` and if matching COU_ID then true (ATTENDANCE);
        d) Revisit all `BookingCertificateStatusMapping` and if all BookingCourseCertificateStatusMapping are true (ATTENDANCE) -> Mark BookingCertificateStatusMapping (ATTENDANCE) to true;
    }



    4. THEORY, PROJECT - [Submit, Publish] :
    Publisher -> COU_ID, STU_ID, STU_FINAL_EXAM_ID (MARK_ELIGIBILITY_ACADEMICS_THEORY | MARK_ELIGIBILITY_ACADEMICS_PROJECT)
    Consumer -> {
        a) find all `BookingCertificateStatusMapping` with COU_ID && STU_ID
        b) Iterate through `BookingCourseCertificateStatusMapping`
            1) set mapping `STU_FINAL_EXAM_ID` (THEORY | PROJECT)
            2) set status to true (THEORY | PROJECT)
        c) Iterate through all `BookingCertificateStatusMapping` if all internal courses are clear then mark `BookingCertificateStatusMapping` true (THEORY | PROJECT)
            1) if PROJECT and `BookingCertificateStatusMapping` (PROJECT) is true then calculate GRADE { Obtained TOTAL for all courses / Total of all courses }
    }



       STUDENT FINAL EXAM MAPPING ADDED
       RELEASER?

    BookingModel STATUS -> COMPLETED if all certificates are released.
    StudentModel STATUS -> PASSED_OUT if all bookings are completed.

    BookingCertificateStatusMapping STATUS -> TO_REVIEW if any criteria is found to be false, send mail to student.
    BookingCertificateStatusMapping STATUS -> READY if all criteria are true.
    BookingCertificateStatusMapping STATUS -> RELEASED if supposeToBeSent is today send certificate via download link in email.
*/
