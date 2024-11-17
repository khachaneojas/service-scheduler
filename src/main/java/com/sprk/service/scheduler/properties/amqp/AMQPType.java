package com.sprk.service.scheduler.properties.amqp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AMQPType {

    String standard;
    String mailer;

    String releaseCriteriaMarker;
    String certificateReleaser;

    String updateExpiryStatus;
    String expiryReminderMail;
    String updateStudentStatus;
    String notifyBookingStart;

    String websiteDataTransfer;

}
