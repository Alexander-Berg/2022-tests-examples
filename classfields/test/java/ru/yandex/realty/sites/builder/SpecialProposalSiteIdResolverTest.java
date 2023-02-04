package ru.yandex.realty.sites.builder;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * User: azakharov
 * Date: 14.10.16
 */
public class SpecialProposalSiteIdResolverTest {

    @Test
    public void testResolve() {

        final Set<Long> siteIds = new HashSet<>(Arrays.asList(1L, 2L, 3L));
        final List<Long> company1SiteIds = Arrays.asList(1L, 10L, 20L);
        final List<Long> company2SiteIds = Arrays.asList(100L, 101L, 102L);

        final Map<Long, List<Long>> companyIdToSiteIds = new LinkedHashMap<>();
        companyIdToSiteIds.put(22L, company1SiteIds);
        companyIdToSiteIds.put(23L, company2SiteIds);

        SpecialProposalSiteIdResolver resolver = new SpecialProposalSiteIdResolver(companyIdToSiteIds);
        final Set<Long> resolved = resolver.resolve(siteIds, Arrays.asList(22L, 23L));
        final Set<Long> expected = new HashSet<>();
        // all siteIds are passed to output
        expected.addAll(siteIds);
        // only those companySiteIds are passed to output which companySiteIds are not in siteIds
        expected.addAll(company2SiteIds);

        assertEquals(expected, resolved);
    }
}
