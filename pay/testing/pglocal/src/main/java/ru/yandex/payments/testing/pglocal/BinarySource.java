package ru.yandex.payments.testing.pglocal;

import java.nio.file.Path;

public interface BinarySource {
    Path fetch();
}
