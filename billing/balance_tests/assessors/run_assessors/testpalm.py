# -*- coding: utf-8 -*-
import json
import requests
from btestlib import reporter

from btestlib import secrets

REGRESSION_SUITES = {
                    'Выставление счетов с разными пейсисами (часть 2)': '5dd50b848d20300e237b7225',
                    'Выставление счетов с разными пейсисами (часть 1)': '5dd5090527a9909ea5a393a2',
                    'КИ страница поиска заказов и страница заказа': '5d937662b5fb6c04c2eac7a8',
                    'Плательщики Физические лица России': '5cdabc90ee4f433e5130c689',
                    'Плательщики Юридические лица России': '5d1e5471b5fb6cf039edd4bc',
                    'Проверка реквизитов на странице счета': '5de4ec869fcbfa595041d152',
                    'Разное: часть 1': '5e9f0a15396e4d94bb4bea98',
                    'Счета Овердрафтный счет': '5d499155af57678ccd16d565',
                    'Счета Предоплатный счёт': '5d405c73af57677d13d410a9',
                    'Счета Промокодный счет': '5d93756a3d42cb93f2a657c6',
                    'Страница счета': '5fb3bedf6f770c00cf21f9c5',
                    'Плательщики kzu, sw_yt, byu: минимальные проверки': '60264db1e48c560011b62db3',
                    'Печатные формы счетов': '60141cf85695b200738a3266',
                    'КИ страница поиска счетов': '5d937624d284f9f30d9d94fa',
                    'КИ кредитная история': '60508c01ca071a001190e21b',
                    'КИ акт сверки онлайн': '604120edeaf99a00cfa6760c',
                    'КИ акты': '6057536eaef393001102b746',
                    'КИ старый ЛС (кредиты и квитанции)': '6058831cdbc8fc0049b6a861',
                    'КИ под бухгалтерским логином': '6054ed91aef3930011fcaa36',
                    'КИ История зачислений': '605253fe6ebf640011d86216'
}

NOT_DIVIDED_RUNS = ['5d499155af57678ccd16d565',     # 'Счета Овердрафтный счет'
                    '5d405c73af57677d13d410a9',     # 'Счета Предоплатный счёт'
                    '5e9f0a15396e4d94bb4bea98',     # 'Разное: часть 1'
                    ]

ENVIRONMENTS = ['Chrome', 'Firefox']

TESTPALM_URL = 'https://testpalm.yandex-team.ru/'
TESTPALM_API_URL = 'https://testpalm-api.yandex-team.ru/'

TESTPALM_PROJECT = 'balanceassessors'
SUITES = REGRESSION_SUITES
VERSION_POSTFIX = '1'
VERSION_NAME = 'persons_debug'

TOKEN = secrets.get_secret(*secrets.Tokens.TESTPALM_OAUTH_TOKEN)


def create_testpalm_version(suites, version_postfix_palm, version_name_palm, testpalm_project=TESTPALM_PROJECT):
    headers_testpalm = {'Content-Type': 'application/json',
                        'Authorization': 'OAuth ' + TOKEN}
    data = {'suites': list(suites.values()),
            'id': str(version_name_palm + version_postfix_palm),
            'title': str(version_name_palm + version_postfix_palm)
            }
    rawBody = json.dumps(data, ensure_ascii=False, separators=(",", ": "))
    url = TESTPALM_API_URL + 'version/' + testpalm_project
    with reporter.step(u'Создаем версию {}'.format(str(version_name_palm + version_postfix_palm))):
        resp = requests.post(url, data=rawBody, headers=headers_testpalm, verify=False)
        reporter.log(resp.text)
        if resp.status_code != 200:
            raise Exception("Something went wrong: " + resp.text)
    return resp.json()['id']


def create_run(version, suites, environments, testpalm_project=TESTPALM_PROJECT):
    headers_testpalm = {'Content-Type': 'application/json',
                        'Authorization': 'OAuth ' + TOKEN}
    for suite in suites:
        for environment in environments:
            tags = "notDividedTestrun" if suites[suite] in NOT_DIVIDED_RUNS else ""
            data = {"title": "{}_{}".format(suite, environment),
                    "version": version,
                    "currentEnvironment": {"title": environment,
                                           "description": environment,
                                            "default": True},
                    "testSuite": {"id": suites[suite],
                                  "title": "{}_{}".format(suite, environment),
                                  "ignoreSuiteOrder": False},
                    "tags": tags,
                    }
            rawBody = json.dumps(data, ensure_ascii=False, separators=(",", ": "))
            url = TESTPALM_API_URL + 'testrun/' + testpalm_project + '/create'
            with reporter.step(u'Создаем ран "{}" с окружением {}'.format(suite.decode('utf-8'), environment)):
                resp = requests.post(url, data=rawBody, headers=headers_testpalm, verify=False)
                if resp.status_code != 200:
                    raise Exception("Something went wrong: " + resp.text)
                else:
                    reporter.log(u'Ссылка на ран: {}{}/testrun/{}'.
                                 format(TESTPALM_URL, TESTPALM_PROJECT, resp.json()[0]['id']))
    reporter.log(u'Ссылка на версию: {}{}/version/{}'.format(TESTPALM_URL, TESTPALM_PROJECT, version))


if __name__ == '__main__':
    # create_testpalm_version(SUITES, VERSION_POSTFIX, VERSION_NAME)
    create_run(str(VERSION_NAME + VERSION_POSTFIX), SUITES, ENVIRONMENTS)


