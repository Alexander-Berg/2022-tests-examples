# -*- coding: utf-8 -*-

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

# TESTS_LAUNCHED = u"Запущены тесты:" \
#                     u"\n\tBALANCE:" \
#                         u"\n\t\tPython:" \
#                             u"\n\t\t\tFull: {Billing_Autotesting_PythonTests_Manual}" \
#                         u"\n\t\tJava: " \
#                             u"\n\t\t\tSmoke TS: {Smoke TS}" \
#                             u"\n\t\t\tRealPayments TS: {RealPayments TS}" \
#                             u"\n\t\t\tSmoke NEW TS: {Smoke NEW TS}" \
#                     u"\n\tTRUST:" \
#                         u"\n\t\tPayments BO: {Billing_Trust_Tests_PaymentsBo}" \
#                         u"\n\t\tSubscriptions BO: {Billing_Trust_Tests_SubscriptionsBoNew}" \
#                         u"\n\t\tExport: {Billing_Trust_Tests_ExportTest}" \
#                         u"\n\t\tRegisters: {Billing_Trust_Tests_RegistersTests}"

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

if __name__ == "__main__":
    pass