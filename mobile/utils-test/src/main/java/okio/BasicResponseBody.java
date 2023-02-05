package okio;

import com.google.common.base.Charsets;
import okhttp3.MediaType;
import okhttp3.ResponseBody;

import javax.annotation.NonnullByDefault;
import javax.annotation.Nullable;
import java.io.IOException;

@NonnullByDefault
public class BasicResponseBody extends ResponseBody {

    private final BufferedSource source;
    private final Buffer buffer;
    private boolean closed;

    public BasicResponseBody(final String body) {
        this(body.getBytes(Charsets.UTF_8));
    }

    public BasicResponseBody(final byte[] body) {
        buffer = new Buffer().write(body);
        source = new RealBufferedSource(new Source() {

            @Override
            public long read(final Buffer sink, final long byteCount) throws IOException {
                return BasicResponseBody.this.read(sink, byteCount);
            }

            @Override
            public Timeout timeout() {
                return buffer.timeout();
            }

            @Override
            public void close() throws IOException {
                closed = true;
            }

        });
    }

    protected long read(final Buffer sink, final long byteCount) throws IOException {
        return buffer.read(sink, byteCount);
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return null;
    }

    @Override
    public long contentLength() {
        return buffer.size();
    }

    @Override
    public BufferedSource source() {
        return source;
    }

    public boolean isClosed() {
        return closed;
    }

}