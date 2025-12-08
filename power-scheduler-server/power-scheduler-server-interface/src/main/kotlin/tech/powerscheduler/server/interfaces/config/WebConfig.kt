package tech.powerscheduler.server.interfaces.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(EnhancedLocalDateTimeConverter)
    }

    object EnhancedLocalDateTimeConverter : Converter<String, LocalDateTime> {
        val dateOnlyFormatParser = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter()
        private val normalFormatters = listOf(
            dateOnlyFormatParser,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )

        override fun convert(source: String): LocalDateTime? {
            if (source.isBlank()) return null

            val s = source.trim()
            // ① Spring 默认：ISO_LOCAL_DATE_TIME
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {}

            // ② Spring 默认：ISO_DATE_TIME（支持带时区）
            try {
                return OffsetDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
            } catch (_: Exception) {}

            try {
                return ZonedDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
            } catch (_: Exception) {}

            // ③ 毫秒 or 秒 时间戳
            if (s.matches(Regex("^\\d{10,13}$"))) {
                val epoch = s.toLong()
                val instant = if (s.length == 10)
                    Instant.ofEpochSecond(epoch)
                else
                    Instant.ofEpochMilli(epoch)

                return instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
            }

            // ④ 你的自定义格式 yyyy-MM-dd HH:mm:ss
            for (formatter in normalFormatters) {
                try {
                    return LocalDateTime.parse(s, formatter)
                } catch (_: Exception) {}
            }
            throw IllegalArgumentException("无法解析时间格式: $source")
        }
    }
}