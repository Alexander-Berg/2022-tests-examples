package ru.yandex.qe.bus.servlet;


import javax.inject.Inject;
import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

@ContextConfiguration(locations = {"classpath:spring/application-with-name-ctx.xml"})
@ActiveProfiles("development")
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
public class ContextWithNameTestCase {

    @Inject
    WebApplicationContext webApplicationContext;

    @Test
    public void springBusServlet() throws ServletException {
        MockServletContext servletContext = new MockServletContext();
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
        SpringBusServlet springBusServlet = new SpringBusServlet();
        springBusServlet.init(servletConfig);
    }
}
