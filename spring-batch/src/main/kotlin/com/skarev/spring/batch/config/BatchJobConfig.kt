package com.skarev.spring.batch.config

import com.skarev.spring.batch.model.StockRate
import com.skarev.spring.batch.steps.StocksReaderFactory
import mu.KotlinLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@EnableBatchProcessing
class BatchJobConfig(private val jobBuilderFactory: JobBuilderFactory,
                     private val stepBuilderFactory: StepBuilderFactory): DefaultBatchConfigurer() {

    private val logger = KotlinLogging.logger {}

    override fun setDataSource(dataSource: DataSource) {}

    @Bean
    fun readStep(stocksReaderFactory: StocksReaderFactory): Step {
        return stepBuilderFactory.get("step1")
                .chunk<StockRate, StockRate>(100)
                .reader(stocksReaderFactory.create())
                .processor(ItemProcessor { it ->
                    Thread.sleep(1)
                    it
                })
                .writer {
                    logger.info { it }
                }
                .build()
    }

    @Bean
    fun job(stocksReaderFactory: StocksReaderFactory): Job {
        return jobBuilderFactory.get("job")
                .start(readStep(stocksReaderFactory))
                .build()
    }

}