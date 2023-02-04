
from django.db import connection

from wiki.intranet.models.intranet_extensions import Group
from wiki.sync.staff.mptt_healthcheck import healthcheck as hc
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class MpttHealthcheckTest(BaseTestCase):
    def _create_group(self, name, parent=None):
        return Group.objects.create(
            name=name,
            created_at='2020-08-03',
            modified_at='2020-08-03',
            url=name,
            parent=parent,
        )

    def test_valid_tree(self):
        Group.objects.all().delete()
        g1t1 = self._create_group('group1t1')
        g2t1 = self._create_group('group2t1', parent=g1t1)
        self._create_group('group3t1', parent=g2t1)

        g1t2 = self._create_group('group1t2')
        g2t2 = self._create_group('group2t2', parent=g1t2)
        self._create_group('group3t2', parent=g2t2)

        forest = hc.plant_forest()
        corrupted = forest.get_corrupted_trees()
        self.assertEqual(len(corrupted), 0)

    def test_mixed_trees(self):
        Group.objects.all().delete()
        g1t1 = self._create_group('group1t1')
        g2t1 = self._create_group('group2t1', parent=g1t1)
        g3t1 = self._create_group('group3t1', parent=g2t1)

        g1t2 = self._create_group('group1t2')
        g2t2 = self._create_group('group2t2', parent=g1t2)
        g3t2 = self._create_group('group3t2', parent=g2t2)

        # Ломаем дерево, без отключенного mptt_updates оно бы правильно перестроилось
        with Group.tree.disable_mptt_updates():
            g2t1.parent = g1t2
            g2t1.save()

        forest = hc.plant_forest()
        corrupted = forest.get_corrupted_trees()
        self.assertSetEqual({g1t1.tree_id, g1t2.tree_id}, corrupted)

        forest.rebuild_tree(g1t1.tree_id)
        forest.rebuild_tree(g1t2.tree_id)

        self.assertEqual(len(forest.trees[g1t1.tree_id].adj), 1)
        self.assertEqual(len(forest.trees[g1t2.tree_id].adj), 4)

        forest.apply_changes()
        for model in (g1t1, g2t1, g3t1, g1t2, g2t2, g3t2):
            model.refresh_from_db()

        self.assertEqual(g1t1.lft, 1)
        self.assertEqual(g1t1.rght, 2)
        self.assertEqual(g1t1.level, 0)

        self.assertEqual(g1t2.lft, 1)
        self.assertEqual(g1t2.rght, 10)
        self.assertEqual(g1t2.level, 0)

        self.assertEqual(g2t1.tree_id, g1t2.tree_id)
        self.assertEqual(g3t1.tree_id, g1t2.tree_id)

    def test_wrong_boudaries(self):
        Group.objects.all().delete()
        g1 = self._create_group('group1')
        g2 = self._create_group('group2', parent=g1)
        self._create_group('group3', parent=g2)

        with Group.tree.disable_mptt_updates():
            g1.rght = 999
            g1.save()

        forest = hc.plant_forest()
        corrupted = forest.get_corrupted_trees()
        self.assertSetEqual({g1.tree_id}, corrupted)

        forest.rebuild_tree(g1.tree_id)
        forest.apply_changes()
        g1.refresh_from_db()
        self.assertEqual(g1.rght, 6)

    def test_many_roots(self):
        Group.objects.all().delete()
        g1 = self._create_group('group1')
        g2 = self._create_group('group2')
        g3 = self._create_group('group3')
        g4 = self._create_group('group4', parent=g3)

        with connection.cursor() as cursor:
            cursor.execute('UPDATE intranet_group SET tree_id = 1')
            cursor.execute('UPDATE intranet_group SET level = 10')

        forest = hc.plant_forest()
        corrupted = forest.get_corrupted_trees()
        self.assertSetEqual({g1.tree_id}, corrupted)

        forest.rebuild_tree(g1.tree_id)
        self.assertEqual(len(forest.trees), 3)

        forest.apply_changes()
        self.assertNotEqual(g1.tree_id, g2.tree_id)
        self.assertNotEqual(g1.tree_id, g3.tree_id)
        self.assertNotEqual(g2.tree_id, g3.tree_id)
        self.assertEqual(g3.tree_id, g4.tree_id)
        self.assertEqual(g1.level, 0)
        self.assertEqual(g2.level, 0)
        self.assertEqual(g3.level, 0)
        self.assertEqual(g4.level, 1)
