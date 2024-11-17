package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.student.StudentModel;
import org.springframework.data.jpa.repository.JpaRepository;



public interface StudentRepository extends JpaRepository<StudentModel, Long> {}
