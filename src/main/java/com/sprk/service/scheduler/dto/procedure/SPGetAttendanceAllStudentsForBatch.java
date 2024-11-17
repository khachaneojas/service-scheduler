package com.sprk.service.scheduler.dto.procedure;

import lombok.*;



@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SPGetAttendanceAllStudentsForBatch {
    private Integer studentId;
    private Integer totalModules;
    private Integer attendedModules;
    private Long bookingCourseGroupMappingId;
    private Long courseGroupId;
    private Long courseId;
}
