package ru.yandex.webmaster3.core.semantic.mf_verifier;

import ru.yandex.common.framework.core.MockServResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
* Created by IntelliJ IDEA.
* User: rasifiel
* Date: 04.09.13
* Time: 12:21
*/
class StubServResponse extends MockServResponse {

    private ByteArrayOutputStream stream = new ByteArrayOutputStream();

    @Override
    public void write(final byte[] what) {
        try {
            stream.write(what);
        } catch (IOException e) {
            throw new RuntimeException("IO", e);
        }
    }

    @Override
    public OutputStream getOutputStream() {
        return stream;
    }

    @Override
    public byte[] getXML() {
        return stream.toByteArray();
    }
}
