package ru.yandex.payments.tvmlocal.testing;

import java.net.ServerSocket;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class Utils {
    @SneakyThrows
    public static int selectRandomPort() {
        try (val socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
