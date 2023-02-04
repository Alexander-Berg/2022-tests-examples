package ru.yandex.qe.dispenser.testing.operations.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.builder.GetQuotasRequestBuilder;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class UserQuotas extends Operation {
    @Nullable
    private final String serviceKey;
    @Nullable
    private final String entitySpecKey;

    public UserQuotas(@JsonProperty("probability") final double probability,
                      @JsonProperty("retryProbability") @Nullable final Double retryProbability,
                      @JsonProperty("serviceKey") @Nullable final String serviceKey,
                      @JsonProperty("entitySpecKey") @Nullable final String entitySpecKey) {
        super(probability, retryProbability);
        this.serviceKey = serviceKey;
        this.entitySpecKey = entitySpecKey;
    }

    @Override
    public void doPerform(final Context ctx, final Dispenser dispenser) {
        final String user = ctx.getAnyUser();
        GetQuotasRequestBuilder<DiQuotaGetResponse, ?> requestBuilder = dispenser.quotas().get().availableFor(user);
        if (serviceKey != null) {
            requestBuilder = requestBuilder.inService(serviceKey);
            if (entitySpecKey != null) {
                requestBuilder = ((GetQuotasRequestBuilder.InService<DiQuotaGetResponse>) requestBuilder).byEntitySpecification(entitySpecKey);
            }
        }
        requestBuilder.perform();
    }
}
