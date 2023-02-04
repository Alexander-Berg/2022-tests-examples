# -*- coding: utf-8 -*-

from mako.template import Template

TEAMCITY_TEST_PACK = [u'Billing_Autotesting_Balalayka_FullTests_Release']

SUMMARY = Template(u' ${version}')

DESCRIPTION = Template(u'''======= 1. До выкладки пакета:

======= 2. Выложить пакет приложения через Кондуктор:

======= 3. После выкладки пакета:

----
======= Задачи, решённые в версии:
''')

TESTS_LAUNCHED = Template(u'''Запущены тесты:
  Release: **${Billing_Autotesting_Balalayka_FullTests_Release}**''')


TESTS_RESULTS = Template(u'''Результаты тестов:
  Release: **${Billing_Autotesting_Balalayka_FullTests_Release}**''')

if __name__ == "__main__":
    pass