# -*- coding: utf-8 -*-

"""
In spite of the name of this file, it is not a file with tests.
"""

import os.path
import threading

from balance.application import Application
from balance.constants import BALANCE_DATABASE_ID, BALANCE_META_DATABASE_ID
from balance import multilang_support

try:
    from butils import arcadia_utils
    IN_BINARY = arcadia_utils.in_binary()
except ImportError:
    IN_BINARY = False


class ApplicationForTests(Application):
    def __init__(self, cfg_path=None, database_id='balance'):
        """
        Uses special config for testing.
        """
        cfg_path = os.path.join(os.path.dirname(__file__), '..',
                                'balance', 'balance-test.cfg')
        if IN_BINARY:
            import yatest.common
            cfg_path = yatest.common.source_path(cfg_path)
        super(ApplicationForTests, self).__init__(cfg_path, database_id)

        # Используем шаблоны из репозитория.
        if hasattr(self, "mako_renderer"):
            from butils.application.plugins.mako_helper import MakoRenderer
            templates_dir = self.cfg.find('Environment/MailTemplates')
            if templates_dir is not None:
                self.mako_renderer = MakoRenderer(templates_dir.text)

        self.phrasemanager = multilang_support.PhraseManager
        self.components_cfg = {}

    # Патчим new_session, чтобы в тестах использовалась всегда одна сессия.
    def new_session(self, *args, **kwargs):
        database_id = kwargs.get('database_id', BALANCE_DATABASE_ID)

        if database_id == BALANCE_META_DATABASE_ID:
            if hasattr(threading.current_thread(), 'test_meta_session'):
                session = threading.current_thread().test_meta_session
                if 'oper_id' in kwargs:
                    session.oper_id = kwargs['oper_id']
                return session
        elif hasattr(threading.current_thread(), 'test_session'):
            session = threading.current_thread().test_session
            if 'oper_id' in kwargs:
                session.oper_id = kwargs['oper_id']
            return session

        session = super(ApplicationForTests, self).new_session(*args, **kwargs)
        session.clone = lambda: session

        if database_id == BALANCE_META_DATABASE_ID:
            threading.current_thread().test_meta_session = session
        else:
            threading.current_thread().test_session = session

        return session

    def real_new_session(self, *args, **kwargs):
        return super(ApplicationForTests, self).new_session(*args, **kwargs)

    def warm_up_pool(self):
        """ Medium and Muzzle expect this method in application """

    def get_component_cfg(self, component_id, _should_expand=True, _expand_data=None, _resolvers=None):
        return self.components_cfg.get(component_id, {})


if __name__ == '__main__':
    ApplicationForTests()
