package ru.auto.tests.desktop.step;

import io.qameta.allure.Step;
import lombok.experimental.Accessors;
import ru.auto.tests.api.SearcherClient;
import ru.auto.tests.api.beans.AutoruBreadcrumbs;
import ru.auto.tests.api.beans.AutoruBreadcrumbsListItem;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Accessors(chain = true)
public class SearcherUserSteps {

    @Inject
    private SearcherClient searcherClient;

    @Step("Серчер: получаем название марки из серчера")
    public AutoruBreadcrumbsListItem getMark(String mark) throws IOException {
        return getItem(searcherClient.autoruBreadcrumbs().execute().body(), mark);
    }

    @Step("Серчер: получаем название модели из серчера")
    public AutoruBreadcrumbsListItem getModel(String mark, String model) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("mark", mark);

        return getItem(searcherClient.autoruBreadcrumbs(params).execute().body(), model);
    }

    private AutoruBreadcrumbsListItem getItem(AutoruBreadcrumbs body, String name) {
        return body.getData()
                .get(0).get(0).getData().stream()
                .filter(searcherMark -> searcherMark.getName().equals(name))
                .findFirst().get();
    }
}
