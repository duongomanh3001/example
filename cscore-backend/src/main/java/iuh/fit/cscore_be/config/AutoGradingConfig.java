package iuh.fit.cscore_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for auto-grading system
 * Enables async processing and configures thread pools
 */
@Configuration
@EnableAsync
public class AutoGradingConfig {
    
    /**
     * Thread pool for auto-grading tasks
     * Separate from main application thread pool
     */
    @Bean(name = "autoGradingExecutor")
    public Executor autoGradingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AutoGrading-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    /**
     * Thread pool for code compilation tasks
     */
    @Bean(name = "compilationExecutor")
    public Executor compilationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Compilation-");
        executor.setKeepAliveSeconds(30);
        executor.initialize();
        return executor;
    }
    
    /**
     * Thread pool for code execution tasks
     */
    @Bean(name = "executionExecutor")
    public Executor executionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Execution-");
        executor.setKeepAliveSeconds(45);
        executor.initialize();
        return executor;
    }
}
