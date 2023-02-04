package com.yandex.maps.testapp.mapdesign

open class MapDesignError(hint: String, message: String) :
        Error("$hint\nExplanation: $message")

class CertificateError(message: String) :
        MapDesignError("Check YandexInternalRootCA in user trusted certificates" +
                "(See info here: https://wiki.yandex-team.ru/security/ssl/sslclientfix/#vandroid)", message)

class ConnectionError(message: String) :
        MapDesignError("Failed to get data from server.", message)
