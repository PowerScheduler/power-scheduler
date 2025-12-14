package tech.powerscheduler.server.application.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JSON {

    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    inline fun <reified T> readValue(json: String?): T? {
        return objectMapper.readValue<T>(json, T::class.java)
    }

    inline fun <reified T> readValueList(json: String?): List<T> {
        val typeFactory = TypeFactory.defaultInstance()
        val javaType = typeFactory.constructCollectionType(List::class.java, T::class.java)
        return objectMapper.readValue(json, javaType)
    }

    fun writeValueAsString(o: Any): String {
        return objectMapper.writeValueAsString(o)
    }

    fun splitJsonArrayToObjectStrings(jsonArrayStr: String?): List<String> {
        if (jsonArrayStr.isNullOrBlank()) {
            return emptyList()
        }
        val root = objectMapper.readTree(jsonArrayStr)
        return if (root.isArray) {
            root.map { objectMapper.writeValueAsString(it) }
        } else {
            emptyList()
        }
    }

}