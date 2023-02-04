# coding: utf-8
import json
import os
import pkgutil
from itertools import chain

from enum import Enum

from btestlib import secrets
from btestlib.utils import ConstantsContainer
from btestlib.utils import cached

__author__ = 'igogor'

# a-vasin: не используются члены Enum, потому что python не умеет смотреть вперед
BALANCE_DEFAULT_ENV = 'tm'
APIKEYS_DEFAULT_ENV = 'APIKEYS_PAYSYS'
SIMPLEAPI_DEFAULT_ENV = 'TEST_BS'
SIMPLEAPI_NEW_DEFAULT_ENV = 'TEST_NG'
BALALAYKA_DEFAULT_ENV = 'balalayka-test.paysys'
WHITESPIRIT_DEFAULT_ENV = 'dev'
DARKSPIRIT_DEFAULT_ENV = 'tm'
MEDVED_DEFAULT_ENV = 'test'
SNOUT_DEFAULT_ENV = 'tm'
OEBSAPI_DEFAULT_ENV = 'balalayka-test1h'

NOT_SET = 'not_set'

# Соответствие среды запуска тестов префиксу, используемому для кеширования данных в S3
ENV_TO_S3_PREFIX = {'tm': 'tm',
                    'tmf': 'tm',
                    'ts': 'ts',
                    'ta': 'ta',
                    'tc': 'tc',
                    'dev': 'dev',
                    'pt': 'pt',
                    'pty': 'pty',
                    'pta': 'pta',
                    'load': 'load'
                    }

# a-vasin: импортируем настройки среды из локального конфига, если он задан
if pkgutil.find_loader('btestlib.local_config'):
    import btestlib.local_config as cfg

    if hasattr(cfg, 'ENVIRONMENT_SETTINGS'):
        for os_key, value in cfg.ENVIRONMENT_SETTINGS.iteritems():
            os.environ[os_key] = value


class AttributeIsNotSet(Exception):
    pass


class UndefinedEnvironmentError(Exception):
    pass


class Environment(object):
    def __getattribute__(self, item):
        attr = object.__getattribute__(self, item)
        if attr is not NOT_SET:
            return attr
        raise AttributeIsNotSet("Attribute '{}' is not defined for environment '{}' ({})"
                                .format(item, self.name, type(self).__name__))

    @cached
    def log_env_name(self):
        from btestlib import reporter
        reporter.log('\n{line}\n'
                     '|  Greetings, brave QA! Dark and dangerous lands of Billing are waiting for you  |\n'
                     '{line}\n'
                     '|  You have entered: {env_name:<60}|\n'
                     '{line}\n'.format(line='|' + '-' * 80 + '|', env_name=self.name))


# ----------------------------------------------------
# Balance

class BalanceEnvironmentConfiguration(Enum):
    APIKEYS_DEV1F = (
        'http://apikeys-dev1f.yandex.ru:8003/xmlrpc',
        'http://apikeys-dev1f.yandex.ru:8005/xmlrpc',
        NOT_SET,
        NOT_SET,
        'http://apikeys-dev1f.yandex.ru:8088',
        'http://apikeys-dev1f.yandex-team.ru:8089'
    )

    APIKEYS_DEV1H = (
        'http://apikeys-dev1h.balance.os.yandex.net:8003/xmlrpc',
        'http://apikeys-dev1h.balance.os.yandex.net:8005/xmlrpc',
        NOT_SET,
        NOT_SET,
        'http://apikeys-dev1h.balance.os.yandex.net:8088',
        'http://apikeys-dev1h.balance.os.yandex.net:8089'
    )


class BalanceEnvironmentTestHost(Enum):
    TM = 'tm'
    TMF = 'tmf'
    TS = 'ts'
    TA = 'ta'
    TC = 'tc'
    DEV = 'dev'
    PT = 'pt'
    PTA = 'pta'
    PTY = 'pty'
    LOAD = 'load'
    LOAD1 = 'load1'
    LOAD2 = 'load2'


class BalanceBranchHosts(Enum):
    DEV4F = 'dev4f'
    DEV2E = 'dev2e'


BalanceHosts = Enum('BalanceHosts', [(i.name, i.value) for i in
                                     chain(BalanceEnvironmentTestHost,
                                           BalanceBranchHosts)])


