package ru.yandex.disk.remote;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.ConnectInterceptor;
import okhttp3.internal.http.RealInterceptorChain;
import ru.yandex.disk.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DisableHttpInterceptor implements Interceptor {
    private final Interceptor requestHandler;
    private static final Field fInterceptors;

    static {
        fInterceptors =
                ReflectionUtils.getAccessibleField(RealInterceptorChain.class, "interceptors");
    }

    public DisableHttpInterceptor(final Interceptor requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        try {
            final List<Interceptor> interceptors = getInterceptors(chain);

            final List<Interceptor> newInterceptors = new ArrayList<>(interceptors.size());

            for (int i = 1, beforeLast = interceptors.size() - 1; i < beforeLast; i++) {
                final Interceptor interceptor = interceptors.get(i);
                if (interceptor instanceof ConnectInterceptor) {
                    continue;
                }
                newInterceptors.add(interceptor);
            }

            newInterceptors.add(requestHandler);

            final Request request = chain.request();
            final RealInterceptorChain newChain =
                //List<Interceptor>,StreamAllocation,HttpCodec,RealConnection,int,Request,Call,EventListener,int,int,int
                    new RealInterceptorChain(newInterceptors, null, null, null, 0, request, null, null, 0, 0, 0);
            return newChain.proceed(request);
        } catch (final IOException e) {
            throw e;
        }
    }

    private static List<Interceptor> getInterceptors(final Chain chain) {
        return ReflectionUtils.getFieldValue(chain, DisableHttpInterceptor.fInterceptors);
    }

    public void prepare(List<Interceptor> interceptors) {
        interceptors = interceptors.subList(1, interceptors.size());
        interceptors.add(requestHandler);
    }

}
