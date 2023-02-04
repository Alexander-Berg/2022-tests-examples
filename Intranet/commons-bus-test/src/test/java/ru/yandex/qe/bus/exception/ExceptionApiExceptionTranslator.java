package ru.yandex.qe.bus.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.ServiceUnavailableException;

/**
 * @author rurikk
 */
public class ExceptionApiExceptionTranslator extends AbstractExceptionTranslator {
    public ExceptionApiExceptionTranslator() {
        on(AppException.E1.class)
                .logInfoMessage()
                .wrap(NotAcceptableException::new);
        on(AppException.E2.class)
                .logInfo("e2")
                .wrap(ForbiddenException::new);
        on(AppException.E3.class)
                .logErrorMessage()
                .wrap(e -> new NotAuthorizedException(e, "challenge"));
        on(AppException.E4.class)
                .logError("e4")
                .wrap(BadRequestException::new);
        on(AppException.E5.class)
                .logWarnMessage()
                .wrap(e -> new NotAllowedException(e, "allowedMethod"));
        on(AppException.E6.class)
                .logWarn("e6")
                .wrap(NotFoundException::new);
        on(AppException.E7.class)
                .peek(e -> log.error("custom " + e))
                .wrap(NotSupportedException::new);
        on(AppException.E8.class)
                .peek(e -> log.error("custom1 " + e))
                .peek(e -> log.error("custom2 " + e))
                .wrap(e -> new ServiceUnavailableException(0L, e));
        on(AppException.E9.class, AppException.E9.class, AppException.E9.class)
                .wrap(InternalServerErrorException::new);

        on(IllegalArgumentException.class)
                .wrap(BadRequestException::new);

        on(AppException.Custom.class)
                .wrap(CustomWebApplicationException::new);

        onWae(CustomWebApplicationException.class)
                .rethrow();
    }
}