class BalanceEnvironment(Environment):
    @staticmethod
    def test_env(code):
        return BalanceEnvironment(
            name=code,
            medium_url='http://greed-{}.paysys.yandex.ru:8002/xmlrpc'.format(code),
            medium_tvm_url='https://balance-xmlrpc-tvm-{}.paysys.yandex.net:8004/xmlrpctvm'.format(code),
            test_balance_url='http://greed-{}.paysys.yandex.ru:30702/xmlrpc'.format(code),
            mdsproxy='https://balance-xmlrpc-tvm-{}.paysys.yandex.net:8004/mdsproxy'.format(code),
            coverage_url='http://greed-{}.paysys.yandex.ru:30702/coverage'.format(code),
            balance_ci='https://user-balance.greed-{}.paysys.yandex.ru'.format(code),
            balance_ai='https://admin-balance.greed-{}.paysys.yandex.ru'.format(code),
            oebs_gate_url='http://greed-{}.paysys.yandex.ru:8003/xmlrpc'.format(code),
            medium_http_url='https://balance-xmlrpc-tvm-{}.paysys.yandex.net:8004/httpapitvm'.format(code),
            snout_url='https://snout.greed-{}.paysys.yandex.ru/v1'.format(code),
            idm_url='https://balance-xmlrpc-tvm-{}.paysys.yandex.net:8004/idm'.format(code),
        )

    @staticmethod
    def branch_env(code):
        code = code.replace("_", "-")
        return BalanceEnvironment(
            name=code,
            medium_url='http://xmlrpc-{}.greed-branch.paysys.yandex.ru/xmlrpc'.format(code),
            medium_tvm_url='https://balance-xmlrpc-tvm-{}.greed-branch.paysys.yandex.ru:8004/xmlrpctvm'.format(code),
            test_balance_url='http://test-xmlrpc-{}.greed-branch.paysys.yandex.ru/xmlrpc'.format(code),
            mdsproxy='https://balance-xmlrpc-tvm-{}.greed-branch.paysys.yandex.ru:8004/mdsproxy'.format(code),
            coverage_url=NOT_SET,
            balance_ci='https://user-{}.greed-branch.paysys.yandex.ru'.format(code),
            balance_ai='https://admin-{}.greed-branch.paysys.yandex.ru'.format(code),
            oebs_gate_url='http://{}.greed-branch.paysys.yandex.ru:8003/xmlrpc'.format(code),
            medium_http_url='https://balance-xmlrpc-tvm-{}.greed-branch.paysys.yandex.ru:8004/httpapitvm'.format(code),
            snout_url='https://snout-{}.greed-branch.paysys.yandex.ru/v1'.format(code),
            idm_url='https://balance-xmlrpc-tvm-{}.greed-branch.paysys.yandex.ru:8004/idm'.format(code),
        )

    @staticmethod
    def docker_tunnel_branch_env(params):
        import ssl
        import xmlrpclib

        for k, v in params['tunnel_params'].iteritems():
            v['cname'] = v['cname'].format(branch=params['branch'])

        # в транспорте нам нужно скипнуть верификацию сертификата, а так же передать cname (реальный Host в заголовке)
        class CNAMESafeTransport(xmlrpclib.SafeTransport):
            def __init__(self, use_datetime=1, context=None, tunnel_params=None):
                xmlrpclib.SafeTransport.__init__(self, use_datetime, context)
                self.tunnel_params = tunnel_params

            @staticmethod
            def remove_host_header(http_connection_buffer):
                for header in http_connection_buffer:
                    if header.startswith('Host'):
                        http_connection_buffer.remove(header)
                        return header

            def send_content(self, connection, request_body):
                # old_host example: 'Host: host.docker.internal:9998'
                old_host = self.remove_host_header(connection._buffer)
                port = int(old_host.split(':')[2].strip())
                # self.tunnel_params задаются изначально в local_config
                (servant_name, servant_params), = \
                    filter(lambda (servant_name, servant_params): int(servant_params['port']) == port,
                           self.tunnel_params.iteritems())
                cname = servant_params['cname']
                connection.putheader("Host", cname)
                connection.putheader("Content-Type", "text/xml")
                connection.putheader("Content-Length", str(len(request_body)))
                connection.endheaders(request_body)

        ssl_context = hasattr(ssl, '_create_unverified_context') and ssl._create_unverified_context() or None
        transport = CNAMESafeTransport(context=ssl_context, tunnel_params=params['tunnel_params'])

        format_params = {'medium_port': params['tunnel_params']['medium']['port'],
                         'test_xmlrpc_port': params['tunnel_params']['test-xmlrpc']['port'],
                         'branch': params['branch'],
                         }

        return BalanceEnvironment(
            name='docker_tunnel_medium_{medium_port}_testxmlrpc_{test_xmlrpc_port}'
                .format(**format_params),
            medium_url='https://host.docker.internal:{medium_port}/xmlrpc'.format(**format_params),
            medium_tvm_url='https://host.docker.internal:{medium_port}/xmlrpctvm'.format(**format_params),
            test_balance_url='https://host.docker.internal:{test_xmlrpc_port}/xmlrpc'.format(**format_params),
            coverage_url=NOT_SET,
            balance_ci='https://user-{branch}.greed-branch.paysys.yandex.ru'.format(**format_params),
            balance_ai='https://admin-{branch}.greed-branch.paysys.yandex.ru'.format(**format_params),
            # TODO тоже стуннелить это
            oebs_gate_url='http://{branch}.greed-branch.paysys.yandex.ru:8003/xmlrpc'.format(**format_params),
            # TODO разобраться с транспортом для рест
            medium_http_url='https://host.docker.internal:{medium_port}/httpapitvm'.format(**format_params),
            snout_url='https://snout-{branch}.greed-branch.paysys.yandex.ru/v1'.format(**format_params),
            idm_url='https://host.docker.internal:{medium_port}/idm'.format(**format_params),
            transport=transport
        )

    def __init__(self, name='', medium_url=NOT_SET, medium_tvm_url=NOT_SET, test_balance_url=NOT_SET,
                 mdsproxy=NOT_SET, coverage_url=NOT_SET,
                 balance_ci=NOT_SET, balance_ai=NOT_SET, oebs_gate_url=NOT_SET, medium_http_url=NOT_SET, snout_url=NOT_SET,
                 idm_url=NOT_SET, transport=None):
        self.name = name
        self.medium_url = medium_url
        self.medium_tvm_url = medium_tvm_url
        self.test_balance_url = test_balance_url
        self.mdsproxy = mdsproxy
        self.coverage_url = coverage_url
        self.balance_ci = balance_ci
        self.balance_ai = balance_ai
        self.oebs_gate_url = oebs_gate_url
        self.medium_http_url = medium_http_url
        self.snout_url = snout_url
        self.transport = transport
        self.idm_url = idm_url


