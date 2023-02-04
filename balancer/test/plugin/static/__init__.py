# -*- coding: utf-8 -*-
import os

from balancer.test.util import fs
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture


pytest_plugins = [
    'balancer.test.plugin.fs',
]


@multiscope.fixture(pytest_fixtures=['request'])
class Static(object):
    __fixturename__ = 'static'

    @staticmethod
    def session_static():
        pass

    @staticmethod
    def module_static():
        pass

    @staticmethod
    def class_static():
        pass

    @staticmethod
    def function_static(fs_manager, request):
        try:
            import static
            src = os.path.dirname(static.__file__)
        except ImportError:
            try:
                import yatest.common
                src = yatest.common.test_source_path('static')
            except ImportError:
                src = os.path.join(os.path.dirname(request.module.__file__), 'static')
        if os.path.exists(src):
            dst = fs_manager.get_unique_name('static')
            fs_manager.copy(src, dst)
            return fs.FileSystemManager(dst)
        else:
            return None


class StaticContext(object):
    @property
    def static(self):
        """
        Directory with static data

        :rtype: FileSystemManager
        """
        return self.manager.static


MANAGERS = [ManagerFixture('static', 'static')]
CONTEXTS = [StaticContext]
