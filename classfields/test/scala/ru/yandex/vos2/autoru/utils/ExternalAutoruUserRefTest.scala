package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.util.ExternalAutoruUserRef

/**
  * Created by andrey on 9/14/16.
  */
@RunWith(classOf[JUnitRunner])
class ExternalAutoruUserRefTest extends AnyFunSuite with OptionValues {
  test("testUserRefParser") {
    assert(ExternalAutoruUserRef.fromExt("user:12345").value == UserRef.refAid(12345))
    assert(ExternalAutoruUserRef.fromExt("dealer:12345").value == UserRef.refAutoruClient(12345))
    assert(ExternalAutoruUserRef.fromExt("pewpew:12345").isEmpty)
    assert(ExternalAutoruUserRef.fromExt("fgsfds").isEmpty)
    assert(ExternalAutoruUserRef.fromExt("12345").isEmpty)
    assert(ExternalAutoruUserRef.fromExt("anon:100600").value == UserRef.refAnon("100600"))
    assert(ExternalAutoruUserRef.fromUserRef("a_12345").contains("user:12345"))
    assert(ExternalAutoruUserRef.fromUserRef("ac_12345").contains("dealer:12345"))
    assert(ExternalAutoruUserRef.fromUserRef("x_12345").isEmpty)
    assert(ExternalAutoruUserRef.fromUserRef("anon_12345").contains("anon:12345"))
  }
}
