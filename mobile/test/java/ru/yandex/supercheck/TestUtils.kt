package ru.yandex.supercheck

import java.io.BufferedReader

fun Any.readFile(fileName: String): String = this::class.java.classLoader
    ?.getResourceAsStream(fileName)
    ?.bufferedReader()
    ?.use(BufferedReader::readText)
    ?: throw IllegalArgumentException("File $fileName not found")