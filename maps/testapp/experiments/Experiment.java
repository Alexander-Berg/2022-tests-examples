package com.yandex.maps.testapp.experiments;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Experiment implements Serializable {
    public final String serviceId;
    public final String parameterName;
    public final String parameterValue;
    public final String group;

    public Experiment(String serviceId, String parameterName, String parameterValue) {
        this(serviceId, parameterName, parameterValue, "");
    }

    public Experiment(String serviceId, String parameterName, String parameterValue, String experimentsGroup) {
        this.serviceId = serviceId;
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
        this.group = experimentsGroup;
    }

    public boolean experimentsConflict(Experiment exp) {
        if (this.serviceId.equals(exp.serviceId) && this.parameterName.equals(exp.parameterName)){
            return true;
        }
        if (!this.group.equals("") && this.group.equals(exp.group)){
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Experiment))
            return false;

        Experiment r = (Experiment) obj;

        return this.serviceId.equals(r.serviceId) &&
               this.parameterValue.equals(r.parameterValue) &&
               this.parameterName.equals(r.parameterName) &&
               this.group.equals(r.group);
    }
}
