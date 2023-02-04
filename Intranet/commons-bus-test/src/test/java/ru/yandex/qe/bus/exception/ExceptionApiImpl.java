package ru.yandex.qe.bus.exception;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

/**
 * @author rurikk
 */
@Service
@ExceptionTranslator(ExceptionApiExceptionTranslator.class)
public class ExceptionApiImpl implements ExceptionApi {
    @Nonnull
    @Override
    public String throwing(String exceptionClass) throws AppException {
        try {
            throw (Exception) Class.forName(exceptionClass).newInstance();
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    private static RuntimeException sneakyThrow(Throwable t) {
        if (t == null) {
            throw new NullPointerException("t");
        }
        sneakyThrowCast(t);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrowCast(Throwable t) throws T {
        throw (T)t;
    }
}