@cached
def balance_env():
    # Для тех, у кого докер на маке и нужно протестировать ветку (сейчас крутится на IPv6 only хосте)
    # Как заполнить этот параметр и поднять туннель см. в local_config_example.py
    docker_tunnel_params = os.getenv('balance.branch.docker_tunnel_params')
    if docker_tunnel_params:
        return BalanceEnvironment.docker_tunnel_branch_env(json.loads(docker_tunnel_params))

    # Получим среду из Balance.branch. Если там ничего, то из balance.
    # balance.custom маппится на переменную среды в teamcity и на аргумент командной строки -env_balance.
    # balance маппится на соответствующую переменную среды в teamcity, в которой список дефолтных сред.
    env_balance = os.getenv('balance.branch') or os.getenv('balance', BALANCE_DEFAULT_ENV)

    # посмотрим, не json ли впихнули. Если json, то парсим его, иначе идём дальше.
    try:
        return BalanceEnvironment(**json.loads(env_balance))
    except ValueError:
        pass  # Для формата названий веток в BALANCE: 'BALANCE-29999_super_branch'
    except TypeError:
        pass  # Для формата названий веток в APIKEYS: '51'

    # если в нас впихнули не json, то давайте посмотрим, не дефолтная ли это среда.
    if env_balance.upper() in BalanceEnvironmentTestHost.__members__:
        return BalanceEnvironment.test_env(env_balance)
    # Если это не json и не дефолтная среда, то вдруг это параметры для APIKEYS:
    if env_balance in BalanceEnvironmentConfiguration.__members__:
        return BalanceEnvironment(env_balance, *BalanceEnvironmentConfiguration[env_balance].value)

    # если ничего из вышеперечисленного, то считаем, что передали имя ветки:
    return BalanceEnvironment.branch_env(env_balance)


# -------------------------------------------------
# APIKeys

class ApikeysEnvironmentConfiguration(Enum):

    APIKEYS_DEV_PAYSYS = (
        'http://apikeys-dev.paysys.yandex.net:8666',
        'http://apikeys-dev.paysys.yandex.net:8025',
        'https://apikeys-dev1h.paysys.yandex.net:8093',
        'https://apikeys-dev1h.paysys.yandex.net:8092',
        'http://apikeys-dev.paysys.yandex.net:8666/balance-client'
    )

    APIKEYS_PAYSYS = (
        'http://apikeys-test.paysys.yandex.net:8666',
        'http://apikeys-test.paysys.yandex.net:18025',
        'https://developer.test1f.tech.yandex.ru',
        'https://admin-developer.test1f.tech.yandex-team.ru',
        'http://apikeys-dev1h.balance.os.yandex.net:8066/balance-client'
    )

    APIKEYS_DEV_PULL_PAYSYS = (
        'http://pull-requests-51-back.apikeys-dev.paysys.yandex.ru',
        'http://test-pull-requests-51-back.apikeys-dev.paysys.yandex.ru',
        'https://apikeys-dev1h.paysys.yandex.net:8093',
        'https://apikeys-dev1h.paysys.yandex.net:8092',
        'http://apikeys-dev.paysys.yandex.net:8666/balance-client'
    )


class ApikeysEnvironment(Environment):

    @staticmethod
    def branch_env(code):
        code = code.replace("_", "-")
        code = code.replace("/", "-")
        return ApikeysEnvironment(
            name=code,
            apikeys_url='http://{}-back.apikeys-dev.paysys.yandex.ru'.format(code),
            test_apikeys_url='http://test-{}-back.apikeys-dev.paysys.yandex.ru'.format(code),
            apikeys_ci='https://apikeys-dev1h.paysys.yandex.net:8093',
            apikeys_ai='https://apikeys-dev1h.paysys.yandex.net:8092',
            apikeys_notify_params='http://apikeys-dev.paysys.yandex.net:8666/balance-client'
        )

    def __init__(self, name='', apikeys_url=NOT_SET, test_apikeys_url=NOT_SET,
                 apikeys_ci=NOT_SET, apikeys_ai=NOT_SET, apikeys_notify_params=NOT_SET, ):
        self.name = name
        self.apikeys_url = apikeys_url
        self.test_apikeys_url = test_apikeys_url
        self.apikeys_ci = apikeys_ci
        self.apikeys_ai = apikeys_ai
        self.apikeys_notify_params = apikeys_notify_params


