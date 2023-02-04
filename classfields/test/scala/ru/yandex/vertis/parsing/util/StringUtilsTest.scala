package ru.yandex.vertis.parsing.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import ru.yandex.vertis.parsing.util.StringUtils.RichString

/**
  * Created by andrey on 1/18/18.
  */
@RunWith(classOf[JUnitRunner])
class StringUtilsTest extends FunSuite with OptionValues {
  test("extractCyrillicName") {
    assert(StringUtils.extractCyrillicName(" Петр\t\n  Алексеевич   ").value == "Петр Алексеевич")
    assert(StringUtils.extractCyrillicName(" Петр\t\n Алексеевич   abc").isEmpty)
    assert(StringUtils.extractCyrillicName("142").isEmpty)
    assert(StringUtils.extractCyrillicName("\t\n   \t").isEmpty)
    assert(StringUtils.extractCyrillicName("\n\t\tООО \"УАЗ Центр\"\t").value == "ООО УАЗ Центр")
    assert(StringUtils.extractCyrillicName("ООО ЮРТО-ТРАК").isEmpty)

  }

  test("noCommas") {
    assert(StringUtils.noCommas(" Петр,  \"Алексеевич\";   ") == "Петр Алексеевич")
  }

  test("containsIgnoreCase") {
    val str = "бензин, 1.4, ГБО"
    assert(str.containsIgnoreCase("гбо"))
    assert(str.containsIgnoreCase("БЕНЗИН"))
  }

  test("trimSlashes") {
    assert(StringUtils.trimSlashes("abc") == "abc")
    assert(StringUtils.trimSlashes("abc/") == "abc")
    assert(StringUtils.trimSlashes("/abc") == "abc")
    assert(StringUtils.trimSlashes("abc//") == "abc")
    assert(StringUtils.trimSlashes("//abc") == "abc")
    assert(StringUtils.trimSlashes("/abc/") == "abc")
    assert(StringUtils.trimSlashes("/abc//") == "abc")
    assert(StringUtils.trimSlashes("//abc/") == "abc")
    assert(StringUtils.trimSlashes("//abc//") == "abc")
  }

  test("ensureLastSlash") {
    assert(StringUtils.ensureLastSlash("abc") == "abc/")
    assert(StringUtils.ensureLastSlash("abc/") == "abc/")
    assert(StringUtils.ensureLastSlash("abc//") == "abc/")
  }

  test("ensureNoLastSlash") {
    assert(StringUtils.ensureNoLastSlash("abc") == "abc")
    assert(StringUtils.ensureNoLastSlash("abc/") == "abc")
    assert(StringUtils.ensureNoLastSlash("abc//") == "abc")
    assert("abc//".noSlash == "abc")
  }

  test("ensureFirstSlash") {
    assert(StringUtils.ensureFirstSlash("abc") == "/abc")
    assert(StringUtils.ensureFirstSlash("/abc") == "/abc")
    assert(StringUtils.ensureFirstSlash("//abc") == "/abc")
  }

  test("ensureNoFirstSlash") {
    assert(StringUtils.ensureNoFirstSlash("abc") == "abc")
    assert(StringUtils.ensureNoFirstSlash("/abc") == "abc")
    assert(StringUtils.ensureNoFirstSlash("//abc") == "abc")
  }

  test("normalizeSlashes") {
    assert(StringUtils.normalizeSlashes("abc/def") == "/abc/def")
    assert(StringUtils.normalizeSlashes("/abc/def") == "/abc/def")
    assert(StringUtils.normalizeSlashes("abc/def/") == "/abc/def")
    assert(StringUtils.normalizeSlashes("abc//def") == "/abc/def")
    assert(StringUtils.normalizeSlashes("/abc//def") == "/abc/def")
    assert(StringUtils.normalizeSlashes("//abc//def") == "/abc/def")
    assert(StringUtils.normalizeSlashes("abc//def/") == "/abc/def")
    assert(StringUtils.normalizeSlashes("abc//def//") == "/abc/def")
    assert(StringUtils.normalizeSlashes("//abc/def//") == "/abc/def")
    assert(StringUtils.normalizeSlashes("//abc//def//") == "/abc/def")
  }

  test("ensureSlash") {
    assert(StringUtils.ensureSlashBetween("abc", "def") == "abc/def")
    assert(("abc".slash("def")) == "abc/def")
    assert(StringUtils.ensureSlashBetween("abc/", "def") == "abc/def")
    assert(StringUtils.ensureSlashBetween("abc", "/def") == "abc/def")
    assert(StringUtils.ensureSlashBetween("abc/", "/def") == "abc/def")

    assert(StringUtils.ensureSlashBetween("abc//", "def") == "abc/def")
    assert(StringUtils.ensureSlashBetween("abc", "//def") == "abc/def")
    assert(StringUtils.ensureSlashBetween("abc//", "/def") == "abc/def")
    assert(StringUtils.ensureSlashBetween("abc/", "//def") == "abc/def")
    assert(StringUtils.ensureSlashBetween("abc//", "//def") == "abc/def")

    assert(StringUtils.ensureSlashBetween("//abc//", "//def//") == "//abc/def//")
  }
}
