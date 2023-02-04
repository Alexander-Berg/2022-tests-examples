package ru.yandex.auto.core.util;

import static ru.yandex.common.util.collections.CollectionFactory.list;

import java.util.Iterator;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;

/** @author ssimonchik */
public class SubListScissorsTest {

  @Test
  public void test1() {
    List<String> orig = list("a", "b", "c");
    SubListScissors<String> sc = SubListScissors.newSubListScissors(orig, 10);
    int i = 0;
    for (List<String> subList : sc) {
      i++;
      Assert.assertEquals(subList, orig);
    }
    Assert.assertTrue(i == 1);
  }

  @Test
  public void test2() {
    List<Integer> orig = list(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    SubListScissors<Integer> sc = SubListScissors.newSubListScissors(orig, 3);
    Iterator<SubList<Integer>> iter = sc.iterator();

    Assert.assertEquals(iter.next(), list(1, 2, 3));
    Assert.assertEquals(iter.next(), list(4, 5, 6));
    Assert.assertEquals(iter.next(), list(7, 8, 9));
    Assert.assertEquals(iter.next(), list(10));

    Assert.assertFalse(iter.hasNext());
  }

  @Test
  public void test3() {
    List<Integer> orig = list(1, 2, 3);
    SubListScissors<Integer> sc = SubListScissors.newSubListScissors(orig, 1);
    Iterator<SubList<Integer>> iter = sc.iterator();

    Assert.assertEquals(iter.next(), list(1));
    Assert.assertEquals(iter.next(), list(2));
    Assert.assertEquals(iter.next(), list(3));

    Assert.assertFalse(iter.hasNext());
  }
}
