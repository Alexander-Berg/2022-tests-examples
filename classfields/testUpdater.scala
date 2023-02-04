#!/bin/sh
exec /home/aborunov/scala-2.11.8/bin/scala -J-Xmx10G -savecompiled -deprecation "$0" "$@"
!#

import scala.sys.process._
import scala.concurrent.duration._

while(true) {
  val start = new java.util.Date()
  println(start.toString + " starting user reply delays update in test")
  val result = "./usersReplyDelaysUpdaterTest.scala".!
  val end = new java.util.Date()
  println(end.toString + " " + (end.getTime - start.getTime) + " " + "./usersReplyDelaysUpdaterTest.scala: " + result)
  Thread.sleep(1.minute.toMillis)
}
