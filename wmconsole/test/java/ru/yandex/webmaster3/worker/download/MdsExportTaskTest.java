package ru.yandex.webmaster3.worker.download;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.bean.CsvBindByName;
import lombok.Builder;
import lombok.Value;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.download.FileFormat;
import ru.yandex.webmaster3.core.download.MdsSerializable;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.functional.ThrowingConsumer;
import ru.yandex.webmaster3.core.xcelite.annotations.Column;
import ru.yandex.webmaster3.core.xcelite.annotations.Row;
import ru.yandex.webmaster3.storage.download.DownloadInfoYDao;
import ru.yandex.webmaster3.storage.download.MDSService;
import ru.yandex.webmaster3.storage.download.common.DownloadFileType;
import ru.yandex.webmaster3.storage.download.common.MdsExportTaskData;
import ru.yandex.webmaster3.storage.hoststat.download.ContentAttrSamplesMdsExportDescriptor;
import ru.yandex.webmaster3.storage.hoststat.download.ContentAttrSamplesMdsExportDescriptor.ContentAttrType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by Oleg Bazdyrev on 22/03/2022.
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class MdsExportTaskTest {

    private static final WebmasterHostId HOST_ID = IdUtils.stringToHostId("http:example.com:80");

    @InjectMocks
    MdsExportTask task;
    @Mock
    Map<DownloadFileType, AbstractMdsDataProvider> providersMap;
    @Mock
    TestMdsService mdsService;
    @Mock
    DownloadInfoYDao downloadInfoYDao;

    @Test(expected = RuntimeException.class, timeout = 5000)
    public void testImmediateErrorCsv() throws Exception {
        when(providersMap.get(eq(DownloadFileType.CONTENT_ATTR_SAMPLES))).thenReturn(new TestProvider());
        when(mdsService.uploadFileAndGetDownloadLink(Mockito.any(InputStream.class), Mockito.anyString())).thenCallRealMethod();
        task.run(new MdsExportTaskData(HOST_ID, FileFormat.EXCEL, "test.csv", 100500L, new ContentAttrSamplesMdsExportDescriptor(ContentAttrType.DESCRIPTION, "example.com")));

    }

    @Value
    @Builder
    @Row(colsOrder = {"Value"})
    private static final class TestRow implements MdsSerializable {

        @CsvBindByName(column = "Value")
        @Column(name = "Value")
        String value;
    }

    private static class TestProvider extends AbstractMdsDataProvider<TestRow> {

        @Override
        public void provide(MdsExportTaskData data, ThrowingConsumer<TestRow, Exception> consumer) throws Exception {
            consumer.accept(new TestRow("123"));
            Thread.sleep(500);
            throw new Exception("something bad");
        }

        @Override
        public Class getRowClass() {
            return TestRow.class;
        }
    }

    private static class TestMdsService extends MDSService {

        public TestMdsService(String uploadHost, String readHost, String namespace, String authUpload) {
            super(uploadHost, readHost, namespace, authUpload);
        }

        @Override
        public String uploadFileAndGetDownloadLink(InputStream is, String fileName) {
            HttpPost post = new HttpPost("http://example.com");
            post.setEntity(new InputStreamEntity(is));
            try {
                HttpClientBuilder.create().build().execute(post);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return "123";
        }
    }

}
