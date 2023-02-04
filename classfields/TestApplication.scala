package ru.yandex.vertis.feedprocessor.app

import com.typesafe.config.{Config, ConfigValueFactory}

/**
  * For tests only!!!
  */
trait TestApplication extends Application {

  override def environment: Environment =
    new DefaultEnvironment {
      override lazy val environmentType: String = "development"

      override lazy val config: Config =
        new DefaultEnvironment().config
          .withValue("akka.stream.materializer.debug.fuzzing-mode", ConfigValueFactory.fromAnyRef("on"))
    }

  private lazy val startHooks = collection.mutable.ListBuffer.empty[() => Unit]
  private lazy val stopHooks = collection.mutable.ListBuffer.empty[() => Unit]

  override def onStart(action: => Unit): Unit = synchronized(startHooks += (() => action))
  override def onStop(action: => Unit): Unit = synchronized(stopHooks += (() => action))

  def start(): Unit = synchronized(startHooks.foreach(_.apply()))
  def stop(): Unit = synchronized(stopHooks.foreach(_.apply()))
}
