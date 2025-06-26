package org.kosign.chatbotapi.controller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("scrapingJob")
    private Job scrapingJob;

    @PostMapping("/run")
    public String runScraper() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("trigger", "manual-api")
                    .toJobParameters();

            jobLauncher.run(scrapingJob, params);
            return "Scraping job launched.";
        } catch (Exception e) {
            return "Error running scraping job: " + e.getMessage();
        }
    }
}
