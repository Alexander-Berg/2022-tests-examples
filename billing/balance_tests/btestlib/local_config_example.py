# coding: utf-8
__author__ = 'a-vasin'

ENVIRONMENT_SETTINGS = {
    # Balance

    # Test
    'balance': 'tm',
    # 'balance': 'ts',
    # 'balance': 'pt',
    # 'balance': 'dev',
    # 'balance': 'load',
    # 'balance': 'load1',
    # 'balance': 'load2',

    # Branches
    # 'balance': 'BALANCE-26377'

    # Localhost
    # 'balance': '{"medium_url": "http://localhost:8002/xmlrpc", ' +
    #            '"test_balance_url": "http://localhost:30702/xmlrpc", ' +
    #            '"balance_ci": "http://localhost:8002", ' +
    #            '"balance_ai": "http://localhost:8002"}',

    # docker на маке не могёт в IPv6, но мы поднимем туннель на хосте докера (ноуте):
    # [ ssh -L 9999:localhost:443 test-xmlrpc-{branchname}.greed-branch.paysys.yandex.ru ],
    # [ ssh -L 9998:localhost:443 xmlrpc-{branchname}.greed-branch.paysys.yandex.ru ].
    # В контейнере хост докера доступен под алиасом: [ host.docker.internal ].
    # теперь коннект к 443 порту нужных cname доступен через:
    # [ https://host.docker.internal:9999 ]
    # [ https://host.docker.internal:9998 ]
    # В транспорте xmlrpclib мы скипнем проверку сертификата и подсунем cname в заголовок Host

    # 'balance.branch.docker_tunnel_params':
    #     '''{"branch": "red-payments",
    #         "tunnel_params": {
    #             "medium": {"port": 9998, "cname": "xmlrpc-{branch}.greed-branch.paysys.yandex.ru"},
    #             "test-xmlrpc": {"port": 9999, "cname": "test-xmlrpc-{branch}.greed-branch.paysys.yandex.ru"}
    #         }
    #     }''',

    # Predefined
    # 'balance': 'APIKEYS_DEV1F',

    # -----------------------
    # Apikeys
    'apikeys': 'APIKEYS_PAYSYS',
    # 'apikeys': 'APIKEYS_DEV_PAYSYS',

    # Branch
    # 'apikeys': '51',

    # -----------------------
    # Simpleapi
    'simpleapi': 'TEST_BS',
    # 'simpleapi': 'TEST_BO',
    # 'simpleapi': 'TEST_NG',
    # 'simpleapi': 'DEV_BS',
    # 'simpleapi': 'DEV_BO',
    # 'simpleapi': 'DEV_NG',

    # -----------------------
    # Balalayka
    'balalayka': 'balalayka-test.paysys',
    # 'balalayka': 'balalayka-dev.paysys',

    # -----------------------
    # Whitespirit
    'whitespirit': 'dev',
    # 'whitespirit': 'test',
    # 'proxy_to_balancer': '1',

    # -----------------------
    # Darkspirit
    'darkspirit': 'tm',
    # 'darkspirit': 'ts'
}

# варианты - имена полей енума btestlib.reporter.Level
REPORTER_LEVEL = 'MANUAL_ONLY'  # рекомендую попробовать 'AUTO_ONE_LINE'
# чтобы выводить полностью - просто закомментировать предыдущую строку или присвоить значение 'ALL'


# XMLRPC_COVERAGE = 'testcontext'

# VAULT_LOGIN = 'igogor'
# igogor: ссылка на получение токена есть в доке https://vault-api.passport.yandex.net/docs/
# VAULT_OAUTH_TOKEN = u'Ни в коем случае не скомитьте свой токен!!! Класть только в local_config.py'

# Ссылка на получение TUS токена
# https://oauth.yandex-team.ru/authorize?response_type=token&client_id=9052de6e4cf142039a7ee44ac03e4614
# TUS_OAUTH_TOKEN = u'Ни в коем случае не скомитьте свой токен!!! Класть только в local_config.py'