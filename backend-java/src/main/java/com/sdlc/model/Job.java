package com.sdlc.model;

/**
 * Represents an asynchronous document processing job.
 * Tracks progress from upload through parsing to GitHub upload.
 */
public class Job {

    public enum Status {
        PROCESSING, COMPLETED, FAILED
    }

    private final String jobId;
    private volatile Status status;
    private volatile int progress;
    private volatile String stage;
    private volatile String githubFileUrl;
    private volatile String error;
    private final String originalFileName;

    public Job(String jobId, String originalFileName) {
        this.jobId = jobId;
        this.originalFileName = originalFileName;
        this.status = Status.PROCESSING;
        this.progress = 0;
        this.stage = "Initializing";
    }

    public String getJobId() { return jobId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getGithubFileUrl() { return githubFileUrl; }
    public void setGithubFileUrl(String githubFileUrl) { this.githubFileUrl = githubFileUrl; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getOriginalFileName() { return originalFileName; }
}
