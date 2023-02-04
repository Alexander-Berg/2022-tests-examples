package ru.yandex.partner.core.junit;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import ru.yandex.partner.libs.memcached.MemcachedService;
import ru.yandex.partner.test.db.QueryLogService;
import ru.yandex.partner.test.db.utils.MySQLRefresherService;

import static org.mockito.Mockito.reset;
import static org.springframework.test.context.junit.jupiter.SpringExtension.getApplicationContext;

/**
 * Usage:
 * <ul>
 *     <li>Annotate test class for managed "partner" schema</li>
 *     <li>Add field in test class for custom schema
 *     <code>@RegisterExtension MySqlRefresher otherSchema = new MySqlRefresher("otherSchema")</code>
 *     </li>
 * </ul>
 */
public class MySqlRefresher implements Extension, BeforeEachCallback, AfterEachCallback {
    private final String managedSchema;

    public MySqlRefresher() {
        this("partner");
    }

    public MySqlRefresher(String managedSchema) {
        this.managedSchema = managedSchema;
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException, URISyntaxException {
        QueryLogService queryLogService = getApplicationContext(context).getBean(QueryLogService.class);
        queryLogService.stop();
        getApplicationContext(context).getBean(MySQLRefresherService.class).refresh(managedSchema);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        reset(getApplicationContext(context).getBean(MemcachedService.class));
        QueryLogService queryLogService = getApplicationContext(context).getBean(QueryLogService.class);
        queryLogService.start();
    }


}
