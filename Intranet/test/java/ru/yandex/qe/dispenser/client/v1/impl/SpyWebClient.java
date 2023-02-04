package ru.yandex.qe.dispenser.client.v1.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Enumeration;
import java.util.Objects;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.message.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.qe.dispenser.api.DiRequest;
import ru.yandex.qe.dispenser.api.util.MoreHeaders;
import ru.yandex.qe.dispenser.solomon.SolomonHolder;
import ru.yandex.qe.dispenser.ws.Idempotent;
import ru.yandex.qe.dispenser.ws.intercept.DiFilter;
import ru.yandex.qe.dispenser.ws.intercept.FilterController;
import ru.yandex.qe.dispenser.ws.intercept.MockPreAuthFilter;

import static org.mockito.Mockito.mock;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
public final class SpyWebClient extends RequestFilterWebClient {
    @Nullable
    private static volatile String lastMethod = null;
    @Nullable
    private static volatile String lastPath = null;
    @Nullable
    private static volatile String lastPathQuery = null;
    @Nullable
    private static volatile Object lastRequestBody = null;
    @Nullable
    private static volatile Integer lastResponseStatus = null;
    @Nullable
    private static volatile MultivaluedMap<String, String> lastResponseHeaders = null;
    @Nullable
    private static volatile String lastResponse = null;

    @NotNull
    private final Filter serverFilter = new FilterController(new MockPreAuthFilter(), new DiFilter());

    public SpyWebClient(@NotNull final String baseAddress) {
        super(baseAddress);
        initFilter(serverFilter);
    }

    @Override
    public boolean onRequest(@NotNull final String method,
                             @NotNull final MultivaluedMap<String, String> headers,
                             @NotNull final URI uri,
                             @Nullable final Object body) {
        lastMethod = method;
        final String path = uri.getPath();
        lastPath = !path.startsWith("/api") ? "/api" + path : path;
        lastPathQuery = "/api" + uri.toString();
        lastRequestBody = body;
        return true;
    }

    @Override
    protected void doRunInterceptorChain(@NotNull final Message m) {
        final DiRequest<?> diRequest = new DiRequestImpl<>(m);
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(diRequest.getFirstHeader(HttpHeaders.AUTHORIZATION));
        Mockito.when(request.getHeader(MoreHeaders.X_DISPENSER_CLIENT_INFO)).thenReturn(diRequest.getFirstHeader(MoreHeaders.X_DISPENSER_CLIENT_INFO));
        Mockito.when(request.getMethod()).thenReturn(diRequest.getMethod());
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(diRequest.getPath()));
        Mockito.when(request.getParameter(Idempotent.REQUEST_ID)).thenReturn(diRequest.getQueryParam(Idempotent.REQUEST_ID));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        try {
            Mockito.when(response.getWriter()).thenReturn(printWriter);
        } catch (IOException ignored) {
        }

        m.getExchange().put("IN_CHAIN_COMPLETE", Boolean.TRUE);
        try {
            mockSendError(response);
            serverFilter.doFilter(request, response, (req, resp) -> super.doRunInterceptorChain(m));
            final String s = stringWriter.toString();
            if (!s.isEmpty()) {
                final Response res = Response.ok(s)
                        .header("Content-Type", "application/json")
                        .build();
                m.getExchange().setInMessage(m);
                m.getExchange().put(Response.class, res);
                m.getExchange().put(Message.RESPONSE_CODE, 200);
            }
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

    private void mockSendError(@NotNull final HttpServletResponse resp) throws IOException {
        Mockito.doAnswer((invocation) -> {
            final Object[] args = invocation.getArguments();
            throw ErrorMessageLogger.toException((int) args[0], (String) args[1]);
        }).when(resp).sendError(Mockito.anyInt(), Mockito.anyString());
    }

    @Nullable
    @Override
    protected <T> T readBody(@NotNull final Response r,
                             @NotNull final Message outMessage,
                             @NotNull final Class<T> cls,
                             @NotNull final Type type,
                             @NotNull final Annotation[] anns) {
        try {
            return super.readBody(spyResponse(r, outMessage), outMessage, cls, type, anns);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    private Response spyResponse(@NotNull final Response r, @NotNull final Message outMessage) throws IOException {
        lastResponseStatus = r.getStatus();
        lastResponseHeaders = r.getStringHeaders();
        final String body = readBody(r);
        lastResponse = body;
        final Response copy = Response.fromResponse(r)
                .entity(new ByteArrayInputStream(body.getBytes()))
                .build();
        ((ResponseImpl) copy).setOutMessage(outMessage);
        return copy;
    }

    private String readBody(Response r) {
        r.bufferEntity();
        if (r.getEntity() instanceof String) {
            return (String) r.getEntity();
        } else {
            return r.readEntity(String.class);
        }
    }

    @Override
    protected @NotNull String getErrorMessage(@NotNull final Response r) {
        return r.readEntity(String.class);
    }

    @NotNull
    public static String lastMethod() {
        return Objects.requireNonNull(lastMethod, "Perform before!");
    }

    @NotNull
    public static String lastPath() {
        return Objects.requireNonNull(lastPath, "Perform before!");
    }

    @NotNull
    public static String lastPathQuery() {
        return Objects.requireNonNull(lastPathQuery, "Perform before!");
    }

    @Nullable
    public static String lastResponseHeader(@NotNull final String name) {
        return Objects.requireNonNull(lastResponseHeaders, "Perform before!").getFirst(name);
    }

    @NotNull
    public static Object lastRequestBody() {
        return Objects.requireNonNull(lastRequestBody, "Perform not GET before!");
    }

    public static int lastResponseStatus() {
        return Objects.requireNonNull(lastResponseStatus, "Perform before!");
    }

    @NotNull
    public static String lastResponse() {
        return Objects.requireNonNull(lastResponse, "Perform before!");
    }

    private static void initFilter(final @NotNull Filter serverFilter) {
        final FilterConfig filterConfig = mock(FilterConfig.class);
        final ServletContext servletContext = mock(ServletContext.class);
        final WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
        final SolomonHolder solomonHolder = new SolomonHolder(2012028);
        @SuppressWarnings("unchecked") final Enumeration<String> initParameters = (Enumeration<String>) mock(Enumeration.class);
        Mockito.when(initParameters.hasMoreElements()).thenReturn(false);
        Mockito.when(filterConfig.getInitParameterNames()).thenReturn(initParameters);
        Mockito.when(filterConfig.getServletContext()).thenReturn(servletContext);
        Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(webApplicationContext);
        Mockito.when(webApplicationContext.getBean(SolomonHolder.class)).thenReturn(solomonHolder);
        try {
            serverFilter.init(filterConfig);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

}
