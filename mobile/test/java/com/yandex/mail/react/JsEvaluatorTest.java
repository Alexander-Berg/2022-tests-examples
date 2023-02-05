package com.yandex.mail.react;

import android.os.Handler;
import android.webkit.WebView;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(IntegrationTestRunner.class)
public class JsEvaluatorTest {

    @Test
    public void postRunnableToJsQueue() {
        WebView webView = mock(WebView.class);
        Handler jsQueueHandler = mock(Handler.class);
        JsEvaluator jsEvaluator = new JsEvaluator(webView, jsQueueHandler);

        verifyNoInteractions(jsQueueHandler);

        Runnable runnable = mock(Runnable.class);
        jsEvaluator.postRunnableToJsQueue(runnable);
        verify(jsQueueHandler).post(runnable);
        verifyNoInteractions(webView, runnable);
    }

    @Test
    public void clearJsQueue() {
        WebView webView = mock(WebView.class);
        Handler jsQueueHandler = mock(Handler.class);
        JsEvaluator jsEvaluator = new JsEvaluator(webView, jsQueueHandler);

        verifyNoInteractions(jsQueueHandler);

        Runnable runnable = mock(Runnable.class);
        jsEvaluator.postRunnableToJsQueue(runnable);
        verify(jsQueueHandler).post(runnable);
        verifyNoMoreInteractions(jsQueueHandler);

        jsEvaluator.clearJsQueue();
        verify(jsQueueHandler).removeCallbacks(runnable);

        verifyNoInteractions(webView, runnable);
    }

    @Test
    public void evaluateFormattedJsDirectly() {
        WebView webView = mock(WebView.class);
        Handler jsQueueHandler = mock(Handler.class);
        JsEvaluator jsEvaluator = new JsEvaluator(webView, jsQueueHandler);

        verifyNoInteractions(jsQueueHandler);
        jsEvaluator.evaluateFormattedJsDirectly("someJs");
        verify(jsQueueHandler).post(any(Runnable.class));
        verifyNoInteractions(webView);
    }

    @Test
    public void evaluateJsFunction() {
        WebView webView = mock(WebView.class);
        Handler jsQueueHandler = mock(Handler.class);
        JsEvaluator jsEvaluator = new JsEvaluator(webView, jsQueueHandler);

        verifyNoInteractions(jsQueueHandler);
        jsEvaluator.evaluateJsFunction("someJsFunctionName", new String[]{"param1AsJson", "param2AsJson"});
        verify(jsQueueHandler).post(any(Runnable.class));
        verifyNoInteractions(webView);
    }
}
