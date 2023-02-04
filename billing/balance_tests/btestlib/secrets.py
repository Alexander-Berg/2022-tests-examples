# coding: utf-8
__author__ = 'chihiro'

import base64
import inspect
import os
import sys
from collections import namedtuple

import vault_client
from tenacity import retry, wait_fixed, stop_after_attempt

from btestlib import utils, reporter, config

OAUTH_PATH = '/home/teamcity/.balance-tests/testuser_balance1_yav'
RSA_LOGIN = 'testuser-balance1'

Secret = namedtuple('Secret', 'uuid, key, path')


def _examples():
    """
    Чтобы использование секретницы в тестах заработало локально надо добавить ssh ключ на стафф и ssh агента локально
    Команда на юниксе: ssh-add

    Чтобы получить текстовое значение поля секрета
    >>> field_value = get_secret(uuid=u"uuid секрета или версии секрета", key=u"имя поля")
    В качестве uuid по умолчанию лучше использовать uuid секрета (начинается с sec- а не ver-).
    Тогда при изменении версии секрета автоматически подтягивается последняя.
    Каждый секрет после похода в секретницу кэшируется и получается только один раз
    Но при запуске в разных процессах (xdist) каждый секрет будет получаться в каждом процессе

    Чтобы получить все поля секрета как словарь
    >>> values_dict = get_secret(uuid=u"uuid секрета или версии секрета")

    Чтобы использовать секрет который является файлом - например сертификат.
    Надо положить его значение как текст в секретницу. И потом вызвать get_secret с переданным path
    >>> file_path = get_secret(uuid=u"uuid секрета или версии секрета", key=u"имя поля",
    ...                        path=u"путь куда положить секрет")
    При этом метод вернет путь к созданному файлу, а не само значение секрета.
    Каждый файл создается только один раз. Далее если он уже создан - не трогается.
    Чтобы положить бинарный файл в секретницу можно воспользоваться утилитой (ставится вместе с питон клиентом)
    >>> yav create secret secret_uuid -f key_name=file_path
    Бинарные данные файла будут закодированы в base64 и положены в секретницу как текст.
    При использовании секрета ничего менять не надо. Я сделал чтобы раскодировалось автоматически.
    Для текстовых файлов можно в принципе делать также но сохранять читаемый текст имхо удобнее.

    Чтобы обеспечить, чтобы при изменении версии секрета файл обновился в путь файла добавляется папка с версией
    >>> file_path = get_secret(uuid=u"uuid секрета или версии секрета", key=u"имя поля",
    ...                        path=utils.project_file('btestlib/resources/xmlrpc_cert.pem'))
    Файл будет положен не в btestlib/resources/xmlrpc_cert.pem
    а в btestlib/resources/ver-01ct5thcqabjz7fb2a8k3cav2x/xmlrpc_cert.key
    Это не совсем прозрачно, но лучшее что я придумал, чтобы обеспечить обновление при подъеме версии

    Чтобы удобно было хранить параметры для получения конкретного секрета добавлена функция
    >>> secret_params = secret(uuid=u"uuid секрета или версии секрета", key=u"имя поля",
    ...                        path=u"путь куда положить секрет")
    Функция возвращает namedtuple с теми параметрами, что были в нее переданы
    Тогда получать секрет в конкретном месте можно без хардкода
    >>> some_secret = get_secret(*secret_params)
    Это удобно потому что параметры секрета лежат в одном месте и сгруппированы
    и при этом сам секрет получается только тогда когда он нужен и код получает только те секреты что использует

    Пока я предлагаю все параметры секретов хранить в этом модуле в полях классов по типу
    Если все параметры секретов будут храниться в этом модуле в классах наследниках SecretContainer,
    то их можно удобно получить функцией
    >>> get_all_secrets_params()
    Удобно если надо добавить всем им право или тэг
    Это не требуется для использования секретов, но кажется так будет структурированнее.

    Секрет можно создавать в интерфейсе yav.yandex-team.ru, через коммандлайн утилиту yav или через python клиент
    Для питон клиента я написал скрипт
    >>> create_secret(name=u'текстовое имя секрета', comment=u'Более подробное описание секрета',
    ...               key_values=u'словарь, ключи - имена полей, значения - сами секреты')
    При создании также добавятся дефолтные тэги и права: на редактирование для отделов биллинга и траста,
    на чтение для testuser-balance1 для получения секретов с тимсити

    Для всех секретов исползующихся в тестах предлагаю добавлять тэг balance-tests и
    teamcity, если используется на тимсити, aqua если используется на акве
    Также неплохо помечать тэгом что хранит секрет - db, certificate, passport, passport-test, ...
    Потому что тэги это единственное с помощью чего можно секреты фильтровать.
    И я говорил с менеджером секретницы - нового пока вроде не предвидится.

    https://vault-api.passport.yandex.net/docs/
    https://wiki.yandex-team.ru/security/secrets/
    """


