# coding: utf-8
import os
import pkgutil

"""
ЗАКОНЫ КОНФИГА
Конфиг не импортит ничего из нашей либы, только стандартные и внешние либы. Конфиг могут импортить все.

В конфиге получаем значения из переменных окружения, используя дефолтное значение если они не заданы
Внутри конфига значения преобразуются в удобный форма если нужна (напр в bool)

Значения наших переменных окружения получается только в конфиге.
Когда эти значения нужны в тесте - брать их из переменных этого модуля

Поля конфига называем в соответствие с переменными окружения reporter.show.all -> REPORTER_SHOW_ALL
"""

# igogor: подбираем настройки из локального конфига, чтобы переопределить глобальные
if pkgutil.find_loader('btestlib.local_config'):
    from btestlib import local_config

    # igogor: заполняем переменные окружения из конфига
    # я бы делал наоборот - получал в конфиге данные из переменных окружения, а дальше их использовал из конфига
    for os_key, value in local_config.ENVIRONMENT_SETTINGS.iteritems():
        os.environ[os_key] = value
else:
    local_config = None


REPORTER_LEVEL = getattr(local_config, 'REPORTER_LEVEL', 'ALL')
REPORTER_SHOW_ALL = os.environ.get('reporter.show.all', 'False') == 'True'
EXTRA_LOGGING = getattr(local_config, 'EXTRA_LOGGING', True)

_LOCAL = 'LOCAL'

ENABLE_SINGLE_ACCOUNT = True if os.getenv('enable_single_account') == 'True' else False

SINGLE_ACCOUNT_ACTIVATED = True if os.getenv('single_account_activated') == 'True' else False

# Teamcity
TEAMCITY_VERSION = os.environ.get('TEAMCITY_VERSION', _LOCAL)
TEAMCITY_PROJECT_NAME = os.environ.get('TEAMCITY_PROJECT_NAME', _LOCAL)
TEAMCITY_BUILDCONF_NAME = os.environ.get('TEAMCITY_BUILDCONF_NAME', _LOCAL)
BUILD_NUMBER = os.environ.get('BUILD_NUMBER', _LOCAL)


# pagediff
PAGEDIFF_REBUILD = os.getenv('pagediff.rebuild')

ENVIRONMENT_SETTINGS = {
    'balance': 'tm',
    'apikeys': 'TMONGO1F',
    'simpleapi': 'TEST_BS',
    'balalayka': 'tmongo1g',
}
ENVIRONMENT_SETTINGS.update(getattr(local_config, 'ENVIRONMENT_SETTINGS', dict()))

# contractor
CONTRACTUS_REBUILD = os.getenv('contractus.rebuild', '')

VAULT_LOGIN = getattr(local_config, 'VAULT_LOGIN', None) or os.getenv('TEST_VAULT_LOGIN', None)
VAULT_OAUTH_TOKEN = getattr(local_config, 'VAULT_OAUTH_TOKEN', None) or os.getenv('TEST_VAULT_OAUTH', None)

TUS_OAUTH_TOKEN = getattr(local_config, 'TUS_OAUTH_TOKEN', None) or os.getenv('TUS_OAUTH_TOKEN', None)

# Trust me, i'm an oebs QA engineer
TRUST_ME_I_AM_OEBS_QA = os.getenv('i_am_oebs', False)

# For contract tests
JSON_CONTRACT_OEBS = os.getenv('json_contract_oebs', False)
FIX_CURRENT_JSON_CONTRACT = os.getenv('fix_current_json_contract', False)

# For trust mocks
USE_TRUST_MOCKS = os.getenv('use_trust_mocks', False)
