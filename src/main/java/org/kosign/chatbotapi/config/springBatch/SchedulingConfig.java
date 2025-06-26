package org.kosign.chatbotapi.config.springBatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("scrapingJob")
    private Job scrapingJob;

    @Scheduled(cron = "${scraper.schedule.cron:0 0 3 * * ?}")
    public void runScrapingJob() {
        try {
            logger.info("Starting scheduled scraping job execution");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("trigger", "scheduled")
                    .toJobParameters();

            jobLauncher.run(scrapingJob, jobParameters);
            logger.info("Scheduled scraping job completed successfully");
        } catch (Exception e) {
            logger.error("Error running scheduled scraping job: {}", e.getMessage(), e);
        }
    }
}