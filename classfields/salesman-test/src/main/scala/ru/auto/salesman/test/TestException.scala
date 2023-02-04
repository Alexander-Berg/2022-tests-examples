package ru.auto.salesman.test

import java.util.UUID

// Тегируем во избежание подобных непонятных падений:
//
// val e1 = new TestException
// val e2 = new TestException
// List(e1, e2) shouldBe List(e2, e1)
//
// падает с List(TestException, TestException) was not equal to List(TestException, Exception)
class TestException(tag: String = UUID.randomUUID().toString) extends Exception(tag)
