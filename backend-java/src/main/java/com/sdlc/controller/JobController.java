package com.sdlc.controller;

import com.sdlc.model.Job;
import com.sdlc.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for job progress tracking.
 * Provides SSE endpoint for real-time progress updates and REST endpoint for polling.
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * SSE endpoint for real-time progress updates.
     * Frontend connects to this after receiving a jobId from upload.
     *
     * GET /api/v1/jobs/{jobId}/progress
     */
    @GetMapping(value = "/{jobId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getJobProgress(@PathVariable String jobId) {
        log.info("SSE connection requested for job: {}", jobId);
        return jobService.createEmitter(jobId);
    }

    /**
     * REST endpoint to poll job status (fallback if SSE is not available).
     *
     * GET /api/v1/jobs/{jobId}
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        Job job = jobService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getJobId());
        response.put("status", job.getStatus().name());
        response.put("progress", job.getProgress());
        response.put("stage", job.getStage());
        if (job.getGithubFileUrl() != null) {
            response.put("githubFileUrl", job.getGithubFileUrl());
        }
        if (job.getError() != null) {
            response.put("error", job.getError());
        }

        return ResponseEntity.ok(response);
    }
}
