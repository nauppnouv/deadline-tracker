package vn.deadline.tracker.service;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tests drive workers explicitly with fixed clocks; ops processes never start background writers. */
@Configuration @EnableScheduling @Profile("!test & !ops")
public class SchedulingConfig {}
