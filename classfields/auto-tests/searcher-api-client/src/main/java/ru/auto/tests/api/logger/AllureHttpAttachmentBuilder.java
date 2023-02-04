package ru.auto.tests.api.logger;

/**
 * Created by vicdev on 02.05.17.
 */

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.valueOf;

/**
 * Build attachment for allure report
 */
public class AllureHttpAttachmentBuilder {
    private String requestMethod;
    private String requestUrl;
    private Map<String, String> queryParams;
    private Map<String, String> requestHeaders;
    private Map<String, String> requestCookies;
    private String requestBody;
    private String responseStatus;
    private int responseStatusCode;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private String curl;

    public AllureHttpAttachmentBuilder() {
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(final String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(final String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(final Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(final Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, String> getRequestCookies() {
        return requestCookies;
    }

    public void setRequestCookies(final Map<String, String> requestCookies) {
        this.requestCookies = requestCookies;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(final String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(final String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(final Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(final String responseBody) {
        this.responseBody = responseBody;
    }

    public int getResponseStatusCode() {
        return responseStatusCode;
    }

    public void setResponseStatusCode(int responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

    public String getCurl() {
        return curl;
    }

    public void setCurl(String curl) {
        this.curl = curl;
    }

    public AllureHttpAttachmentBuilder withRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
        return this;
    }

    public AllureHttpAttachmentBuilder withRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
        return this;
    }

    public AllureHttpAttachmentBuilder withQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    public AllureHttpAttachmentBuilder withRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
        return this;
    }

    public AllureHttpAttachmentBuilder withRequestCookies(Map<String, String> requestCookies) {
        this.requestCookies = requestCookies;
        return this;
    }

    public AllureHttpAttachmentBuilder withRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public AllureHttpAttachmentBuilder withResponseStatusCode(int responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
        return this;
    }

    public AllureHttpAttachmentBuilder withResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    public AllureHttpAttachmentBuilder withResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
        return this;
    }

    public AllureHttpAttachmentBuilder withResponseBody(String responseBody) {
        this.responseBody = responseBody;
        return this;
    }

    public void addRequestHeaders(final String name, final String value) {
        requestHeaders.put(name, value);
    }

    private String generateCurl() {
        return new CurlBuilder(requestMethod, requestUrl).cookie(requestCookies).header(requestHeaders)
                .body(requestBody).toString();
    }

    public void build() {
        curl = generateCurl();
        final byte[] bytes = process("report_api", this);
        AllureLifecycle lifecycle = Allure.getLifecycle();
        lifecycle.startStep(
                UUID.randomUUID().toString(),
                new StepResult().withName(String.format("%s: %s", requestMethod, requestUrl))
                        .withStatus(Status.PASSED)
                        .withParameters(new Parameter().withName("body").withValue(responseBody),
                                new Parameter().withName("status").withValue(valueOf(responseStatusCode)))
        );
        lifecycle.addAttachment("Api report log", "text/html", "html", bytes);
        lifecycle.stopStep();
    }

    //use default template for http attachment
    private static byte[] process(final String templateName, final Object object) {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setClassForTemplateLoading(FreemarkerUtils.class, "/templates");
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            final Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            final Template template = cfg.getTemplate(String.format("%s.ftl", templateName));
            template.process(object, writer);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Can't read template", e);
        } catch (TemplateException e) {
            throw new IllegalStateException("Can't process template", e);
        }
    }
}
