package ru.yandex.qe.dispenser.standalone;

import ru.yandex.qe.dispenser.ws.billing.BillingApi;
import ru.yandex.qe.dispenser.ws.billing.BillingDryRunRequest;
import ru.yandex.qe.dispenser.ws.billing.BillingDryRunResponse;

import java.util.ArrayList;

public class MockBillingApi implements BillingApi {

    @Override
    public BillingDryRunResponse dryRun(BillingDryRunRequest request) {
        return new BillingDryRunResponse(request.getCurrency(), new ArrayList<>(), new ArrayList<>());
    }

}