@cached
def apikeys_env():
    # json_config = os.getenv('apikeys.custom')
    # if json_config:
    #     return ApikeysEnvironment(**json.loads(json_config))

    # Получим среду из Apikeys.branch. Если там ничего, то из balance.
    # balance.custom маппится на переменную среды в teamcity и на аргумент командной строки -env_balance.
    # balance маппится на соответствующую переменную среды в teamcity, в которой список дефолтных сред.
    env_apikeys = os.getenv('apikeys.branch') or os.getenv('apikeys', APIKEYS_DEFAULT_ENV)

    # посмотрим, не json ли впихнули. Если json, то парсим его, иначе идём дальше.
    try:
        return ApikeysEnvironment(**json.loads(env_apikeys))
    except ValueError:
        pass  # Для формата названий веток в BALANCE: 'BALANCE-29999_super_branch'
    except TypeError:
        pass  # Для формата названий веток в APIKEYS: '51'

    if env_apikeys in ApikeysEnvironmentConfiguration.__members__:
        return ApikeysEnvironment(env_apikeys, *ApikeysEnvironmentConfiguration[env_apikeys].value)

    # если ничего из вышеперечисленного, то считаем, что передали имя ветки:
    return ApikeysEnvironment.branch_env(env_apikeys)


# -----------------------------------
# SimpleAPI
# https://wiki.yandex-team.ru/balance/simple/#sredyidostupy1


