package ru.yandex.partner.test.db;

import ru.yandex.direct.test.mysql.DirectMysqlRunner;
import ru.yandex.direct.test.mysql.TestMysqlConfig;

public class MysqlRunner {
    private MysqlRunner() {
    }

    public static void main(String[] args) throws InterruptedException {
        TestMysqlConfig config = MysqlTestConfiguration.MYSQL_CONFIG;
        DirectMysqlRunner.runMysqlServer(config);
    }
}
