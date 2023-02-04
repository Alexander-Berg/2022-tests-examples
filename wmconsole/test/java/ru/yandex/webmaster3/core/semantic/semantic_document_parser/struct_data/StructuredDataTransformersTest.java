package ru.yandex.webmaster3.core.semantic.semantic_document_parser.struct_data;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.webmaster3.core.semantic.review_business.auto.model.agg.AggAutoReview;
import ru.yandex.webmaster3.core.semantic.review_business.biz.model.BizReview;
import ru.yandex.webmaster3.core.semantic.review_business.biz.model.impl.json.BizReviewJsonConversions;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.MicrodataUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.ComplexMicrodata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.Microdata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.FrontEnd;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.MicroformatsUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.data.MicroformatData;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.exceptions.MFException;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.spec.instances.*;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.struct_data.wrapper.AggAutoReviewWrapper;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static ru.yandex.common.util.collections.CollectionFactory.newList;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 8/10/11
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */
@Ignore
public class StructuredDataTransformersTest{
    @Before
    public void setLog(){
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");

    }
    @Test
    public void testFromSchemaOrgOrganization() throws Exception {
        final URL url = new URL("http://www.apoi.ru/internet-kompaniya-sweetcode-nizhnij-novgorod-ul-kujbysheva-13#reviews&id=69838");
        final String content = IOUtils.readInputStream(url.openStream());
        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        for (final Microdata data : result) {
            final Organization org =
                    StructuredDataTransformers.fromSchemaOrgOrganization((ComplexMicrodata) data, url.toString());
            if (org != null) {
                final StringBuilder sb = new StringBuilder();
                org.toXml().toXml(sb);
                System.out.println(sb.toString());
            }
        }
    }

    @Test
    public void testFromHOrgAuto() throws Exception {
        final URL url = new URL("http://bizovo.ru/reviews/prokopevsk-toyota-caldina-1994-3584.html");
        final String content = IOUtils.readInputStream(url.openStream());
        final Pair<List<MicroformatData>, List<MFException>> result = MicroformatsUtils.extractMF(content, url.toString(), MicroformatsManager.managerForMFsAndIncluded(HCard.getInstance(), HReview.getInstance(),
                HRecipe.getInstance(), HMedia.getInstance(), HCalendarEvent.getInstance(),
                HResume.getInstance(), HNews.getInstance(), HAtom.getInstance(), HAudio.getInstance(),
                HCalendar.getInstance(), HReviewAggregate.getInstance()), false);
        for (final MicroformatData data : result.getFirst()) {
            final AggAutoReview org = StructuredDataTransformers.fromAutoHReview(data, url.toString());
            if (org != null) {
                final StringBuilder sb = new StringBuilder();
                sb.append(new AggAutoReviewWrapper(org).toJsonString());
                System.out.println(sb.toString());
            }
        }
    }

    @Test
    public void testFromSchemaOrgAuto() throws Exception {
        final URL url = new URL("http://otzovik.com/review_746095.html");
        final String content = IOUtils.readInputStream(url.openStream());
        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        for (final Microdata data : result) {
            final AggAutoReview org = StructuredDataTransformers.fromSchemaOrgAutoReview((ComplexMicrodata) data, url.toString());
            if (org != null) {
                final StringBuilder sb = new StringBuilder();
                sb.append(new AggAutoReviewWrapper(org).toJsonString());
                System.out.println(sb.toString());
            }
        }
    }


    @Test
    public void testMovie() throws Exception {
        final URL url = new URL("http://www.imdb.com/title/tt2090463/");
        final String content = (new FrontEnd()).downloadWithTimeout(url);
        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        for (final Microdata data : result) {
            final Movie org =
                    StructuredDataTransformers.fromSchemaOrgMovie((ComplexMicrodata) data, url.toString(), "eng");
            if (org != null) {
                final StringBuilder sb = new StringBuilder();
                org.toXml().toXml(sb);
                System.out.println(sb.toString());
            }

        }
    }

    @Ignore
    @Test
    public void test() throws IOException {
        Iterable<String> classes = IOUtils.readLines("/Users/slewa/Documents/test");
        StringBuilder sb = new StringBuilder();
        for (String aClass : classes) {
            sb.append("\"").append(aClass).append("\"").append(", ");
        }
        System.out.println(sb.toString());
    }

