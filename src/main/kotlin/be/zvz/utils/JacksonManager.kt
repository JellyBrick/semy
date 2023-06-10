package be.zvz.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JacksonManager {
    val jacksonObjectMapper = jacksonObjectMapper()
}