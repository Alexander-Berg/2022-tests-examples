# -*- coding: utf-8 -*-

from mako.template import Template
from collections import OrderedDict

PACKAGES = OrderedDict([(u'prestable', [u'yandex-balance-apikeys-frontend-user']),
                           (u'stable', [u'yandex-balance-apikeys-frontend-user',
                                        u'yandex-balance-apikeys-frontend-user-static'
                                       ])
                        ])

RESPONSIBLE = [u'ealieva', u'torvald']

SUMMARY = Template(u'Выкладка yandex-balance-apikeys-frontend-user ${testing}')

DESCRIPTION = Template(
u'''**Conductor**
% for stage, pkgs in PACKAGES.items():
//${stage}://
  * (${', '.join(pkgs)})
% endfor

**Задачи**
% for issue in issues:
  * ${issue}
% endfor


**Подтверждение**
% for login in RESPONSIBLE:
  * staff:${login}
% endfor

Все подтверждающие должны сказать \"ok\". ((https://developer.apikeys-pt.yandex.ru Prestable-среда)) должна работать корректно.

**Изменения в коде**
${diff}",

''')

TEAMCITY_BUILD_URL = u'https://teamcity.yandex-team.ru/viewLog.html?buildId={}&tab=buildResultsDiv'

TEAMCITY_TEST_PACK = [u'Billing_Autotesting_PythonTests_Smoke']

TESTS_LAUNCHED = Template(u'''Запущены тесты:
  ((https://teamcity.yandex-team.ru/viewLog.html?buildId=${Billing_Autotesting_PythonTests_Smoke}&tab=buildResultsDiv Full))''')

TESTS_RESULTS = Template(u'''<%def name="dotted(data, widths, addition=None)">
    %for text, template in zip(data, ['{:.<%s}' % n for n in widths]):
${template.format(text)}\\
    %endfor
</%def>\\
<%def name="dotted_python(data, widths=[20, 10, 10, 10, 10, 2], addition=None)">\\
${dotted(data, widths)}\\
</%def>\\
<%def name="dotted_java(data, widths=[20, 10, 10, 10, 2], addition=None)">\\
${dotted(data, widths)}\\
</%def>\\
##
## template itself:
##
${"##BALANCE##"}

${dotted_python(data=['##Python', 'Failed', 'Passed', 'Ignored', 'Muted', '##'])}\
${dotted_python(data=['##Full', Billing_Autotesting_PythonTests_Smoke['failed'],
                                Billing_Autotesting_PythonTests_Smoke['passed'],
                                Billing_Autotesting_PythonTests_Smoke['ignored'],
                                Billing_Autotesting_PythonTests_Smoke['muted'], '##'])}

${dotted_java(data=['##Java', 'Failed', 'Passed', 'Revoked', '##'])}\
${dotted_java(data=['##Smoke TS', 1, 10, 0, '##'])}\
''')


# TESTS_RESULTS = Template(u'''Результаты тестов:
#   Full: **${Billing_Autotesting_PythonTests_Smoke}**''')



if __name__ == "__main__":
    pass