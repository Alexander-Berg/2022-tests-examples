package com.yandex.mail.util;

import com.yandex.mail.util.mailbox.MailboxEditor;

import java.util.Arrays;
import java.util.Collection;

import androidx.annotation.NonNull;

import static com.yandex.mail.util.mailbox.Mailbox.nonThreaded;
import static com.yandex.mail.util.mailbox.Mailbox.threaded;

/**
 * Base class for test class with parameterized and non parameterized tests.
 *
 * In order parameterized worked as expected you should write (just copy it from doc):
 * <pre>
 * {@code
 * @literal @ParameterizedRobolectricTestRunner.Parameters(name = PARAMETERIZED_PLACEHOLDER)
 *     public static Collection<Object[]> data() {
 *         return threadedParameters;
 *     }
 * }
 * </pre>
 */
public class BaseParameterizedTest extends BaseIntegrationTest {

    @NonNull
    protected static final String PARAMETERIZED_PLACEHOLDER = "Threaded = {0}";

    protected static final boolean NON_THREADED = false;

    protected static final boolean THREADED = true;

    @NonNull
    protected static final Collection<Object[]> threadedParameters = Arrays.asList(new Object[][]{{NON_THREADED}, {THREADED}});

    @NonNull
    protected MailboxEditor editor;

    protected boolean threaded;

    protected void setUp(boolean threaded) {
        this.editor = threaded ? threaded(this) : nonThreaded(this);
        this.threaded = threaded;
    }
}
