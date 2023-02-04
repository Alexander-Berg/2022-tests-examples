package ru.yandex.vertis.banker

case class ArtificialException(msg: String = "artificial") extends RuntimeException(msg)
