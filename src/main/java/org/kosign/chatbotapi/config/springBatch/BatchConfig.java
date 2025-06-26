package org.kosign.chatbotapi.config.springBatch;

import org.kosign.chatbotapi.batch.listeners.JobCompletionListener;
import org.kosign.chatbotapi.batch.tasklets.PythonScrapingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Value("${python.scraper.path}")
    private String pythonScraperPath;

    @Bean(name = "scrapingJob")
    public Job scrapingJob(JobRepository jobRepository,
                           Step scrapingStep,
                           JobCompletionListener jobCompletionListener) {
        return new JobBuilder("websiteScrapingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(scrapingStep)
                .build();
    }

    @Bean(name = "scrapingStep")
    public Step scrapingStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             PythonScrapingTasklet tasklet) {
        return new StepBuilder("scrapingStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean(name = "pythonScrapingTasklet")
    public PythonScrapingTasklet pythonScrapingTasklet(Environment environment) {
        return new PythonScrapingTasklet(pythonScraperPath, environment);
    }

    @Bean
    public JobCompletionListener jobCompletionListener() {
        return new JobCompletionListener();
    }
}