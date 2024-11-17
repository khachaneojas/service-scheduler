package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.student.mapping.BookingCourseGroupMapping;
import com.sprk.commons.entity.primary.student.tag.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface BookingCourseGroupMappingRepository extends JpaRepository<BookingCourseGroupMapping, Long> {
    @Query("SELECT m FROM BookingCourseGroupMapping m " +
            "JOIN m.booking b " +
            "WHERE m.status = :status ")
    List<BookingCourseGroupMapping> findAllByReleasableTrue(
            @Param("status") BookingStatus status
    );
}