class SecretsContainer(object):
    pass


def secret(uuid, key=None, path=None):
    return Secret(uuid=uuid, key=key, path=path)


class Tokens(SecretsContainer):
    STARTREK_CHECK_TOKEN = secret(uuid='sec-01g4hmytkbyhz8h7safrtdrhf2', key='token')
    PIPELINER_OAUTH_TOKEN = secret(uuid='sec-01ctqsn5pwjkezp5jwmg5fgwq5', key='token')
    YT_OAUTH_TOKEN = secret(uuid='sec-01cv2bchctscwf9ze7hf19hk4f', key='token')
    TESTPALM_OAUTH_TOKEN = secret(uuid='sec-01ef4ajzbem4ftjzp6yttmvqjp', key='oauth_token')

class Certificates(SecretsContainer):
    _XMLRPC_CLIENT_SSL_CERTIFICATE_SECRET = 'sec-01ct0mabpcfzmgspr8s9g7vt17'
    XMLRPC_CLIENT_CERT = secret(uuid=_XMLRPC_CLIENT_SSL_CERTIFICATE_SECRET, key='cert.pem',
                                path=utils.project_file('btestlib/resources/xmlrpc_cert.pem'))
    XMLRPC_CLIENT_KEY = secret(uuid=_XMLRPC_CLIENT_SSL_CERTIFICATE_SECRET, key='cert.key',
                               path=utils.project_file('btestlib/resources/xmlrpc_cert.key'))
    XMLRPC_CLIENT_KEY_PWD = secret(uuid=_XMLRPC_CLIENT_SSL_CERTIFICATE_SECRET, key='password')


class Robots(SecretsContainer):
    _BALANCE_TESTS_ROBOTS_SECRET = 'sec-01ct0ns9w94qgxr67xjzjxz488'
    TESTUSER_BALANCE1_PWD = secret(uuid=_BALANCE_TESTS_ROBOTS_SECRET, key='testuser-balance1')
    TESTUSER_BALANCE2_PWD = secret(uuid=_BALANCE_TESTS_ROBOTS_SECRET, key='testuser-balance2')
    TESTUSER_BALANCE3_PWD = secret(uuid=_BALANCE_TESTS_ROBOTS_SECRET, key='testuser-balance3')
    TESTUSER_BALANCE4_PWD = secret(uuid=_BALANCE_TESTS_ROBOTS_SECRET, key='testuser-balance4')


