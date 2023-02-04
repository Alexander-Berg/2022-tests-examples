package ru.yandex.realty.beans.developer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class DeveloperTab {

    private int order;
    private String type;
    private String regionLink;
    private String callTradeInTitle;
    private String callTradeInDescription;
    private String webSiteUrl;
    private String buttonText;
    private String siteId;
    private String phone;

    public static DeveloperTab tab() {
        return new DeveloperTab();
    }

    //TODO: отделить наполнение данными от структуры данных
    public static DeveloperTab tradeInTabTemplate() {
        return tab().setOrder(1)
                .setType("TRADE_IN")
                .setCallTradeInTitle("Ипотека 6,1%")
                .setCallTradeInDescription("Ипотека со ставкой 6,1%* от Сбербанка на приобретение квартир.")
                .setWebSiteUrl("//realty.yandex.ru/moskva_i_moskovskaya_oblast/kupit/novostrojka/?developerId=269952&hasSiteMortgage=YES")
                .setButtonText("Выбрать объект");
    }

    public static DeveloperTab callTabTemplate() {
        return tab().setOrder(1)
                .setType("CALL")
                .setSiteId("2011383")
                .setCallTradeInTitle("Апартаменты в ЖК «Поклонная 9»")
                .setCallTradeInDescription("Рассрочка 0% до 30 мес. 3 стиля дизайнерской отделки. Сервис отеля 5*.")
                .setPhone("+7 (495) 401-70-08");
    }


}
