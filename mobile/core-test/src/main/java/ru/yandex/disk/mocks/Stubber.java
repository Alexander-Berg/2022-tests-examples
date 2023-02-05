package ru.yandex.disk.mocks;

import static org.mockito.Mockito.mock;

public class Stubber {
    public static <T> T stub(Class<T> classToMock) {
        return mock(classToMock);
    }
}
