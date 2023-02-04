package ru.yandex.partner.core.entity.block.type.siteversionandcontextpage;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.InternalRtbBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.type.commonshowvideoandstrategy.SiteVersionType;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.model.InternalContextPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

@CoreTest
class SiteVersionAvailabilityServiceTest {
    @Autowired
    SiteVersionAvailabilityService service;

    @Test
    void externalRtbAvailableVersionsTest() {
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.TURBO,
                SiteVersionType.TURBO_DESKTOP,
                SiteVersionType.AMP,
                SiteVersionType.MOBILE_FULLSCREEN,
                SiteVersionType.MOBILE_REWARDED,
                SiteVersionType.MOBILE_FLOORAD
        );
        Set<String> actual = service.getAvailableVersionsLiterals(RtbBlock.class);

        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void internallRtbAvailableVersionsTest() {
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.TURBO,
                SiteVersionType.TURBO_DESKTOP,
                SiteVersionType.MOBILE_FULLSCREEN,
                SiteVersionType.MOBILE_REWARDED,
                SiteVersionType.MOBILE_FLOORAD
        );
        Set<String> actual = service.getAvailableVersionsLiterals(InternalRtbBlock.class);

        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void internalRtbBaseVersionsTest() {
        InternalContextPage page = new InternalContextPage();

        Set<String> expected = Set.of(SiteVersionType.DESKTOP.getLiteral(), SiteVersionType.MOBILE.getLiteral());
        var actual = service.getBlockSiteVersions(page,
                service.getSiteVersionsThatDependOnFeature(InternalRtbBlock.class, Set.of()), true,
                InternalRtbBlock.class);

        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void externalRtbNoAllowedTurboNoTurboDesktopNoAmpTest() {
        ContextPage page = new ContextPage();
        var expected = Set.of(SiteVersionType.DESKTOP.getLiteral(), SiteVersionType.MOBILE.getLiteral());
        var depend = service.getSiteVersionsThatDependOnFeature(RtbBlock.class, Set.of());
        var actual = service.getBlockSiteVersions(
                page,
                depend,
                depend.contains(SiteVersionType.TURBO_DESKTOP.getLiteral()),
                RtbBlock.class
        );
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void externalRtbWithAllowedTurboNoTurboDesktopTest() {
        ContextPage page = new ContextPage()
                .withAllowedTurbo(true);
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.TURBO
        );
        var depend = service.getSiteVersionsThatDependOnFeature(RtbBlock.class, Set.of());
        var actual = service.getBlockSiteVersions(
                page,
                depend,
                depend.contains(SiteVersionType.TURBO_DESKTOP.getLiteral()),
                RtbBlock.class
        );
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }


    @Test
    void externalRtbNoAllowedTurboWithTurboDesktopTest() {
        ContextPage page = new ContextPage();
        var expected = Set.of(SiteVersionType.DESKTOP.getLiteral(), SiteVersionType.MOBILE.getLiteral());
        var depend = service.getSiteVersionsThatDependOnFeature(RtbBlock.class, Set.of("turbo_desktop_available"));
        var actual = service.getBlockSiteVersions(
                page,
                depend,
                depend.contains(SiteVersionType.TURBO_DESKTOP.getLiteral()), RtbBlock.class
        );
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void externalRtbWithAllowedTurboWithTurboDesktopTest() {
        ContextPage page = new ContextPage()
                .withAllowedTurbo(true);
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.TURBO,
                SiteVersionType.TURBO_DESKTOP
        );
        var depend = service.getSiteVersionsThatDependOnFeature(RtbBlock.class, Set.of("turbo_desktop_available"));
        var actual = service.getBlockSiteVersions(
                page,
                depend,
                depend.contains(SiteVersionType.TURBO_DESKTOP.getLiteral()),
                RtbBlock.class
        );
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void externalRtbWithMobileSitesTest() {
        ContextPage page = new ContextPage();
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.MOBILE_FULLSCREEN,
                SiteVersionType.MOBILE_REWARDED,
                SiteVersionType.MOBILE_FLOORAD
        );
        var depend = service.getSiteVersionsThatDependOnFeature(RtbBlock.class, Set.of("mobile_fullscreen_available",
                "mobile_rewarded_available", "mobile_floorad_available"));
        var actual = service.getBlockSiteVersions(
                page,
                depend,
                depend.contains(SiteVersionType.TURBO_DESKTOP.getLiteral()),
                RtbBlock.class
        );
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void internalRtbWithMobileSitesTest() {
        ContextPage page = new ContextPage();
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.MOBILE_FULLSCREEN,
                SiteVersionType.MOBILE_REWARDED,
                SiteVersionType.MOBILE_FLOORAD
        );
        var depend = service.getSiteVersionsThatDependOnFeature(InternalRtbBlock.class, Set.of(
                "mobile_fullscreen_available", "mobile_rewarded_available", "mobile_floorad_available"));
        var actual = service.getBlockSiteVersions(page, depend, true, InternalRtbBlock.class);
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void externalRtbWithAmpTest() {
        ContextPage page = new ContextPage()
                .withAllowedAmp(true);
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.AMP
        );
        var depend = service.getSiteVersionsThatDependOnFeature(RtbBlock.class, Set.of());
        var actual = service.getBlockSiteVersions(
                page,
                depend,
                depend.contains(SiteVersionType.TURBO_DESKTOP.getLiteral()),
                RtbBlock.class
        );
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void internalRtbNoTurboVersionTest() {
        InternalContextPage page = new InternalContextPage();
        var expected = Set.of(SiteVersionType.DESKTOP.getLiteral(), SiteVersionType.MOBILE.getLiteral());
        var depend = service.getSiteVersionsThatDependOnFeature(InternalRtbBlock.class, Set.of());
        var actual = service.getBlockSiteVersions(page, depend, true, InternalRtbBlock.class);
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    void internalRtbWithTurboVersionTest() {
        InternalContextPage page = new InternalContextPage()
                .withAllowedTurbo(true);
        var expected = SiteVersionType.literals(
                SiteVersionType.DESKTOP,
                SiteVersionType.MOBILE,
                SiteVersionType.TURBO_DESKTOP,
                SiteVersionType.TURBO
        );
        var depend = service.getSiteVersionsThatDependOnFeature(InternalRtbBlock.class, Set.of());
        var actual = service.getBlockSiteVersions(page, depend, true, InternalRtbBlock.class);
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }
}
