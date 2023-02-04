package ru.yandex.solomon.expression.analytics;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.math.Crop;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayListOrView;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class GraphDataLoaderStub implements GraphDataLoader {
    private Map<Selectors, NamedGraphData[]> selectorToGraphData = new HashMap<>();

    public void putSelectorValue(String selectors, GraphData... graphData) {
        selectorToGraphData.put(Selectors.parse(selectors),
                Stream.of(graphData)
                        .map(NamedGraphData::of)
                        .toArray(NamedGraphData[]::new));
    }

    public void putSelectorValue(String selectors, NamedGraphData... namedGraphData) {
        selectorToGraphData.put(Selectors.parse(selectors), namedGraphData);
    }

    @Override
    public NamedGraphData[] loadGraphData(GraphDataLoadRequest request) {
        NamedGraphData[] result = selectorToGraphData.get(request.getSelectors());
        if (result == null || result.length == 0) {
            return new NamedGraphData[0];
        }

        return Stream.of(result)
            .map(namedGraphData -> toInterval(request, namedGraphData))
            .toArray(NamedGraphData[]::new);
    }

    private NamedGraphData toInterval(GraphDataLoadRequest request, NamedGraphData namedGraphData) {
        AggrGraphDataArrayListOrView cropped =
            Crop.crop(namedGraphData.getAggrGraphDataArrayList(), request.getInterval());
        return namedGraphData.toBuilder()
            .setGraphData(cropped)
            .build();
    }
}
