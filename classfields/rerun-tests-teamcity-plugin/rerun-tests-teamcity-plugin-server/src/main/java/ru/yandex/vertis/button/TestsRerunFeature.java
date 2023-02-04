package ru.yandex.vertis.button;

import jetbrains.buildServer.serverSide.BuildFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.yandex.vertis.RerunConstant;

public class TestsRerunFeature extends BuildFeature {

    @NotNull
    @Override
    public String getType() {
        return RerunConstant.RUN_TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return RerunConstant.SERVER_DISPLAY_NAME;
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return null;
    }
}
