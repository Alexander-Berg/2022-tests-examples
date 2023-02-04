package ru.yandex.webmaster3.core.events2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum;
import ru.yandex.webmaster3.core.events2.events.HostGenerationUpdatedEvent;
import ru.yandex.webmaster3.core.events2.events.LinksStatisticsReceivedEvent;
import ru.yandex.webmaster3.core.events2.events.SanctionsGenerationUpdatedEvent;
import ru.yandex.webmaster3.core.events2.events.SecurityWarningStateUpdatedEvent;
import ru.yandex.webmaster3.core.events2.events.SiteStructureReceivedEvent;
import ru.yandex.webmaster3.core.events2.events.recheck.InfectionRecheckRequestedEvent;
import ru.yandex.webmaster3.core.events2.events.recheck.SanctionsRecheckRequestedEvent;
import ru.yandex.webmaster3.core.events2.events.recheck.SiteProblemRecheckRequested;
import ru.yandex.webmaster3.core.events2.events.sitemap.AddUserSitemapEvent;
import ru.yandex.webmaster3.core.events2.events.sitemap.DeleteUserSitemapEvent;
import ru.yandex.webmaster3.core.events2.events.sitemap.UpdateSitemapGenerationEvent;
import ru.yandex.webmaster3.core.events2.events.tools.CheckUrlRequestedEvent;
import ru.yandex.webmaster3.core.events2.events.tools.MobileAuditForUrlRequestedEvent;
import ru.yandex.webmaster3.core.events2.events.userhost.UserHostAddedEvent;
import ru.yandex.webmaster3.core.events2.events.userhost.UserHostDeletedEvent;
import ru.yandex.webmaster3.core.util.IdUtils;

import java.util.Collections;
import java.util.UUID;

/**
 * @author avhaliullin
 */
public class HostEventJsonUtilsTest {
    @Test
    public void testCompatibility() throws Exception {
        // Ситуация, когда что-то не удается распарсить из хранилища - очень неприятная. Вот как мы с ней боремся:
        // 1. Для каждого типа события должен быть хотя бы один тест на десериализацию
        // 2. Для каждого типа события должен быть хотя бы один тест на десериализацию текущего формата
        // Конструкция ниже форсит эти правила.
        // При добавлении нового события этот тест свалится, потому что для нового события нужно добавить тест
        // При изменении формата хранения события тест скорее всего свалится, потому что для текущего формата проверяется
        // "идемпотентность" сериализации. В таком случае нужно текущий тест сделать "старым" (helper.testCurrent -> helper.testOld),
        // а так же добавить хотя бы один новый тест (с новым форматом)
        // Удалять старые тесты не стоит, потому что облом старого теста означает, что в базе могут быть события,
        // которые текущий код не в состоянии распарсить

        CompatibilityTestHelper helper = new CompatibilityTestHelper();

        helper.testCurrent(
                AddUserSitemapEvent.class,
                "{\"sitemapUrl\":\"http://ya.ru\",\"sitemapId\":\"305cdcf0-efa6-11e5-b46c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals("http://ya.ru", event.getSitemapUrl());
                    Assert.assertEquals(UUID.fromString("305cdcf0-efa6-11e5-b46c-e5cd6e5d0104"), event.getSitemapId());
                }
        );

