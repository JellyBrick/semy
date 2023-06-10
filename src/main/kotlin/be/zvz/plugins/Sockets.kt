package be.zvz.plugins

import be.zvz.clova.Language
import be.zvz.clova.LanguageSetting
import be.zvz.dto.LLaMAResponse
import be.zvz.utils.ClovaManager
import be.zvz.utils.JacksonManager
import be.zvz.utils.LLaMAManager
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
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
            val multipart = call.receiveMultipart()
            var data = ""
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "text") {
                            data = part.value
                        } else {
                            close(notValidCloseReason)
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "image") {
                            val ocrResult = ClovaManager.ocr.doOCR(
                                LanguageSetting(Language.AUTO, Language.KOREAN),
                                part.streamProvider().readBytes()
                            )

                            data = buildString {
                                ocrResult.ocrs?.forEach {
                                    append(it.text)
                                    append(" ")
                                }
                            }
                        }
                    }
                    else -> close(notValidCloseReason)
                }
                part.dispose()
            }

            val inferenceResult = LLaMAManager.inference(data)
            outgoing.send(
                Frame.Text(
                    JacksonManager.jacksonObjectMapper.writeValueAsString(
                        LLaMAResponse(
                            inferenceResult,
                        ),
                    ),
                ),
            )

            // TODO: Implement additional prompt
            /*
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val additionalPrompt = frame.readText()
                }
            }
            */
        }
    }
}
