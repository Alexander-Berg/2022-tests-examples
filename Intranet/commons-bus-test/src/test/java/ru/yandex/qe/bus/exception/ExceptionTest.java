package ru.yandex.qe.bus.exception;

import java.net.SocketException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.qe.bus.servlet.BusServlet;
import ru.yandex.qe.spring.profiles.Profiles;
import ru.yandex.qe.testing.web.TestingWebServer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("SpringJavaAutowiringInspection")
@ActiveProfiles(Profiles.TESTING)
@ContextConfiguration({
        "classpath*:spring/qe-plugin-spring.xml",
        "classpath:spring/exception-test.xml",
})
@DirtiesContext
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExceptionTest {
    private TestingWebServer testingServer;

    @Inject
    @Named("exceptionApiClient")
    private ExceptionApi service;

    @Inject
    @Named("exceptionApiClient404")
    private ExceptionApi service404;

    @Inject
    @Named("exceptionApiClientReject")
    private ExceptionApi serviceRejecting;

    @Inject
    private ApplicationContext applicationContext;

    public static Object[][] exceptions() {
        return new Object[][]{
                {AppException.E1.class, NotAcceptableException.class},
                {AppException.E2.class, ForbiddenException.class},
                {AppException.E3.class, NotAuthorizedException.class},
                {AppException.E4.class, BadRequestException.class},
                {AppException.E5.class, NotAllowedException.class},
                {AppException.E6.class, NotFoundException.class},
                {AppException.E7.class, NotSupportedException.class},
                {AppException.E8.class, ServiceUnavailableException.class},
                {AppException.E9.class, InternalServerErrorException.class},
                {AppException.Unmapped.class, InternalServerErrorException.class},
                {BadRequestException.class, InternalServerErrorException.class},
                {AppException.Custom.class, BadRequestException.class},
                {CustomWebApplicationException.class, BadRequestException.class},
        };
    }

    @MethodSource("exceptions")
    @ParameterizedTest
    public void testExceptions(Class<? extends Exception> serverException, Class<? extends WebApplicationException> resultException) {
        try {
            service.throwing(serverException.getName());
            fail();
        } catch (WebApplicationException e) {
            assertThat(e.getClass().getName(), is(resultException.getName()));
            assertThat(e.getResponse().getStatus(), is(not(200)));
        }
    }

    @Test
    public void test404() {
        try {
            service404.throwing("");
            fail();
        } catch (NotFoundException ignored) {
        }
    }

    @Test
    public void testRejected() {
        try {
            serviceRejecting.throwing("");
            fail();
        } catch (ProcessingException e) {
            assertThat(e.getCause(), instanceOf(SocketException.class));
        }
    }

    @BeforeAll
    void startServer() throws Exception {
        testingServer = new TestingWebServer(12345);

        testingServer.addEventListener(new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent sce) {
                final ServletRegistration.Dynamic dispatcher =
                        sce.getServletContext().addServlet("CXFServlet", new BusServlet(applicationContext));
                dispatcher.setLoadOnStartup(1);
                dispatcher.addMapping("/api/*");
            }

            @Override
            public void contextDestroyed(ServletContextEvent sce) {
            }
        });

        testingServer.start();
    }

    @AfterAll
    void stopServer() throws Exception {
        testingServer.stop();
    }

}
