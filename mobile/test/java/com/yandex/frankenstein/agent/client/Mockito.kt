package com.yandex.frankenstein.agent.client

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

fun <T> any(): T = Mockito.any<T>()

@Suppress("FunctionMinLength")
fun <T> eq(value: T?): T = Mockito.eq<T>(value)

fun <T> refEq(value: T?): T = Mockito.refEq<T>(value)

fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()
