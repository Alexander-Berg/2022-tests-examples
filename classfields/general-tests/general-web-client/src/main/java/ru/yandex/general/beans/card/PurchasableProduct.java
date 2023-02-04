package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class PurchasableProduct {

    String productCode;
    String priceKopecks;
    String productType;

    private static PurchasableProduct purchasableProduct() {
        return new PurchasableProduct();
    }

    public static PurchasableProduct defaultFreeVas() {
        return purchasableProduct().setProductCode("raise_1").setPriceKopecks("4900").setProductType("FreeRaiseVas");
    }

}
