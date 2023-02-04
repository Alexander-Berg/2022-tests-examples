import mock
import unittest

import maps.analyzer.tools.sandboxy.lib.sandboxy as sboxy
import maps.analyzer.tools.sandboxy.lib.scheduler as scheduler


_sch_info = scheduler.SchedulerInfo


class TestScheduler(unittest.TestCase):
    def setUp(self):
        self.client = sboxy.Client()

    @mock.patch('scheduler_test.scheduler.find_schedulers')
    def test_from_env_ambiguous(self, find_schedulers_mock):
        task = {'tags': ['tag1']}
        find_schedulers_mock.return_value = (
            [_sch_info({'id': 123, 'task': task}), _sch_info({'id': 345, 'task': task})],
            2
        )
        scheduler_instance = scheduler.Scheduler({'task': task})
        self.assertIsNone(scheduler_instance.from_env(self.client, sboxy.Env.TESTING))

    @mock.patch('scheduler_test.scheduler.find_schedulers')
    @mock.patch('scheduler_test.scheduler.Scheduler.read')
    def test_from_env_common_tag(self, read_scheduler_instance_mock, find_schedulers_mock):
        tags1 = ['common-tag', 'tag1']
        tags2 = ['common-tag', 'tag2']
        find_schedulers_mock.return_value = (
            [
                _sch_info({'id': 123, 'task': {'tags': tags1}}),
                _sch_info({'id': 345, 'task': {'tags': tags2}})
            ],
            2
        )
        read_scheduler_instance_mock.return_value = scheduler.Scheduler({'id': 1})
        scheduler_instance = scheduler.Scheduler({'task': {'tags': tags1}})
        self.assertIsNotNone(scheduler_instance.from_env(self.client, sboxy.Env.TESTING))

    @mock.patch('scheduler_test.scheduler.read_scheduler')
    @mock.patch('scheduler_test.scheduler.find_schedulers')
    def test_from_env(self, find_schedulers_mock, read_scheduler_mock):
        scheduler_id = 123
        task = {'tags': ['tag1']}
        read_scheduler_mock.return_value = {'id': scheduler_id}
        find_schedulers_mock.return_value = [_sch_info({'id': scheduler_id, 'task': task})], 1
        scheduler_instance = scheduler.Scheduler({'task': task})
        new_scheduler_instance = scheduler_instance.from_env(self.client, sboxy.Env.TESTING)
        self.assertEqual(scheduler_id, new_scheduler_instance.id)

    @mock.patch('scheduler_test.scheduler.update_scheduler')
    @mock.patch('scheduler_test.scheduler.format_status')
    @mock.patch('scheduler_test.scheduler.Scheduler.is_update_permitted')
    def test_update_not_permitted(self, is_update_permitted_mock, format_status_mock, update_scheduler_mock):
        is_update_permitted_mock.return_value = False
        format_status_mock.return_value = ""
        update_scheduler_mock.return_value = None
        new_values = {'revision': '1'}
        scheduler_instance = scheduler.Scheduler({'id': 1, 'task': {'description': 'foo', 'custom_fields': {}}})
        self.assertFalse(scheduler_instance.update(self.client, new_values, False, False))
        self.assertTrue(scheduler_instance.update(self.client, new_values, False, True))
        self.assertFalse(scheduler_instance.update(self.client, new_values, True, False))
        self.assertFalse(scheduler_instance.update(self.client, new_values, True, True))

    @mock.patch('scheduler_test.scheduler.update_scheduler')
    @mock.patch('scheduler_test.scheduler.Scheduler.is_update_permitted')
    def test_update_exception(self, is_update_permitted_mock, update_scheduler_mock):
        is_update_permitted_mock.return_value = True
        update_scheduler_mock.side_effect = Exception
        new_values = {'revision': '1'}
        scheduler_instance = scheduler.Scheduler({'id': 1, 'task': {'description': 'foo', 'custom_fields': {}}})
        self.assertFalse(scheduler_instance.update(self.client, new_values, False, False))
        self.assertFalse(scheduler_instance.update(self.client, new_values, True, False))

    @mock.patch('scheduler_test.scheduler.update_scheduler')
    @mock.patch('scheduler_test.scheduler.Scheduler.is_update_permitted')
    def test_update(self, is_update_permitted_mock, update_scheduler_mock):
        is_update_permitted_mock.return_value = True
        update_scheduler_mock.return_value = None
        scheduler_instance = scheduler.Scheduler({'id': 1, 'task': {'description': 'foo', 'custom_fields': {}}})
        self.assertTrue(scheduler_instance.update(self.client, {'revision': '1'}, False, False))
        self.assertFalse(scheduler_instance.update(self.client, {'revision': '1'}, True, False))

    def test_is_update_permitted_testing(self):
        scheduler_instance = scheduler.Scheduler(
            {'task': {'custom_fields': [{'name': 'environment', 'value': 'testing'}]}}
        )
        self.assertTrue(scheduler_instance.is_update_permitted(self.client, []))

    @mock.patch('scheduler_test.scheduler.Scheduler.from_env')
    @mock.patch('scheduler_test.scheduler.Scheduler.succeeded')
    @mock.patch('scheduler_test.scheduler.Scheduler.mismatches')
    def test_is_update_permitted_production_not_succeeded(self, mismatches_mock, succeeded_mock, from_env_mock):
        mismatches_mock.return_value = {}
        succeeded_mock.return_value = False
        from_env_mock.return_value = scheduler.Scheduler({'id': 123})
        scheduler_instance = scheduler.Scheduler(
            {'task': {'custom_fields': [{'name': 'environment', 'value': 'production'}]}}
        )
        self.assertFalse(scheduler_instance.is_update_permitted(self.client, []))

    @mock.patch('scheduler_test.scheduler.Scheduler.from_env')
    @mock.patch('scheduler_test.scheduler.Scheduler.succeeded')
    @mock.patch('scheduler_test.scheduler.Scheduler.mismatches')
    def test_is_update_permitted_production_mismatches(self, mismatches_mock, succeeded_mock, from_env_mock):
        mismatches_mock.return_value = {'revision': ('123', None)}
        succeeded_mock.return_value = True
        from_env_mock.return_value = scheduler.Scheduler({'id': 123})
        scheduler_instance = scheduler.Scheduler(
            {'task': {'custom_fields': [{'name': 'environment', 'value': 'production'}]}}
        )
        self.assertFalse(scheduler_instance.is_update_permitted(self.client, []))

    @mock.patch('scheduler_test.scheduler.Scheduler.from_env')
    @mock.patch('scheduler_test.scheduler.Scheduler.succeeded')
    @mock.patch('scheduler_test.scheduler.Scheduler.mismatches')
    def test_is_update_permitted_production_true(self, mismatches_mock, succeeded_mock, from_env_mock):
        mismatches_mock.return_value = {}
        succeeded_mock.return_value = True
        from_env_mock.return_value = scheduler.Scheduler({'id': 123})
        scheduler_instance = scheduler.Scheduler(
            {'task': {'custom_fields': [{'name': 'environment', 'value': 'production'}]}}
        )
        self.assertTrue(scheduler_instance.is_update_permitted(self.client, []))

    @mock.patch('scheduler_test.scheduler.find_tasks')
    def test_succeeded(self, find_tasks_mock):
        revision = 123
        scheduler_id = 567
        find_tasks_mock.return_value = {'items': [
            {'input_parameters': {'revision': revision, 'environment': 'testing'}}
        ]}
        custom_fields_first = [{'name': 'revision', 'value': revision}, {'name': 'environment', 'value': 'production'}]
        scheduler_instance_first = scheduler.Scheduler({
            'id': scheduler_id, 'task': {'custom_fields': custom_fields_first}
        })
        self.assertEqual(scheduler_instance_first.id, scheduler_id)
        self.assertTrue(scheduler_instance_first.succeeded(self.client))

        custom_fields_second = [{'name': 'revision', 'value': revision + 1}, {'name': 'environment', 'value': 'production'}]
        scheduler_instance_second = scheduler.Scheduler({
            'id': scheduler_id, 'task': {'custom_fields': custom_fields_second}
        })
        self.assertEqual(scheduler_instance_second.id, scheduler_id)
        self.assertFalse(scheduler_instance_second.succeeded(self.client))

    def test_values(self):
        custom_fields = [{'name': 'revision', 'value': 456}, {'name': 'environment', 'value': 'production'}]
        scheduler_instance = scheduler.Scheduler({'task': {'custom_fields': custom_fields}})
        self.assertEqual(
            scheduler_instance.values(),
            {'revision': '456', 'environment': 'production'}
        )

    def test_common_values(self):
        extra_values = {'binary_params': 'foo', 'environment': 'testing', 'vault_owner': 'bar'}
        common = {'revision': '123'}
        merged = {**extra_values, **common}
        scheduler_instance = scheduler.Scheduler({'id': 567})
        self.assertEqual(scheduler_instance.common_values(merged), common)

    def test_mismatches(self):
        common_revision = '123'
        custom_fields = [
            {'name': 'revision', 'value': common_revision},
            {'name': 'environment', 'value': 'production'}
        ]
        scheduler_instance = scheduler.Scheduler({'task': {'custom_fields': custom_fields}})
        self.assertEqual(scheduler_instance.mismatches({'revision': common_revision}), {})
        self.assertEqual(
            scheduler_instance.mismatches({'revision': '567'}),
            {'revision': ('123', '567')}
        )
