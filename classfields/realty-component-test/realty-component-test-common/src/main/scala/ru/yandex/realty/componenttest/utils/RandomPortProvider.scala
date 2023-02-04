package ru.yandex.realty.componenttest.utils

import java.net.ServerSocket

trait RandomPortProvider {

  def getRandomPort(): Int = {
    var serverSocket: ServerSocket = null
    try {
      serverSocket = new ServerSocket(0)
      serverSocket.getLocalPort
    } finally {
      if (serverSocket != null) {
        serverSocket.close()
      }
    }
  }

}
