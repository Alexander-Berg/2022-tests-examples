package tools.docker.test

import java.lang.management.ManagementFactory
import scala.jdk.CollectionConverters._

object TestDockerApp extends App {
  Debug.print(args, getClass)
}

object AlternativeMain extends App {
  Debug.print(args, getClass)
}

object Debug {

  def print(args: Array[String], caller: Class[_]): Unit = {
    val runtimeMXBean = ManagementFactory.getRuntimeMXBean
    val jvmArgs = runtimeMXBean.getInputArguments.asScala

    println("Main: " + caller.getSimpleName)
    println("Args: " + (if (args.isEmpty) "<empty>" else args.mkString(" ")))
    jvmArgs.foreach(arg => println("Jvm flag: " + arg))
  }
}
