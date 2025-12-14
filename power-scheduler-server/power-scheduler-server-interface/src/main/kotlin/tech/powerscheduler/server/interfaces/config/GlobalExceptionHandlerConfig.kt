package tech.powerscheduler.server.interfaces.config

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import tech.powerscheduler.common.dto.response.ResponseWrapper
import tech.powerscheduler.common.exception.BizException

/**
 * @author grayrat
 * @since 2025/5/09
 */
@RestControllerAdvice
internal class GlobalExceptionHandlerConfig {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandlerConfig::class.java)

    @ExceptionHandler(BizException::class)
    fun handleBizExceptionException(e: BizException): ResponseWrapper<Nothing> {
        val formatStackTraceInfo = e.stackTrace
            .filter { it.className.startsWith("tech.powerscheduler") }
            .joinToString("\n") { "\tat $it" }
        val fullStackTraceInfo = "${e::class.java.name}: ${e.message}\n" + formatStackTraceInfo
        log.info("occurred BizException: {}\n{}", e.message, fullStackTraceInfo)
        return error(message = e.message)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationExceptionException(e: ConstraintViolationException): ResponseWrapper<Nothing> {
        log.info("occurred ConstraintViolationException: {}", e.message)
        return error(message = e.message)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseWrapper<Nothing> {
        return error(
            message = "参数【${ex.name}】格式错误，期望类型：${ex.requiredType?.simpleName}"
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseWrapper<Nothing> {
        val errorMessage = e.bindingResult.fieldErrors.first().let {
            val fieldName = it.field
            val message = it.defaultMessage ?: "非法"
            return@let "[$fieldName]字段$message"
        }
        log.info("occurred MethodArgumentNotValidException: {}", errorMessage)
        return error(message = errorMessage)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseWrapper<Nothing> {
        log.error("occurred RuntimeException: {}", e.message, e)
        return error(message = e.message)
    }

    internal fun <T> error(message: String?) = ResponseWrapper<T>(
        data = null,
        success = false,
        code = "",
        message = message ?: ""
    )
}