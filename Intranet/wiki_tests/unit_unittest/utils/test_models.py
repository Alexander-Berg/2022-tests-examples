
from django.contrib.auth.models import Group

from wiki.utils.models import queryset_iterator
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class QuerySetIteratorTest(WikiDjangoTestCase):
    def setUp(self):
        Group.objects.bulk_create([Group(name='group_' + str(idx)) for idx in range(10)])
        self.all_objects = list(Group.objects.order_by('pk'))

    def test_chunksize_bigger_than_count(self):
        qs = Group.objects.all()

        with self.assertNumQueries(1):
            items = list(queryset_iterator(qs, chunk_size=15))
        self.assertEqual(items, self.all_objects)

    def test_chunksize_less_than_count_but_not_divisible(self):
        qs = Group.objects.all()

        with self.assertNumQueries(4):  # 3 + 3 + 3 + 1
            items = list(queryset_iterator(qs, chunk_size=3))
        self.assertEqual(items, self.all_objects)

    def test_chunksize_less_than_count_and_divisible(self):
        qs = Group.objects.all()

        with self.assertNumQueries(3):  # 5 + 5 + 0
            items = list(queryset_iterator(qs, chunk_size=5))
        self.assertEqual(items, self.all_objects)

    def test_chunksize_equal_count(self):
        qs = Group.objects.all()

        with self.assertNumQueries(2):  # 10 + 0
            items = list(queryset_iterator(qs, chunk_size=10))
        self.assertEqual(items, self.all_objects)

    def test_blank_queryset(self):
        qs = Group.objects.none()

        items = list(queryset_iterator(qs))
        self.assertEqual(items, [])
