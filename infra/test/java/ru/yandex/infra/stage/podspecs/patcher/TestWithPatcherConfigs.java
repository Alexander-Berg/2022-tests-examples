package ru.yandex.infra.stage.podspecs.patcher;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public abstract class TestWithPatcherConfigs {

    protected static final Config CORRECT_PATCHERS_CONFIG;

    static {
        CORRECT_PATCHERS_CONFIG = ConfigFactory.load("patcher_contexts.conf");
    }
}
