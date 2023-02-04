package ru.yandex.auto.searcher.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * TODO
 *
 * @author Anton Volokhov <logab@yandex-team.ru>
 */
public class XmlRequestHandlerTest {
  private static final Logger log = LoggerFactory.getLogger(XmlRequestHandlerTest.class);

  private static final String request =
      "<entries>\n"
          + "<entry>\n"
          + "<mark-raw>Рено</mark-raw>\n"
          + "<model-raw>Логан</model-raw>\n"
          + "</entry>\n"
          + "<entry>\n"
          + "<model-raw>Land Rover Oulander</model-raw>\n"
          + "</entry>\n"
          + "</entries>";

  static Server jetty;
  static ApplicationContext context;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    context = new ClassPathXmlApplicationContext("auto-searcher-http-server-test.xml");
    jetty = (Server) context.getBean("httpServerInitializer");
    jetty.start();
  }

  @AfterClass
  public static void oneTimeTearDown() throws Exception {
    jetty.stop();
  }

  @Test
  @Ignore
  public void testHandle() throws URISyntaxException, IOException {
    final URI uri = new URI("http://127.0.0.1:34389/unifyMarkModel");
    HttpPost post = new HttpPost(uri);
    post.addHeader("content-type", "application/xml");
    BasicHttpEntity content = new BasicHttpEntity();
    ByteArrayInputStream is = new ByteArrayInputStream(request.getBytes());
    content.setContent(is);
    content.setContentLength(request.getBytes().length);
    post.setEntity(content);
    HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(params, 600000);
    DefaultHttpClient client = new DefaultHttpClient(params);
    HttpResponse response = client.execute(post);
    log.info(new String(IOUtils.toByteArray(response.getEntity().getContent())));
  }
}
