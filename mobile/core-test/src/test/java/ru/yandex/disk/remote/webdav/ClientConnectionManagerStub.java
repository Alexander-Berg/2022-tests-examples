package ru.yandex.disk.remote.webdav;

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

class ClientConnectionManagerStub implements ClientConnectionManager {

    @Override
    public SchemeRegistry getSchemeRegistry() {
        return null;
    }

    @Override
    public ClientConnectionRequest requestConnection(final HttpRoute route, final Object state) {
        return new ClientConnectionRequest() {
            @Override
            public ManagedClientConnection getConnection(final long timeout, final TimeUnit tunit) throws InterruptedException, ConnectionPoolTimeoutException {
                return new ManagedClientConnection() {
                    public HttpRoute route;

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public HttpRoute getRoute() {
                        return route;
                    }

                    @Override
                    public SSLSession getSSLSession() {
                        return null;
                    }

                    @Override
                    public void open(final HttpRoute route, final HttpContext context, final HttpParams params) throws IOException {
                        this.route = route;

                    }

                    @Override
                    public void tunnelTarget(final boolean secure, final HttpParams params) throws IOException {

                    }

                    @Override
                    public void tunnelProxy(final HttpHost next, final boolean secure, final HttpParams params) throws IOException {

                    }

                    @Override
                    public void layerProtocol(final HttpContext context, final HttpParams params) throws IOException {

                    }

                    @Override
                    public void markReusable() {

                    }

                    @Override
                    public void unmarkReusable() {

                    }

                    @Override
                    public boolean isMarkedReusable() {
                        return false;
                    }

                    @Override
                    public void setState(final Object state) {

                    }

                    @Override
                    public Object getState() {
                        return null;
                    }

                    @Override
                    public void setIdleDuration(final long duration, final TimeUnit unit) {

                    }

                    @Override
                    public void releaseConnection() throws IOException {

                    }

                    @Override
                    public void abortConnection() throws IOException {

                    }

                    @Override
                    public boolean isResponseAvailable(final int timeout) throws IOException {
                        return false;
                    }

                    @Override
                    public void sendRequestHeader(final HttpRequest request) throws HttpException, IOException {

                    }

                    @Override
                    public void sendRequestEntity(final HttpEntityEnclosingRequest request) throws HttpException, IOException {

                    }

                    @Override
                    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
                        return null;
                    }

                    @Override
                    public void receiveResponseEntity(final HttpResponse response) throws HttpException, IOException {

                    }

                    @Override
                    public void flush() throws IOException {

                    }

                    @Override
                    public InetAddress getLocalAddress() {
                        return null;
                    }

                    @Override
                    public int getLocalPort() {
                        return 0;
                    }

                    @Override
                    public InetAddress getRemoteAddress() {
                        return null;
                    }

                    @Override
                    public int getRemotePort() {
                        return 0;
                    }

                    @Override
                    public void close() throws IOException {

                    }

                    @Override
                    public boolean isOpen() {
                        return true;
                    }

                    @Override
                    public boolean isStale() {
                        return false;
                    }

                    @Override
                    public void setSocketTimeout(final int timeout) {

                    }

                    @Override
                    public int getSocketTimeout() {
                        return 0;
                    }

                    @Override
                    public void shutdown() throws IOException {

                    }

                    @Override
                    public HttpConnectionMetrics getMetrics() {
                        return null;
                    }
                };
            }

            @Override
            public void abortRequest() {

            }
        };
    }

    @Override
    public void releaseConnection(final ManagedClientConnection conn, final long validDuration, final TimeUnit timeUnit) {

    }

    @Override
    public void closeIdleConnections(final long idletime, final TimeUnit tunit) {

    }

    @Override
    public void closeExpiredConnections() {

    }

    @Override
    public void shutdown() {

    }
}
