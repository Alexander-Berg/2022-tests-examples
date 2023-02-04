package ru.yandex.auto.core.statistics.tree.arraytree;

import java.nio.ByteBuffer;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.auto.core.statistics.tree.LeafNode;
import ru.yandex.auto.core.statistics.tree.impl.LeafNodeImpl;
import ru.yandex.auto.core.statistics.tree.impl.TreeNodeImpl;

/** @author Leonid Nalchadzhi (nalchadzhi@yandex-team.ru) */
public class ArrayTreeBuilderTest {

  @Test
  public void getChildrenByIntKeysTest() {
    ArrayTreeBuilder atb =
        new ArrayTreeBuilder(TestNodeType.values(), Collections.EMPTY_MAP, null) {
          @NotNull
          @Override
          protected byte[] serializeLeaf(@NotNull LeafNode leafNode) {
            return ((String) leafNode.getLeafData()).getBytes();
          }
        };

    TreeNodeImpl<Integer> treeNode = buildTestTree();
    treeNode.acceptVisitor(atb);

    ByteBuffer result = atb.getResult();

    int[] firstLevelChildren = ArrayTreeUtils.getChildrenOfIntTree(result, 0);
    Assert.assertArrayEquals(firstLevelChildren, new int[] {26, 70, 145});
  }

  @Test
  public void getIntKeysTest() {

    ArrayTreeBuilder atb =
        new ArrayTreeBuilder(TestNodeType.values(), Collections.EMPTY_MAP, null) {
          @NotNull
          @Override
          protected byte[] serializeLeaf(@NotNull LeafNode leafNode) {
            return ((String) leafNode.getLeafData()).getBytes();
          }
        };

    TreeNodeImpl<Integer> treeNode = buildTestTree();
    treeNode.acceptVisitor(atb);

    ByteBuffer result = atb.getResult();

    int[] firstLevelChildren = ArrayTreeUtils.getChildrenOfIntTree(result, 0);

    Assert.assertArrayEquals(firstLevelChildren, new int[] {26, 70, 145});

    int[] intKeys = ArrayTreeUtils.getIntKeys(result, 70);

    Assert.assertArrayEquals(intKeys, new int[] {-1, 6, 8, 10});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70), new int[] {104, 106, 119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, null, null),
        new int[] {104, 106, 119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, null, 9), new int[] {106, 119});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, null, 11), new int[] {106, 119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 5, null), new int[] {106, 119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 7, null), new int[] {119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 5, 11), new int[] {106, 119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 7, 11), new int[] {119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 6, 11), new int[] {106, 119, 132});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 6, 9), new int[] {106, 119});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 6, 6), new int[] {106});
    Assert.assertArrayEquals(
        ArrayTreeUtils.getChildrenOfIntTree(result, 70, 6, 7), new int[] {106});
    Assert.assertArrayEquals(ArrayTreeUtils.getChildrenOfIntTree(result, 70, 7, 7), new int[] {});
  }

  private enum TestNodeType implements ArrayNodeType {
    L1,
    L2,
    L3;

    @NotNull
    @Override
    public KeyType getKeyType() {
      return KeyType.INT;
    }

    @Override
    public boolean isFromDictionary() {
      return false;
    }
  }

  @NotNull
  private TreeNodeImpl<Integer> buildTestTree() {
    TreeNodeImpl<Integer> root = new TreeNodeImpl<Integer>();

    TreeNodeImpl<Integer> l11 = new TreeNodeImpl<Integer>();
    TreeNodeImpl<Integer> l12 = new TreeNodeImpl<Integer>();
    TreeNodeImpl<Integer> l13 = new TreeNodeImpl<Integer>();

    root.addChild(1, l11);
    root.addChild(2, l12);
    root.addChild(3, l13);

    TreeNodeImpl<Integer> l21 = new TreeNodeImpl<Integer>();
    TreeNodeImpl<Integer> l22 = new TreeNodeImpl<Integer>();

    l11.addChild(4, l21);
    l11.addChild(5, l22);

    TreeNodeImpl<Integer> lnull = new TreeNodeImpl<Integer>();
    TreeNodeImpl<Integer> l23 = new TreeNodeImpl<Integer>();
    TreeNodeImpl<Integer> l24 = new TreeNodeImpl<Integer>();
    TreeNodeImpl<Integer> l25 = new TreeNodeImpl<Integer>();

    l12.addChild(null, lnull);
    l12.addChild(6, l23);
    l12.addChild(8, l24);
    l12.addChild(10, l25);

    TreeNodeImpl<Integer> l28 = new TreeNodeImpl<Integer>();
    TreeNodeImpl<Integer> l29 = new TreeNodeImpl<Integer>();

    l13.addChild(8, l28);
    l13.addChild(7, l29);

    LeafNodeImpl<String> leaf1 = new LeafNodeImpl<String>("a");
    LeafNodeImpl<String> leaf2 = new LeafNodeImpl<String>("b");
    LeafNodeImpl<String> leaf3 = new LeafNodeImpl<String>("c");
    LeafNodeImpl<String> leaf4 = new LeafNodeImpl<String>("d");
    LeafNodeImpl<String> leaf5 = new LeafNodeImpl<String>("e");
    LeafNodeImpl<String> leaf6 = new LeafNodeImpl<String>("f");
    LeafNodeImpl<String> leaf7 = new LeafNodeImpl<String>("f");

    l21.addChild(11, leaf1);
    l22.addChild(12, leaf2);

    l23.addChild(13, leaf3);
    l24.addChild(14, leaf4);
    l25.addChild(15, leaf7);

    l28.addChild(16, leaf5);
    l29.addChild(17, leaf6);

    return root;
  }
}