class UsersPwd(SecretsContainer):
    _CLIENTUID_PWD_SECRET = 'sec-01ct5hm65f6tz30kxygvwa9n2n'
    CLIENTUID_PWD = secret(uuid=_CLIENTUID_PWD_SECRET, key='password')
    CLIENTUID_PAY_PWD = secret(uuid=_CLIENTUID_PWD_SECRET, key='pay_password')
    YANDEX_TEAM_REG_CQR5_PWD = secret(uuid='sec-01g4hn6mmcfz0btap2eb39g47b', key='password')
    TRUSTTESTUSR_PWD = secret(uuid='sec-01ctng0r1qpp1g93nam0ys97qz', key='trusttestusr2')
    YB_ADM_PWD_SECRET = secret(uuid='sec-01e083hg5gn298b0t4y8rjvjf7', key='password')
    YB_ADM_NEW_PWD_SECRET = secret(uuid='sec-01fzwhn9qkpmjreqm9f9n7txdc', key='password')
    CLT_MANAGER_PWD = secret(uuid='sec-01ct5jak8mcydt217jcjxenj38', key='password')
    HERMIONE_CI_PWD = secret(uuid='sec-01fgs0taw2qneh0ad96hwrndj7', key='password')
    HERMIONE_CI_PWD_NEW = secret(uuid='sec-01fx2ddpmdp6mxgn3zvsez0wfa', key='password')


class DbPwd(SecretsContainer):
    _BALANCE_SCHEMAS_PWD = 'sec-01djn78bkny3z2kz04scmrkr91'
    BALANCE_BO_PWD = secret(uuid=_BALANCE_SCHEMAS_PWD, key='default_value')


class Infr(SecretsContainer):
    _AUTOTESTS_S3_SECRETS = 'sec-01cv2keh4bpj2bywgczdp99czw'
    S3_ACCESS_KEY_ID = secret(uuid=_AUTOTESTS_S3_SECRETS, key='access_key_id')
    S3_ACCESS_KEY_SECRET = secret(uuid=_AUTOTESTS_S3_SECRETS, key='access_key_secret')


class OebsInfr(SecretsContainer):
    OEBS_DB_BALANCE_QA_PWD = secret(uuid='sec-01ct61eksgdskcy5b23y7wxkfz', key='password')


class TrustInfr(SecretsContainer):
    _PCI_DSS_DEV_AUTH = 'sec-01ctnkgc4jcbp1axpj3htbd0vq'
    PCI_DSS_DEV_CONFPATCH = secret(uuid=_PCI_DSS_DEV_AUTH, key='confpatch')
    PCI_DSS_DEV_KEYKEEPER = secret(uuid=_PCI_DSS_DEV_AUTH, key='keykeeper')


class TrustOauthTokens(SecretsContainer):
    _TRUST_OAUTH_SECRET = 'sec-01ctebgavbqj3ynast1k5wjc6d'

    TRUST_OAUTH_TOKENS = secret(uuid=_TRUST_OAUTH_SECRET)


class TvmSecrets(SecretsContainer):
    # Основное приложение тестов
    BALANCE_TESTS_SECRET = secret(uuid='sec-01dnq0bgqg0ybxdng3t4zpw0xz', key='default_value')
    YB_MEDIUM_SECRET = secret(uuid='sec-01ctgrp00p6ahvmvtvbbkw07z2', key='secret')

    BCL_TEST_SECRET = secret(uuid='sec-01d9n2nk0qmf5h2hqe0ggxjnby', key='default_value')
    CASHMASHINES_SECRET = secret(uuid='sec-01ctgjzj2wr7qnc53hqf0nwd4y', key='secret')


class Telegram(SecretsContainer):
    BLAME_BOT_TOKEN = secret(uuid='sec-01ctgqatkc58vdyd1nryq912x8', key='bot_token')
    TESTBALANCE_BOT_TOKEN = secret(uuid='sec-01ctjjn2xm9r5h3e5jrnq3a5a2', key='token')


class DIToken(SecretsContainer):
    DI_TOKEN = secret(uuid='sec-01cy25151ya88r1zxya7vv6y8w', key='token')


class MuzzleSecret(SecretsContainer):
    HMAC_SECRET = secret(uuid='sec-01ekfndr2nfygexyt3jx5395h7', key='default_value')


_SECRETS = {}


