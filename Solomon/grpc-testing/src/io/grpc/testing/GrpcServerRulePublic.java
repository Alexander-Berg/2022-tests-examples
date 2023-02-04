package io.grpc.testing;

import io.grpc.Server;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.rules.ExternalResource;


/**
 * @author Vladimir Gordiychuk
 */
public class GrpcServerRulePublic extends ExternalResource {

    private final GrpcServerRule delegate = new GrpcServerRule();
    private boolean closed = false;

    public String getServerName() {
        return delegate.getServerName();
    }

    public MutableHandlerRegistry getServiceRegistry() {
        return delegate.getServiceRegistry();
    }

    public Server getServer() {
        return delegate.getServer();
    }

    @Override
    public void after() {
        if (!closed) {
            closed = true;
            delegate.after();
        }
    }

    @Override
    public void before() throws Throwable {
        closed = false;
        delegate.before();
    }
}
