package be.zvz.semy.plugins

import be.zvz.clova.Language
import be.zvz.clova.LanguageSetting
import be.zvz.clova.Speed
import be.zvz.clova.tts.ClovaTTS
import be.zvz.semy.dto.UserInputResponse
import be.zvz.semy.dto.UserInput
import be.zvz.semy.utils.ClovaManager
import be.zvz.semy.utils.JacksonManager
import be.zvz.semy.utils.LLaMAManager
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import com.github.pemistahl.lingua.api.Language as LinguaLanguage
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration


private val notValidCloseReason = CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Not a valid item")
private val detector = LanguageDetectorBuilder.fromLanguages(
    LinguaLanguage.ENGLISH,
    LinguaLanguage.KOREAN,
    LinguaLanguage.JAPANESE,
    LinguaLanguage.CHINESE,
    LinguaLanguage.SPANISH,
    LinguaLanguage.FRENCH,
    LinguaLanguage.DUTCH,
    LinguaLanguage.RUSSIAN,
    LinguaLanguage.THAI
).build()

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
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
                } else if (frame is Frame.Text) {
                    val userInput = try {
                        JacksonManager.jacksonObjectMapper.readValue<UserInput>(frame.readText())
                    } catch (e: JsonProcessingException) {
                        close(notValidCloseReason)
                        throw e
                    }
                    if (userInput.command == "tts") {
                        val targetLanguage = detector.detectLanguageOf(userInput.text)
                        val speakers = ClovaTTS.SPEAKERS.getValue(
                            when (targetLanguage) {
                                LinguaLanguage.ENGLISH -> Language.ENGLISH
                                LinguaLanguage.KOREAN -> Language.KOREAN
                                LinguaLanguage.JAPANESE -> Language.JAPANESE
                                LinguaLanguage.CHINESE -> Language.SIMPLIFIED_CHINESE
                                LinguaLanguage.SPANISH -> Language.SPANISH
                                LinguaLanguage.FRENCH -> Language.FRENCH
                                LinguaLanguage.DUTCH -> Language.DUTCH
                                LinguaLanguage.RUSSIAN -> Language.RUSSIAN
                                LinguaLanguage.THAI -> Language.THAI
                                else -> Language.KOREAN
                            }
                        )
                        val ttsResult = ClovaManager.speech.doTextToSpeech(
                            userInput.text,
                            speakers.female.first(),
                            Speed.NORMAL,
                        )
                        outgoing.send(Frame.Binary(true, ttsResult))
                    } else {
                        data = userInput.text
                    }
                }

                if (data.isNotBlank()) {
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
}
