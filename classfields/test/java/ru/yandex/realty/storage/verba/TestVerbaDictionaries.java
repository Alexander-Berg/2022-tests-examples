package ru.yandex.realty.storage.verba;

import org.joda.time.DateTime;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.realty.model.offer.AreaUnit;
import ru.yandex.realty.model.offer.DealStatus;
import ru.yandex.realty.model.offer.OfferType;
import ru.yandex.realty.model.offer.PricingPeriod;
import ru.yandex.verba2.model.Dictionary;
import ru.yandex.verba2.model.Term;
import ru.yandex.verba2.model.attribute.AliasAttribute;
import ru.yandex.verba2.model.attribute.Attribute;
import ru.yandex.verba2.model.attribute.WeightedValue;

import java.util.*;

public class TestVerbaDictionaries {
    private static Map<String, String> MONTH_NAMES = new HashMap<>();
    private static Map<String, String> SQUARE_M_NAMES = new HashMap<>();
    private static Map<String, String> SOTKA_NAMES = new HashMap<>();
    private static Map<String, String> RUB_NAMES = new HashMap<>();
    static {
        MONTH_NAMES.put(VerbaAliasBuilder.DEFAULT_LANG, "месяц");
        SQUARE_M_NAMES.put(VerbaAliasBuilder.DEFAULT_LANG, "кв.м");
        SOTKA_NAMES.put(VerbaAliasBuilder.DEFAULT_LANG, "сотка");
        RUB_NAMES.put(VerbaAliasBuilder.DEFAULT_LANG, "rub");
    }

    public static Dictionary PERIOD_TYPE = new Dictionary(1L, 1L, VerbaDictionary.PERIOD_TYPE.getCode(), "период", VerbaDictionary.PERIOD_TYPE.getCode(),
            Arrays.asList(
                    new Term(new Term(1L, PricingPeriod.PER_MONTH.name(), "месяц", 1, "test", DateTime.now(), DateTime.now()),
                            Cf.list((Attribute)new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), MONTH_NAMES)),
                            Collections.<Dictionary>emptyList())
            ));

    public static Dictionary AREA_TYPE = new Dictionary(2L, 1L, VerbaDictionary.AREA_UNIT_TYPE.getCode(), "площадь", VerbaDictionary.AREA_UNIT_TYPE.getCode(),
            Arrays.asList(
                    new Term(new Term(1L, AreaUnit.SQUARE_METER.name(), "кв.м", 1, "test", DateTime.now(), DateTime.now()), Cf.list((Attribute)
                            new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), SQUARE_M_NAMES)),
                            Collections.<Dictionary>emptyList()),
                    new Term(new Term(2L, AreaUnit.ARE.name(), "сотка", 1, "test", DateTime.now(), DateTime.now()), Cf.list((Attribute)
                            new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), SOTKA_NAMES)),                                Collections.<Dictionary>emptyList())
            ));

    public static Dictionary CURRENCY = new Dictionary(3L, 1L, VerbaDictionary.CURRENCY.getCode(), "Валюта", VerbaDictionary.CURRENCY.getCode(),
            Arrays.asList(
                    new Term(new Term(3L, Currency.RUR.name(), "Рубль", 1, "test", DateTime.now(), DateTime.now()), Cf.list((Attribute)
                            new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), RUB_NAMES)),
                            Collections.<Dictionary>emptyList())
            ));

    public static Dictionary TYPE = new Dictionary(4L, 1L, VerbaDictionary.TYPE.getCode(), "тип оффера", VerbaDictionary.TYPE.getCode(),
            Arrays.asList(
                    new Term(new Term(1L, OfferType.RENT.name(), "аренда", 1, "test", DateTime.now(), DateTime.now()),
                            Cf.list((Attribute)new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), Map.of(VerbaAliasBuilder.DEFAULT_LANG, "аренда"))),
                            Collections.<Dictionary>emptyList()),
                    new Term(new Term(2L, OfferType.SELL.name(), "продажа", 1, "test", DateTime.now(), DateTime.now()),
                            Cf.list((Attribute)new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), Map.of(VerbaAliasBuilder.DEFAULT_LANG, "продажа"))),
                            Collections.<Dictionary>emptyList())
            ));

    public static Dictionary DEAL_STATUS = new Dictionary(5L, 1L, VerbaDictionary.DEAL_STATUS.getCode(), "статус сделки", VerbaDictionary.DEAL_STATUS.getCode(),
            Arrays.asList(
                    new Term(new Term(1L, DealStatus.SALE.name(), "прямая продажа", 1, "test", DateTime.now(), DateTime.now()),
                            Cf.list((Attribute)new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), Map.of(VerbaAliasBuilder.DEFAULT_LANG, "прямая продажа"))),
                            Collections.<Dictionary>emptyList()),
                    new Term(new Term(2L, DealStatus.DIRECT_RENT.name(), "прямая аренда", 1, "test", DateTime.now(), DateTime.now()),
                            Cf.list((Attribute)new AliasAttribute("name", Collections.<String, List<WeightedValue>>emptyMap(), Map.of(VerbaAliasBuilder.DEFAULT_LANG, "прямая аренда"))),
                            Collections.<Dictionary>emptyList())
            ));

}
