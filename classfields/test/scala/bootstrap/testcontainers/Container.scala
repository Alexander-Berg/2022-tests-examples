package bootstrap.testcontainers

import org.testcontainers.containers.GenericContainer

trait Container[T <: GenericContainer[T]] {
  protected def container: T
}
