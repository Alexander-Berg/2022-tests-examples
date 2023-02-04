package tools.docker.test

import com.google.devtools.build.runfiles.Runfiles
import org.scalatest.wordspec.AnyWordSpec
import scala.sys.process._
import org.scalatest.matchers.should.Matchers._

class ScalaImageTest extends AnyWordSpec {

  val runfiles: Runfiles = Runfiles.create()

  def runScalaImage(target: String, env: (String, String)*): String = {
    val command = runfiles.rlocation("verticals_backend/" + target.replace(':', '/') + ".executable")
    println("Loading image: " + Seq(command, "--norun").!!)
    val dockerCommand =
      Seq("docker", "run") ++ env.flatMap(e => Seq("-e", e._1 + "=" + e._2)) ++ Seq("--rm", s"bazel/$target")
    println(s"Running: $command")
    dockerCommand.!!
  }

  "scala_image" should {
    "inherit attributes from scala_binary" in {
      val out = runScalaImage("tools/docker/test:test-image-base")
      out should include("Main: TestDockerApp$")
      out should include("Args: <empty>")
      out should include("Jvm flag: -Xmx128m")
    }

    "can override jvm_flags" in {
      val out = runScalaImage("tools/docker/test:test-image-jvm-flags")
      out should include("Main: TestDockerApp$")
      out should include("Args: <empty>")
      out should include("Jvm flag: -Xmx129m")
    }

    "can override args" in {
      val out = runScalaImage("tools/docker/test:test-image-args")
      out should include("Main: TestDockerApp$")
      out should include("Args: ab bc cd")
      out should include("Jvm flag: -Xmx128m")
    }

    "can override main_class" in {
      val out = runScalaImage("tools/docker/test:test-image-main-class")
      out should include("Main: AlternativeMain$")
      out should include("Args: <empty>")
      out should include("Jvm flag: -Xmx128m")
    }

    "override jvm_flags via JVM_FLAGS env" in {
      val out = runScalaImage("tools/docker/test:test-image-base", "JVM_FLAGS" -> "-Xmx129m")
      out should include("Main: TestDockerApp$")
      out should include("Args: <empty>")
      out should include("Jvm flag: -Xmx128m")
      out should include("Jvm flag: -Xmx129m")
      out.indexOf("Jvm flag: -Xmx129m") shouldBe >(out.indexOf("Jvm flag: -Xmx128m"))
    }

    "expand_location in args" in {
      val out = runScalaImage("tools/docker/test:test-image-expand-args")
      out should include("Main: TestDockerApp$")
      out should include("Args: tools/docker/test/test.txt")
      out should include("Jvm flag: -Xmx128m")
    }

    "use heap dump paths from env and jvm_flags" in {
      val out = runScalaImage(
        "tools/docker/test:test-image-heap-dumps",
        ("_DEPLOY_HPROF_DIRECTORY" -> "/alloc/logs/heapdump")
      )
      val defaultFlag = "Jvm flag: -XX:HeapDumpPath=/alloc/logs/heapdump"
      val customFlag = "Jvm flag: -XX:HeapDumpPath=/alloc/custom"
      out should include(defaultFlag)
      out should include(customFlag)
      out.indexOf(customFlag) shouldBe >(out.indexOf(defaultFlag))
    }

    "not include -XX:HeapDumpPath if there is no env variable nor jvm flag" in {
      val out = runScalaImage("tools/docker/test:test-image-base")
      val defaultFlag = "Jvm flag: -XX:HeapDumpPath="
      (out should not).include(defaultFlag)
    }
  }
}
