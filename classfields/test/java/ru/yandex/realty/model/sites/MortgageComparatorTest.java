package ru.yandex.realty.model.sites;

import org.junit.Test;
import ru.yandex.realty.model.message.Mortgages.Bank;
import ru.yandex.realty.model.sites.special.proposals.MortgageComparator;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;

/**
 * User: azakharov
 * Date: 21.10.16
 */
public class MortgageComparatorTest {

    private final Comparator<Mortgage> c = new MortgageComparator();

    @Test
    public void testGeneralCase() {
        Mortgage m1 = new Mortgage(1, 1L, "Ипотека 1");
        m1.setMinRate(5.0f);
        Mortgage m2 = new Mortgage(1, 1L, "Ипотека 2");
        m2.setMinRate(12.0f);
        assertEquals(-1, c.compare(m1, m2));
        assertEquals(1, c.compare(m2, m1));
    }

    @Test
    public void testMainProposal() {
        Mortgage m1 = new Mortgage(1, 1L, "Ипотека 1");
        m1.setMinRate(5.0f);
        Mortgage m2 = new Mortgage(1, 1L, "Ипотека 2");
        m2.setMinRate(12.0f);
        m2.setMainProposal(true);

        // Main proposal has highest priority
        assertEquals(1, c.compare(m1, m2));
        assertEquals(-1, c.compare(m2, m1));

        // Two main proposals are sorted as they are not main
        m1.setMainProposal(true);
        m2.setMainProposal(true);
        assertEquals(-1, c.compare(m1, m2));
        assertEquals(1, c.compare(m2, m1));
    }
}
