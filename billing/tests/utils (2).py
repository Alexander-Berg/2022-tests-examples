# coding: utf-8

import logging
import unittest
import threading
from datetime import datetime
from SimpleXMLRPCServer import SimpleXMLRPCServer

import mock

from billing.dcs.dcs import settings
from billing.dcs.dcs.constants import XML_CONFIG
from billing.dcs.dcs.utils.mnclose import MNCloseService
from billing.dcs.dcs.utils.common import change_logging_level, relative_date
from billing.dcs.dcs.utils.mnclose import MNCloseTaskStatuses, MNCloseTaskActions
from billing.dcs.dcs.temporary.reports_utils.mnclose import NirvanaMnCloseSyncStatuses

__all__ = ['BaseTestCase', 'MNCloseBaseTestCase', 'MNCloseServer',
           'create_patcher', 'create_application', 'change_logging_level']


class BaseTestCase(unittest.TestCase):
    """
    Все функциональные тесты, которые используют БД,
      должны наследоваться от этого класса
    """

    application = None

    def setUp(self):
        # Функциональные тесты не должны работать без Application
        if settings.IS_FUNCTEST_ENV:
            self.application = create_application()

    def check_mock_calls(self, mock_, calls, check_order=True):
        """
        Одновременно проверяет аргументы вызовов и общее количество вызовов
        """
        self.assertEqual(mock_.call_count, len(calls))
        mock_.assert_has_calls(calls, any_order=not check_order)


def create_patcher(module_path):
    """
    Создает обертку над mock.patch с прописанным module path до
    тестируемого модуля
    """
    # e = entity name
    return lambda e: mock.patch('%s.%s' % (module_path, e), autospec=True)