class SimpleapiEnvironmentConfiguration(Enum):
    # ============================== TS SECTION ==============================
    # ------------------------------- POSTGRES -------------------------------
    # .................................. BS ..................................
    TEST_NG = dict(
        simple_url='https://trust-payments-test.paysys.yandex.net:8028/simple/xmlrpc',
        simple_url_old='https://balance-payments-test.paysys.yandex.net:8023/simpleapi/xmlrpc',
        pcidss_api_url='https://pci-tf.fin.yandex.net:443/api/',
        pcidss_web_url='https://pci-tf.fin.yandex.net:443/web/',
        trust_web_url='https://trust-test.yandex.{}/web/',
        payments_api_url='https://trust-payments-test.paysys.yandex.net:8028/trust-payments/v2/',
        account_api_url='https://trust-payments-xg-test.paysys.yandex.net:8038/trust-payments/v2/',
        db_driver='postgre',
        pcidss_inside_url=NOT_SET,
        balance_xmlrpc_url='http://greed-ts.paysys.yandex.ru:8002/xmlrpc',
        balance_test_xmlrpc_url='http://xmlrpc-balance.greed-ts.paysys.yandex.ru:30702/xmlrpc',
        masterpass_wallet='https://testwallet.masterpass.ru/',
        masterpass_pspapi='https://testpspapi.masterpass.ru/',
        trust_log_url='trust-test1h.paysys.yandex.net',
        yam_test_phone_url='http://autoserver.yandex1.ymdev.yandex.ru:39080/sms_list_xml',
        kinopoisk_plus_payment_url='https://trust-ext-cert-proxy-test.paysys.yandex.ru/web/payment',
        uber_oauth_url='https://login.uber.com/oauth/v2/token',
        mongo_url='trust-mongo-test1{}.paysys.yandex.net',
        mongo_usr='bo',
        # todo-igogor пароли совпадают но вообще лучше выделить в отдельный секрет
        mongo_pwd=secrets.get_secret(*secrets.DbPwd.BALANCE_BO_PWD),
        trust_paysys_url='https://trust-paysys-test.paysys.yandex.net:8025/',
        emulator_inner_url='http://trust-test1h.paysys.yandex.net:18303/',
        emulator_outer_url='https://trust-test1h.paysys.yandex.net:18304/',
        kinopoisk_plus_api_url='https://kp1-api.tst.kp.yandex.net/',
    )

    # -------------------------------- ORACLE --------------------------------
    # .................................. BS ..................................
    TEST_BS = dict(
        simple_url='https://trust-payments-old-test.paysys.yandex.net:8027/simple/xmlrpc',
        simple_url_old='https://balance-payments-test.paysys.yandex.net:8023/simpleapi/xmlrpc',
        pcidss_api_url='https://pci-tf.fin.yandex.net:443/api/',
        pcidss_web_url='https://pci-tf.fin.yandex.net:443/web/',
        trust_web_url='https://trust-test.yandex.{}/web/',
        payments_api_url='https://trust-payments-old-test.paysys.yandex.net:8027/trust-payments/v2/',
        db_driver='oracle',
        pcidss_inside_url=NOT_SET,
        balance_xmlrpc_url='http://greed-ts.paysys.yandex.ru:8002/xmlrpc',
        balance_test_xmlrpc_url='http://xmlrpc-balance.greed-ts.paysys.yandex.net:30702/xmlrpc',
        masterpass_wallet='https://testwallet.masterpass.ru/',
        masterpass_pspapi='https://testpspapi.masterpass.ru/',
        trust_log_url='trust-test1h.paysys.yandex.net',
        yam_test_phone_url='http://autoserver.yandex1.ymdev.yandex.ru:39080/sms_list_xml',
        kinopoisk_plus_payment_url='https://trust-ext-cert-proxy-test.paysys.yandex.ru/web/payment',
        uber_oauth_url='https://login.uber.com/oauth/v2/token',
        mongo_url='trust-mongo-test1{}.paysys.yandex.net',
        mongo_usr='bo',
        mongo_pwd=secrets.get_secret(*secrets.DbPwd.BALANCE_BO_PWD),
        trust_paysys_url='https://trust-paysys-test.paysys.yandex.net:8025/',
        emulator_inner_url='http://trust-test1h.paysys.yandex.net:18303/',
        emulator_outer_url='https://trust-test1h.paysys.yandex.net:18304/',
        kinopoisk_plus_api_url='https://kp1-api.tst.kp.yandex.net/',
    )

    # .................................. BO ..................................
    TEST_BO = dict(
        simple_url='https://balance-payments-test.paysys.yandex.net:8023/simpleapi/xmlrpc',
        simple_url_old='https://balance-payments-test.paysys.yandex.net:8023/simpleapi/xmlrpc',
        pcidss_api_url='https://pci-tf.fin.yandex.net:443/api/',
        pcidss_web_url='https://pci-tf.fin.yandex.net:443/web/',
        trust_web_url='https://trust-test.yandex.{}/web/',
        payments_api_url='https://trust-payments-old-test.paysys.yandex.net:8027/trust-payments/v2/',
        db_driver='oracle',
        pcidss_inside_url=NOT_SET,
        balance_xmlrpc_url='http://greed-ts.paysys.yandex.ru:8002/xmlrpc',
        balance_test_xmlrpc_url='http://xmlrpc-balance.greed-ts.paysys.yandex.ru:30702/xmlrpc',
        masterpass_wallet='https://testwallet.masterpass.ru/',
        masterpass_pspapi='https://testpspapi.masterpass.ru/',
        trust_log_url='trust-test1h.paysys.yandex.net',
        yam_test_phone_url='http://autoserver.yandex1.ymdev.yandex.ru:39080/sms_list_xml',
        kinopoisk_plus_payment_url='https://trust-ext-cert-proxy-test.paysys.yandex.ru/web/payment',
        uber_oauth_url='https://login.uber.com/oauth/v2/token',
        mongo_url='trust-mongo-test1{}.paysys.yandex.net',
        mongo_usr='bo',
        mongo_pwd=secrets.get_secret(*secrets.DbPwd.BALANCE_BO_PWD),
        trust_paysys_url='https://trust-paysys-test.paysys.yandex.net:8025/',
        emulator_inner_url='http://trust-test1h.paysys.yandex.net:18303/',
        emulator_outer_url='https://trust-test1h.paysys.yandex.net:18304/',
        kinopoisk_plus_api_url='https://kp1-api.tst.kp.yandex.net/',
    )

    # ============================== DEV SECTION =============================
    # ------------------------------- POSTGRES -------------------------------
    # .................................. BS ..................................
    DEV_NG = dict(
        simple_url='https://trust-payments-dev.paysys.yandex.net:8028/simple/xmlrpc',
        simple_url_old='https://balance-payments-dev.paysys.yandex.net:8023/simpleapi/xmlrpc',
        pcidss_api_url='https://pci-dev1f.paysys.yandex.net:443/api/',
        pcidss_web_url='https://pci-dev1f.paysys.yandex.net:443/web/',
        trust_web_url='https://trust-dev.yandex.{}/web/',
        payments_api_url='https://trust-payments-dev.paysys.yandex.net:8028/trust-payments/v2/',
        db_driver='postgre',
        pcidss_inside_url='https://pci-dev1{}.paysys.yandex.net:{}/',
        balance_xmlrpc_url='http://greed-dev.paysys.yandex.ru:8002/xmlrpc',
        balance_test_xmlrpc_url='http://xmlrpc-balance.greed-tm.paysys.yandex.ru:30702/xmlrpc',
        masterpass_wallet='https://testwallet.masterpass.ru/',
        masterpass_pspapi='https://testpspapi.masterpass.ru/',
        trust_log_url='trust-dev1h.paysys.yandex.net',
        yam_test_phone_url='http://autoserver.yandex1.ymdev.yandex.ru:39080/sms_list_xml',
        kinopoisk_plus_payment_url=NOT_SET,
        uber_oauth_url='https://login.uber.com/oauth/v2/token',
        mongo_url='trust-mongo-dev1{}.paysys.yandex.net',
        mongo_usr='trust',
        mongo_pwd='trust',
        trust_paysys_url='https://trust-paysys-dev.paysys.yandex.net:8025/',
        emulator_inner_url='http://trust-dev1h.paysys.yandex.net:18303/',
        emulator_outer_url='https://trust-dev1h.paysys.yandex.net:18304/',
        kinopoisk_plus_api_url='https://kp1-api.tst.kp.yandex.net/',
    )

    # -------------------------------- ORACLE --------------------------------
    # .................................. BS ..................................
    DEV_BS = dict(
        simple_url='https://trust-payments-old-dev.paysys.yandex.net:8027/simple/xmlrpc',
        simple_url_old='https://balance-payments-dev.paysys.yandex.net:8023/simpleapi/xmlrpc',
        pcidss_api_url='https://pci-dev1f.paysys.yandex.net:443/api/',
        pcidss_web_url='https://pci-dev1f.paysys.yandex.net:443/web/',
        trust_web_url='https://trust-dev.yandex.{}/web/',
        payments_api_url='https://trust-payments-old-dev.paysys.yandex.net:8027/trust-payments/v2/',
        db_driver='oracle',
        pcidss_inside_url='https://pci-dev1{}.paysys.yandex.net:{}/',
        balance_xmlrpc_url='http://greed-dev.paysys.yandex.ru:8002/xmlrpc',
        balance_test_xmlrpc_url='http://xmlrpc-balance.greed-tm.paysys.yandex.ru:30702/xmlrpc',
        masterpass_wallet='https://testwallet.masterpass.ru/',
        masterpass_pspapi='https://testpspapi.masterpass.ru/',
        trust_log_url='trust-dev1h.paysys.yandex.net',
        yam_test_phone_url='http://autoserver.yandex1.ymdev.yandex.ru:39080/sms_list_xml',
        kinopoisk_plus_payment_url=NOT_SET,
        uber_oauth_url='https://login.uber.com/oauth/v2/token',
        mongo_url='trust-mongo-dev1{}.paysys.yandex.net',
        mongo_usr='trust',
        mongo_pwd='trust',
        trust_paysys_url='https://trust-paysys-dev.paysys.yandex.net:8025/',
        emulator_inner_url='http://trust-dev1h.paysys.yandex.net:18303/',
        emulator_outer_url='https://trust-dev1h.paysys.yandex.net:18304/',
        kinopoisk_plus_api_url='https://kp1-api.tst.kp.yandex.net/',
    )

    # .................................. BO ..................................
    DEV_BO = dict(
        simple_url='https://balance-payments-dev.paysys.yandex.net:8023/simpleapi/xmlrpc',
        simple_url_old='https://balance-payments-dev.paysys.yandex.net:8023/simpleapi/xmlrpc',
        pcidss_api_url='https://pci-dev1f.paysys.yandex.net:443/api/',
        pcidss_web_url='https://pci-dev1f.paysys.yandex.net:443/web/',
        trust_web_url='https://trust-dev.yandex.{}/web/',
        payments_api_url='https://trust-payments-old-dev.paysys.yandex.net:8027/trust-payments/v2/',
        db_driver='oracle',
        pcidss_inside_url='https://pci-dev1{}.paysys.yandex.net:{}/',
        balance_xmlrpc_url='http://greed-dev.paysys.yandex.ru:8002/xmlrpc',
        balance_test_xmlrpc_url='http://xmlrpc-balance.greed-tm.paysys.yandex.ru:30702/xmlrpc',
        masterpass_wallet='https://testwallet.masterpass.ru/',
        masterpass_pspapi='https://testpspapi.masterpass.ru/',
        trust_log_url='trust-dev1h.paysys.yandex.net',
        yam_test_phone_url='http://autoserver.yandex1.ymdev.yandex.ru:39080/sms_list_xml',
        kinopoisk_plus_payment_url=NOT_SET,
        uber_oauth_url='https://login.uber.com/oauth/v2/token',
        mongo_url='trust-mongo-dev1{}.paysys.yandex.net',
        mongo_usr='trust',
        mongo_pwd='trust',
        trust_paysys_url='https://trust-paysys-dev.paysys.yandex.net:8025/',
        emulator_inner_url='http://trust-dev1h.paysys.yandex.net:18303/',
        emulator_outer_url='https://trust-dev1h.paysys.yandex.net:18304/',
        kinopoisk_plus_api_url='https://kp1-api.tst.kp.yandex.net/',
    )

    # **************************** DOCKER SECTION ****************************
    DOCKER_LOCAL = dict(
        simple_url='http://localhost:18018/simple/xmlrpc',
        simple_url_old=NOT_SET,
        pcidss_api_url='http://localhost:18025/api/',
        pcidss_web_url='http://localhost:18025/web/',
        trust_web_url=NOT_SET,
        payments_api_url=NOT_SET,
        db_driver='mysql',
        pcidss_inside_url=NOT_SET,
        balance_xmlrpc_url='http://greed-ts.paysys.yandex.ru:8002/xmlrpc',
        balance_test_xmlrpc_url='http://xmlrpc-balance.greed-ts.paysys.yandex.ru:30702/xmlrpc',
        masterpass_wallet='https://testwallet.masterpass.ru/',
        masterpass_pspapi='https://testpspapi.masterpass.ru/',
        trust_log_url=NOT_SET,
        yam_test_phone_url='http://autoserver.yandex1.ymdev.yandex.ru:39080/sms_list_xml',
        kinopoisk_plus_payment_url=NOT_SET,
        uber_oauth_url='https://login.uber.com/oauth/v2/token',
        mongo_url=NOT_SET,
        mongo_usr=NOT_SET,
        mongo_pwd=NOT_SET,
        trust_paysys_url=NOT_SET,
        emulator_inner_url='http://localhost:18303/',
        emulator_outer_url='http://localhost:18303/',
    )

    def __getattr__(self, item):
        return SimpleapiEnvironment(self.name, *self.value).__getattribute__(item)


