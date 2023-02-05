package ru.yandex.android.preinstall.provider;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DoubleCheckTest {

    @Mock
    Provider provider;

    @InjectMocks
    DoubleCheck doubleCheck;

    @Test
    public void testFirstGetCallAsksProviderForValue() {
        final Object providedObject = new Object();
        when(provider.get()).thenReturn(providedObject);

        final Object resultObject = doubleCheck.get();

        verify(provider).get();
        Assert.assertThat(resultObject, sameInstance(providedObject));
    }

    @Test
    public void testSecondAndPastGetCallsUseCachedValue() {
        when(provider.get()).thenReturn(new Object());

        doubleCheck.get();
        doubleCheck.get();
        doubleCheck.get();

        verify(provider, times(1)).get();
    }
}