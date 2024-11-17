package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.student.mapping.BookingCourseCertificateStatusMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface BookingCourseCertificateStatusMappingRepository extends JpaRepository<BookingCourseCertificateStatusMapping, Long> {
    @Query("SELECT cm FROM BookingCourseCertificateStatusMapping cm " +
            "JOIN cm.bookingCertificateStatusMapping m " +
            "JOIN m.bookingCourseGroupMapping bcg " +
            "JOIN cm.courseModel c " +
            "WHERE bcg.bookingCourseGroupId = :bookingCourseGroupId " +
            "AND c.courseId = :courseId")
    BookingCourseCertificateStatusMapping findByBookingCourseGroupMappingIdAndCourseId(
            @Param("bookingCourseGroupId") Long bookingCourseGroupId,
            @Param("courseId") Long courseId
    );
}
