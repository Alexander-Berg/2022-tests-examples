package ru.yandex.auto.core.statistics.tree.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.auto.core.statistics.tree.TreeNode;
import ru.yandex.common.util.collections.Pair;

/** @author Leonid Nalchadzhi (nalchadzhi@yandex-team.ru) */
public class TreeNodeImplTest {

  @Test
  public void testStorage() {
    TreeNodeImpl<Integer> treeNodeImpl =
        new TreeNodeImpl<Integer>(new Integer[] {}, new LeafNodeImpl[] {});

    Assert.assertEquals(treeNodeImpl.size(), 0);

    treeNodeImpl.addChild(null, create(1));
    Assert.assertEquals(treeNodeImpl.size(), 1);
    Assert.assertTrue(Arrays.equals(treeNodeImpl.getKeys(), new Integer[] {null}));

    treeNodeImpl.addChild(1, create(2));
    Assert.assertEquals(treeNodeImpl.size(), 2);
    Assert.assertTrue(Arrays.equals(treeNodeImpl.getKeys(), new Integer[] {null, 1}));

    treeNodeImpl.addChild(2, create(3));
    Assert.assertEquals(treeNodeImpl.size(), 3);
    Assert.assertTrue(Arrays.equals(treeNodeImpl.getKeys(), new Integer[] {null, 1, 2}));

    treeNodeImpl.addChild(4, create(4));
    Assert.assertEquals(treeNodeImpl.size(), 4);
    Assert.assertTrue(Arrays.equals(treeNodeImpl.getKeys(), new Integer[] {null, 1, 2, 4}));

    treeNodeImpl.addChild(5, create(5));
    Assert.assertEquals(treeNodeImpl.size(), 5);
    Assert.assertTrue(Arrays.equals(treeNodeImpl.getKeys(), new Integer[] {null, 1, 2, 4, 5}));

    treeNodeImpl.addChild(null, create(6));
    Assert.assertTrue(Arrays.equals(treeNodeImpl.getKeys(), new Integer[] {null, 1, 2, 4, 5}));
    Assert.assertEquals(treeNodeImpl.size(), 5);

    treeNodeImpl.addChild(3, create(7));

    Assert.assertTrue(Arrays.equals(treeNodeImpl.getKeys(), new Integer[] {null, 1, 2, 3, 4, 5}));
    Assert.assertEquals(treeNodeImpl.size(), 6);
  }

  @Test
  public void testSearchStorage() {

    TreeNodeImpl<Integer> nodeStorage =
        new TreeNodeImpl<Integer>(new Integer[] {}, new LeafNodeImpl[] {});
    nodeStorage.addChild(null, create(0));
    nodeStorage.addChild(1, create(1));
    nodeStorage.addChild(2, create(2));
    nodeStorage.addChild(3, create(3));
    nodeStorage.addChild(4, create(4));

    List<Pair<Integer, TreeNode<?>>> sublist1 = nodeStorage.getChildrenSubListWithKeys(1, 2);
    Assert.assertEquals(sublist1.get(0).first.intValue(), 1);
    Assert.assertEquals(sublist1.get(1).first.intValue(), 2);
    Assert.assertEquals(sublist1.size(), 2);

    List<Pair<Integer, TreeNode<?>>> sublist2 = nodeStorage.getChildrenSubListWithKeys(4, null);
    Assert.assertEquals(sublist2.get(0).first.intValue(), 4);
    Assert.assertEquals(sublist2.size(), 1);

    List<Pair<Integer, TreeNode<?>>> sublist3 = nodeStorage.getChildrenSubListWithKeys(5, 2);
    Assert.assertEquals(sublist3.size(), 0);

    List<Pair<Integer, TreeNode<?>>> sublist4 = nodeStorage.getChildrenSubListWithKeys(5, null);
    Assert.assertEquals(sublist4.size(), 0);

    List<Pair<Integer, TreeNode<?>>> sublist5 = nodeStorage.getChildrenSubListWithKeys(null, 1);
    Assert.assertEquals(sublist5.size(), 2);
  }

  @Test
  public void testRemoveAllButThis() {
    TreeNodeImpl<Integer> t1 = new TreeNodeImpl<Integer>();
    t1.addChild(1, new TreeNodeImpl<Integer>());
    t1.addChild(2, new TreeNodeImpl<Integer>());
    t1.addChild(3, new TreeNodeImpl<Integer>());
    t1.addChild(null, new TreeNodeImpl<Integer>());

    Assert.assertEquals(t1.size(), 4);

    Set<Integer> toRetain = new HashSet<Integer>();
    toRetain.add(null);
    toRetain.add(1);
    t1 = TreeNodeImpl.removeKeysButThese(t1, toRetain);
    Assert.assertEquals(t1.size(), 2);

    Set<Integer> retainAgain = new HashSet<Integer>();
    retainAgain.add(1);
    retainAgain.add(2);
    t1 = TreeNodeImpl.removeKeysButThese(t1, retainAgain);
    Assert.assertEquals(t1.size(), 1);
  }

  @NotNull
  public LeafNodeImpl<Integer> create(Integer val) {
    return new LeafNodeImpl<Integer>(val);
  }
}
