from django.test import TestCase

from staff.celery_tools.domain import list_running


class CeleryCurrentAppMock(object):
    class ControllMock(object):
        class InspectMock(object):
            """
            Класс для подмены ф-ии celery.current_app.control.inspect
            в domain.list_running()
            Используется для тестирования на адекватную обработку поля time_start
            """
            _data = {
                'host1': [{'name': 'mock_task1', 'time_start': 1402254347},
                          {'name': 'mock_task2', 'time_start': {'wrong': 321}}],
                'host2': [{'name': 'mock_task3', 'time_start': 1402874367.434},
                          {'name': 'mock_task4', 'time_start': None}],
                'host3': [{'name': 'mock_task5', 'time_start': 'broken_val'}],
            }

            def active(self):
                return self._data

            def scheduled(self):
                return self._data

            def reserved(self):
                return self._data

        def inspect(self):
            return self.InspectMock()

    def __init__(self):
        super(CeleryCurrentAppMock, self).__init__()
        self.control = self.ControllMock()


current_app = CeleryCurrentAppMock()


class CeleryToolsTestCase(TestCase):
    def test_list_running_not_fail(self):
        result = list_running()
        for value in result.values():
            for tasks in value.values():
                for task in tasks:
                    if not isinstance(task.get('time_start'), (int, float)):
                        self.assertNotIn('time_start_humanized', task.keys())
                    else:
                        self.assertIn('time_start_humanized', task.keys())
