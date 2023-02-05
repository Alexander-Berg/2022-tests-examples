package com.yandex.launcher;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DoNothingAnswer<T> implements Answer<T> {
    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        return null;
    }
}
