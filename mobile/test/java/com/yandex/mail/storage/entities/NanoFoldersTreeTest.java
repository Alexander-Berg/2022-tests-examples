package com.yandex.mail.storage.entities;

import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.FolderType;
import com.yandex.mail.entity.NanoFoldersTree;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.util.UtilsKt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static java.util.Collections.emptyMap;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class NanoFoldersTreeTest {

    public static final long ROOT_FID = -1;

    public static final long INBOX_FID = -2;

    @Test
    public void sorted_sortsByType() {
        final Folder folder1 = makeFolder(INBOX_FID, "f1", 1, null, 0);
        final Folder folder2 = makeFolder(INBOX_FID, "f2", 2, null, 0);
        final Folder folder3 = makeFolder(INBOX_FID, "f3", 11, null, 0);

        assertFolderTreeSorts(listOf(folder1, folder2, folder3), 10, 31241);
    }

    @Test
    public void folderTree_sortsChildren() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "r|c0", 2, ROOT_FID, 0);
        final Folder child00 = makeFolder(100, "r|c0|c00", 2, 10L, 0);

        assertFolderTreeSorts(listOf(root, child0, child00), 10, 21432);
    }

    @Test
    public void folderTrees_sortsChildrenComplex() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "r|c0", 2, ROOT_FID, 0);
        final Folder child1 = makeFolder(11, "r|c1", 2, ROOT_FID, 0);
        final Folder child00 = makeFolder(100, "r|c0|c00", 2, 10L, 0);
        final Folder child01 = makeFolder(101, "r|c0|c01", 2, 10L, 0);
        final Folder child10 = makeFolder(110, "r|c1|c10", 2, 11L, 0);
        final Folder child11 = makeFolder(111, "r|c1|c11", 2, 11L, 0);
        final Folder child12 = makeFolder(112, "r|c1|c12", 2, 11L, 0);

        assertFolderTreeSorts(
                listOf(
                        root,
                        child0,
                        child00,
                        child01,
                        child1,
                        child10,
                        child11,
                        child12
                ),
                1000,
                3543
        );
    }

    @Test
    public void folderTree_sortsChildrenOnSameLevelByPosition() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "r|c0", 2, ROOT_FID, 1);
        final Folder child1 = makeFolder(11, "r|c1", 2, ROOT_FID, 5);
        final Folder child2 = makeFolder(12, "r|c2", 2, ROOT_FID, 20);
        final Folder child3 = makeFolder(13, "r|c3", 2, ROOT_FID, 100);

        assertFolderTreeSorts(listOf(root, child0, child1, child2, child3), 30, 64563);
    }

    @Test
    public void folderTree_sortsByCaseInsensitiveName() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "r|Bazinga", 2, ROOT_FID, 0);
        // note: our behavior is inconsistent with web:
        // we display uppercase first, web displays lowercase first
        final Folder child1 = makeFolder(11, "r|Child", 2, ROOT_FID, 0);
        final Folder child2 = makeFolder(12, "r|child", 2, ROOT_FID, 0);
        final Folder child3 = makeFolder(13, "r|whatever", 2, ROOT_FID, 0);

        assertFolderTreeSorts(listOf(root, child0, child1, child2, child3), 500, 325366);
    }

    @Test
    public void folderTree_sortsTopLevelFolders() {
        final Folder folder1 = makeFolder(ROOT_FID, "aaa", 1, null, 0);
        final Folder folder2 = makeFolder(ROOT_FID, "bbb", 2, null, 0);
        final Folder folder3 = makeFolder(ROOT_FID, "ccc", 11, null, 0);
        final Folder folder4 = makeFolder(ROOT_FID, "ddd", 2, null, 0);
        final Folder folder5 = makeFolder(ROOT_FID, "eee", 2, null, 0);

        assertFolderTreeSorts(listOf(folder1, folder2, folder4, folder5, folder3), 100, 30305);
    }

    @Config(qualifiers = "ru")
    @Test
    public void getFullDisplayName_isAwareOfLocale() {
        final Folder inbox = makeFolder(1, "Inbox", FolderType.INBOX.getServerType(), ROOT_FID, 0);
        final Folder child = makeFolder(2, "Inbox|Child", FolderType.USER.getServerType(), inbox.getFid(), 0);
        final Folder grandChild = makeFolder(3, "Inbox|Child|Grandchild", FolderType.USER.getServerType(), child.getFid(), 0);

        final NanoFoldersTree NanoFoldersTree = new NanoFoldersTree(IntegrationTestRunner.app(), listOf(grandChild, inbox, child), emptyMap());
        assertThat(NanoFoldersTree.getFullDisplayName(inbox)).isEqualTo("Входящие");
        assertThat(NanoFoldersTree.getFullDisplayName(child)).isEqualTo("Входящие|Child");
        assertThat(NanoFoldersTree.getFullDisplayName(grandChild)).isEqualTo("Входящие|Child|Grandchild");
    }

    @Test
    public void getDepth() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "r|c0", 2, ROOT_FID, 0);
        final Folder child1 = makeFolder(11, "r|c1", 2, ROOT_FID, 0);
        final Folder child00 = makeFolder(100, "r|c0|c00", 2, 10L, 0);
        final Folder child01 = makeFolder(101, "r|c0|c01", 2, 10L, 0);
        final Folder child10 = makeFolder(110, "r|c1|c10", 2, 11L, 0);
        final Folder child11 = makeFolder(111, "r|c1|c11", 2, 11L, 0);
        final Folder child12 = makeFolder(112, "r|c1|c12", 2, 11L, 0);

        final NanoFoldersTree tree = new NanoFoldersTree(
                RuntimeEnvironment.application,
                listOf(root, child0, child1, child00, child01, child10, child11, child12),
                emptyMap()
        );

        assertThat(tree.getDepth(root)).isEqualTo(0);
        assertThat(tree.getDepth(child0)).isEqualTo(1);
        assertThat(tree.getDepth(child1)).isEqualTo(1);
        assertThat(tree.getDepth(child00)).isEqualTo(2);
        assertThat(tree.getDepth(child01)).isEqualTo(2);
        assertThat(tree.getDepth(child10)).isEqualTo(2);
        assertThat(tree.getDepth(child11)).isEqualTo(2);
        assertThat(tree.getDepth(child12)).isEqualTo(2);
    }

    @Test
    public void handlesParentsAndChildren() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder inbox = makeFolder(INBOX_FID, "Inbox", 1, null, 0);
        final Folder child0 = makeFolder(10, "r|c0", 2, ROOT_FID, 0);
        final Folder child1 = makeFolder(11, "r|c1", 2, ROOT_FID, 0);
        final Folder child00 = makeFolder(100, "r|c0|c00", 2, 10L, 0);
        final Folder child01 = makeFolder(101, "r|c0|c01", 2, 10L, 0);
        final Folder inboxChild = makeFolder(-10, "Inbox|some_child", 2, INBOX_FID, 0);

        final NanoFoldersTree tree = new NanoFoldersTree(
                RuntimeEnvironment.application,
                listOf(root, inbox, child0, child1, child00, child01, inboxChild),
                emptyMap()
        );

        assertThat(tree.hasChildren(root)).isTrue();
        assertThat(tree.hasChildren(inboxChild)).isFalse();
        assertThat(tree.hasChildren(child0)).isTrue();

        assertThat(tree.getChildren(inbox)).containsOnly(inboxChild);

        assertThat(tree.getParent(root)).isNull();
        assertThat(tree.getParent(inboxChild)).isEqualTo(inbox);
        assertThat(tree.getParent(child00)).isEqualTo(child0);
    }

    @Test
    public void test_firstLastChild() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "r|c0", 2, ROOT_FID, 0);
        final Folder child1 = makeFolder(11, "r|c1", 2, ROOT_FID, 0);
        final Folder child00 = makeFolder(100, "r|c0|c00", 2, 10L, 0);
        final Folder child11 = makeFolder(111, "r|c1|c11", 2, 11L, 0);
        final Folder child12 = makeFolder(112, "r|c1|c12", 2, 11L, 0);
        final Folder child13 = makeFolder(113, "r|c1|c13", 2, 11L, 0);

        NanoFoldersTree tree = new NanoFoldersTree(
                RuntimeEnvironment.application,
                listOf(child12, child00, root, child13, child11, child1, child0),
                emptyMap()
        );

        assertThat(tree.isFirstChild(child0)).isTrue();
        assertThat(tree.isLastChild(child0)).isFalse();

        assertThat(tree.isFirstChild(child1)).isFalse();
        assertThat(tree.isLastChild(child1)).isTrue();

        assertThat(tree.isFirstChild(child00)).isTrue();
        assertThat(tree.isLastChild(child00)).isTrue();

        assertThat(tree.isFirstChild(child11)).isTrue();
        assertThat(tree.isLastChild(child11)).isFalse();

        assertThat(tree.isFirstChild(child12)).isFalse();
        assertThat(tree.isLastChild(child12)).isFalse();

        assertThat(tree.isFirstChild(child13)).isFalse();
        assertThat(tree.isLastChild(child13)).isTrue();
    }

    @Test
    public void isSystemFolderName_shouldReturnTrueForRootSystemName() {
        for (String name : UtilsKt.getSystemFoldersNames()) {
            assertThat(NanoFoldersTree.isSystemFolderName(name, null)).isTrue();
        }
    }

    @Test
    public void isSystemFolderName_shouldReturnFalseForNotSystemName() {
        final String notSystemName = "not system name";
        assertThat(UtilsKt.getSystemFoldersNames()).doesNotContain(notSystemName);
        assertThat(NanoFoldersTree.isSystemFolderName(notSystemName, null)).isFalse();
    }

    @Test
    public void isSystemFolderName_shouldReturnFalseForNotRootFolders() {
        for (String name : UtilsKt.getSystemFoldersNames()) {
            assertThat(NanoFoldersTree.isSystemFolderName(name, 1L)).isFalse();
        }
        final String notSystemName = "not system name";
        assertThat(UtilsKt.getSystemFoldersNames()).doesNotContain(notSystemName);
        assertThat(NanoFoldersTree.isSystemFolderName(notSystemName, 1L)).isFalse();
    }

    @Test
    public void getLocaleAwareName_shouldReturnFolderName() {
        final Folder folder = makeFolder(ROOT_FID, "r", 2, null, 0);

        final NanoFoldersTree tree = new NanoFoldersTree(
                RuntimeEnvironment.application,
                listOf(folder),
                emptyMap()
        );

        assertThat(tree.getLocaleAwareName(folder.getFid())).isEqualTo(folder.getName());
    }

    @Test
    public void getLocaleAwareName_shouldReturnChildName() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "r|c0", 2, ROOT_FID, 0);
        final Folder child1 = makeFolder(11, "r|c1", 2, ROOT_FID, 0);
        final Folder child00 = makeFolder(100, "r|c0|c00", 2, 10L, 0);
        final Folder child11 = makeFolder(111, "r|c1|c11", 2, 11L, 0);
        final Folder child12 = makeFolder(112, "r|c1|c12", 2, 11L, 0);
        final Folder child13 = makeFolder(113, "r|c1|c13", 2, 11L, 0);

        final NanoFoldersTree tree = new NanoFoldersTree(
                RuntimeEnvironment.application,
                listOf(root, child0, child1, child00, child11, child12, child13),
                emptyMap()
        );

        assertThat(tree.getLocaleAwareName(child12.getFid())).isEqualTo("c12");
        assertThat(tree.getLocaleAwareName(child13.getFid())).isEqualTo("c13");
        assertThat(tree.getLocaleAwareName(child1.getFid())).isEqualTo("c1");
    }

    @Test
    public void getLocaleAwareName_shouldReturnChildName_evenIfServerNamesDontContainParents() {
        final Folder root = makeFolder(ROOT_FID, "r", 2, null, 0);
        final Folder child0 = makeFolder(10, "c0", 2, ROOT_FID, 0);
        final Folder child1 = makeFolder(11, "c1", 2, ROOT_FID, 0);
        final Folder child00 = makeFolder(100, "c00", 2, 10L, 0);
        final Folder child11 = makeFolder(111, "c11", 2, 11L, 0);
        final Folder child12 = makeFolder(112, "c12", 2, 11L, 0);
        final Folder child13 = makeFolder(113, "c13", 2, 11L, 0);

        final NanoFoldersTree tree = new NanoFoldersTree(
                RuntimeEnvironment.application,
                listOf(root, child0, child1, child00, child11, child12, child13),
                emptyMap()
        );

        assertThat(tree.getLocaleAwareName(child12.getFid())).isEqualTo("c12");
        assertThat(tree.getLocaleAwareName(child13.getFid())).isEqualTo("c13");
        assertThat(tree.getLocaleAwareName(child1.getFid())).isEqualTo("c1");
    }

    @NonNull
    private static Folder makeFolder(
            long fid,
            @NonNull String name,
            int type,
            @Nullable Long parentServerFid,
            int position
    ) {
        Folder nanoFolder = EntitiesTestFactory.buildNanoFolder();
        return nanoFolder.copy(
                fid,
                type,
                name,
                position,
                parentServerFid,
                nanoFolder.getUnread_counter(),
                nanoFolder.getTotal_counter()
        );
    }

    private static void assertFolderTreeSorts(@NonNull List<Folder> sortedFolders, long retries, long randomSeed) {
        final Random random = new Random(randomSeed);
        for (int i = 0; i < retries; i++) {
            List<Folder> folders = new ArrayList<>(sortedFolders);
            Collections.shuffle(folders, random);
            assertThat(new NanoFoldersTree(IntegrationTestRunner.app(), folders, emptyMap()).getSortedFolders())
                    .containsExactlyElementsOf(sortedFolders);
        }
    }
}