        helper.testCurrent(
                DeleteUserSitemapEvent.class,
                "{\"sitemapUrl\":\"http://ya.ru\",\"sitemapId\":\"305cdcf0-efa6-11e5-b46c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals("http://ya.ru", event.getSitemapUrl());
                    Assert.assertEquals(UUID.fromString("305cdcf0-efa6-11e5-b46c-e5cd6e5d0104"), event.getSitemapId());
                }
        );

        helper.testCurrent(
                HostGenerationUpdatedEvent.class,
                "{\"generationId\":\"305cdcf0-efa6-11e5-b46c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals(UUID.fromString("305cdcf0-efa6-11e5-b46c-e5cd6e5d0104"), event.getGenerationId());
                }
        );

        helper.testCurrent(
                UpdateSitemapGenerationEvent.class,
                "{\"generationId\":\"305cdcf0-efa6-11e5-b46c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals(UUID.fromString("305cdcf0-efa6-11e5-b46c-e5cd6e5d0104"), event.getGenerationId());
                }
        );

        helper.testOld(
                SiteStructureReceivedEvent.class,
                "{\"structureId\":\"305cdcf0-efa6-11e5-b46c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals(UUID.fromString("305cdcf0-efa6-11e5-b46c-e5cd6e5d0104"), event.getStructureId());
                }
        );

        helper.testCurrent(
                SiteStructureReceivedEvent.class,
                "{\"structureId\":\"305cdcf0-efa6-11e5-b46c-e5cd6e5d0104\",\"collectionDate\":1459423109753}",
                event -> {
                    Assert.assertEquals(UUID.fromString("305cdcf0-efa6-11e5-b46c-e5cd6e5d0104"), event.getStructureId());
                    Assert.assertEquals(new Instant(1459423109753L), event.getCollectionDate());
                }
        );

        helper.testCurrent(
                SecurityWarningStateUpdatedEvent.class,
                "{\"warningType\":\"SPAM\",\"state\":\"CLEAR\"}",
                event -> {
                    Assert.assertEquals("SPAM", event.getWarningType());
                    Assert.assertEquals("CLEAR", event.getState());
                }
        );

        helper.testCurrent(
                LinksStatisticsReceivedEvent.class,
                "{\"generationId\":\"305cdcf0-efa6-11e5-b46c-e5cd6e5d0104\",\"collectionDate\":1459423109753}",
                event -> {
                    Assert.assertEquals(UUID.fromString("305cdcf0-efa6-11e5-b46c-e5cd6e5d0104"), event.getGenerationId());
                    Assert.assertEquals(new Instant(1459423109753L), event.getCollectionDate());
                }
        );

        helper.testCurrent(
                CheckUrlRequestedEvent.class,
                "{\"url\":\"http://moskva.beeline.ru/ololo2\",\"taskId\": \"9c575620-f74d-11e5-b98c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals("http://moskva.beeline.ru/ololo2", event.getUrl());
                    Assert.assertEquals(UUID.fromString("9c575620-f74d-11e5-b98c-e5cd6e5d0104"), event.getTaskId());
                }
        );

        helper.testCurrent(
                MobileAuditForUrlRequestedEvent.class,
                "{\"url\":\"http://moskva.beeline.ru/ololo2\",\"taskId\": \"9c575620-f74d-11e5-b98c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals("http://moskva.beeline.ru/ololo2", event.getUrl().toExternalForm());
                    Assert.assertEquals(UUID.fromString("9c575620-f74d-11e5-b98c-e5cd6e5d0104"), event.getTaskId());
                }
        );

        helper.testCurrent(
                InfectionRecheckRequestedEvent.class,
                "{\"generationId\":\"9c575620-f74d-11e5-b98c-e5cd6e5d0104\"}",
                event -> {
                    Assert.assertEquals(UUID.fromString("9c575620-f74d-11e5-b98c-e5cd6e5d0104"), event.getGenerationId());
                }
        );

        helper.testCurrent(
                SiteProblemRecheckRequested.class,
                "{\"problemType\":\"DNS_ERROR\"}",
                event -> {
                    Assert.assertEquals(SiteProblemTypeEnum.DNS_ERROR, event.getProblemType());
                }
        );

        helper.testCurrent(
                SanctionsRecheckRequestedEvent.class,
                "{\"sanctions\":[\"ANTI_PF\"]}",
                event -> {
                    Assert.assertEquals(
                            Collections.singleton("ANTI_PF"),
                            event.getSanctions()
                    );
                }
        );

        helper.testCurrent(
                SanctionsGenerationUpdatedEvent.class,
                "{\"generationId\":\"9c575620-f74d-11e5-b98c-e5cd6e5d0104\", \"haveSanctions\": true}",
                event -> {
                    Assert.assertEquals(UUID.fromString("9c575620-f74d-11e5-b98c-e5cd6e5d0104"), event.getGenerationId());
                    Assert.assertEquals(true, event.isHaveSanctions());
                }
        );
        helper.testCurrent(
                SanctionsGenerationUpdatedEvent.class,
                "{\"generationId\":null, \"haveSanctions\": false}",
                event -> {
                    Assert.assertNull(event.getGenerationId());
                    Assert.assertEquals(false, event.isHaveSanctions());
                }
        );

        helper.testCurrent(
                UserHostAddedEvent.class,
                "{\"userId\":120160451}",
                event -> {
                    Assert.assertEquals(120160451, event.getUserId());
                }
        );

        helper.testCurrent(
                UserHostDeletedEvent.class,
                "{\"userId\":120160451}",
                event -> {
                    Assert.assertEquals(120160451, event.getUserId());
                }
        );

        helper.assertEverythingTested();
    }


    @Test
    public void testEventIdempotency() throws Exception {
        testEventIdempotency(
                new HostEvent("reqid", Instant.now(), UUID.randomUUID(), 1L, 2L, IdUtils.stringToHostId("http:ya.ru:80"),
                        new AddUserSitemapEvent("http://ya.ru/sm.xml", UUID.randomUUID())
                )
        );
        testEventIdempotency(
                new HostEvent("reqid", Instant.now(), null, 1L, null, IdUtils.stringToHostId("http:ya.ru:80"),
                        new AddUserSitemapEvent("http://ya.ru/sm.xml", UUID.randomUUID())
                )
        );
        testEventIdempotency(
                new HostEvent(null, Instant.now(), UUID.randomUUID(), null, 2L, IdUtils.stringToHostId("http:ya.ru:80"),
                        new DeleteUserSitemapEvent("http://ya.ru/sm.xml", UUID.randomUUID())
                )
        );
    }

    private void testEventIdempotency(HostEvent event) throws Exception {
        ObjectMapper om = new ObjectMapper();
        String serialized = HostEventJsonUtils.serializeEvent(event);
        HostEvent deserialized = HostEventJsonUtils.deserializeEvent(serialized);
        String serializedTwice = HostEventJsonUtils.serializeEvent(deserialized);
        Assert.assertEquals(om.readTree(serialized), om.readTree(serializedTwice));
    }

}
