package ru.yandex.maps;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import androidx.annotation.CallSuper;

@RunWith(MapsTestsRunner.class)
public abstract class BaseTest {

    @CallSuper
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
}
