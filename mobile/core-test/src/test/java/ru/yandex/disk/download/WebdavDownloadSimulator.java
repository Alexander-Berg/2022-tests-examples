package ru.yandex.disk.download;

import com.yandex.disk.client.CustomHeader;
import com.yandex.disk.client.DownloadListener;
import com.yandex.disk.client.exceptions.CancelledDownloadException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.disk.remote.webdav.WebdavClient;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

public class WebdavDownloadSimulator {

    private final Queue<DownloadAnswer> answers = new LinkedList<DownloadAnswer>();

    public WebdavDownloadSimulator(WebdavClient webdav) throws Exception {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String path = (String) invocation.getArguments()[0];
                @SuppressWarnings("unchecked")
                List<CustomHeader> headers = (List<CustomHeader>) invocation.getArguments()[1];
                DownloadListener listener = (DownloadListener) invocation.getArguments()[2];
                DownloadAnswer answer = answers.remove();
                answer.answer(path, headers, listener);
                return null;
            }
        }).when(webdav).downloadFile(anyString(), anyList(), anyDownloadListener());
    }

    public void prepareWebdavToOkResponse() throws Exception {
        DownloadAnswer answer = new DownloadAnswer();
        answer.setEtag("ETAG");
        answers.add(answer);
    }

    private static DownloadListener anyDownloadListener() {
        return any(DownloadListener.class);
    }

    public void addAnswer(DownloadAnswer answer) {
        answers.add(answer);
    }

    public static class DownloadAnswer {

        private Exception exceptionBeforeDowloading;
        private String etag;
        private String data;
        private Runnable actionDuringDownload;
        private long expectedLocalLength = -1;
        private String expectedEtag;
        private String contentType;
        private long contentLength = -1;
        private Exception exceptionDuringDownloading;
        private boolean checkRequest = true;

        private void answer(String path, List<CustomHeader> headers, DownloadListener downloadListener) throws Exception {
            if (checkRequest) {
                assertThat(downloadListener.getLocalLength(), equalTo(expectedLocalLength));
                assertThat(downloadListener.getETag(), equalTo(expectedEtag));
            }
            if (exceptionBeforeDowloading != null) {
                throw exceptionBeforeDowloading;
            }
            if (etag != null) {
                downloadListener.setEtag(etag);
            } else {
                downloadListener.setStartPosition(expectedLocalLength);
            }
            if (contentType != null) {
                downloadListener.setContentType(contentType);
            }
            long contentLength = this.contentLength >= 0 ? this.contentLength : data.length();
            downloadListener.setContentLength(contentLength);

            OutputStream out = downloadListener.getOutputStream(expectedLocalLength > 0);
            byte[] data = this.data.getBytes();
            out.write(data);
            out.close();
            downloadListener.updateProgress(data.length, contentLength);

            if (actionDuringDownload != null) {
                actionDuringDownload.run();
            }

            if (exceptionDuringDownloading != null) {
                throw exceptionDuringDownloading;
            }

            if (downloadListener.hasCancelled()) {
                throw new CancelledDownloadException();
            }
        }

        public DownloadAnswer setEtag(String etag) {
            this.etag = etag;
            return this;
        }

        public DownloadAnswer setData(String data) {
            this.data = data;
            return this;
        }

        public DownloadAnswer setActionDuringDownload(Runnable actionDuringDownload) {
            this.actionDuringDownload = actionDuringDownload;
            return this;
        }

        public DownloadAnswer setExpectedLocalLength(int expectedLocalLength) {
            this.expectedLocalLength = expectedLocalLength;
            return this;
        }

        public DownloadAnswer setExpectedEtag(String expectedEtag) {
            this.expectedEtag = expectedEtag;
            return this;
        }

        public DownloadAnswer setExceptionDuringDownloading(Exception exception) {
            this.exceptionDuringDownloading = exception;
            return this;
        }

        public DownloadAnswer setExceptionBeforeDownloading(Exception exception) {
            this.exceptionBeforeDowloading = exception;
            return this;
        }

        public DownloadAnswer setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public DownloadAnswer setContentLength(long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public DownloadAnswer setCheckRequest(boolean check) {
            this.checkRequest = check;
            return this;
        }

    }

}
