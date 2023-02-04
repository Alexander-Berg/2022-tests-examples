package ru.auto.salesman.test.docker

import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork

object TestDocker {

  val network: Network = newNetwork()
}
