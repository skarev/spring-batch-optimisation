package com.skarev.spring.batch.model

import java.math.BigDecimal
import java.time.LocalDate

data class StockRate(
        val date: LocalDate,
        val open: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal,
        val close: BigDecimal,
        val volume: Long
)