def get_secret(uuid, key=None, path=None):
    """
    Обеспечивает беспечивает, чтобы ходить за каждым секретом только один раз и добавляет возможность получать из
    секретницы файлы и сохранять их по заданному пути

    :param uuid: uid секрета или версии секрета
    :param key: ключ поля в секрете. Если не задан вернется словать со всеми полями секрета
    :param path: путь к файлу куда положить секрет. Если задан - файл с содержимым секрета будет создан и возвращен путь
    :param isbase64: если содержимое секрета кодировано в base64 - декодируем (для файлов)
    """
    if not uuid in _SECRETS:
        _SECRETS[uuid] = get_from_vault(uuid)

    secret_value = _SECRETS[uuid]['value'][key] if key else _SECRETS[uuid]['value']
    secret_version = _SECRETS[uuid]['version']
    if path:
        version_path = create_file_if_needed(path=path, secret_value=secret_value, secret_version=secret_version)
        return version_path
    else:
        return secret_value


def create_file_if_needed(path, secret_value, secret_version):
    directory_path = os.path.dirname(path)
    filename = os.path.split(path)[-1]

    version_directory_path = os.path.join(directory_path, secret_version)
    version_path = os.path.join(version_directory_path, filename)

    reporter.log('Check if secret file exists: {}'.format(version_path))
    if not os.path.exists(version_path):
        if not os.path.exists(version_directory_path):
            reporter.log('Creating directory: {}'.format(version_directory_path))
            os.makedirs(version_directory_path)
        reporter.log('Creating file: {}'.format(version_path))
        with open(version_path, 'w+') as f:
            f.write(secret_value)

    return version_path


# igogor: не используем cached т.к. модифицируем секрет
@retry(wait=wait_fixed(1), stop=stop_after_attempt(10), reraise=True)
def get_from_vault(secret_uuid_or_version):
    reporter.log('Getting secret: ' + secret_uuid_or_version)
    yav = get_vault_client()

    reporter.log('Trying to get secret from Vault: {}'.format(secret_uuid_or_version))
    secret_ = yav.get_version(secret_uuid_or_version, packed_value=False)
    reporter.log('Got secret successfully: {}'.format(secret_uuid_or_version))

    packed_value = dict()
    for row in secret_['value']:
        processed_value = row['value']
        # igogor: если класть в файл в секрет через yav то он будет автоматически закодирован в base64
        if row.get('encoding', '') == 'base64':
            try:
                processed_value = base64.b64decode(processed_value)
            except TypeError as e:
                # igogor: значение encoding не всегда поддерживается в актуальном состоянии
                # если попытка декодировать не удалась - считаем, что в поле лежит не base64 и возвращаем само значение
                pass
        packed_value[row['key']] = processed_value

    secret_['unpacked_value'] = secret_['value']
    secret_['value'] = packed_value

    return secret_


@utils.cached
def get_vault_client():
    reporter.log('Getting vault client')

    # igogor: отдельная функция т.к. хочу лог перед ретраями и не хочу разбираться как провзаимодействуют декораторы.
    # При валидном использовании wraps должны корректно теоретически.
    @retry(wait=wait_fixed(1), stop=stop_after_attempt(10), reraise=True)
    def with_retry():
        VAULT_LOGIN = getattr(config, 'VAULT_LOGIN', None)
        VAULT_OAUTH_TOKEN = getattr(config, 'VAULT_OAUTH_TOKEN', None)

        if VAULT_OAUTH_TOKEN is not None and VAULT_LOGIN is not None:
            return vault_client.instances.Production(rsa_login=VAULT_LOGIN, authorization=VAULT_OAUTH_TOKEN)

        try:
            reporter.log('Trying to read oauth token from file: ' + OAUTH_PATH)
            with open(OAUTH_PATH, 'r') as f:
                oauth_token = f.read().strip()
        except IOError:
            oauth_token = config.VAULT_OAUTH_TOKEN

        if oauth_token:
            reporter.log('Will use oauth token authorisation to go to Vault, with login: {}'.format(RSA_LOGIN))
            return vault_client.instances.Production(rsa_login=RSA_LOGIN,
                                                     authorization=oauth_token)

        # igogor: если не получилось считать oauth токен из файла (мы на локальной машине) -
        # подключаемся с авторизацией через ssh ключ
        # Чтобы это работало ключ должен быть добавлен на стафф и добавлен в ssh агент на локальной машине.
        # Команда на юниксе:   ssh-add
        elif config.VAULT_LOGIN:
            reporter.log('Will use ssh authorisation to go to Vault, with login: {}'.format(config.VAULT_LOGIN))
            # Если в переменной окружения 'test.vault.login' лежит этот логин используем его (высший приоритет)
            # иначе если в btestlib.local_config.VAULT_LOGIN лежит значение то используем его (второй приоритет)
            return vault_client.instances.Production(rsa_login=config.VAULT_LOGIN)
        else:
            reporter.log('Will use ssh authorisation to go to Vault, with ssh agent current user')
            # если логин не задан - логинимся с дефолтным юзером ssh агента
            return vault_client.instances.Production()

    return with_retry()


