package ru.yandex.realty.searcher.response;

import junit.framework.Assert;
import junit.framework.TestCase;
import ru.yandex.realty.searcher.response.builders.OfferResponseBuilder;

import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA.
 * User: alesavin
 * Date: 4/25/11
 * Time: 3:09 PM
 */
public class AddCgiParamTest extends TestCase {

    public void testA() throws MalformedURLException {
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?utm_source=advmaker",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276?utm_source=advmaker", "utm_source=advmaker"));
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?&utm_source=advmaker",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276?", "utm_source=advmaker"));
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?utm_source=advmaker",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276", "utm_source=advmaker"));
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?rrrr&utm_source=advmaker",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276?rrrr", "utm_source=advmaker"));


        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?utm_source=advmaker#",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276#", "utm_source=advmaker"));

        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?&utm_source=advmaker#",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276?#", "utm_source=advmaker"));
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?utm_source=advmaker#?",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276#?", "utm_source=advmaker"));
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?utm_source=advmaker#?fffff",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276#?fffff", "utm_source=advmaker"));
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?fffff&utm_source=advmaker#",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276?fffff#", "utm_source=advmaker"));
        Assert.assertEquals("http://alesavin.ru/sale/flat/2888276?fffff&utm_source=advmaker#eeeee",
                OfferResponseBuilder.addCgiParam("http://alesavin.ru/sale/flat/2888276?fffff#eeeee", "utm_source=advmaker"));


        Assert.assertEquals("alesavin",
                OfferResponseBuilder.addCgiParam("alesavin", "utm_source=advmaker"));


    }





}
