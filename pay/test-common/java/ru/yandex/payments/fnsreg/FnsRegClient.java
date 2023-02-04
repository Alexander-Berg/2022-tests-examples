package ru.yandex.payments.fnsreg;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Mono;

import ru.yandex.payments.fnsreg.controller.dto.CreateRegReportRequest;
import ru.yandex.payments.fnsreg.controller.dto.CreateRegistrationAppRequest;
import ru.yandex.payments.fnsreg.controller.dto.CreateReregistrationAppRequest;
import ru.yandex.payments.fnsreg.controller.dto.CreateWithdrawAppRequest;
import ru.yandex.payments.fnsreg.controller.dto.RegReportState;
import ru.yandex.payments.fnsreg.controller.dto.RegState;
import ru.yandex.payments.fnsreg.controller.dto.WithdrawState;
import ru.yandex.payments.fnsreg.types.Application;
import ru.yandex.payments.fnsreg.types.ModelName;
import ru.yandex.payments.fnsreg.types.RegRequestId;
import ru.yandex.payments.fnsreg.types.SerialNumber;
import ru.yandex.payments.tvm.auth.TvmSecured;

import static ru.yandex.payments.fnsreg.FnsRegClient.ID;

@TvmSecured
@Client(id = ID, errorType = String.class)
public interface FnsRegClient {
    String ID = "self";

    @Post(uri = "/registration/application/create", consumes = MediaType.APPLICATION_XML)
    Application createApplication(@Valid @Body CreateRegistrationAppRequest request);

    @Post(uri = "/reregistration/application/create", consumes = MediaType.APPLICATION_XML)
    Application createApplication(@Valid @Body CreateReregistrationAppRequest request);

    @Post(uri = "/withdraw/application/create", consumes = MediaType.APPLICATION_XML)
    Application createApplication(@Valid @Body CreateWithdrawAppRequest request);

    @Post(uri = "/registration/create", produces = MediaType.APPLICATION_XML)
    HttpResponse<Void> createRegistration(@Valid @QueryValue("request_id") RegRequestId requestId,
                                          @Body @NotBlank String application);

    @Post(uri = "/reregistration/create", produces = MediaType.APPLICATION_XML)
    HttpResponse<Void> createReregistration(@Valid @QueryValue("request_id") RegRequestId requestId,
                                            @Body @NotBlank String application);

    @Post(uri = "/withdraw/create", produces = MediaType.APPLICATION_XML)
    HttpResponse<Void> createWithdraw(@Valid @QueryValue("request_id") RegRequestId requestId,
                                      @Body @NotBlank String application);

    @Get("/registration/state")
    RegState getRegistrationState(@Valid @QueryValue("request_id") RegRequestId request);

    @Get("/reregistration/state")
    RegState getReregistrationState(@Valid @QueryValue("request_id") RegRequestId request);

    @Get("/registration/report/state")
    RegReportState getRegReportState(@Valid @QueryValue("request_id") RegRequestId request);

    @Get("/withdraw/state")
    WithdrawState getWithdrawState(@Valid @QueryValue("request_id") RegRequestId request);

    @Post(uri = "/registration/report/application/create", consumes = MediaType.APPLICATION_XML)
    Application createApplication(@Valid @Body CreateRegReportRequest request);

    @Post(uri = "/registration/report/create", produces = MediaType.APPLICATION_XML)
    HttpResponse<Void> createReport(@Valid @QueryValue("request_id") RegRequestId requestId,
                                   @Valid @QueryValue("kkt_sn") SerialNumber kktSerialNumber,
                                   @Valid @QueryValue("model_name") ModelName modelName,
                            @Body @NotBlank String application);
}