class MNCloseServer(object):
    """ Класс для имитации MNClose сервера """
    def __init__(self, tasks):
        self.log = logging.getLogger('MNCloseServer')
        self.server, self.thread = None, None

        for month, month_tasks_map in tasks.iteritems():
            # Приводим структуру задач к виду, в котором они приходят из ручки
            # tasks.monthindex
            for task_group_name in ('next', 'current', 'done'):
                month_tasks_map.setdefault(task_group_name, [])
                for task in month_tasks_map[task_group_name]:
                    task['inst_dt'] = month

        self.tasks = tasks

    def get_statuses(self, instantiation_date):
        self.log.debug('get_statuses: %s', instantiation_date)
        instantiation_date = datetime.strptime(instantiation_date, '%y-%m')

        statuses = {}
        month_tasks_map = self.tasks[instantiation_date]
        for task_group_name in ('next', 'current', 'done'):
            month_tasks_map.setdefault(task_group_name, [])
            for task in month_tasks_map[task_group_name]:
                statuses[task.get('name_id')] = self._convert_status(task['status_name_id'],
                                                                     task['available_actions'])

        return {'scheme_version': 0, 'statuses': statuses}

    def get_status(self, task_name, instantiation_date):
        self.log.debug('get_status: %s, %s', task_name, instantiation_date)
        instantiation_date = datetime.strptime(instantiation_date, '%y-%m')

        status = None
        month_tasks_map = self.tasks[instantiation_date]
        for task_group_name in ('next', 'current', 'done'):
            month_tasks_map.setdefault(task_group_name, [])
            for task in month_tasks_map[task_group_name]:
                if task.get('name_id') == task_name:
                    status = self._convert_status(task['status_name_id'], task['available_actions'])

        if not status:
            return {'scheme_version': 0, 'error': 'unknown task'}

        return {'scheme_version': 0, 'status': status}

    @staticmethod
    def get_available_actions(task_group_name, status_name_id):
        """
        Имитируем получение возможных действий по задаче
        """
        if task_group_name == 'done':
            return [MNCloseTaskActions.reopen, ]
        elif task_group_name == 'current':
            s = (MNCloseTaskStatuses.new, MNCloseTaskStatuses.stalled)
            if status_name_id in s:
                return [MNCloseTaskActions.open, ]
            elif status_name_id == MNCloseTaskStatuses.open:
                return [MNCloseTaskActions.resolve, MNCloseTaskActions.stall]
        return []

    @staticmethod
    def get_next_status(status_name_id, action):
        """
        Имитируем переключение статуса на действие
        """
        s = MNCloseTaskStatuses
        a = MNCloseTaskActions

        key = (status_name_id, action)
        return {
            (s.new, a.open): s.open,
            (s.open, a.resolve): s.resolved,
            (s.open, a.stall): s.stalled,
            (s.stalled, a.open): s.open,
            (s.resolved, a.reopen): s.open,
        }.get(key)

    @classmethod
    def _convert_status(cls, status, actions):
        if status == MNCloseTaskStatuses.new and len(actions) == 0:
            return NirvanaMnCloseSyncStatuses.new_unopenable
        elif status == MNCloseTaskStatuses.new and len(actions) > 0:
            return NirvanaMnCloseSyncStatuses.new_openable
        elif status == MNCloseTaskStatuses.open:
            return NirvanaMnCloseSyncStatuses.opened
        elif status == MNCloseTaskStatuses.stalled:
            return NirvanaMnCloseSyncStatuses.stalled
        elif status == MNCloseTaskStatuses.resolved:
            return NirvanaMnCloseSyncStatuses.resolved
        return None

    @classmethod
    def _convert_new_status_to_action(status, new_status):
        if status == NirvanaMnCloseSyncStatuses.resolved and new_status == NirvanaMnCloseSyncStatuses.opened:
            return MNCloseTaskActions.reopen
        elif new_status == NirvanaMnCloseSyncStatuses.opened:
            return MNCloseTaskActions.open
        elif new_status == NirvanaMnCloseSyncStatuses.stalled:
            return MNCloseTaskActions.stall
        elif new_status == NirvanaMnCloseSyncStatuses.resolved:
            return MNCloseTaskActions.resolve
        return None

    def set_status(self, name, instantiation_date, new_status):
        self.log.debug('set_status: %s, %s, %s',
                       instantiation_date, name, new_status)
        instantiation_date = datetime.strptime(instantiation_date, '%y-%m')

        action = self._convert_new_status_to_action(new_status)

        tasks = self.tasks[instantiation_date]
        for task_group_name, task_group in tasks.iteritems():
            for task in task_group:
                if task.get('name_id') == name:
                    available_actions = task.get('available_actions') or []
                    if action in available_actions:
                        task['status_name_id'] = self.get_next_status(
                            task['status_name_id'], action)
                        task['available_actions'] = self.get_available_actions(
                            task_group_name, task['status_name_id'])
                        return {
                            'scheme_version': 0,
                            'status': self._convert_status(task['status_name_id'], task['available_actions'])
                        }
                    else:
                        self.log.debug('err1!')
                        return {
                            'scheme_version': 0,
                            'error': 'error'
                        }
        return {
            'scheme_version': 0,
            'error': 'error'
        }

    def thread_worker(self):
        self.log.debug('Starting server')
        self.server.serve_forever(poll_interval=0.1)

    def start(self):
        host, port = \
            XML_CONFIG.findtext_strict('MNClose/NirvanaProxyURL').split('//')[1].split(':')
        server = SimpleXMLRPCServer(addr=(host, int(port)), logRequests=False)
        server.register_introspection_functions()
        server.register_function(self.set_status, 'NirvanaMnCloseTasks.set_status')
        server.register_function(self.get_status, 'NirvanaMnCloseTasks.get_status')
        server.register_function(self.get_statuses, 'NirvanaMnCloseTasks.get_statuses')
        self.server = server
        self.thread = threading.Thread(target=self.thread_worker)
        self.thread.start()
        return self

    def stop(self):
        self.log.debug('Stopping server')
        self.server.shutdown()
        self.thread.join()
        self.server.server_close()
        self.server, self.thread = None, None

    def __enter__(self):
        return self.start()

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.stop()


class MNCloseBaseTestCase(BaseTestCase):
    def setUp(self):
        self.month = relative_date(day=1)

        self.task_name = 'dcs_task'
        self.task_group = 'current'

        task = {
            'itask_id': 1,
            'name_id': self.task_name,
            'status_name_id': MNCloseTaskStatuses.new,
            'available_actions': MNCloseServer.get_available_actions(
                self.task_group, MNCloseTaskStatuses.new)
        }
        self.initial_state = {self.month: {self.task_group: [task, ]}}

        self.mnclose_server = MNCloseServer(self.initial_state)
        self.mnclose_server.start()

    def tearDown(self):
        self.mnclose_server.stop()

    @property
    def mnclose(self):
        return MNCloseService(instantiation_date=self.month)


def create_application():
    try:
        from billing.dcs.dcs.temporary.butils.application import getApplication
        return getApplication()
    except RuntimeError:
        from billing.dcs.dcs.utils.application import DCSApplication

        # Обманываем application для того, чтобы управлять логированием в
        # одном месте
        with mock.patch('billing.dcs.dcs.temporary.butils.application.logging_configured',
                        return_value=True):
            return DCSApplication(name='dcs', cfg_path=settings.XML_CONFIG_PATH)
