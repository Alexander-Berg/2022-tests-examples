import os

# Turn on settings for functional tests
os.environ['YANDEX_BALANCE_DCS_RUN_TEST'] = '1'

from billing.dcs.dcs import settings

assert settings.IS_FUNCTEST_ENV, \
    'You need special settings.py for these tests'
