package ru.yandex.disk.remote.webdav;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.entity.BasicHttpEntity;

public class UnrepeatableURLEntity extends BasicHttpEntity {

    public UnrepeatableURLEntity(URL url) throws IOException {
        URLConnection openConnection = url.openConnection();
        setContent(openConnection.getInputStream());
        setContentLength(openConnection.getContentLength());
    }

}