class TrustDbNames(ConstantsContainer):
    constant_type = str

    BS_ORACLE = 'bs'
    BS_ORACLE_DEV = 'bs_dev'
    BS_PG = 'bs_ng_single_host'
    BS_PG_DEV = 'bs_new_dev'
    BS_XG = 'bs_xg_single_host'
    BS_XG_DEV = 'bs_xg_dev'


class TrustApiUrls(ConstantsContainer):
    constant_type = str

    XMLRPC_ORA = 'https://trust-payments-old-test.paysys.yandex.net:8027/simple/xmlrpc'
    XMLRPC_PG = 'https://trust-payments-test.paysys.yandex.net:8028/simple/xmlrpc'
    XMLRPC_PG_TAXI = 'https://trust-payments-xg-test.paysys.yandex.net:8038/simple/xmlrpc'


class SimpleapiEnvironment(Environment):
    # todo-igogor эти параметры на самом деле должны заменять значения в энве
    DB_NAME = TrustDbNames.BS_ORACLE
    XMLRPC_URL = TrustApiUrls.XMLRPC_ORA

    @staticmethod
    def switch_param(service=None, dbname=TrustDbNames.BS_ORACLE, xmlrpc_url=TrustApiUrls.XMLRPC_ORA):
        if service:
            dbname = service.trust_dbname
            xmlrpc_url = service.trust_xmlrpc

        SimpleapiEnvironment.DB_NAME = dbname
        SimpleapiEnvironment.XMLRPC_URL = xmlrpc_url


    def __init__(self, name='', simple_url=NOT_SET, simple_url_old=NOT_SET, pcidss_api_url=NOT_SET,
                 pcidss_web_url=NOT_SET, trust_web_url=NOT_SET, payments_api_url=NOT_SET,
                 db_driver=NOT_SET, pcidss_inside_url=NOT_SET, balance_xmlrpc_url=NOT_SET,
                 balance_test_xmlrpc_url=NOT_SET, masterpass_wallet=NOT_SET, masterpass_pspapi=NOT_SET,
                 trust_log_url=NOT_SET, yam_test_phone_url=NOT_SET, kinopoisk_plus_payment_url=NOT_SET,
                 uber_oauth_url=NOT_SET, mongo_url=NOT_SET, mongo_usr=NOT_SET, mongo_pwd=NOT_SET,
                 trust_paysys_url=NOT_SET, emulator_inner_url=NOT_SET, emulator_outer_url=NOT_SET,
                 kinopoisk_plus_api_url=NOT_SET, account_api_url=NOT_SET):
        self.name = name
        self.simple_url = simple_url
        self.simple_url_old = simple_url_old
        self.pcidss_api_url = pcidss_api_url
        self.pcidss_web_url = pcidss_web_url
        self.trust_web_url = trust_web_url
        self.payments_api_url = payments_api_url
        self.db_driver = db_driver
        self.pcidss_inside_url = pcidss_inside_url
        self.balance_xmlrpc_url = balance_xmlrpc_url
        self.balance_test_xmlrpc_url = balance_test_xmlrpc_url
        self.masterpass_wallet = masterpass_wallet
        self.masterpass_pspapi = masterpass_pspapi
        self.trust_log_url = trust_log_url
        self.yam_test_phone_url = yam_test_phone_url
        self.kinopoisk_plus_payment_url = kinopoisk_plus_payment_url
        self.uber_oauth_url = uber_oauth_url
        self.mongo_url = mongo_url
        self.mongo_usr = mongo_usr
        self.mongo_pwd = mongo_pwd
        self.trust_paysys_url = trust_paysys_url
        self.emulator_inner_url = emulator_inner_url
        self.emulator_outer_url = emulator_outer_url
        self.kinopoisk_plus_api_url = kinopoisk_plus_api_url
        self.account_api_url = account_api_url


