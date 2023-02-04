package com.yandex.maps.testapp.experiments;

import java.util.Comparator;

public class ExperimentComparator implements Comparator<Experiment> {

    @Override
    public int compare(Experiment left, Experiment right) {
        if (!left.serviceId.equals(right.serviceId))
            return left.serviceId.compareTo(right.serviceId);

        if (!left.parameterName.equals(right.parameterName))
            return left.parameterName.compareTo(right.parameterName);

        if (!left.parameterValue.equals(right.parameterValue))
            return left.parameterValue.compareTo(right.parameterValue);

        return left.group.compareTo(right.group);
    }
}
