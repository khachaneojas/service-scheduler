package com.sprk.service.scheduler.service;

import com.sprk.commons.entity.mq.JobModel;
import com.sprk.commons.entity.mq.RegistryModel;
import com.sprk.commons.entity.mq.tag.JobStatus;
import com.sprk.commons.entity.mq.tag.JobType;
import com.sprk.commons.entity.mq.tag.ScheduleType;

import com.sprk.service.scheduler.dao.JPAProxy;
import com.sprk.service.scheduler.properties.amqp.AMQPConfigProperties;
import com.sprk.service.scheduler.tag.DeviceAddressType;
import com.sprk.service.scheduler.util.DeviceIdentityWizard;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.tuple.ImmutablePair;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;



@Component
@RequiredArgsConstructor
public class JobProcessor {

    private final JPAProxy jpaProxy;
    private final AMQPConfigProperties amqpConfigProperties;
    private final DeviceIdentityWizard deviceIdentity;
    private final RabbitTemplate rabbitTemplate;

    private final Predicate<ScheduleType> isJobScheduleIsEveryday = ScheduleType.EVERYDAY::equals;
    private final Predicate<Instant> isLastRanAtNull = Objects::isNull;
    private final Predicate<Instant> isLastRanAtPastNow = lastRanAt -> LocalDate.now(ZoneOffset.UTC).isAfter(lastRanAt.atZone(ZoneId.of("UTC")).toLocalDate());

    /**
     * Determines if a job is ready to be executed based on its model properties.
     *
     * @param model The job model containing the job's properties.
     * @return true if the job is ready to run, false otherwise.
     */
    boolean isJobReadyToRun(JobModel model) {
        // If the job has never run before, it's ready to run.
        if (model.getLastRanAt() == null) {
            return true;
        }

        Instant now = Instant.now();
        // Check if the retry delay has been exceeded since the last run.
        boolean retryDelayExceeded = (model.getLastRanAt().toEpochMilli() + amqpConfigProperties.getRetryDelay()) < now.toEpochMilli();
        // Check if the number of attempts has not been exceeded.
        boolean attemptsNotExceeded = isJobAttemptsNotExceeded(model.getAttempts());
        // Check if the job is scheduled to run every day and the last run was yesterday or earlier.
        boolean isLastRanDayPast = isJobScheduleIsEverydayAndPastLastRanAtOrNull(model::getScheduleType, model::getLastRanAt);
        // Check if the job status is not success.
        boolean isJobStatusNotSuccess = !JobStatus.SUCCESS.equals(model.getStatus());

        // Reset attempts if they are exceeded but the last ran day is past.
        if (!attemptsNotExceeded && isLastRanDayPast) {
            model.setAttempts(0);
        }

        // Check if all conditions are met for the job to run.
        if (attemptsNotExceeded && retryDelayExceeded && !isLastRanDayPast && isJobStatusNotSuccess) {
            return true;
        }

        // If the job is scheduled to run once and has not succeeded yet, check if it's time to run.
        if (ScheduleType.ONCE.equals(model.getScheduleType()) && isJobStatusNotSuccess) {
            return isExecuteAtBeforeOrEqualToNow(model::getExecuteAt);
        }

        // If the job is scheduled to run every day, check if it's ready to run today.
        if (ScheduleType.EVERYDAY.equals(model.getScheduleType())) {
            return isEverydayJobReadyToRun(model::getExecuteAt, model::getLastRanAt);
        }

        // If none of the above conditions are met, the job is not ready to run.
        return false;
    }


    /**
     * Checks if the specified execution time is before or equal to the current time.
     *
     * @param specifiedExecuteAt A supplier that provides the execution time as an Instant.
     * @return true if the execution time is before or equal to the current time, false otherwise.
     */
    private boolean isExecuteAtBeforeOrEqualToNow(Supplier<Instant> specifiedExecuteAt) {
        // Convert the specified execution time from Instant to LocalDateTime in UTC.
        LocalDateTime executeAt = specifiedExecuteAt.get().atZone(ZoneId.of("UTC")).toLocalDateTime();
        // Get the current time as LocalDateTime in UTC.
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        // Check if the execution time is before or equal to the current time.
        return executeAt.isBefore(now) || executeAt.isEqual(now);
    }



    /**
     * Checks if an everyday job is ready to run based on its specified execution time and the last time it ran.
     *
     * @param specifiedExecuteAt A supplier that provides the execution time as an Instant.
     * @param lastRanAt A supplier that provides the last execution time as an Instant.
     * @return true if the job is ready to run, false otherwise.
     */
    private boolean isEverydayJobReadyToRun(Supplier<Instant> specifiedExecuteAt, Supplier<Instant> lastRanAt) {
        // Convert the specified execution time from Instant to ZonedDateTime in UTC.
        ZonedDateTime executeAt = specifiedExecuteAt.get().atZone(ZoneId.of("UTC"));
        // Create a LocalDateTime for today's execution time at the specified hour and minute in UTC.
        LocalDateTime todayExecuteAt = LocalDateTime.now(ZoneOffset.UTC)
                .withHour(executeAt.getHour())
                .withMinute(executeAt.getMinute());

        // Convert the last execution time from Instant to LocalDateTime in UTC.
        LocalDateTime lastRanAtValue = lastRanAt.get().atZone(ZoneId.of("UTC")).toLocalDateTime();
        // Get the current time as LocalDateTime in UTC.
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Check if today's execution time is after the last execution time and before or equal to the current time.
        return todayExecuteAt.isAfter(lastRanAtValue) && (todayExecuteAt.isBefore(now) || todayExecuteAt.isEqual(now));
    }




