package org.kosign.chatbotapi.batch.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;

public class JobCompletionListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(JobCompletionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("🚀 Starting scraping job {} at {}",
                jobExecution.getJobId(), jobExecution.getStartTime());
        logger.info("📋 Job parameters: {}", jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String durationMessage = "N/A";

        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            Duration duration = Duration.between(
                    jobExecution.getStartTime().toLocalTime(),
                    jobExecution.getEndTime().toLocalTime()
            );
            durationMessage = duration.getSeconds() + " seconds";
        }

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            logger.info("✅ Job {} completed successfully at {} (Duration: {})",
                    jobExecution.getJobId(), jobExecution.getEndTime(), durationMessage);
        } else {
            logger.error("❌ Job {} failed with status {} at {} (Duration: {})",
                    jobExecution.getJobId(), jobExecution.getStatus(), jobExecution.getEndTime(), durationMessage);

            // Log step-level failures
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
                    logger.error("❌ Step '{}' failed with status: {}",
                            stepExecution.getStepName(), stepExecution.getStatus());

                    if (stepExecution.getFailureExceptions() != null && !stepExecution.getFailureExceptions().isEmpty()) {
                        stepExecution.getFailureExceptions().forEach(exception ->
                                logger.error("❌ Step failure exception: {}", exception.getMessage(), exception));
                    }
                }
            }
        }

        // Log job execution summary
        logger.info("📊 Job execution summary:");
        logger.info("   - Job ID: {}", jobExecution.getJobId());
        logger.info("   - Status: {}", jobExecution.getStatus());
        logger.info("   - Start Time: {}", jobExecution.getStartTime());
        logger.info("   - End Time: {}", jobExecution.getEndTime());
        logger.info("   - Duration: {}", durationMessage);
        logger.info("   - Exit Code: {}", jobExecution.getExitStatus().getExitCode());

        if (jobExecution.getExitStatus().getExitDescription() != null &&
                !jobExecution.getExitStatus().getExitDescription().isEmpty()) {
            logger.info("   - Exit Description: {}", jobExecution.getExitStatus().getExitDescription());
        }
    }
}