@cached
def simpleapi_env_ora():
    json_config = os.getenv('simpleapi.custom')
    if json_config:
        return SimpleapiEnvironment(**json.loads(json_config))

    env_code = os.environ.get('simpleapi', SIMPLEAPI_DEFAULT_ENV)

    if env_code in SimpleapiEnvironmentConfiguration.__members__:
        return SimpleapiEnvironment(env_code, **SimpleapiEnvironmentConfiguration[env_code].value)

    raise UndefinedEnvironmentError(u"Unknown environment for SimpleAPI: " + env_code)


@cached
def simpleapi_env_pg():
    json_config = os.getenv('simpleapi.new.custom')
    if json_config:
        return SimpleapiEnvironment(**json.loads(json_config))

    env_code = os.environ.get('simpleapi.new', SIMPLEAPI_NEW_DEFAULT_ENV)

    if env_code in SimpleapiEnvironmentConfiguration.__members__:
        return SimpleapiEnvironment(env_code, **SimpleapiEnvironmentConfiguration[env_code].value)

    raise UndefinedEnvironmentError(u"Unknown environment for new SimpleAPI: " + env_code)


def simpleapi_env():
    # todo-igogor здесь надо сделать прямо.
    '''
    Надо чтобы в классе енва можно было положить набор параметров который будет хранить инстанс текущего енва
    Создать метод который будет изменять инстанс. Этот метод надо будет звать перед тестом если он кастомизирует енв
    В хуке возвращать инстанс в исходную форму.
    '''
    if SimpleapiEnvironment.DB_NAME == TrustDbNames.BS_ORACLE:
        return simpleapi_env_ora()
    else:
        return simpleapi_env_pg()


# -----------------------------------
# Medved

class MedvedEnvironmentHost(Enum):
    TEST = 'test'
    DEV = 'dev'


