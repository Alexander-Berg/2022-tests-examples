package ru.yandex.qe.dispenser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

@WebAppConfiguration
@ContextConfiguration(locations = {"classpath*:spring/application-migration-ctx.xml"})
@Transactional
@ExtendWith(SpringExtension.class)
public class DatabaseMigrationTestCase {

    @Test
    public void initTestContext() {
        // do nothing
    }
}
