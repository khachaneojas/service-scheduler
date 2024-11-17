package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.examination.StudentFinalExamModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface StudentFinalExamRepository extends JpaRepository<StudentFinalExamModel, Long> {
    @Procedure(procedureName = "SP_GET_ATTENDANCE_BY_BATCH_ID")
    List<Object[]> sp_get_attendance_by_batch_uid(
            @Param("BOOK_UID") String bookingUid
    );
}
