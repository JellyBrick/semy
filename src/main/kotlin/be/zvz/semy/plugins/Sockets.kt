package be.zvz.semy.plugins

import be.zvz.clova.Language
import be.zvz.clova.LanguageSetting
import be.zvz.semy.dto.UserInputResponse
import be.zvz.semy.dto.UserInput
import be.zvz.semy.utils.ClovaManager
import be.zvz.semy.utils.JacksonManager
import be.zvz.semy.utils.LLaMAManager
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val notValidCloseReason = CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Not a valid item")
        webSocket("/inference") { // websocketSession
            for (frame in incoming) {
                var data = ""
                if (frame is Frame.Binary) {
                    val ocrResult = ClovaManager.ocr.doOCR(
                        LanguageSetting(Language.AUTO, Language.KOREAN),
                        frame.readBytes(),
                    )

                    data = buildString {
                        println(ocrResult)
                        ocrResult.ocrs?.forEach {
                            append(it.text)
                            append(" ")
                        }
                    }
                    println(data)
                } else if (frame is Frame.Text) {
                    data = try {
                        JacksonManager.jacksonObjectMapper.readValue<UserInput>(frame.readText()).text
                    } catch (e: JsonProcessingException) {
                        close(notValidCloseReason)
                        throw e
                    }
                }

                val inferenceResult = LLaMAManager.inference(data)
                outgoing.send(
                    Frame.Text(
                        JacksonManager.jacksonObjectMapper.writeValueAsString(
                            UserInputResponse(
                                inferenceResult.response,
                            ),
                        ),
                    ),
                )
            }
        }
    }
}
