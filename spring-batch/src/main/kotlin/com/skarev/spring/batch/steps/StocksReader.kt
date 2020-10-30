package com.skarev.spring.batch.steps

import com.skarev.spring.batch.model.StockRate
import mu.KotlinLogging
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.MultiResourceItemReader
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Component
class StocksReaderFactory(@Value("classpath:/stocks/*.us.txt") private val stocksDataFiles: Array<Resource>) {

    private val logger = KotlinLogging.logger {}

    fun create(): MultiResourceItemReader<StockRate> {

        logger.info { "Read ${stocksDataFiles.size} files" }
        val reader = MultiResourceItemReader<StockRate>()
        reader.setResources(stocksDataFiles)
        reader.setDelegate(reader())
        return reader
    }

    fun reader(): FlatFileItemReader<StockRate> {
        val tokenizer = DelimitedLineTokenizer()
        tokenizer.setNames("Date", "Open", "High", "Low", "Close", "Volume", "OpenInt")
        val customerLineMapper: DefaultLineMapper<StockRate> = DefaultLineMapper<StockRate>()
        customerLineMapper.setLineTokenizer(tokenizer)
        customerLineMapper.setFieldSetMapper(StockRateFieldsMapper())
        customerLineMapper.afterPropertiesSet()
        val reader: FlatFileItemReader<StockRate> = FlatFileItemReader<StockRate>()
        reader.setLineMapper(customerLineMapper)
        reader.setLinesToSkip(1)
        return reader
    }
}

class StockRateFieldsMapper : FieldSetMapper<StockRate> {
    override fun mapFieldSet(fieldSet: FieldSet): StockRate {

        return StockRate(
                LocalDate.parse(fieldSet.readString("Date")),
                BigDecimal(fieldSet.readString("Open")),
                BigDecimal(fieldSet.readString("High")),
                BigDecimal(fieldSet.readString("Low")),
                BigDecimal(fieldSet.readString("Close")),
                fieldSet.readLong("Volume")
        )
    }

}
