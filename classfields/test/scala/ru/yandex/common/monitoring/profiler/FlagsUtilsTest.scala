package ru.yandex.common.monitoring.profiler

import org.scalatest.FunSuite

/**
  * Created by sievmi on 28.03.19  
  */

class FlagsUtilsTest extends FunSuite {

  test("no params") {
    val res = FlagUtils.requestParamsToFlagsString(Map("z" -> ""))
    assert(res === Right(""))
  }

  test("-e alloc -t -d 120") {
    val params = Map("e" -> "alloc", "d" -> "120", "t" -> "")
    val res = FlagUtils.requestParamsToFlagsString(params)

    assert(res === Right("-d 120 -e alloc -t"))
  }

  test("not supported event") {
    val params = Map("e" -> "zzz", "d" -> "120", "t" -> "")
    val res = FlagUtils.requestParamsToFlagsString(params)

    assert(res === Left("Found 1 errors: e flag expect value one of alloc, lock, itimer, wall, cpu, cache-misses"))
  }

  test("several errors") {
    val params = Map("e" -> "zzz", "d" -> "1000000000", "t" -> "")
    val res = FlagUtils.requestParamsToFlagsString(params)

    assert(res === Left("Found 2 errors: " +
      "d flag expect integer between 1 and 1800; " +
      "e flag expect value one of alloc, lock, itimer, wall, cpu, cache-misses"
      ))
  }
}
