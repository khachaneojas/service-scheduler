package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.student.mapping.BookingCertificateStatusMapping;
import com.sprk.commons.entity.primary.student.tag.CertificateReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;


public interface BookingCertificateStatusMappingRepository extends JpaRepository<BookingCertificateStatusMapping, Long> {
    @Query("SELECT m FROM BookingCertificateStatusMapping m " +
            "JOIN m.courseCertificateStatusMappings cm " +
            "JOIN cm.courseModel c " +
            "JOIN m.bookingCourseGroupMapping bcg " +
            "JOIN bcg.booking boo " +
            "JOIN boo.student stu " +
            "WHERE m.certificateStartAt IS NULL " +
            "AND stu.studentId IN :studentIds " +
            "AND c.courseId = :courseId")
    List<BookingCertificateStatusMapping> findAllMappingsWithoutStartDate(
            @Param("studentIds") Collection<Long> studentIds,
            @Param("courseId") Long courseId
    );

    @Query("SELECT m FROM BookingCertificateStatusMapping m " +
            "WHERE m.certificateReleaseStatus = :status")
    List<BookingCertificateStatusMapping> findAllMappingsByStatus(@Param("status")CertificateReleaseStatus status);

    List<BookingCertificateStatusMapping> findByBookingCourseGroupMappingBookingCourseGroupIdIn(List<Long> bookingCourseGroupIds);

    @Query("SELECT b FROM BookingCertificateStatusMapping b WHERE b.certificate.certificateUid = :certificateUid")
    BookingCertificateStatusMapping findReadyAt(@Param("certificateUid") String certificateUid);
}