    @Test
    public void testCreativeWork() throws Exception {
        List<URL> urls = newList();
//        urls.add(new URL("http://www.google.com.tr/patents/US20030167568"));
        urls.add(new URL("http://www.scrapjazz.com/gallery/image/digital/550442.html"));
//        urls.add(new URL("http://gdevkurske.ru/otzyv-n198.html"));
        urls.add(new URL("http://www.ftbpro.com/es/posts/michael.finkel/110511/cr%C3%B3nica-atlas-2-1-am%C3%A9rica"));
        for (URL url : urls) {
//            final String content = (new FrontEnd()).downloadWithTimeout(url);
            String content = IOUtils.readInputStream(url.openStream());
            final List<Microdata> result = MicrodataUtils.extractMD(content, "http://localhost", true, false);
            for (final Microdata data : result) {
                final CreativeWork creativeWork =
                        StructuredDataTransformers.fromSchemaOrgCreativeWork((ComplexMicrodata) data,
                                url.toString(),"eng");
                if (creativeWork != null) {
                    final StringBuilder sb = new StringBuilder();
                    creativeWork.toXml().toXml(sb);
                    System.out.println(sb.toString());
                }
            }

        }

    }

    @Test
    public void testAggregateRating() throws Exception {
        final String content = "  <div itemscope itemtype=\"http://schema.org/AggregateRating\">\n" +
                "    <span itemprop=\"ratingValue\">4</span> stars -\n" +
                "    <span itemprop=\"worstRating\">4</span> stars -\n" +
                "    <span itemprop=\"bestRating\">4</span> stars -\n" +
                "    based on <span itemprop=\"reviewCount\">250</span> reviews\n" +
                "  </div>";
        final List<Microdata> result = MicrodataUtils.extractMD(content, "http://localhost", true, false);
        for (final Microdata data : result) {
            final AggregateRating rating =
                    StructuredDataTransformers.fromSchemaOrgAggregateRating((ComplexMicrodata) data,
                            "http://localhost");
            if (rating != null) {
                final StringBuilder sb = new StringBuilder();
                rating.toXml().toXml(sb);
                System.out.println(sb.toString());
            }
        }

    }

    @Test
    public void testChords() throws Exception {
        final URL url =
                new URL("http://vvorobey.ru/song/ne_prognemsya");
        final String content = (new FrontEnd()).downloadWithTimeout(url);
        final List<Microdata> result = MicrodataUtils.extractMD(content, "http://vvorobey.ru/song/ne_prognemsya", true, false);
        for (final Microdata data : result) {
            final Chords rating =
                    StructuredDataTransformers.fromSchemaOrgChords((ComplexMicrodata) data, "http://vvorobey.ru/song/ne_prognemsya", "rus");
            if (rating != null) {
                final StringBuilder sb = new StringBuilder();
                rating.toXml().toXml(sb);
                System.out.println(sb.toString());
            }
        }

    }

    @Test
    public void testEncArticleJson() throws Exception {
        final URL url = new URL("http://universalium.academic.ru/195264/settlement_worker");
        final String content = (new FrontEnd()).downloadWithTimeout(url);
        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        for (final Microdata data : result) {
            final EncArticle org =
                    StructuredDataTransformers.fromYandexMicrodataEncArticle((ComplexMicrodata) data, url.toString());
            if (org != null) {
                System.out.println(org.asJson().toString());
            }
        }
    }

