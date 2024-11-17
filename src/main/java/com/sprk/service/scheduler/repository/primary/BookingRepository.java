package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.student.BookingModel;
import com.sprk.commons.entity.primary.student.tag.BookingStatus;
import com.sprk.commons.entity.primary.student.tag.ClearanceStatus;
import com.sprk.commons.entity.primary.student.tag.StudentStatus;
import com.sprk.commons.entity.primary.student.tag.CertificateReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;


public interface BookingRepository extends JpaRepository<BookingModel, Long> {

    @Query(value = "SELECT distinct b.* FROM booking b " +
            "JOIN booking_course_group_mapping cg ON b.boo_id = cg.boo_id " +
            "JOIN booking_certificate_status_mapping csm ON cg.boo_cou_gro_id = csm.boo_cou_gro_id " +
            "JOIN payment_installments i ON b.boo_id = i.instal_book_id " +
            "JOIN student s ON b.boo_student_id = s.stu_id " +
            "WHERE cg.boo_status IN :bookingStatus " +
            "AND csm.cer_fin_sts = :certificateStatus " +
            "AND s.stu_status NOT IN :studentStatus " +
            "AND i.instal_pay_id IS NULL " +
            "AND DATEDIFF(:queryDate, i.instal_due_date) > :daysDifference",
            nativeQuery = true)
    List<BookingModel> findBookingsByConditions(@Param("bookingStatus") List<BookingStatus> bookingStatus,
                                                @Param("certificateStatus") ClearanceStatus certificateStatus,
                                                @Param("studentStatus") List<StudentStatus> studentStatus,
                                                @Param("queryDate") LocalDate queryDate,
                                                @Param("daysDifference") int daysDifference);


    @Query(value = "SELECT distinct b.* FROM booking b " +
            "JOIN booking_course_group_mapping cg ON b.boo_id = cg.boo_id " +
            "JOIN student s ON b.boo_student_id = s.stu_id " +
            "WHERE cg.boo_status = :bookingStatus " +
            "AND s.stu_status <> :studentStatus " +
            "AND DATEDIFF(b.est_exp_date, :queryDate) = :daysDifference",
            nativeQuery = true)
    List<BookingModel> findBookingsAboutToExpire(@Param("bookingStatus") BookingStatus bookingStatus,
                                                @Param("studentStatus") StudentStatus studentStatus,
                                                @Param("queryDate") LocalDate queryDate,
                                                @Param("daysDifference") int daysDifference);


    @Query(value = "SELECT distinct b.* FROM booking b " +
            "JOIN booking_course_group_mapping cg ON b.boo_id = cg.boo_id " +
            "WHERE cg.boo_status = :bookingStatus " +
            "AND DATEDIFF(b.boo_start_date, :queryDate) = :daysDifference",
            nativeQuery = true)
    List<BookingModel> findBookingsByStartDate(@Param("bookingStatus") BookingStatus bookingStatus,
                                                 @Param("queryDate") LocalDate queryDate,
                                                 @Param("daysDifference") int daysDifference);



    @Query("SELECT DISTINCT b FROM BookingModel b " +
            "WHERE b.estimatedExpirationDate <= :expirationDate " +
            "AND EXISTS (" +
            "    SELECT bcg FROM BookingCourseGroupMapping bcg " +
            "    JOIN bcg.certificateStatusMapping csm " +
            "    WHERE bcg.booking = b " +
            "    AND bcg.status = :status " +
            "    AND csm.certificateReleaseStatus NOT IN :excludedStatuses" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT bcg FROM BookingCourseGroupMapping bcg " +
            "    JOIN bcg.certificateStatusMapping csm " +
            "    WHERE bcg.booking = b " +
            "    AND bcg.status = :status " +
            "    AND csm.certificateReleaseStatus IN :excludedStatuses" +
            ")")
    List<BookingModel> findDistinctBookings(
            @Param("expirationDate") Instant expirationDate,
            @Param("status") BookingStatus status,
            @Param("excludedStatuses") List<CertificateReleaseStatus> excludedStatuses);



    @Query("SELECT DISTINCT b FROM BookingModel b " +
            "JOIN b.courseGroupMappings bcg " +
            "WHERE b.estimatedExpirationDate <= :expirationDate " +
            "AND bcg.status = :status")
    List<BookingModel> findDistinctBookingsByBookingStatus(
            @Param("expirationDate") Instant expirationDate,
            @Param("status") BookingStatus status);


}
