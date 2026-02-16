package com.sdlc.service;

import com.sdlc.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages async jobs and their SSE emitters for real-time progress tracking.
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private final ConcurrentHashMap<String, Job> jobs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Create a new processing job.
     */
    public Job createJob(String originalFileName) {
        String jobId = "job_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
        Job job = new Job(jobId, originalFileName);
        jobs.put(jobId, job);
        log.info("Created job: {} for file: {}", jobId, originalFileName);
        return job;
    }

    /**
     * Get a job by ID.
     */
    public Job getJob(String jobId) {
        return jobs.get(jobId);
    }

    /**
     * Update job progress and emit SSE event.
     */
    public void updateProgress(String jobId, int progress, String stage) {
        Job job = jobs.get(jobId);
        if (job != null) {
            job.setProgress(progress);
            job.setStage(stage);
            emitEvent(jobId, job);
            log.info("Job {} progress: {}% - {}", jobId, progress, stage);
        }
    }

    /**
     * Mark a job as completed.
     */
    public void completeJob(String jobId, String githubUrl) {
        Job job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(Job.Status.COMPLETED);
            job.setProgress(100);
            job.setStage("Completed");
            job.setGithubFileUrl(githubUrl);
            emitEvent(jobId, job);
            closeEmitter(jobId);
            log.info("Job {} completed. GitHub URL: {}", jobId, githubUrl);
        }
    }

    /**
     * Mark a job as failed.
     */
    public void failJob(String jobId, String error) {
        Job job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(Job.Status.FAILED);
            job.setError(error);
            job.setStage("Failed");
            emitEvent(jobId, job);
            closeEmitter(jobId);
            log.error("Job {} failed: {}", jobId, error);
        }
    }

    /**
     * Create an SSE emitter for a job. Frontend connects to this for real-time updates.
     */
    public SseEmitter createEmitter(String jobId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minute timeout
        emitters.put(jobId, emitter);

        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError(e -> emitters.remove(jobId));

        // Send the current state immediately if the job already exists
        Job job = jobs.get(jobId);
        if (job != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(buildEventData(job)));
            } catch (IOException e) {
                log.warn("Failed to send initial SSE event for job {}", jobId);
            }
        }

        return emitter;
    }

    /**
     * Emit an SSE event to the connected client.
     */
    private void emitEvent(String jobId, Job job) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(buildEventData(job)));
            } catch (Exception e) {
                log.warn("Failed to send SSE event for job {}: {}", jobId, e.getMessage());
                emitters.remove(jobId);
            }
        }
    }

    /**
     * Build the event data map from a Job.
     */
    private Map<String, Object> buildEventData(Job job) {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", job.getJobId());
        data.put("progress", job.getProgress());
        data.put("stage", job.getStage());
        data.put("status", job.getStatus().name());
        if (job.getGithubFileUrl() != null) {
            data.put("githubFileUrl", job.getGithubFileUrl());
        }
        if (job.getError() != null) {
            data.put("error", job.getError());
        }
        return data;
    }

    /**
     * Close and remove an SSE emitter.
     */
    private void closeEmitter(String jobId) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // Emitter may already be closed
            }
            emitters.remove(jobId);
        }
    }
}
