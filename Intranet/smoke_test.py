# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.common.smoke import _run_smoke_tests
from intranet.yandex_directory.src.yandex_directory.common.commands.base import BaseCommand


class Command(BaseCommand):
    name = 'smoke-test'

    def run(self):
        print('Start smoke testing.\n')
        response = _run_smoke_tests(
            # этот параметр полезно раскомментировать для отладки
            # default_timeout=0.01,
            only_errors=True,
        )

        print('Environment:', response['environment'])
        print('Default timeout:', response['default_timeout'])
        print('Has errors in vital services:', 'yes' if response['has_errors_in_vital_services'] else 'no')
        if response.get('errors'):
            print('Errors:')
            for service, error in list(response['errors'].items()):
                print(' [{0}] {1}'.format(service, error).encode('utf-8'))

        # в случае критических ошибок, вернём ненулевой статус, как принято в Unix
        if response['has_errors_in_vital_services']:
            return 1