    /**
     * Enqueues a job to be processed by sending it to a message queue.
     *
     * @param model The JobModel object that contains the details of the job to be enqueued.
     * @return An ImmutablePair containing the job ID and its type, or null if the routing key is null.
     */
    ImmutablePair<Long, JobType> enqueueJob(JobModel model) {
        // Determine the routing key based on the job type
        String routingKey = switch (model.getJobType()) {
            case FAILED_TEMPLATES -> null;
            case EMAIL -> amqpConfigProperties.getRoutingKey().getMailer();
            case RELEASE_CERTIFICATES -> amqpConfigProperties.getRoutingKey().getCertificateReleaser();
            case STUDENT_PAYMENT_DUE_MAIL,
                 EXAM_STATUS_CHANGE,
                 DELETE_UNPAID_BOOKINGS -> amqpConfigProperties.getRoutingKey().getStandard();
            case MARK_ELIGIBILITY_ACADEMICS_THEORY,
                 MARK_ELIGIBILITY_ACADEMICS_PROJECT,
                 MARK_ELIGIBILITY_ATTENDANCE,
                 MARK_ELIGIBILITY_ADD_START_DATE,
                 MARK_ELIGIBILITY_FINANCE -> amqpConfigProperties.getRoutingKey().getReleaseCriteriaMarker();
            case WEBSITE_DATA_TRANSFER -> amqpConfigProperties.getRoutingKey().getWebsiteDataTransfer();

            case UPDATE_EXPIRY_STATUS -> amqpConfigProperties.getRoutingKey().getUpdateExpiryStatus();
            case EXPIRY_REMINDER_MAIL -> amqpConfigProperties.getRoutingKey().getExpiryReminderMail();
            case UPDATE_STUDENT_STATUS -> amqpConfigProperties.getRoutingKey().getUpdateStudentStatus();
            case NOTIFY_BOOKING_START -> amqpConfigProperties.getRoutingKey().getNotifyBookingStart();
        };

        // Return null if the routing key is null
        if (null == routingKey) {
            return null;
        }

        // Update the job model's status and other details
        model.setStatus(JobStatus.RUNNING);
        model.setLastRanAt(Instant.now());
        RegistryModel instance = getInstance();
        model.setLastRanBy(instance.getMacAddress());
        // Save the job model to the database and flush the changes
        JobModel job = jpaProxy.saveJobAndFlush(model);

        // Send the job ID to the RabbitMQ exchange with the specified routing key
        rabbitTemplate.convertAndSend(amqpConfigProperties.getExchange(), routingKey, job.getId());
        // Return the job ID and type as an ImmutablePair
        return ImmutablePair.of(job.getId(), job.getJobType());
    }



    /**
     * Checks if the number of job attempts has not exceeded the maximum retry limit.
     *
     * @param attempts The current number of attempts made for the job.
     * @return true if the number of attempts is less than the configured retry limit; otherwise, false.
     */
    boolean isJobAttemptsNotExceeded(int attempts) {
        // Compare the current number of attempts with the maximum retry limit from configuration.
        return attempts < amqpConfigProperties.getRetryLimit();
    }



    /**
     * Checks if a job scheduled to run every day should be executed based on its last run time.
     * The job is eligible to run if:
     * 1. The schedule type is set to "every day".
     * 2. The last run time is either null or in the past compared to the current time.
     *
     * @param scheduleType A supplier providing the job's schedule type.
     * @param lastRanAt A supplier providing the timestamp of the last time the job ran.
     * @return true if the job is scheduled to run every day and the last run time is either null or in the past; otherwise, false.
     */
    boolean isJobScheduleIsEverydayAndPastLastRanAtOrNull(Supplier<ScheduleType> scheduleType, Supplier<Instant> lastRanAt) {
        return isJobScheduleIsEveryday.test(scheduleType.get()) && (isLastRanAtNull.or(isLastRanAtPastNow).test(lastRanAt.get()));
    }




    /**
     * Retrieves the instance from the registry based on the current device's MAC address.
     * This method is transactional with serializable isolation level and required propagation.
     * It retrieves the instance from the registry repository based on the current device's MAC address.
     * If no instance is found for the current device's MAC address, a new instance is created
     * using the current device's IP and MAC addresses.
     * @return The instance from the registry based on the current device's MAC address.
     */
    public RegistryModel getInstance() {
        // Retrieve the current device's IP and MAC addresses.
        String currentInstanceIpAddress = deviceIdentity.getDeviceAddress(DeviceAddressType.IP);
        String currentInstanceMacAddress = deviceIdentity.getDeviceAddress(DeviceAddressType.MAC);
        // Check if an instance exists for the current device's MAC address.
        // If found, return the first instance from the list of instances.
        // Otherwise, create a new instance using the current device's IP and MAC addresses.
        return Optional.of(currentInstanceMacAddress)
                .map(jpaProxy::getRegistryByMacAddress)
                .orElse(RegistryModel.builder()
                        .ipAddress(currentInstanceIpAddress)
                        .macAddress(currentInstanceMacAddress)
                        .build());
    }
}
