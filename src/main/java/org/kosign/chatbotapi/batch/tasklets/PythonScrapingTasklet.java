package org.kosign.chatbotapi.batch.tasklets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.env.Environment;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PythonScrapingTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(PythonScrapingTasklet.class);
    private final String pythonScriptPath;
    private final Environment environment;
    private static final int TIMEOUT_MINUTES = 30;

    public PythonScrapingTasklet(String pythonScriptPath, Environment environment) {
        this.pythonScriptPath = pythonScriptPath;
        this.environment = environment;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Resolve absolute path
        Path scriptPath = Paths.get(pythonScriptPath);
        if (!scriptPath.isAbsolute()) {
            scriptPath = Paths.get(System.getProperty("user.dir")).resolve(pythonScriptPath);
        }
        File scriptFile = scriptPath.toFile();

        logger.info("🚀 Attempting to run Python script at: {}", scriptFile.getAbsolutePath());

        if (!scriptFile.exists()) {
            throw new RuntimeException("❌ Python script not found at: " + scriptFile.getAbsolutePath());
        }

        // Find Python executable
        String pythonExecutable = findPythonExecutable();
        logger.info("🐍 Using Python executable: {}", pythonExecutable);

        // Build command
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                scriptFile.getAbsolutePath(),
                "--spring-data-mode"
        );

        // Set environment variables
        Map<String, String> env = processBuilder.environment();
        setupEnvironmentVariables(env);

        // Set working directory to script's parent directory
        File workingDir = scriptFile.getParentFile();
        if (workingDir != null && workingDir.exists()) {
            processBuilder.directory(workingDir);
            logger.info("🔧 Working directory: {}", workingDir.getAbsolutePath());
        } else {
            logger.warn("⚠️ Script parent directory not found, using current directory");
            processBuilder.directory(new File(System.getProperty("user.dir")));
        }

        // Separate stdout and stderr for better debugging
        processBuilder.redirectErrorStream(false);

        logger.info("🚀 Starting Python process...");
        Process process = processBuilder.start();

        // Handle stdout and stderr in separate threads
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[PYTHON-OUT] {}", line);
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                logger.error("Error reading stdout: {}", e.getMessage());
            }
        });

        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.error("[PYTHON-ERR] {}", line);
                    errorOutput.append(line).append("\n");
                }
            } catch (IOException e) {
                logger.error("Error reading stderr: {}", e.getMessage());
            }
        });

        outputThread.start();
        errorThread.start();

        // Wait for process with timeout
        boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("❌ Python script timed out after " + TIMEOUT_MINUTES + " minutes");
        }

        // Wait for output threads to finish
        outputThread.join(5000);
        errorThread.join(5000);

        int exitCode = process.exitValue();
        logger.info("✅ Python script exited with code: {}", exitCode);

        if (exitCode != 0) {
            String errorMsg = String.format("❌ Python script failed with exit code: %d\n" +
                            "Error output: %s\nStandard output: %s",
                    exitCode, errorOutput.toString(), output.toString());
            throw new RuntimeException(errorMsg);
        }

        logger.info("✅ Python scraping task completed successfully");
        return RepeatStatus.FINISHED;
    }

    private String findPythonExecutable() {
        // First try to use virtual environment if it exists
        String venvPython = System.getProperty("user.dir") + "/venv/bin/python";
        File venvPythonFile = new File(venvPython);
        if (venvPythonFile.exists()) {
            logger.info("✅ Found virtual environment Python: {}", venvPython);
            return venvPython;
        }

        // Fallback to system Python
        String[] pythonCommands = {"python3", "python", "py"};

        for (String cmd : pythonCommands) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                Process process = pb.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    logger.info("✅ Found Python executable: {}", cmd);
                    return cmd;
                }
            } catch (Exception e) {
                logger.debug("Failed to test Python command '{}': {}", cmd, e.getMessage());
            }
        }

        logger.warn("⚠️ No Python executable found, defaulting to 'python3'");
        return "python3";
    }

    private void setupEnvironmentVariables(Map<String, String> env) {
        // Parse database connection details
        String datasourceUrl = environment.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/web_scraper");
        String dbName = extractDbName(datasourceUrl);
        String[] hostPort = extractHostAndPort(datasourceUrl);

        // Database configuration
        env.put("DB_NAME", dbName);
        env.put("DB_USER", environment.getProperty("spring.datasource.username", "postgres"));
        env.put("DB_PASSWORD", environment.getProperty("spring.datasource.password", "12345678"));
        env.put("DB_HOST", hostPort[0]);
        env.put("DB_PORT", hostPort[1]);

        // Scraper configuration
        env.put("BASE_URL", environment.getProperty("python.env.BASE_URL", "https://www.ppcbank.com.kh/"));
        env.put("CRAWL_DELAY", environment.getProperty("python.env.CRAWL_DELAY", "0.5"));
        env.put("MAX_DEPTH", environment.getProperty("python.env.MAX_DEPTH", "3"));
        env.put("MAX_CONTENT_LENGTH", environment.getProperty("python.env.MAX_CONTENT_LENGTH", "5000"));
        env.put("MAX_WORKERS", environment.getProperty("python.env.MAX_WORKERS", "5"));

        // Add PATH from system environment to ensure Python can find dependencies
        String systemPath = System.getenv("PATH");
        if (systemPath != null) {
            env.put("PATH", systemPath);
        }

        // Add PYTHONPATH if needed
        String pythonPath = System.getenv("PYTHONPATH");
        if (pythonPath != null) {
            env.put("PYTHONPATH", pythonPath);
        }

        // Set Python unbuffered output
        env.put("PYTHONUNBUFFERED", "1");

        // Log environment variables
        logger.info("📦 Environment variables passed to Python script:");
        env.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("DB_") ||
                        entry.getKey().startsWith("BASE_") ||
                        entry.getKey().startsWith("MAX_") ||
                        entry.getKey().equals("CRAWL_DELAY") ||
                        entry.getKey().equals("PYTHONUNBUFFERED"))
                .forEach(entry -> logger.info("   {} = {}", entry.getKey(), entry.getValue()));
    }

    private String extractDbName(String datasourceUrl) {
        try {
            return datasourceUrl.replaceAll(".*/([^/?]+).*", "$1");
        } catch (Exception e) {
            logger.warn("⚠️ Failed to parse DB name from URL, using default");
            return "web_scraper";
        }
    }

    private String[] extractHostAndPort(String datasourceUrl) {
        String host = "localhost";
        String port = "5433";

        try {
            String hostPortPart = datasourceUrl.split("://")[1].split("/")[0];
            String[] parts = hostPortPart.split(":");
            host = parts[0];
            if (parts.length > 1) {
                port = parts[1];
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to parse DB host/port from URL, using defaults: {}:{}", host, port);
        }

        return new String[]{host, port};
    }
}