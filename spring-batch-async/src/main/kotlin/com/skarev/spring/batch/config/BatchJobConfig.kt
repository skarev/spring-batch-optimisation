package com.skarev.spring.batch.config

import com.skarev.spring.batch.model.StockRate
import com.skarev.spring.batch.steps.StocksReaderFactory
import mu.KotlinLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.integration.async.AsyncItemProcessor
import org.springframework.batch.integration.async.AsyncItemWriter
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import javax.sql.DataSource


@Configuration
@EnableBatchProcessing
class BatchJobConfig(private val jobBuilderFactory: JobBuilderFactory,
                     private val stepBuilderFactory: StepBuilderFactory,
                     @Value("\${processor.pool-size}") val processorPoolSize: Int) : DefaultBatchConfigurer() {

    private val logger = KotlinLogging.logger {}

    override fun setDataSource(dataSource: DataSource) {}

    @Bean
    fun readStep(stocksReaderFactory: StocksReaderFactory, threadPoolTaskExecutor: ThreadPoolTaskExecutor): Step {
        return stepBuilderFactory.get("step1")
                .chunk<StockRate, Future<StockRate>>(100)
                .reader(stocksReaderFactory.create())
                .processor(asyncItemProcessor(threadPoolTaskExecutor))
                .writer(asyncItemWriter())
                .build()
    }

    @Bean
    fun job(stocksReaderFactory: StocksReaderFactory, threadPoolTaskExecutor: ThreadPoolTaskExecutor): Job {
        return jobBuilderFactory.get("job")
                .listener(jobExecutionListener(threadPoolTaskExecutor))
                .start(readStep(stocksReaderFactory, threadPoolTaskExecutor))
                .build()
    }


    /**
     * Asynchronous item processor.
     * Allows to process number of customer verification in parallel
     */
    @Bean
    fun asyncItemProcessor(threadPoolTaskExecutor: ThreadPoolTaskExecutor): ItemProcessor<StockRate, Future<StockRate>> {
        logger.info { "Init batch processor with thread pool of size $processorPoolSize" }
        val asyncItemProcessor: AsyncItemProcessor<StockRate, StockRate> = AsyncItemProcessor()
        asyncItemProcessor.setDelegate {
            Thread.sleep(1)
            it
        }
        asyncItemProcessor.setTaskExecutor(threadPoolTaskExecutor)
        asyncItemProcessor.afterPropertiesSet()
        return asyncItemProcessor
    }

    /**
     * Asynchronous item writer.
     * Allows to take items for writing from asynchronous item processor
     */
    @Bean
    fun asyncItemWriter(): ItemWriter<Future<StockRate>> {
        val asyncItemWriter = AsyncItemWriter<StockRate>()
        asyncItemWriter.setDelegate {
            logger.info { it }
        }
        return asyncItemWriter
    }

    @Bean
    fun threadPoolTaskExecutor(): ThreadPoolTaskExecutor {
        val taskExecutor = ThreadPoolTaskExecutor()
        taskExecutor.maxPoolSize = processorPoolSize
        taskExecutor.corePoolSize = processorPoolSize
        taskExecutor.setQueueCapacity(processorPoolSize)
        taskExecutor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        taskExecutor.threadNamePrefix = "processor"
        taskExecutor.afterPropertiesSet()
        return taskExecutor
    }

    @Bean
    fun jobExecutionListener(threadPoolTaskExecutor: ThreadPoolTaskExecutor): JobExecutionListener {
        return object : JobExecutionListener {
            private val taskExecutor = threadPoolTaskExecutor
            override fun beforeJob(jobExecution: JobExecution?) {}
            override fun afterJob(jobExecution: JobExecution?) {
                taskExecutor.shutdown()
            }
        }
    }
}