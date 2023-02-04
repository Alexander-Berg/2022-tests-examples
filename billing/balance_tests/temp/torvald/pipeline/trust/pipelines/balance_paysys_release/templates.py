# -*- coding: utf-8 -*-

from mako.template import Template

TEAMCITY_TEST_PACK = [u'Billing_Autotesting_PythonTests_Manual',  # BALANCE Manual
                      u'Billing_Trust_Tests_PaymentsBo',          # TRUST:TS Payments BO
                      u'Billing_Trust_Tests_SubscriptionsBoNew',  # TRUST:TS Subscriptions BO
                      u'Billing_Trust_Tests_ExportTest',          # TRUST:TS Export
                      u'Billing_Trust_Tests_RegistersTests'       # TRUST:TS Registers
                      ]

AQUA_TEST_PACK = [u'Smoke_TS',  # Java Smoke TS
                  u'RealPayments_TS',  # Java RealPayments TS
                  u'Smoke_NEW_TS'   # Java Smoke NEW TS
                  ]

DESCRIPTION = Template(u'Version: ${version}')
SUMMARY = Template(u'Релиз yb-balance-paysys ${version}')

CONDUCTOR_TICKETS = u'Создан тикет на выкладку: {conductor_ticket}'
CHANGES = u'Изменения: {changes}'

TESTS_LAUNCHED = u'''Запущены тесты:
  BALANCE:
    Python:
      Full: **{Billing_Autotesting_PythonTests_Manual}**
    Java:
      Smoke TS: **{Smoke_TS}**
      RealPayments TS: **{RealPayments_TS}**
      Smoke NEW TS: **{Smoke_NEW_TS}**
  TRUST:
    Payments BO: **{Billing_Trust_Tests_PaymentsBo}**
    Subscriptions BO: **{Billing_Trust_Tests_SubscriptionsBoNew}**
    Export: **{Billing_Trust_Tests_ExportTest}**
    Registers: **{Billing_Trust_Tests_RegistersTests}**'''


TESTS_RESULTS = u'''Результаты тестов:
  BALANCE:
    Python:
      Full: **{Billing_Autotesting_PythonTests_Manual}**
    Java:
      Smoke TS: **{Smoke_TS}**
      RealPayments TS: **{RealPayments_TS}**
      Smoke NEW TS: **{Smoke_NEW_TS}**
  TRUST:
    Payments BO: **{Billing_Trust_Tests_PaymentsBo}**
    Subscriptions BO: **{Billing_Trust_Tests_SubscriptionsBoNew}**
    Export: **{Billing_Trust_Tests_ExportTest}**
    Registers: **{Billing_Trust_Tests_RegistersTests}**'''

TESTS_RESULTS2 = Template(u'''<%def name="dotted(data, widths, addition=None)">
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
${"##BALANCE##"}\
${dotted_python(data=['##Python', 'Failed', 'Passed', 'Ignored', 'Muted', '##'])}\
${dotted_python(data=['##Full', 25, 16, 5, 0, '##'])}\
${dotted_java(data=['##Java', 'Failed', 'Passed', 'Revoked', '##'])}\
${dotted_java(data=['##Smoke TS', 1, 10, 0, '##'])}\
${dotted_java(data=['##RealPayments TS', 2, 15, 2, '##'])}\
${dotted_java(data=['##Smoke NEW TS', 0, 12, 0, '##'])}
${"##TRUST##"}\
${dotted_python(data=['##Python', 'Failed', 'Passed', 'Ignored', 'Muted', '##'])}\
${dotted_python(data=['##Payments BO', 25, 16, 5, 0, '##'])}\
${dotted_python(data=['##Subscriptions BO', 25, 16, 5, 0, '##'])}\
${dotted_python(data=['##Export', 25, 16, 5, 0, '##'])}\
${dotted_python(data=['##Registers', 25, 16, 5, 0, '##'])}\
''')

if __name__ == "__main__":
    pass