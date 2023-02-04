package ru.yandex.vertis.telepony.util

/**
  * @author neron
  */
trait TestComponent {

  val component: Component = new Component {
    override def name: String = "test-component-name"
    override def hostName: String = "test-hostname"
    override def formatName: String = "test-format-name"
  }
}
object TestComponent extends TestComponent
