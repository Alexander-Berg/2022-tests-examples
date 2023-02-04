package ru.yandex.auto.core.phone.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.auto.core.util.IntDisjointSet;

/** @author Anton Irinev (airinev@yandex-team.ru) */
public class IntDisjointSetTest {

  @Test
  public void testInitializedSet() {
    IntDisjointSet dset = new IntDisjointSet(5);

    int iterCount = 0;
    for (IntList list : dset) {
      assertEquals(1, list.size());
      iterCount++;
    }
    assertEquals(5, iterCount);
  }

  @Test
  public void testOneSet() {
    IntDisjointSet dset = new IntDisjointSet(5);
    dset.unite(0, 1);
    dset.unite(1, 2);
    dset.unite(2, 3);
    dset.unite(3, 4);

    Iterator<IntList> iter = dset.iterator();
    assertEquals("[0, 1, 2, 3, 4]", sort(iter.next()));
    assertFalse(iter.hasNext());
  }

  @Test
  public void testTwoSets() {
    IntDisjointSet dset = new IntDisjointSet(5);
    dset.unite(0, 1);
    dset.unite(1, 2);
    dset.unite(3, 4);

    Iterator<IntList> iter = dset.iterator();
    assertEquals("[0, 1, 2]", sort(iter.next()));
    assertEquals("[3, 4]", sort(iter.next()));
    assertFalse(iter.hasNext());
  }

  @Test
  public void testIteratorReentrance() {
    IntDisjointSet dset = new IntDisjointSet(5);
    dset.unite(0, 1);
    dset.unite(1, 2);
    dset.unite(2, 3);
    dset.unite(3, 4);

    Iterator<IntList> iter = dset.iterator();
    assertEquals("[0, 1, 2, 3, 4]", sort(iter.next()));
    assertFalse(iter.hasNext());

    Iterator<IntList> iter2 = dset.iterator();
    assertEquals("[0, 1, 2, 3, 4]", sort(iter2.next()));
    assertFalse(iter2.hasNext());
  }

  // helper methods

  private static String sort(@NotNull IntList list) {
    List<Integer> indexes = new ArrayList<Integer>(list);
    Collections.sort(indexes);
    return indexes.toString();
  }
}
