package ru.yandex.qe.hitman.tvm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

/**
 * Created by akhvorov on 07.09.17.
 */
public class FakeTVMService {

    public HttpResponse execute(HttpPost request) {
        final StringBuilder sb = new StringBuilder(request.getRequestLine().getUri());
        sb.append('?');
        try {
            IOUtils.readLines(request.getEntity().getContent()).forEach(sb::append);
            final BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(sb.toString().getBytes()));
            final HttpResponse response = new BasicHttpResponse(
                    new BasicStatusLine(
                            new ProtocolVersion("", 0, 0),
                            200,
                            "nothing")
            );
            response.setEntity(entity);
            return response;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
