package ru.yandex.wmconsole.service;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster.common.urltree.YandexSearchShard;
import ru.yandex.wmconsole.data.info.all.about.url.*;

import java.util.Collections;
import java.util.Date;

/**
 * User: azakharov
 * Date: 02.12.13
 * Time: 13:36
 */
public class AllAboutUrlServiceTest {

    private AllAboutUrlService allAboutUrlService = new AllAboutUrlService();

    private static DbPartialUrlInfo createEmptyUrlInfo() {
        return new DbPartialUrlInfo(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void testKiwiProdHasDataAndIsSearchable() {
        DbPartialUrlInfo prodUrlInfo_SEMIDUP_OR_ANTISPAM = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEMIDUP_OR_ANTISPAM = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEMIDUP_OR_ANTISPAM,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SEMIDUP_OR_ANTISPAM, allAboutUrlService.calculateStatus(dbUrlInfo_SEMIDUP_OR_ANTISPAM));

        DbPartialUrlInfo prodUrlInfo_SEARCHABLE_FAKE_PROD = new DbPartialUrlInfo(
                new Date(), null, "http://test.ru", null, null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEARCHABLE_FAKE_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEARCHABLE_FAKE_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, true, null, null);
        Assert.assertEquals(UrlStatusEnum.SEARCHABLE_FAKE_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_SEARCHABLE_FAKE_PROD));

        DbPartialUrlInfo prodUrlInfo_SEARCHABLE_PROD = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEARCHABLE_PROD = new DbUrlInfo(
               (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEARCHABLE_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, true, null, null);
        Assert.assertEquals(UrlStatusEnum.SEARCHABLE_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_SEARCHABLE_PROD));

        DbPartialUrlInfo prodUrlInfo_SEARCHABLE_REDIRECT_PROD = new DbPartialUrlInfo(
                new Date(), 301, "http://test.ru/index.html", "http://test.ru/index.html", null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEARCHABLE_REDIRECT_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEARCHABLE_REDIRECT_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, true, null, null);
        Assert.assertEquals(UrlStatusEnum.SEARCHABLE_REDIRECT_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_SEARCHABLE_REDIRECT_PROD));

