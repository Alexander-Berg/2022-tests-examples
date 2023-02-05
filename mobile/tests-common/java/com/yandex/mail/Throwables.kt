package com.yandex.mail

class TestRuntimeException(message: String = "Synthetic exception for test purpose") : RuntimeException(message)
class TestError(message: String = "Synthetic error for test purpose") : Error(message)
