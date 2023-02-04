package ru.yandex.qe.dispenser.integration;

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.qe.dispenser.domain.hierarchy.HierarchySupplier;
import ru.yandex.qe.dispenser.ws.admin.ReinitializeStateService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:spring/application-test-ctx.xml"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseExternalApiTest implements ApplicationContextAware {

    @Autowired
    protected HierarchySupplier hierarchy;

    @Autowired
    protected ReinitializeStateService reinitializeStateService;

    protected ApplicationContext applicationContext;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        reinitialize();
        updateHierarchy();
    }

    protected void reinitialize() throws URISyntaxException {
        reinitializeStateService.reinitialize();
    }

    protected void updateHierarchy() {
        hierarchy.update();
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
