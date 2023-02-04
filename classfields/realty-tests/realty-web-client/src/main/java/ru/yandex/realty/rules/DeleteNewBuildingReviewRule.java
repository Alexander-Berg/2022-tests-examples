package ru.yandex.realty.rules;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import org.junit.rules.ExternalResource;
import ru.yandex.realty.step.NewBuildingSteps;

/**
 * @author kantemirov
 */
@Log4j
public class DeleteNewBuildingReviewRule extends ExternalResource {

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Override
    protected void after() {
        try {
            newBuildingSteps.deleteReview();
        } catch (Throwable e) {
            log.info(String.format("Can't delete reviews. Exception: %s", e.getMessage()));
        }
    }
}
