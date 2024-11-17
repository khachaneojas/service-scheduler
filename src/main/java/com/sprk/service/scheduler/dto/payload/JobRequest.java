package com.sprk.service.scheduler.dto.payload;

import com.sprk.commons.entity.mq.tag.JobType;
import com.sprk.commons.entity.mq.tag.ScheduleType;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRequest {
    String name;
    String description;
    String json;
    JobType job_type;
    ScheduleType schedule_type;
    Instant time;
    List<DayOfWeek> days;
    List<Integer> dates;
}
