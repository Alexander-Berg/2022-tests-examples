package ru.yandex.vertis.chat.util.versionchecker

import org.scalatest.FunSuite

/**
  * TODO
  *
  * @author aborunov
  */
class VersionCheckerTest extends FunSuite {

  val acceptedVersions: Map[Platform, Version] = Map(
    Platform.Android -> Version(6, 0, 0, 0),
    Platform.Ios -> Version(9, 0, 0, 8970),
    Platform.Frontend -> Version.Disabled
  )

  val acceptedVersionsWithFrontend: Map[Platform, Version] = Map(
    Platform.Android -> Version(6, 0, 0, 0),
    Platform.Ios -> Version(9, 0, 0, 8970),
    Platform.Frontend -> Version.Undefined
  )

  test("versions") {
    assert(VersionChecker.checkVersion("[ANDROID 6.0.0]", acceptedVersions))
    assert(VersionChecker.checkVersion("[ANDROID 6.0.1]", acceptedVersions))
    assert(VersionChecker.checkVersion("[ANDROID 6.1.0]", acceptedVersions))
    assert(VersionChecker.checkVersion("[ANDROID 7.0.0]", acceptedVersions))
    assert(VersionChecker.checkVersion("[ANDROID 7.0.1]", acceptedVersions))
    assert(VersionChecker.checkVersion("[ANDROID 7.1.1]", acceptedVersions))
    assert(!VersionChecker.checkVersion("[ANDROID 5.8.0]", acceptedVersions))
    assert(!VersionChecker.checkVersion("[ANDROID 5.8.1]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 9.0.0.8970]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 9.0.0.8971]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 9.0.1.8970]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 9.1.0.8970]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 10.0.0.8970]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 10.0.0.8971]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 10.0.1.8971]", acceptedVersions))
    assert(VersionChecker.checkVersion("[IOS 10.1.1.8971]", acceptedVersions))
    assert(!VersionChecker.checkVersion("[IOS 8.5.1.7326]", acceptedVersions))
    assert(!VersionChecker.checkVersion("[IOS 8.8.0.8370]", acceptedVersions))
    assert(!VersionChecker.checkVersion("[IOS 8.9.0.8732]", acceptedVersions))
    assert(!VersionChecker.checkVersion("[FRONTEND]", acceptedVersions))
    assert(VersionChecker.checkVersion("[FRONTEND]", acceptedVersionsWithFrontend))
    assert(!VersionChecker.checkVersion("bla bla", acceptedVersions))
    assert(!VersionChecker.checkVersion("blabla", acceptedVersions))
  }
}
