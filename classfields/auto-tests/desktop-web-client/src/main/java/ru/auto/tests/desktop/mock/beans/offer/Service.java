package ru.auto.tests.desktop.mock.beans.offer;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.consts.SaleServices;

@Setter
@Getter
@Accessors(chain = true)
public class Service {

    String service;
    boolean prolongable;
    @SerializedName("is_active")
    boolean isActive;
    @SerializedName("create_date")
    String createDate;
    @SerializedName("expire_date")
    String expireDate;
    @SerializedName("auto_prolong_price")
    Integer autoProlongPrice;
    Integer days;
    @SerializedName("prolongation_forced_not_togglable")
    boolean prolongationForcedNotTogglable;

    public Service setService(SaleServices.VasProduct serviceName) {
        this.service = serviceName.getValue();
        return this;
    }

    public static Service service() {
        return new Service();
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
