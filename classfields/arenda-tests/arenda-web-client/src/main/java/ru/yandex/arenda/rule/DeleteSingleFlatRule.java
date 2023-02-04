package ru.yandex.arenda.rule;

import com.google.inject.Inject;
import org.junit.rules.ExternalResource;
import ru.yandex.arenda.account.FlatsKeeper;
import ru.yandex.arenda.steps.RetrofitApiSteps;

public class DeleteSingleFlatRule extends ExternalResource {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(DeleteSingleFlatRule.class);

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private FlatsKeeper flatsKeeper;

    @Override
    protected void after() {
        try {
            flatsKeeper.get().forEach(retrofitApiSteps::deleteFlat);
        } catch (Exception e) {
            LOGGER.info(String.format("Can't delete flats; Exception: %s", e.getMessage()));
        }
    }
}
