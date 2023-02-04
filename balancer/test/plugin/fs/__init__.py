# -*- coding: utf-8 -*-
import yatest.common

from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture
from balancer.test.util.fs import FileSystemManager


pytest_plugins = [
]


def pytest_addoption(parser):
    parser.addoption('-L', '--log_dir', dest='log_dir', default='./logs', help='path to logs directory')

    debug = parser.getgroup('debug')
    debug.addoption('--clean_logs', dest='clean_logs', action='store_true', default=False, help='clean logs of passed tests')


@multiscope.fixture(pytest_fixtures=['request', 'settings', 'session_fs_manager', 'module_fs_manager', 'class_fs_manager'])
class FileSystemManagerFixture(object):
    __fixturename__ = 'fs_manager'

    @staticmethod
    def session_fs_manager(request, settings):
        path = yatest.common.output_path()
        fs_mgr = FileSystemManager(path)

        def fin():
            fs_mgr.chmod_default_recursive(path)

        request.addfinalizer(fin)
        return fs_mgr

    @staticmethod
    def module_fs_manager(session_fs_manager, request):
        path = session_fs_manager.create_dir(request.module.__name__)
        return FileSystemManager(path)

    @staticmethod
    def class_fs_manager(module_fs_manager, request):
        if request.cls is not None:
            path = module_fs_manager.create_dir(request.cls.__name__)
            return FileSystemManager(path)
        else:
            return None

    @staticmethod
    def function_fs_manager(module_fs_manager, class_fs_manager, request):
        fs_mgr = class_fs_manager if class_fs_manager is not None else module_fs_manager
        path = fs_mgr.create_dir(request.node.name)
        return FileSystemManager(path)


MANAGERS = [ManagerFixture('fs', 'fs_manager')]
