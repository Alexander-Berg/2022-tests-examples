package ru.yandex.webmaster3.storage.originaltext;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class OriginalTextServiceTest {
    @Test
    public void parseOriginalTextsXml() throws Exception {
        String xml = "<wsw-data add-data=\"true\">\n"
                + "\t<wsw-task edit=\"true\" moderate=\"false\" moderate-enable=\"false\" id=\"wmtest.people.yandex.net\">\n"
                + "\t\t<wsw-fields page=\"Originals-submission-form\" pager-from=\"1\" pager-count=\"9\" edit=\"true\">\n"
                + "\t\t\t<wsw-field base-id=\"cf92967bdd52bae4ed344d9294f4c892\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1468256320\">0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef ...</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"7d83a5b6fa97284b09c543ff2b68294b\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1428918350\">zzzzzzzzzzzz abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee ...</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"38daa8794e206a78363c7eddb673e18b\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1428918304\">abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee abbcccddddeeeee ...</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"8e2dddd1193e6a2196b24fb4ccdb7fc1\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1414758577\">.,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х\n"
                + ".,! - 2-мя 2-х ...</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"8c07cb80cdd572748f821c451f56f27f\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1414596115\">super test super test super test super test super test super test super test super test super test super test\n"
                + "super test super test super test super test super test super test super test super test super test super test\n"
                + "super test super test super test ...</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"7a806a02824b14c739d5d3874958cd1d\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1413805343\">Если вы публикуете на своем сайте оригинальные тексты, а их перепечатывают другие интернет-ресурсы, предупредите Яндекс о скором выходе текста. Мы будем знать, что оригинальный текст впервые появился именно на вашем сайте, и попробуем использовать это в ...</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"7ac0f6a959a6065337ec8546118596ab\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1400071183\">testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"6ecfb8f2ac4e22f123454cb7a62b840e\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1397225977\">asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t\t<wsw-field base-id=\"0f583ef0a40aed1bb70412200bb98f74\" name=\"Original_text\" deleted=\"false\" editable=\"false\">\n"
                + "\t\t\t\t<wsw-value last-update=\"1382016771\">test test test test test test test testtest test test test test test test testtest test test test test test test testtest test test test test test test testtest test test test test test test testtest test test test test test test testtest test test test ...</wsw-value>\n"
                + "\t\t\t</wsw-field>\n"
                + "\t\t</wsw-fields>\n"
                + "\t</wsw-task> \t<result>OK</result>\n"
                + "</wsw-data>";
        OriginalsResponse result = DirectOriginalTextService.parseOriginalTextsXml(xml);
        Assert.assertEquals(9, result.getTotalTexts());
        Assert.assertEquals(9, result.getTexts().size());

        Assert.assertEquals("cf92967bdd52bae4ed344d9294f4c892", result.getTexts().get(0).getTextId());
        Assert.assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                        + " 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                        + " 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                        + " 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                        + " ...",
                result.getTexts().get(0).getText()
        );
        DateTimeZone timeZone = DateTimeZone.forOffsetHours(3);
        Assert.assertEquals(new DateTime("2016-07-11T19:58:40+03:00").withZone(timeZone),
                result.getTexts().get(0).getDate().withZone(timeZone));
    }
}
