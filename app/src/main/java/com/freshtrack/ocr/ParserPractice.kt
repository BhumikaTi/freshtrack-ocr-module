package com.freshtrack.ocr

fun main() {

    val text = """
    * 174/-, 2.05/g
    #07/25 B02 @ 06/28
""".trimIndent()


    val expiryDate = ExpiryParser.extractExpiryDate(text)

    println("Expiry Date: $expiryDate")
}