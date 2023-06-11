package be.zvz.semy.utils

import be.zvz.clova.Clova

object ClovaManager {
    private val clova = Clova()
    val ocr = clova.ocr.getValue(Clova.OCR.PAPAGO)
    val speech = clova.tts.getValue(Clova.TTS.CLOVA)
    val translate = clova.translation.getValue(Clova.Translation.N2MT)
}