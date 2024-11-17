package com.sprk.service.scheduler.dao;

import com.sprk.commons.entity.mq.JobModel;
import com.sprk.commons.entity.mq.RegistryModel;
import com.sprk.commons.entity.mq.tag.JobType;
import com.sprk.service.scheduler.dto.procedure.SPGetAttendanceAllStudentsForBatch;
import com.sprk.service.scheduler.repository.mq.JobRepository;
import com.sprk.service.scheduler.repository.mq.RegistryRepository;
import com.sprk.service.scheduler.repository.primary.BookingCertificateStatusMappingRepository;
import com.sprk.service.scheduler.repository.primary.BookingCourseCertificateStatusMappingRepository;
import com.sprk.service.scheduler.repository.primary.StudentFinalExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JPAProxy {
    private final JobRepository jobRepository;
    private final RegistryRepository registryRepository;
    private final StudentFinalExamRepository studentFinalExamRepository;


//    JOB
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public long getAllJobCount() {
        return jobRepository.count();
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public long getAllJobCountByType(JobType jobType) {
        return jobRepository.countByJobType(jobType);
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public List<JobModel> getAllJobs() {
        return jobRepository.findAll();
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public Optional<JobModel> getJobById(Long jobId) {
        return jobRepository.findById(jobId);
    }

    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public void createJob(JobModel model) {
        jobRepository.save(model);
    }


//    REGISTRY
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public int getMACCount(String address) {
        return registryRepository.countByMacAddress(address);
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public List<RegistryModel> getMostRecentInstance() {
        return registryRepository
                .findMostRecent();
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public RegistryModel getRegistryByMacAddress(String address) {
        return registryRepository.findByMacAddress(address);
    }

    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public void saveRegistry(RegistryModel model) {
        registryRepository.save(model);
    }

    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public void saveJob(JobModel model) {
        jobRepository.save(model);
    }

    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public JobModel saveJobAndFlush(JobModel model) {
        return jobRepository.saveAndFlush(model);
    }



    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.MANDATORY
    )
    public List<SPGetAttendanceAllStudentsForBatch> getSPGetAttendanceAllStudentsForBatch(String batchUid) {
        Objects.requireNonNull(batchUid);
        List<Object[]> results = studentFinalExamRepository.sp_get_attendance_by_batch_uid(batchUid);
        return Optional
                .ofNullable(results)
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(this::mapToDTOForSPGetAttendanceAllStudentsForBatch)
                .collect(Collectors.toList());
    }

    private SPGetAttendanceAllStudentsForBatch mapToDTOForSPGetAttendanceAllStudentsForBatch(Object[] row) {
        Objects.requireNonNull(row);
        SPGetAttendanceAllStudentsForBatch dto = new SPGetAttendanceAllStudentsForBatch();
        int length = row.length;
        for (int index = 0; index < length; index++) {
            Object value = row[index];
            if (null != value) {
                switch (index) {
                    case 0:
                        dto.setStudentId((Integer) value);
                        break;
                    case 1:
                        dto.setTotalModules((Integer) value);
                        break;
                    case 2:
                        dto.setAttendedModules((Integer) value);
                        break;
                    case 3:
                        dto.setBookingCourseGroupMappingId((Long) value);
                        break;
                    case 4:
                        dto.setCourseGroupId((Long) value);
                        break;
                    case 6:
                        dto.setCourseId((Long) value);
                        break;
                    default:
                        break;
                }
            }
        }
        return dto;
    }
}