class MedvedEnvironment(Environment):
    @staticmethod
    def env(code):
        return MedvedEnvironment(
            name=code,
            api_url='https://medved-{}.paysys.yandex.net:8025/api/v1.0'.format(code),
            web_url='https://medved-{}.paysys.yandex.net'.format(code)
        )

    def __init__(self, name='', api_url=NOT_SET, xmlrpc_test_url=NOT_SET, web_url=NOT_SET):
        self.name = name
        self.api_url = api_url
        self.xmlrpc_test_url = xmlrpc_test_url
        self.web_url = web_url


@cached
def medved_env():
    json_config = os.getenv('medved.custom')
    if json_config:
        return MedvedEnvironment(**json.loads(json_config))

    env_code = os.getenv('medved', MEDVED_DEFAULT_ENV)

    if env_code.upper() in MedvedEnvironmentHost.__members__:
        return MedvedEnvironment.env(env_code)

    raise UndefinedEnvironmentError(u"Unknown environment for Medved: " + env_code)


# -----------------------------------
# Whitespirit

class WhitespiritEnvironmentHost(Enum):
    DEV = 'dev'
    TEST = 'test'


class WhitespiritEnvironment(Environment):
    @staticmethod
    def env(code):
        return WhitespiritEnvironment(
            name=code,
            whitespirit_url='https://whitespirit-{}.paysys.yandex.net:8080'.format(code),
            balancer_url='https://balance-hudsucker-{}.paysys.yandex.net:8081'.format(code),
            debug_url='https://whitespirit-{}.paysys.yandex.net:8080'.format(code)
        )

    def __init__(self, name='', whitespirit_url=NOT_SET, balancer_url=NOT_SET, debug_url=NOT_SET):
        self.name = name
        self.whitespirit_url = whitespirit_url
        self.balancer_url = balancer_url
        self.debug_url = debug_url


@cached
def whitespirit_env():
    json_config = os.getenv('whitespirit.custom')
    if json_config:
        return WhitespiritEnvironment(**json.loads(json_config))

    env_code = os.getenv('whitespirit', WHITESPIRIT_DEFAULT_ENV)

    if env_code.upper() in WhitespiritEnvironmentHost.__members__:
        return WhitespiritEnvironment.env(env_code)

    raise UndefinedEnvironmentError(u"Unknown environment for Whitespirit: " + env_code)


def get_ws_url():
    if os.getenv('proxy_to_balancer', ''):
        return whitespirit_env().balancer_url
    return whitespirit_env().whitespirit_url


# -----------------------------------
# Darkspirit

class DarkspiritEnvironmentHost(Enum):
    TS = 'ts'
    TM = 'tm'


class DarkspiritEnvironment(Environment):
    @staticmethod
    def env(code):
        return DarkspiritEnvironment(
            name=code,
            darkspirit_url='https://greed-{}.paysys.yandex.net:8616/v1'.format(code)
        )

    def __init__(self, name='', darkspirit_url=NOT_SET):
        self.name = name
        self.darkspirit_url = darkspirit_url


@cached
def darkspirit_env():
    json_config = os.getenv('darkspirit.custom')
    if json_config:
        return DarkspiritEnvironment(**json.loads(json_config))

    env_code = os.getenv('darkspirit', DARKSPIRIT_DEFAULT_ENV)

    if env_code.upper() in DarkspiritEnvironmentHost.__members__:
        return DarkspiritEnvironment.env(env_code)

    raise UndefinedEnvironmentError(u"Unknown environment for Darkspirit: " + env_code)


# -----------------------------------
# Snout

class SnoutEnvironmentHost(Enum):
    TM = 'tm'


class SnoutEnvironment(Environment):
    @staticmethod
    def env(code):
        return SnoutEnvironment(
            name=code,
            snout_url='https://snout.greed-{}.paysys.yandex.ru/v1'.format(code)
        )

    def __init__(self, name='', snout_url=NOT_SET):
        self.name = name
        self.snout_url = snout_url


@cached
def snout_env():
    json_config = os.getenv('snout.custom')
    if json_config:
        return SnoutEnvironment(**json.loads(json_config))

    env_code = os.getenv('snout', SNOUT_DEFAULT_ENV)

    if env_code.upper() in SnoutEnvironmentHost.__members__:
        return SnoutEnvironment.env(env_code)

    raise UndefinedEnvironmentError(u"Unknown environment for Snout: " + env_code)


# -----------------------------------
# Oebsapi

class OebsapiEnvironmentHost(Enum):
    TEST = 'balalayka-test1h'
    DEV = 'balalayka-dev1h'


class OebsapiEnvironment(Environment):
    @staticmethod
    def env(code):
        return OebsapiEnvironment(
            name=code,
            url='https://{}.paysys.yandex.net:8002'.format(code)
        )

    def __init__(self, name='', url=NOT_SET):
        self.name = name
        self.url = url


@cached
def oebsapi_env():
    json_config = os.getenv('oebsapi.custom')
    if json_config:
        return OebsapiEnvironment(**json.loads(json_config))

    env_code = os.getenv('oebsapi', OEBSAPI_DEFAULT_ENV)

    if env_code.lower() in [member._value_ for name, member in OebsapiEnvironmentHost.__members__.items()]:
        return OebsapiEnvironment.env(env_code)

    raise UndefinedEnvironmentError(u"Unknown environment for Oebsapi: " + env_code)
