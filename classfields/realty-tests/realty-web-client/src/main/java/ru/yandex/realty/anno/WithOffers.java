package ru.yandex.realty.anno;

import ru.auto.test.api.realty.OfferType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author kurau (Yuri Kalinin)
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface WithOffers {
    int count();

    OfferType offerType() default OfferType.APARTMENT_SELL;

    int createDay() default 0;
    int updateDay() default 0;

    String accountType() default "owner";
}
