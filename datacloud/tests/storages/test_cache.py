# -*- coding: utf-8 -*-
import unittest
import os
import murmurhash
from yt.wrapper import ypath_join, ypath_split
from datacloud.dev_utils.yt import yt_utils
from datacloud.ml_utils.benchmark_v2.tables_cache import (
    YtTablesCacher,
    YtCacheKey,
    YtAliasesStorage,
    AliasesStorageRec,
)


class TestYtTablesCacher(unittest.TestCase):
    def setUp(self):
        self.root = '//cache'
        self.storage_path = ypath_join(self.root, 'caches_storage')

        self.yt_client = yt_utils.get_yt_client(os.environ["YT_PROXY"])

        self.ytc = YtTablesCacher(
            root=self.root,
            storage_path=self.storage_path,
            yt_client=self.yt_client
        )
        self.ytc._storage._yt_table.create_table(force=True)

        self.original_path = '//home/timezone'
        self.modification_time = self.yt_client.get_attribute(self.original_path, 'modification_time')
        self.hash_key = murmurhash.hash64(self.original_path + self.modification_time)

        self.expected_cache_key = YtCacheKey(
            hash=self.hash_key,
            original_path=self.original_path,
            modification_time=self.modification_time
        )
        self.expected_cache_path = ypath_join(
            self.root, ypath_split(self.original_path)[-1]) + '_' + self.modification_time

    def _get_rows(self):
        return list(yt_utils.DynTable.get_rows_from_table(param_dict={
            'hash': self.hash_key
        }, yt_client=self.yt_client, table=self.storage_path))

    def tearDown(self):
        for table in self.yt_client.list(self.root, absolute=True):
            self.yt_client.remove(table)

    def test_empty_table(self):
        assert self.ytc.read_cache(None) is None
        assert self.ytc.read_cache(self.expected_cache_key) is None

    def test_remove_unexisting(self):
        "Test: remove of unexisting shouldn't lead to Exception"
        self.ytc.remove_cache(self.expected_cache_key)
        self.ytc.remove_cache(None)

    def test_regular_add_delete(self):
        cache_key, cache_path, _ = self.ytc.cache_table(self.original_path)
        assert cache_key == self.expected_cache_key
        assert cache_path == self.expected_cache_path
        assert self.yt_client.exists(cache_path)
        assert self._get_rows() == [dict(cache_path=cache_path, additional={}, **cache_key.__dict__)]

        self.ytc.remove_cache(cache_key)
        assert not self.yt_client.exists(cache_path)
        assert len(self._get_rows()) == 0
        assert self.ytc.read_cache(cache_key) is None

    def test_cache_and_translate(self):
        translated = self.ytc.cache_and_translate(self.original_path)
        assert translated == self.expected_cache_path
        assert self.expected_cache_path == self.ytc.cache_and_translate(self.original_path)
        assert self.expected_cache_path == self.ytc.cache_and_translate(self.expected_cache_path)
        assert len(self.yt_client.list(self.root)) == 2

    def test_cache_link(self):
        link_path = '//home/some_link'
        self.yt_client.link(self.original_path, link_path)
        translated_link = self.ytc.cache_and_translate(link_path)
        translated_original = self.ytc.cache_and_translate(self.original_path)

        assert translated_link == translated_original
        assert len(self.yt_client.list(self.root)) == 2
        assert len(self._get_rows()) == 1


class TestYtAliasesStorage(unittest.TestCase):
    def setUp(self):
        self.yt_client = yt_utils.get_yt_client(os.environ["YT_PROXY"])
        self.storage_path = '//cache/aliases_storage'

        self.yas = YtAliasesStorage(table_path=self.storage_path, yt_client=self.yt_client)
        self.yas._yt_table.create_table()

        original_path = '//home/timezone'
        modification_time = self.yt_client.get_attribute(original_path, 'modification_time')
        self.cache_key = YtCacheKey(
            hash=murmurhash.hash64(original_path + modification_time),
            original_path=original_path,
            modification_time=modification_time
        )

    def tearDown(self):
        self.yt_client.remove(self.storage_path)

    def test_add_alias(self):
        alias = 'alias'
        self.yas.add_translation(alias=alias, cache_key=self.cache_key)
        assert self.yas.translate(alias=alias) == AliasesStorageRec(
            alias=alias,
            cache_key=self.cache_key,
            additional={}
        )

    def test_assert_no_force(self):
        alias = 'alias'
        self.yas.add_translation(alias=alias, cache_key=self.cache_key)
        with self.assertRaises(AssertionError):
            self.yas.add_translation(alias=alias, cache_key=self.cache_key)

    def test_alias_not_exist(self):
        alias = 'alias'
        assert self.yas.translate(alias=alias) is None
        self.yas.add_translation(alias=alias, cache_key=self.cache_key)
        self.yas.remove_translation(alias=alias)
        assert self.yas.translate(alias=alias) is None
