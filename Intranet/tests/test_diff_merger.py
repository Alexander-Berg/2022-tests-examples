# coding: utf-8
from __future__ import unicode_literals

from sync_tools.datagenerators import BaseDataGenerator
from sync_tools.diff_merger import BaseDataDiffMerger


class SyncObject(object):
    def __init__(self, slug, name):
        self.slug = slug
        self.name = name

    def __unicode__(self):
        return 'slug=%s, name=%s' % (self.slug, self.name)

    def __eq__(self, other):
        return self.slug == other.slug and self.name == other.name

    def __repr__(self):
        return self.__unicode__()


class FakeDataGenerator(BaseDataGenerator):
    sync_fields = ('slug',)
    diff_fields = ('name',)

    def __init__(self):
        super(FakeDataGenerator, self).__init__()
        self._int_objects = [
            SyncObject(slug='old', name='Old'),
            SyncObject(slug='both', name='Old name'),
        ]

    def get_internal_objects(self):
        return [
            {'slug': obj.slug, 'name': obj.name} for obj in self._int_objects
        ]

    def get_external_objects(self):
        return [
            {'slug': 'new', 'name': 'New!'},
            {'slug': 'both', 'name': 'New name'},
        ]

    def get_object(self, sync_key):
        found = next((obj for obj in self._int_objects if obj.slug == sync_key[0]))
        return found


class DumbDiffMerger(BaseDataDiffMerger):
    def create_object(self, ext_data):
        self._created = ext_data

    def update_object(self, obj, ext_data):
        self._updated = [obj, ext_data]

    def delete_object(self, obj):
        self._deleted = obj


def test_base_diff_merger():
    merger = DumbDiffMerger(data_generator=FakeDataGenerator(), create=True, delete=True, update=True)
    merger.execute()
    expected = {
        'updated': 1,
        'skipped': 0,
        'created': 1,
        'deleted': 1,
        'all': 3,
        'errors': 0
    }
    assert merger.results == expected
    assert merger._created == {'slug': 'new', 'name': 'New!'}
    assert merger._updated == [SyncObject(slug='both', name='Old name'), {'name': 'New name'}]
    assert merger._deleted == SyncObject(slug='old', name='Old')
