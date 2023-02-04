# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import unittest
import yatest.common

from saas.library.python.nanny_rest.resource import NannyResource, SandboxFile, StaticFile, UrlFile, TemplateSetFile
from saas.library.python.nanny_rest.shard_resource import RegisteredShard, SandboxShard, SandboxShardmap


class TestSandboxFile(unittest.TestCase):
    def test_sandbox_file_init(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                sandbox_file = SandboxFile(**test_entity)
                self.assertIsInstance(sandbox_file, NannyResource)
                self.assertEqual(sandbox_file.local_path, test_entity['local_path'])
                self.assertEqual(sandbox_file.task_type, test_entity['task_type'])
                self.assertEqual(sandbox_file.task_id, test_entity['task_id'])
                self.assertEqual(sandbox_file.resource_type, test_entity['resource_type'])
                self.assertEqual(sandbox_file.resource_id, test_entity.get('resource_id', None))
                self.assertEqual(sandbox_file.is_dynamic, test_entity['is_dynamic'])
                self.assertEqual(sandbox_file.extract_path, test_entity.get('extract_path'))

    def test_sandbox_file_dict(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                sandbox_file = SandboxFile(**test_entity)
                self.assertDictEqual(test_entity, sandbox_file.dict(), 'original: {}; sandbox_file: {}'.format(test_entity, sandbox_file.dict()))


class TestStaticFile(unittest.TestCase):
    def test_static_file_init(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/static_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                static_file = StaticFile(**test_entity)
                self.assertIsInstance(static_file, NannyResource)
                self.assertEqual(static_file.local_path, test_entity['local_path'])
                self.assertEqual(static_file.content, test_entity['content'])
                self.assertEqual(static_file.is_dynamic, test_entity['is_dynamic'])

    def test_static_file_dict(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/static_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                static_file = StaticFile(**test_entity)
                self.assertDictEqual(test_entity, static_file.dict(), 'original: {}; static_file: {}'.format(test_entity, static_file.dict()))


class TestUrlFile(unittest.TestCase):
    def test_url_file_init(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/url_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                url_file = UrlFile(**test_entity)
                self.assertIsInstance(url_file, NannyResource)
                self.assertEqual(url_file.local_path, test_entity['local_path'])
                self.assertEqual(url_file.url, test_entity['url'])
                self.assertEqual(url_file.extract_path, test_entity['extract_path'])

    def test_url_file_dict(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/url_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                url_file = UrlFile(**test_entity)
                self.assertDictEqual(test_entity, url_file.dict(), 'original: {}; static_file: {}'.format(test_entity, url_file.dict()))


class TestTemplateSetFile(unittest.TestCase):
    def test_template_set_file_init(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/template_set_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                template_set_file = TemplateSetFile(**test_entity)
                self.assertIsInstance(template_set_file, NannyResource)
                self.assertEqual(template_set_file.local_path, test_entity['local_path'])
                self.assertEqual(template_set_file.layout, test_entity['layout'])
                self.assertDictEqual(template_set_file.templates, {t['name']: t['content'] for t in test_entity['templates']})

    def test_template_set_file_dict(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/template_set_files.json'), 'rb') as f:
            test_data = json.load(f)
            for test_entity in test_data:
                template_set_file = TemplateSetFile(**test_entity)
                self.assertDictEqual(test_entity, template_set_file.dict(), 'original: {}; template_set_file: {}'.format(test_entity, template_set_file.dict()))


class TestShards(unittest.TestCase):
    def test_registered_shard_init(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_bsc_shard.json'), 'rb') as f:
            test_data = json.load(f)['REGISTERED_SHARD']
            for test_entity in test_data:
                registered_shard = RegisteredShard(**test_entity)
                self.assertEqual(registered_shard.chosen_type, 'REGISTERED_SHARD')
                self.assertEqual(registered_shard.shard_id, test_entity['registered_shard']['shard_id'])

    def test_registered_shard_dict(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_bsc_shard.json'), 'rb') as f:
            test_data = json.load(f)['REGISTERED_SHARD']
            for test_entity in test_data:
                registered_shard = RegisteredShard(**test_entity)
                self.assertDictEqual(test_entity, registered_shard.dict(), 'original: {}; registered_shard: {}'.format(test_entity, registered_shard.dict()))

    def test_sandbox_shard_init(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_bsc_shard.json'), 'rb') as f:
            test_data = json.load(f)['SANDBOX_SHARD']
            for test_entity in test_data:
                sandbox_shard = SandboxShard(**test_entity)
                self.assertEqual(sandbox_shard.chosen_type, 'SANDBOX_SHARD')
                self.assertEqual(sandbox_shard.task_type, test_entity['task_type'])
                self.assertEqual(sandbox_shard.task_id, test_entity['task_id'])
                self.assertEqual(sandbox_shard.resource_type, test_entity['resource_type'])

    def test_sandbox_shard_dict(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_bsc_shard.json'), 'rb') as f:
            test_data = json.load(f)['SANDBOX_SHARD']
            for test_entity in test_data:
                sandbox_shard = SandboxShard(**test_entity)
                self.assertDictEqual(test_entity, sandbox_shard.dict(), 'original: {}; sandbox_shard: {}'.format(test_entity, sandbox_shard.dict()))

    def test_sandbox_shardmap_init(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_bsc_shard.json'), 'rb') as f:
            test_data = json.load(f)['SANDBOX_SHARDMAP']
            for test_entity in test_data:
                sandbox_shardmap = SandboxShardmap(**test_entity)
                self.assertEqual(sandbox_shardmap.chosen_type, 'SANDBOX_SHARDMAP')
                self.assertEqual(sandbox_shardmap.task_type, test_entity['sandbox_shardmap']['task_type'])
                self.assertEqual(sandbox_shardmap.task_id, test_entity['sandbox_shardmap']['task_id'])
                self.assertEqual(sandbox_shardmap.resource_type, test_entity['sandbox_shardmap']['resource_type'])

    def test_sandbox_shardmap_dict(self):
        with open(yatest.common.source_path('saas/library/python/nanny_rest/tests/data/sandbox_bsc_shard.json'), 'rb') as f:
            test_data = json.load(f)['SANDBOX_SHARDMAP']
            for test_entity in test_data:
                sandbox_shardmap = SandboxShardmap(**test_entity)
                self.assertDictEqual(test_entity, sandbox_shardmap.dict(), 'original: {}; sandbox_shardmap: {}'.format(test_entity, sandbox_shardmap.dict()))


if __name__ == '__main__':
    unittest.main()