    @Test
    public void testReview() throws Exception {
        final URL url =
                new URL("http://www.autodrive.ru/company/obuhov-kievskoe-shosse/reviews/39239923890/");
        final String content = (new FrontEnd()).downloadWithTimeout(url);
        final String content1 = "<div itemscope itemtype=\"http://schema.org/Review\">\n" +
                "  <h2 itemprop=\"name\"><a href=\"http://example.com/review?10231\" itemprop=\"url\">Русская кухня в изгнании</a></h2>\n" +
                "  <div>Отзыв написал <span itemprop=\"author\" itemscope itemtype=\"http://schema.org/Person\">\n" +
                "    <span itemprop=\"name\">\n" +
                "      <a itemprop=\"url\" href=\"http://example.com/users/vasya\">Вася Пупкин</a>\n" +
                "    </span>\n" +
                "  </span>\n" +
                "    <meta itemprop=\"datePublished\" content=\"2012-07-15\" />\n" +
                "    15 июля 2012.\n" +
                "  </div>\n" +
                "  <div itemprop=\"reviewRating\" itemscope itemtype=\"http://schema.org/Rating\">\n" +
                "    <meta itemprop=\"worstRating\" content=\"0\"/>\n" +
                "    <p>Оценка: <span itemprop=\"ratingValue\">9</span> из <span itemprop=\"bestRating\">10</span>.</p>\n" +
                "  </div>\n" +
                "  <div itemprop=\"pro\">Бесплатная стоянка, прекрасная детская комната и предупредительные официанты.</div>\n" +
                "  <div itemprop=\"contra\">Большой и шумный некурящий зал.</div>\n" +
                "  <div itemprop=\"reviewBody\">\n" +
                "    <p>Заказ был готов сравнительно быстро, а напитки приготовили практически сразу.\n" +
                "    Обслуживание на уровне, хотя грязная посуда иногда застаивалась.</p>\n" +
                "    <p>Рекомендую русскую кухню, особенно супы.</p>\n" +
                "  </div>\n" +
                "  <div>Автор посетил заведение <meta itemprop=\"dateVisited\" content=\"2012-07-10\">10 июля 2012.</div>\n" +
                "  <div>Оценки характеристик ресторана:\n" +
                "    <ul>\n" +
                "      <li itemprop=\"tag\" itemscope itemtype=\"http://schema.org/Rating\">\n" +
                "        <link itemprop=\"ratingTarget\" href=\"http://webmaster.yandex.ru/vocabularies/ReviewBusiness/Cuisine.xml\">\n" +
                "        Кухня: <span itemprop=\"ratingValue\">5</span> из <span itemprop=\"bestRating\">5</span>;\n" +
                "      </li>\n" +
                "      <li itemprop=\"tag\" itemscope itemtype=\"http://schema.org/Rating\">\n" +
                "        <link itemprop=\"ratingTarget\" href=\"http://webmaster.yandex.ru/vocabularies/ReviewBusiness/Hall.xml\">\n" +
                "        Зал: <span itemprop=\"ratingValue\">3</span> из <span itemprop=\"bestRating\">5</span>;\n" +
                "      </li>\n" +
                "    </ul>\n" +
                "  </div>\n" +
                "  <div itemprop=\"itemReviewed\" itemscope itemtype=\"http://schema.org/Hotel\">\n" +
                "    <h3>Информация о ресторане</h3>\n" +
                "    <p>Название: <span itemprop=\"name\">Заграница</span></p>\n" +
                "    <p itemprop=\"address\" itemscope itemtype=\"http://schema.org/PostalAddress\">Адрес:\n" +
                "      <span itemprop=\"addressLocality\">Москва</span>, <span itemprop=\"streetAddress\">Тверская, 7</span>.\n" +
                "    </p>\n" +
                "    <p itemprop=\"address\" itemscope itemtype=\"http://schema.org/PostalAddress\">Адрес:\n" +
                "      <span itemprop=\"addressLocality\">Нижневартовск</span>, <span itemprop=\"streetAddress\">Псковская, 7</span>.\n" +
                "    </p>\n" +
                "    <p>Телефон: <span itemprop=\"telephone\">123-45-12</span>.</p>\n" +
                "    <p>Сайт ресторана: <a itemprop=\"url\" href=\"http://zagranica.ru\">http://zagranica.ru</a></p>\n" +
                "    <p>Электронный адрес администрации:\n" +
                "      <a itemprop=\"email\" href=\"mailto:hostess@zagranica.ru\">hostess@zagranica.ru</a>\n" +
                "    </p>\n" +
                "  </div>\n" +
                "</div>";
        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        for (final Microdata data : result) {
            final BizReview org =
                    StructuredDataTransformers.fromSchemaOrgReview((ComplexMicrodata) data, url.toString());
            if (org != null) {
                System.out.println(BizReviewJsonConversions.toJsonString(org));
            }
        }

    }




    @Test
    public void testHReview() throws Exception {
        final URL url =
                new URL("http://www.gazeta-a.ru/uslugi/msk/avtoshkoli_/start_altyfievo/item/4646");
        final String content = (new FrontEnd()).downloadWithTimeout(url);
        final Pair<List<MicroformatData>, List<MFException>> result =
                MicroformatsUtils.extractMF(content, url.toString(),
                        MicroformatsManager.managerForMFsAndIncluded(HReview.getInstance()), true);
        for (final MicroformatData data : result.first) {
            final BizReview org = StructuredDataTransformers.fromBizHReview(data, url.toString());
            if (org != null) {
                System.out.println(BizReviewJsonConversions.toJsonString(org));
            }
        }
    }
    @Test
    public void testRecipe() throws Exception {
        String urlSource = "http://www.familyoven.com/recipe/game-day-food/309194";
        final URL url = new URL(urlSource);
        final String content = (new FrontEnd()).downloadWithTimeout(url);
        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        for (final Microdata data : result) {
            final Recipe org =
                    StructuredDataTransformers.fromSchemaOrgRecipe((ComplexMicrodata) data, url.toString());
            if (org != null) {
                final StringBuilder sb = new StringBuilder();
                org.toXml().toXml(sb);
                System.out.println(sb.toString());
            }
        }
    }
}
