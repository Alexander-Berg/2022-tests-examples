package ru.yandex.webmaster3.storage.turbo.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import net.htmlparser.jericho.PHPTagTypes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import ru.yandex.webmaster3.core.util.json.JsonMapping;

import static org.junit.Assert.*;

/**
 * ishalaru
 * 03.09.2020
 **/
public class TurboAutoAdvServiceTest {

    @SneakyThrows
    @Test
    public void test(){
        String PI_DOMAIN_QUERY = "[\"AND\",[[\"domain\",\"=\",\"%s\"],[\"login\",\"MATCH\",\\[\"user\",\"MATCH\",\\[\"login\",\"=\",\"%s\"\\]\\]\\]\\]\\]";
        URIBuilder uriBuilder = new URIBuilder("https://yandex.ru");
        uriBuilder.addParameter("test", PI_DOMAIN_QUERY);
        uriBuilder.setCharset(StandardCharsets.US_ASCII);
        System.out.println(uriBuilder.build().toString());
        HttpGet httpGet = new HttpGet(uriBuilder.build().toString());
        httpGet.setConfig(RequestConfig.DEFAULT);
        System.out.println();
    }

    @Test
    public void test2(){
        final ObjectNode jsonNodes = new ObjectNode(new JsonNodeFactory(true));
        jsonNodes.put("data", JsonMapping.writeValueAsString(List.of(1,2,3)));
        System.out.println(jsonNodes.toString());
    }
}