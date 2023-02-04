# -*- coding: utf-8 -*-
"""
http://nose.readthedocs.org/en/latest/plugins/interface.html
"""
from nose.plugins import Plugin


class DbMigrationPlugin(Plugin):
    name = 'migrate'

    def prepareTest(self, test):
        from intranet.yandex_directory.src.yandex_directory import app
        from intranet.yandex_directory.src.yandex_directory.setup import setup_app
        setup_app(app)

        from intranet.yandex_directory.src.yandex_directory.common.commands.migrate_database import migrate

        with app.app_context():
            migrate(
                rebuild=self.conf.options.rebuild_database,
                verbose=self.conf.options.verbose_migration,
            )

    def options(self, parser, env):
        super(DbMigrationPlugin, self).options(parser, env)
        parser.add_option(
            '--rebuild-database',
            action='store_true',
            default=False,
            dest='rebuild_database',
            help='Rebuild database by dropping tables and running migrations before tests.',
        )
        parser.add_option(
            '--recreate-database',
            action='store_true',
            default=False,
            dest='rebuild_database',
            help='Just a synonym to --rebuild-database.',
        )
        parser.add_option(
            '--verbose-migration',
            action='store_true',
            default=False,
            dest='verbose_migration',
            help='Output verbose information during database migration.',
        )


class ConsoleLoggingPlugin(Plugin):
    """
    Плагин выводит в консоль стэктрейс, если тест завершается со статусами FAIL или ERROR

    Для включения плагина использовать опцию `--with-console-logging`
    Пример:
    ``$ tox -- --with-console-logging <path/to/test>``
    """
    name = 'console-logging'
    stream_output = None

    def _write_trace(self, test):
        # Если передана опция --test-name, перед трейсом вывести имя теста
        # Это нужно для дрона, где тесты запускаются не в вербозном режиме
        if self.conf.options.test_name:
            print('\n', test)

    def addError(self, test, err, capt=None):
        self._write_trace(test)

    def addFailure(self, test, err, capt=None, tb_info=None):
        self._write_trace(test)

    def addSuccess(self, test, capt=None):
        pass

    def options(self, parser, env):
        super(ConsoleLoggingPlugin, self).options(parser, env)
        parser.add_option(
            '--test-name',
            action='store_true',
            default=False,
            dest='test_name',
            help='Print unsuccessful test name (option for unverbose mode)',
        )
