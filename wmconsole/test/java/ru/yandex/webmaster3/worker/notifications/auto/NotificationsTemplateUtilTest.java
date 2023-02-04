package ru.yandex.webmaster3.worker.notifications.auto;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.checklist.data.MobileAuditResolution;
import ru.yandex.webmaster3.core.checklist.data.NotMobileFriendlyStatus;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemContent;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum;
import ru.yandex.webmaster3.core.checklist.data.YaBrowserBadAdFormat;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.notification.LanguageEnum;
import ru.yandex.webmaster3.core.notification.UTMLabels;
import ru.yandex.webmaster3.core.sitemap.SitemapInfo;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.user.message.content.MessageContent;
import ru.yandex.wmtools.common.util.http.YandexHttpStatus;

/**
 * @author avhaliullin
 */
public class NotificationsTemplateUtilTest {
    @Test
    public void allEnabledProblemTypesShouldBeRenderable() {
        LanguageEnum lang = LanguageEnum.RU;
        String hostName = "https://ya.ru";
        WebmasterHostId hostId = IdUtils.urlToHostId(hostName);
        String hostIdString = "https:ya.ru:443";
        UTMLabels labels = UTMLabels.createEmail("test", LocalDate.now(), null);

        for (SiteProblemTypeEnum problemType : SiteProblemTypeEnum.ENABLED_PROBLEMS) {
            StringBuilder sb = new StringBuilder();
            SiteProblemContent content;
            switch (problemType) {
                case DISALLOWED_IN_ROBOTS:
                    content = new SiteProblemContent.DisallowedInRobots();
                    break;
                case DNS_ERROR:
                    content = new SiteProblemContent.DNSError();
                    break;
                case DOCUMENTS_MISSING_DESCRIPTION:
                    content = new SiteProblemContent.DocumentsMissingDescription();
                    break;
                case DOCUMENTS_MISSING_TITLE:
                    content = new SiteProblemContent.DocumentsMissingTitle();
                    break;
                case ERRORS_IN_SITEMAPS:
                    content = new SiteProblemContent.ErrorsInSitemaps(Collections.singletonList(
                            SitemapInfo.builder().id(UUID.randomUUID()).parentId(UUID.randomUUID()).url("https://example.com/123").build())
                    );
                    break;
                case MISSING_FAVICON:
                    content = new SiteProblemContent.MissingFavicon();
                    break;
                case MORDA_ERROR:
                    content = new SiteProblemContent.MordaError(DateTime.now(), SiteProblemContent.MordaError.ExtendedStatus.URL_NOT_FOUND, YandexHttpStatus.EXT_HTTP_2005_NOINDEX, true);
                    break;
                case MORDA_REDIRECTS:
                    content = new SiteProblemContent.MordaRedirects(DateTime.now(), hostName);
                    break;
                case NO_404_ERRORS:
                    content = new SiteProblemContent.No404Errors();
                    break;
                case NO_DICTIONARY_REGIONS:
                    content = new SiteProblemContent.NoDictionaryRegions();
                    break;
                case NO_METRIKA_COUNTER:
                    content = new SiteProblemContent.NoMetrikaCounter();
                    break;
                case NO_REGIONS:
                    content = new SiteProblemContent.NoRegions();
                    break;
                case NO_ROBOTS_TXT:
                    content = new SiteProblemContent.NoRobotsTxt();
                    break;
                case NO_SITEMAP_MODIFICATIONS:
                    content = new SiteProblemContent.NoSitemapModifications(DateTime.now());
                    break;
                case NOT_MOBILE_FRIENDLY:
                    content = new SiteProblemContent.NotMobileFriendly(MobileAuditResolution.BAD_TARGET_OWNER_FOR_BASE_URL, NotMobileFriendlyStatus.NOT_MOBILE_FRIENDLY.value());
                    break;
                case ROBOTS_TXT_ERROR:
                    content = new SiteProblemContent.RobotsTxtError();
                    break;
                case SITEMAP_NOT_SET:
                    content = new SiteProblemContent.SitemapNotSet();
                    break;
                case SLOW_AVG_RESPONSE:
                    content = new SiteProblemContent.SlowResponse(1000);
                    break;
                case THREATS:
                    content = new SiteProblemContent.Threats();
                    break;
                case TOO_MANY_BROKEN_LINKS:
                    content = new SiteProblemContent.TooManyBrokenLinks();
                    break;
                case TOO_MANY_URL_DUPLICATES:
                    content = new SiteProblemContent.TooManyUrlDuplicates(DateTime.now(), 1000);
                    break;
                case TURBO_DOCUMENT_BAN:
                    content = new SiteProblemContent.TurboDocumentBan(true, Collections.emptySet(), DateTime.now(), 100, Collections.emptyList());
                    break;
                case TURBO_ERROR:
                    content = new SiteProblemContent.TurboError(Collections.emptyList());
                    break;
                case TURBO_FEED_BAN:
                    content = new SiteProblemContent.TurboFeedBan(false, Collections.emptySet(), DateTime.now(), Collections.emptyList());
                    break;
                case TURBO_HOST_BAN:
                    content = new SiteProblemContent.TurboHostBan(SiteProblemContent.TurboHostBan.TurboHostBanSeverity.WARNING, 0L);
                    break;
                case TURBO_HOST_BAN_INFO:
                    content = new SiteProblemContent.TurboHostBan(SiteProblemContent.TurboHostBan.TurboHostBanSeverity.INFO, 0L);
                    break;
                case TURBO_WARNING:
                    content = new SiteProblemContent.TurboWarning(Collections.emptyList());
                    break;
                case TURBO_INSUFFICIENT_CLICKS_SHARE:
                    content = new SiteProblemContent.TurboInsufficientClicksShare(75);
                    break;
                case YABROWSER_BADAD:
                    content = new SiteProblemContent.YaBrowserBadAd(DateTime.now(), 1L, DateTime.now(), SiteProblemContent.YaBrowserBadAd.YaBrowserBadAdStatus.ACTIVE, EnumSet.noneOf(YaBrowserBadAdFormat.class));
                    break;
                case HOST_COMPANY_PROFILE_NOT_FILLED:
                    content = new SiteProblemContent.HostCompanyProfileNotFilled(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80), Collections.EMPTY_LIST);
                    break;
                case TOO_MANY_DOMAINS_ON_SEARCH:
                    content = new SiteProblemContent.TooManyDomainsOnSearch(Arrays.asList("http://a.khaliullin.info", "http://b.khaliullin.info"));
                    break;
                case MAIN_MIRROR_IS_NOT_HTTPS:
                    content = new SiteProblemContent.MainMirrorIsNotHttps();
                    break;
                case SSL_CERTIFICATE_ERROR:
                    content = new SiteProblemContent.SslCertificateError();
                    break;
                case HOST_COMPANY_PROFILE_CREATED:
                    content = new SiteProblemContent.HostCompanyProfileCreated("https://yandex.ru/sprav/1611673/edit?utm_source=webmaster_not_active");
                    break;
                case FAVICON_ERROR:
                    content = new SiteProblemContent.FaviconError();
                    break;
                case TURBO_LISTING_ERROR:
                    // ignore
                    return;
                case NO_METRIKA_COUNTER_CRAWL_ENABLED:
                    content = new SiteProblemContent.NoMetrikaCounterCrawlEnabled();
                    break;
                case NO_METRIKA_COUNTER_BINDING:
                    content = new SiteProblemContent.NoMetrikaCounterBinding();
                    break;
                default:
                    Assert.fail("Unknown problem type " + problemType);
                    content = null; // потому что джава не знает, что fail всегда выходит с ошибкой
            }
            NotificationsTemplateUtil.buildChecklistChangesMessage(lang, sb, hostName, hostIdString, labels,
                    new MessageContent.ChecklistChanges(hostId, problemType, DateTime.now(), DateTime.now(), content, 0), new StringBuilder()
            );
            Assert.assertTrue("Message should not be empty for type " + problemType, sb.length() > 0);
        }
    }
}