def get_all_secrets_params():
    secret_containers = inspect.getmembers(sys.modules[__name__],
                                           lambda obj: inspect.isclass(obj) and SecretsContainer in obj.mro())
    return utils.flatten([inspect.getmembers(container, lambda field: isinstance(field, Secret))
                          for name, container in secret_containers])


def delete_all_secret_files():
    for name, secret_params in get_all_secrets_params():
        if secret_params.path is not None and os.path.isfile(secret_params.path):
            os.remove(secret_params.path)


def create_all_secret_files():
    for name, secret_params in get_all_secrets_params():
        if secret_params.path is not None:
            # igogor: в методе создаются нужные файлы, если их нет
            get_secret(*secret_params)


def create_secret(name, comment, key_values, tags=None, abc_ids=None, staff_ids=None, uids=None):
    tags = ['balance-tests', 'teamcity'] + (tags or [])
    abc_ids = abc_ids or []
    billing_staff_id = 88468
    trust_staff_id = 88467
    staff_ids = [billing_staff_id, trust_staff_id] + (staff_ids or [])
    testuser_balance1_uid = '1120000000008037'
    uids = uids or []

    vault = get_vault_client()

    secret_uuid = vault.create_secret(name=name, comment=comment, tags=tags)
    version_uuid = vault.create_secret_version(secret_uuid=secret_uuid, value=key_values)
    for abc_id in abc_ids:
        vault.add_user_role_to_secret(secret_uuid=secret_uuid, role='OWNER', abc_id=abc_id)
    for staff_id in staff_ids:
        vault.add_user_role_to_secret(secret_uuid=secret_uuid, role='OWNER', staff_id=staff_id)

    vault.add_user_role_to_secret(secret_uuid=secret_uuid, role='READER', uid=testuser_balance1_uid)
    for uid in uids:
        vault.add_user_role_to_secret(secret_uuid=secret_uuid, role='OWNER', uid=uid)
    print secret_uuid


if __name__ == '__main__':
    # create_secret(name='partner-test-tvm',
    #               comment=u'TVM secret приложения добавленного в ручки партнерки как тестовое',
    #               key_values=dict(secret=''),
    #               tags=['tvm'])
    for name, secret_params in get_all_secrets_params():
        billing_staff_id = 88468
        trust_staff_id = 88467
        # res = get_vault_client().add_user_role_to_secret(secret_uuid=secret_params.uuid,
        #                                                       role='OWNER', staff_id=billing_staff_id)
        # res = get_vault_client().add_user_role_to_secret(secret_uuid=secret_params.uuid,
        #                                                       role='OWNER', staff_id=trust_staff_id)

        # testuser_balance1_uid = '1120000000008037'
        # res = get_vault_client().delete_user_role_from_secret(secret_uuid=secret_params.uuid,
        #                                                       role='OWNER', uid=testuser_balance1_uid)
        # res = get_vault_client().add_user_role_to_secret(secret_uuid=secret_params.uuid,
        #                                                       role='READER', uid=testuser_balance1_uid)
        # print secret_params.uuid, res