        DbPartialUrlInfo prodUrlInfo_SEARCHABLE_REFRESH_PROD = new DbPartialUrlInfo(
                new Date(), 2004, "http://test.ru/index.html", null, null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEARCHABLE_REFRESH_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEARCHABLE_REFRESH_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, true, null, null);
        Assert.assertEquals(UrlStatusEnum.SEARCHABLE_REFRESH_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_SEARCHABLE_REFRESH_PROD));

        DbPartialUrlInfo prodUrlInfo_SEARCHABLE_DUPLICATE_PROD = new DbPartialUrlInfo(
                new Date(), 100, "http://test.ru/index.html", null, null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEARCHABLE_DUPLICATE_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEARCHABLE_DUPLICATE_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, true, null, null);
        Assert.assertEquals(UrlStatusEnum.SEARCHABLE_DUPLICATE_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_SEARCHABLE_DUPLICATE_PROD));

        DbPartialUrlInfo prodUrlInfo_SEARCHABLE_DUPLICATE_SLASH = new DbPartialUrlInfo(
                new Date(), 100, "http://test.ru/", null, null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEARCHABLE_DUPLICATE_SLASH = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEARCHABLE_DUPLICATE_SLASH,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, true, null, null);
        Assert.assertEquals(UrlStatusEnum.SEARCHABLE_DUPLICATE_SLASH, allAboutUrlService.calculateStatus(dbUrlInfo_SEARCHABLE_DUPLICATE_SLASH));
    }

    @Test
    public void testKiwiProdHasDataAndNotSearchableAndHasHtarcData() {
        DbPartialUrlInfo prodUrlInfo_SELECTION_RANK1 = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, false, UrlNoUploadReasonEnum.SR, true, null, null);
        DbPartialUrlInfo htarcData_SELECTION_RANK1 = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, true, null, null, null, null);
        DbUrlInfo dbUrlInfo_SELECTION_RANK1 = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SELECTION_RANK1,
                createEmptyUrlInfo(), htarcData_SELECTION_RANK1, YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SELECTION_RANK, allAboutUrlService.calculateStatus(dbUrlInfo_SELECTION_RANK1));

        DbPartialUrlInfo prodUrlInfo_SELECTION_RANK2 = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, false, null, true, null, null);
        DbPartialUrlInfo htarcData_SELECTION_RANK2 = new DbPartialUrlInfo(
                new Date(), 3021, "http://test.ru", null, null, null, true, null, null, null, null);
        DbUrlInfo dbUrlInfo_SELECTION_RANK2 = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SELECTION_RANK2,
                createEmptyUrlInfo(), htarcData_SELECTION_RANK2, YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SELECTION_RANK, allAboutUrlService.calculateStatus(dbUrlInfo_SELECTION_RANK2));

        DbPartialUrlInfo prodUrlInfo_NOT_CANONICAL = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, false, null, true, null, null);
        DbPartialUrlInfo htarcData_NOT_CANONICAL = new DbPartialUrlInfo(
                new Date(), 2025, "http://test.ru", null, null, null, true, null, null, null, null);
        DbUrlInfo dbUrlInfo_NOT_CANONICAL = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_NOT_CANONICAL,
                createEmptyUrlInfo(), htarcData_NOT_CANONICAL, YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.NOT_CANONICAL, allAboutUrlService.calculateStatus(dbUrlInfo_NOT_CANONICAL));

        DbPartialUrlInfo prodUrlInfo_CLEAN_PARAMS = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, false, null, true, null, null);
        DbPartialUrlInfo htarcData_CLEAN_PARAMS = new DbPartialUrlInfo(
                new Date(), 2021, "http://test.ru", null, null, null, true, null, true, null, null);
        DbUrlInfo dbUrlInfo_CLEAN_PARAMS = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_CLEAN_PARAMS,
                createEmptyUrlInfo(), htarcData_CLEAN_PARAMS, YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.CLEAN_PARAMS, allAboutUrlService.calculateStatus(dbUrlInfo_CLEAN_PARAMS));

        DbPartialUrlInfo prodUrlInfo_SEMIDUP_OR_ANTISPAM = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, false, null, true, null, null);
        DbPartialUrlInfo htarcData_DUPLICATE_PROD1 = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, true, null, null, null, null);
        DbUrlInfo dbUrlInfo_SEMIDUP_OR_ANTISPAM = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEMIDUP_OR_ANTISPAM,
                createEmptyUrlInfo(), htarcData_DUPLICATE_PROD1, YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SEMIDUP_OR_ANTISPAM, allAboutUrlService.calculateStatus(dbUrlInfo_SEMIDUP_OR_ANTISPAM));

        DbPartialUrlInfo prodUrlInfo_NOT_CANONICAL_PROD = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru/canonical.html", null, null, null, false, null, true, null, null);
        DbPartialUrlInfo htarcData_NOT_CANONICAL_PROD = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru/canonical.html", null, null, null, true, null, null, null, null);
        DbUrlInfo dbUrlInfo_NOT_CANONICAL_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_NOT_CANONICAL_PROD,
                createEmptyUrlInfo(), htarcData_NOT_CANONICAL_PROD, YandexSearchShard.RU, false, null, null);
        dbUrlInfo_NOT_CANONICAL_PROD.setRelCanonicalTargetUrls(Collections.singletonList("http://test.ru/canonical.html"));
        Assert.assertEquals(UrlStatusEnum.NOT_CANONICAL_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_NOT_CANONICAL_PROD));
    }

    @Test
    public void testKiwiProdHasDataAndNotSearchableAndHasNoHtarcData() {
        DbPartialUrlInfo prodUrlInfo_SELECTION_RANK = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, false, UrlNoUploadReasonEnum.SR, true, null, null);
        DbUrlInfo dbUrlInfo_SELECTION_RANK = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SELECTION_RANK,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SELECTION_RANK, allAboutUrlService.calculateStatus(dbUrlInfo_SELECTION_RANK));

        DbPartialUrlInfo prodUrlInfo_SEMIDUP_OR_ANTISPAM = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru", null, null, null, false, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEMIDUP_OR_ANTISPAM = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEMIDUP_OR_ANTISPAM,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SEMIDUP_OR_ANTISPAM, allAboutUrlService.calculateStatus(dbUrlInfo_SEMIDUP_OR_ANTISPAM));

        DbPartialUrlInfo prodUrlInfo_NOT_CANONICAL_PROD = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru/canonical.html", null, null, null, false, null, true, null, null);
        DbUrlInfo dbUrlInfo_NOT_CANONICAL_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_NOT_CANONICAL_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        dbUrlInfo_NOT_CANONICAL_PROD.setRelCanonicalTargetUrls(Collections.singletonList("http://test.ru/canonical.html"));
        Assert.assertEquals(UrlStatusEnum.NOT_CANONICAL_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_NOT_CANONICAL_PROD));

        DbPartialUrlInfo prodUrlInfo_DUPLICATE_PROD = new DbPartialUrlInfo(
                new Date(), 200, "http://test.ru/canonical.html", null, null, null, false, null, true, null, null);
        DbUrlInfo dbUrlInfo_DUPLICATE_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_DUPLICATE_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        dbUrlInfo_DUPLICATE_PROD.setRelCanonicalTargetUrls(Collections.singletonList("http://test.ru"));
        Assert.assertEquals(UrlStatusEnum.DUPLICATE_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_DUPLICATE_PROD));

        DbPartialUrlInfo prodUrlInfo_REDIRECT_PROD = new DbPartialUrlInfo(
                new Date(), 302, "http://test.ru", null, null, null, false, null, true, null, null);
        DbUrlInfo dbUrlInfo_REDIRECT_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_REDIRECT_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.REDIRECT_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_REDIRECT_PROD));

        DbPartialUrlInfo prodUrlInfo_REFRESH_PROD = new DbPartialUrlInfo(
                new Date(), 2004, "http://test.ru", null, null, null, false, null, true, null, null);
        DbUrlInfo dbUrlInfo_REFRESH_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_REFRESH_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.REFRESH_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_REFRESH_PROD));

        DbPartialUrlInfo prodUrlInfo_SITE_ERROR_PROD = new DbPartialUrlInfo(
                new Date(), 404, "http://test.ru", null, null, null, false, null, true, null, null);
        DbUrlInfo dbUrlInfo_SITE_ERROR_PROD = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SITE_ERROR_PROD,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SITE_ERROR_PROD, allAboutUrlService.calculateStatus(dbUrlInfo_SITE_ERROR_PROD));

        DbPartialUrlInfo prodUrlInfo_SEMIDUP_OR_ANTISPAM2 = new DbPartialUrlInfo(
                new Date(), 201, "http://test.ru", null, null, null, false, null, true, null, null);
        DbUrlInfo dbUrlInfo_SEMIDUP_OR_ANTISPAM2 = new DbUrlInfo(
                (long)1, "http://test.ru", "http://test.ru", UrlInfoStatusEnum.READY, new Date(), false, null, new Date(), 0L, prodUrlInfo_SEMIDUP_OR_ANTISPAM2,
                createEmptyUrlInfo(), createEmptyUrlInfo(), YandexSearchShard.RU, false, null, null);
        Assert.assertEquals(UrlStatusEnum.SEMIDUP_OR_ANTISPAM, allAboutUrlService.calculateStatus(dbUrlInfo_SEMIDUP_OR_ANTISPAM2));
    }
}
