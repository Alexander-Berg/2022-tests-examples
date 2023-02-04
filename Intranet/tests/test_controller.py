from itertools import chain

from django.test import TestCase

from staff.navigation.controller import get_navigations_dict, NAVIGATION_FIELDS
from staff.navigation.models import Navigation


def pairs_iterator(iterable):
    iterator = iter(iterable)
    prev = None
    for cur in iterator:
        if prev:
            yield prev, cur
        prev = cur


class GetNavigationsDictTest(TestCase):
    def setUp(self):
        self.main = [
            Navigation(
                is_main=True,
                position=1,
                name_en='main_1',
                is_active=True,
                application_id='main_1',
            ),
            Navigation(
                is_main=True,
                position=2,
                name_en='main_2',
                is_active=True,
                application_id='main_2',
            ),
            Navigation(
                is_main=True,
                position=3,
                name_en='main_3',
                is_active=True,
                application_id='main_3',
            ),
        ]
        self.secondary = [
            Navigation(
                is_main=False,
                position=1,
                name_en='secondary_1',
                is_active=True,
                application_id='secondary_1',
            ),
            Navigation(
                is_main=False,
                position=2,
                name_en='secondary_2',
                is_active=True,
                application_id='secondary_2',
            ),
        ]
        self.navigations = list(
            chain.from_iterable([self.main, self.secondary])
        )
        for n in self.navigations:
            n.save()

    def test_groups(self):
        navigations_dict = get_navigations_dict()

        self.assertIn('main', navigations_dict)
        self.assertIn('secondary', navigations_dict)

        self.assertEqual(
            len(navigations_dict['main']),
            len(self.main)
        )
        self.assertEqual(
            len(navigations_dict['secondary']),
            len(self.secondary)
        )

    def test_fields(self):
        navigations_dict = get_navigations_dict()
        for group in ['main', 'secondary']:
            for n in navigations_dict[group]:

                self.assertNotIn(NAVIGATION_FIELDS[-1], n)

                for field in NAVIGATION_FIELDS[:-1]:
                    self.assertIn(field, n)

    def test_filter_active(self):
        self.main[1].is_active = False
        self.main[1].save()

        navigations_dict = get_navigations_dict()

        self.assertEqual(
            len(navigations_dict['main']),
            len(self.main) - 1
        )
