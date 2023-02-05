package com.yandex.mail.test;

import com.yandex.mail.proxy.BlockManager;

import java.io.IOException;

public class TestNetworkBlockResolver implements BlockManager.NetworkBlockResolver {

    private boolean blocked;

    public TestNetworkBlockResolver(boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public boolean isBlocked() throws IOException {
        return blocked;
    }

    public void setRemotelyBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
