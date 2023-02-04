# coding: utf-8

import collections

from balance.actions.nirvana.client import clone_and_run_instance


class TestCloneAndRunInstance(object):
    """
    Тестирование простого сценария с клонированием и запуском workflow instance
    """

    def test_success(self, nirvana_api_mock):
        """
        Запустились, скопировали оригинальный workflow instance и запустили, без установки параметров
        """
        original_instance_id = nirvana_api_mock.mock_instance_id
        cloned_instance_id = nirvana_api_mock.mock_clone()
        nirvana_api_mock.mock_state()

        clone_and_run_instance(original_instance_id)

        nirvana_api_mock.clone_workflow_instance.assert_called_once_with(workflow_instance_id=original_instance_id)
        nirvana_api_mock.start_workflow.assert_called_once_with(workflow_instance_id=cloned_instance_id)

    def test_global_parameters(self, nirvana_api_mock):
        global_parameters = collections.OrderedDict([
            ('p1', 1),
            ('p2', 'v2'),
            ('p3', u'текст'),
        ])
        expected_parameters = [
            {'parameter': 'p1', 'value': '1'},
            {'parameter': 'p2', 'value': 'v2'},
            {'parameter': 'p3', 'value': '\xd1\x82\xd0\xb5\xd0\xba\xd1\x81\xd1\x82'},
        ]

        cloned_instance_id = nirvana_api_mock.mock_clone()
        nirvana_api_mock.mock_state()

        clone_and_run_instance(nirvana_api_mock.mock_instance_id, global_parameters=global_parameters)

        nirvana_api_mock.set_global_parameters.assert_called_once_with(workflow_instance_id=cloned_instance_id,
                                                                       param_values=expected_parameters)
        nirvana_api_mock.start_workflow.assert_called_once_with(workflow_instance_id=cloned_instance_id)
