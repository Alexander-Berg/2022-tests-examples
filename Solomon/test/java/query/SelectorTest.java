package ru.yandex.solomon.labels.query;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Oleg Baryshnikov
 */
public class SelectorTest {

    @Test
    public void extendedGlob() {
        assertEquals(Selector.extendedGlob("host", "-"), Selector.absent("host"));
        assertEquals(Selector.extendedGlob("host", "*"), Selector.any("host"));
        assertEquals(Selector.extendedGlob("host", "cluster"), Selector.glob("host", "cluster"));
    }

    @Test
    public void extendedNotGlob() {
        assertEquals(Selector.extendedNotGlob("host", "-"), Selector.any("host"));
        assertEquals(Selector.extendedNotGlob("host", "*"), Selector.absent("host"));
        assertEquals(Selector.extendedNotGlob("host", "cluster"), Selector.notGlob("host", "cluster"));
    }

    @Test
    public void extendedGlobAbsentOrTotal() {
        Selector selector = Selector.extendedGlob("target", "-|total");
        assertThat(selector.match(null), equalTo(true));
        assertThat(selector.match("total"), equalTo(true));
        assertThat(selector.match("solomon-01"), equalTo(false));
    }

    @Test
    public void any() {
        Selector selector = Selector.any("host");
        assertThat(selector.match(null), equalTo(false));
        assertThat(selector.match("solomon-01"), equalTo(true));
    }

    @Test
    public void absent() {
        Selector selector = Selector.absent("target");
        assertThat(selector.match(null), equalTo(true));
        assertThat(selector.match("solomon-01"), equalTo(false));
    }
}
