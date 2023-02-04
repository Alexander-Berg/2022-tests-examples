# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    contains_inanyorder,
    equal_to,
    has_entries,
)

from intranet.yandex_directory.src.yandex_directory.core.models.service import ServiceModel
from intranet.yandex_directory.src.yandex_directory.core.models.preset import PresetModel
from testutils import TestCase


class TestPresetModel(TestCase):
    def setUp(self):
        super(TestPresetModel, self).setUp()
        PresetModel(self.meta_connection).delete(force_remove_all=True)
        self.tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
        )['slug']
        self.wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
        )['slug']
        self.disk = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='disk',
            name='disk',
        )['slug']
        self.settings = {
            'shared_contacts': True
        }

    def test_create(self):
        # создаем новую запись
        params = {
            'name': 'wiki-and-tracker',
            'service_slugs': [self.tracker, self.wiki],
            'settings': self.settings
        }
        preset = PresetModel(self.meta_connection).create(**params)
        # в пустой таблице появилась запись
        assert_that(
            PresetModel(self.meta_connection).count(),
            equal_to(1)
        )
        assert_that(
            preset,
                has_entries(
                **params
            )
        )

    def test_get(self):
        # создаем новую запись
        params = {
            'name': 'wiki',
            'service_slugs': [self.wiki],
            'settings': self.settings
        }
        preset = PresetModel(self.meta_connection).create(**params)
        # получаем данные по name
        assert_that(
            PresetModel(self.meta_connection).get(params['name']),
            preset
        )

    def test_filter(self):
        params = {
            'name': 'wiki',
            'service_slugs': [self.wiki],
            'settings': self.settings
        }
        wiki_preset = PresetModel(self.meta_connection).create(**params)
        params = {
            'name': 'wiki-disk',
            'service_slugs': [self.wiki, self.disk],
            'settings': self.settings
        }
        wiki_disk_preset = PresetModel(self.meta_connection).create(**params)
        params = {
            'name': 'tracker',
            'service_slugs': [self.tracker],
            'settings': self.settings
        }
        tracker_preset = PresetModel(self.meta_connection).create(**params)
        has_wiki_presets = PresetModel(self.meta_connection).find(
            filter_data={'service_slugs__contains': self.wiki}
        )
        assert_that(
            has_wiki_presets,
            contains_inanyorder(
                wiki_preset,
                wiki_disk_preset
            )
        )

        list_presets = PresetModel(self.meta_connection).find(
            filter_data={'name__in': ['wiki', 'tracker']}
        )
        assert_that(
            list_presets,
            contains_inanyorder(
                wiki_preset,
                tracker_preset
            )
        )

    def test_raise_error_if_incorrect_name(self):
        params = {
            'name': 'wiki!',
            'service_slugs': [self.wiki],
            'settings': self.settings
        }
        with self.assertRaises(RuntimeError):
            PresetModel(self.meta_connection).create(**params)

    def test_raise_error_if_incorrect_slug(self):
        params = {
            'name': 'wiki234',
            'service_slugs': ['some_slug'],
            'settings': self.settings
        }
        with self.assertRaises(RuntimeError):
            PresetModel(self.meta_connection).create(**params)
