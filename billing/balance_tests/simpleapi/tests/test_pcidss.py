# coding=utf-8

import time

import pytest
from hamcrest import matches_regexp, not_none, is_, equal_to
from xmltodict import parse, unparse

import btestlib.reporter as reporter
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.data import defaults
from simpleapi.data import features
from simpleapi.steps.check_steps import check_that
from simpleapi.steps.pcidss_steps import KeyKeeper, KeyApi, ConfPatch, Scheduler
from simpleapi.steps.pcidss_steps import set_state, get_state

''' https://wiki.yandex-team.ru/balance/simple/pcidss/ '''

__author__ = 'slppls'

log = logger.get_logger()

KEK_PART1 = {
    'id': 'KEK_VER9000_PART1',
    'data': 'TESTDATAFORGODOFTESTTESTDATAFORGODOFTEST',
}

KEK_PART1_OTHER = {
    'id': 'KEK_VER9001_PART1',
    'data': 'SOMEOTHERKEKPART1FORTESTPURPOSES',
}

KEK_PART2 = {
    'id': 'KEK_VER9000_PART2',
    'data': 'TESTDATAFORGOODTESTTESTDATAFORGOODTEST',
}

KEK_PART2_OTHER = {
    'id': 'KEK_VER9001_PART2',
    'data': 'SOMEOTHERKEKPART2FORTESTPURPOSES',
}

# kek для прохождения валидации на стороне keykeeper
KEK_64SYMB_PART1 = {
    'id': 'KEK_VER9002_PART1',
    'data': 'THISKEKHAVE64SYMBOLSLENGHTANDTHISSVERYIMPOSSIBLEFORONEFUNNYLOGIC',
}


@pytest.fixture(scope='class')
def save_and_load_kek_part1(request):
    keks_storage = 'test_pcidss/kek_storage_part1'
    request.config.cache.set(keks_storage, unparse(KeyKeeper.get()))

    def fin():
        keks_tmp = request.config.cache.get(keks_storage, None)
        keks = KeyKeeper.parse_keks(parse(keks_tmp))
        for kek in keks:
            KeyKeeper.set(kek)

    request.addfinalizer(fin)


servants = [{'name': 'python-servant', 'port': defaults.PCIDSS.Port.keykeeper_python},
            {'name': 'c-servant', 'port': defaults.PCIDSS.Port.keykeeper_c}]


def ids_servant_keykeeper(val):
    return val['name']


def ids_letter(val):
    return 'host=pci-dev1{}'.format(val)


@reporter.feature(features.PCIDSS.KeyKeeper)
@pytest.mark.usefixtures('save_and_load_kek_part1')
class TestKeyKeeper(object):
    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_get(self, servant):
        resp = KeyKeeper.get(port=servant['port'])

        keks = KeyKeeper.parse_keks(resp)
        for kek in keks:
            check_that(kek['id'], matches_regexp(r'KEK_VER\d+_PART1'),
                       step=u'Проверяем, что id KEKа типичного формата',
                       error=u'id KEKа не подходит под типичный формат!')

    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_set(self, servant):
        set_resp = KeyKeeper.set(KEK_PART1, port=servant['port'])
        KeyKeeper.get_status(set_resp)
        for letter in defaults.PCIDSS.servant_pool:
            get_resp = KeyKeeper.get(letter=letter, port=servant['port'])
            check_that(KeyKeeper.is_kek_in_response(get_resp, KEK_PART1), not_none(),
                       step=u'Проверяем, что KEK {} присутствует в ответе'.format(KEK_PART1),
                       error=u'KEK {} не приутствует в ответе!'.format(KEK_PART1))
        KeyKeeper.unset(KEK_PART1, port=servant['port'])

    # Если кек уже есть, повторный вызов set не должен вызывать ошибки
    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_set_already_existed(self, servant):
        KeyKeeper.set(KEK_PART1, port=servant['port'])
        set_resp = KeyKeeper.set(KEK_PART1, port=servant['port'])
        KeyKeeper.get_status(set_resp)

        for letter in defaults.PCIDSS.servant_pool:
            get_resp = KeyKeeper.get(letter=letter, port=servant['port'])
            check_that(KeyKeeper.is_kek_in_response(get_resp, KEK_PART1), not_none(),
                       step=u'Проверяем, что KEK {} присутствует в ответе'.format(KEK_PART1),
                       error=u'KEK {} отсутствует в ответе!'.format(KEK_PART1))
        KeyKeeper.unset(KEK_PART1, port=servant['port'])

    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_unset(self, servant):
        KeyKeeper.set(KEK_PART1, port=servant['port'])
        KeyKeeper.unset(KEK_PART1, port=servant['port'])
        for letter in defaults.PCIDSS.servant_pool:
            get_resp = KeyKeeper.get(letter=letter, port=servant['port'])
            check_that(KeyKeeper.is_kek_in_response(get_resp, KEK_PART1), not_none(),
                       step=u'Проверяем, что KEK {} присутствует в ответе'.format(KEK_PART1),
                       error=u'KEK {} отсутствует в ответе!'.format(KEK_PART1))

    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_cleanup(self, servant):
        KeyKeeper.set(KEK_PART1, port=servant['port'])
        KeyKeeper.set(KEK_PART1_OTHER, port=servant['port'])
        KeyKeeper.cleanup(KEK_PART1, port=servant['port'])
        for letter in defaults.PCIDSS.servant_pool:
            get_resp = KeyKeeper.get(letter=letter, port=servant['port'])
            check_that(KeyKeeper.is_kek_in_response(get_resp, KEK_PART1), not_none(),
                       step=u'Проверяем, что KEK {} присутствует в ответе'.format(KEK_PART1),
                       error=u'KEK {} отсутствует в ответе!'.format(KEK_PART1))
            check_that(KeyKeeper.is_kek_in_response(get_resp, KEK_PART1_OTHER), not_none(),
                       step=u'Проверяем, что KEK {} присутствует в ответе'.format(KEK_PART1_OTHER),
                       error=u'KEK {} отсутствует в ответе!'.format(KEK_PART1_OTHER))
        KeyKeeper.unset(KEK_PART1, port=servant['port'])

    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_get_noauth(self, servant):
        with check_mode(CheckMode.IGNORED):
            resp = KeyKeeper.get(port=servant['port'], auth=None)
            check_that(KeyKeeper.get_status_code(resp), is_(equal_to('missing_header')),
                       step=u'Проверяем корректность ошибки',
                       error=u'Ошибка некорректна!')

    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_set_noauth(self, servant):
        with check_mode(CheckMode.IGNORED):
            resp = KeyKeeper.set(KEK_PART1, port=servant['port'], auth=None)
            check_that(KeyKeeper.get_status_code(resp), is_(equal_to('missing_header')),
                       step=u'Проверяем корректность ошибки',
                       error=u'Ошибка некорректна!')

    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_unset_noauth(self, servant):
        with check_mode(CheckMode.IGNORED):
            resp = KeyKeeper.unset(KEK_PART1, port=servant['port'], auth=None)

        check_that(KeyKeeper.get_status_code(resp), is_(equal_to('missing_header')),
                   step=u'Проверяем корректность ошибки',
                   error=u'Ошибка некорректна!')

    @pytest.mark.parametrize('servant', servants, ids=ids_servant_keykeeper)
    def test_cleanup_noauth(self, servant):
        with check_mode(CheckMode.IGNORED):
            resp = KeyKeeper.cleanup(KEK_PART1, port=servant['port'], auth=None)

        check_that(KeyKeeper.get_status_code(resp), is_(equal_to('missing_header')),
                   step=u'Проверяем корректность ошибки',
                   error=u'Ошибка некорректна!')

    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_recover_component_wo_comp(self, letter):
        kek_version = KeyApi.get_version_from_status(KeyApi.status(letter=letter))
        KeyKeeper.set(KEK_PART1, letter=letter)
        KeyKeeper.cleanup(KEK_PART1, letter=letter)
        KeyKeeper.unset(KEK_PART1, letter=letter)

        KeyApi.recover_component(KEK_64SYMB_PART1['data'], kek_version, letter=letter)

    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_recover_component_wrong_kek(self, letter):
        wrong_kek_version = 9009
        KeyKeeper.set(KEK_PART1, letter=letter)
        KeyKeeper.cleanup(KEK_PART1, letter=letter)
        KeyKeeper.unset(KEK_PART1, letter=letter)

        with check_mode(CheckMode.IGNORED):
            resp = KeyApi.recover_component(KEK_64SYMB_PART1['data'], wrong_kek_version, letter)

        check_that(KeyKeeper.get_status(resp), is_(equal_to('failure')),
                   step=u'Проверяем корректность статуса',
                   error=u'Статус некорректен!')
        check_that(KeyKeeper.get_status_desc(resp), is_(equal_to('no such version in database')),
                   step=u'Проверяем корректность описания ошибки',
                   error=u'Описание ошибки некорректно!')

    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_recover_component_already_wrong_comp(self, letter):
        kek_version = KeyApi.get_version_from_status(KeyApi.status(letter=letter))
        KeyKeeper.set(KEK_PART1, letter=letter)
        KeyKeeper.cleanup(KEK_PART1, letter=letter)

        KeyApi.recover_component(KEK_64SYMB_PART1['data'], kek_version, letter=letter)

    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_recover_component_already_good_comp(self, letter):
        kek_version = KeyApi.get_version_from_status(KeyApi.status(letter=letter))

        with check_mode(CheckMode.IGNORED):
            resp = KeyApi.recover_component(KEK_64SYMB_PART1['data'], kek_version, letter=letter)
        check_that(KeyKeeper.get_status(resp), is_(equal_to('failure')),
                   step=u'Проверяем корректность статуса',
                   error=u'Статус некорректен!')
        check_that(KeyKeeper.get_status_desc(resp), is_(equal_to('We already have valid component, get lost')),
                   step=u'Проверяем корректность описания ошибки',
                   error=u'Описание ошибки некорректно!')


# Серванты, которые должны перезапускаться после выполнения любого из методов ConfPatch
SERVANTS = [
    {
        'name': 'card_pyproxy_tokenizer',
        'path': 'cp_tokenizer/card_pyproxy_tokenizer.log',
        'restart_regexp': '.+Listening at: http://127.0.0.1:32192+',
    },
    {
        'name': 'card_proxy_tokenizer',
        'path': 'cp_tokenizer/card_proxy_tokenizer.log',
        'restart_regexp': '.+listen at: http://127.0.0.1:19119/+',
    },
    {
        'name': 'card_pyproxy_keyapi',
        'path': 'cp_keyapi/card_pyproxy_keyapi.log',
        'restart_regexp': '.+Listening at: http://127.0.0.1:14300+',
    }
]


@pytest.fixture(scope='class')
def save_and_load_kek_part2(request):
    keks_storage = 'test_pcidss/kek_storage_part2'
    request.config.cache.set(keks_storage, ConfPatch.get_from_config())

    def fin():
        for letter in defaults.PCIDSS.servant_pool:
            keks = request.config.cache.get(keks_storage, None)
            for kek in keks:
                ConfPatch.set(kek, letter=letter)

    request.addfinalizer(fin)


# Если так получилось, что тестовый кек уже установлен, удалим его вначале теста
@pytest.fixture(scope='function')
def delete_test_kek_if_exists():
    for letter in defaults.PCIDSS.servant_pool:
        if ConfPatch.is_kek_in_config(KEK_PART2, letter=letter):
            ConfPatch.unset(KEK_PART2, letter=letter)


@reporter.feature(features.PCIDSS.ConfPatch)
@pytest.mark.usefixtures('save_and_load_kek_part2')
class TestConfPatch(object):
    @pytest.mark.usefixtures('delete_test_kek_if_exists')
    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_set(self, letter):
        ConfPatch.set(KEK_PART2, letter=letter)
        check_that(ConfPatch.is_kek_in_config(KEK_PART2, letter=letter), not_none(),
                   step=u'Проверяем наличие KEKа в key_settings.cfg.xml',
                   error=u'KEK отсутствует в key_settings.cfg.xml!')
        for servant in SERVANTS:
            check_that(ConfPatch.is_servant_was_restarted(servant, letter=letter), not_none(),
                       step=u'Проверяем, что сервант {} рестартанулся'.format(servant['name']),
                       error=u'Сервант {} не рестартанулся!'.format(servant['name']))
        ConfPatch.unset(KEK_PART2, letter=letter)

    # Если KEK уже есть, повторный вызов set не должен вызывать ошибки
    @pytest.mark.usefixtures('delete_test_kek_if_exists')
    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_set_already_existed(self, letter):
        ConfPatch.set(KEK_PART2, letter=letter)
        ConfPatch.set(KEK_PART2, letter=letter)
        check_that(ConfPatch.is_kek_in_config(KEK_PART2, letter=letter), not_none(),
                   step=u'Проверяем наличие KEKа в key_settings.cfg.xml',
                   error=u'KEK отсутствует в key_settings.cfg.xml!')
        for servant in SERVANTS:
            check_that(ConfPatch.is_servant_was_restarted(servant, letter=letter), not_none(),
                       step=u'Проверяем, что сервант {} рестартанулся'.format(servant['name']),
                       error=u'Сервант {} не рестартанулся!'.format(servant['name']))
        ConfPatch.unset(KEK_PART2, letter=letter)

    @pytest.mark.usefixtures('delete_test_kek_if_exists')
    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_unset(self, letter):
        ConfPatch.set(KEK_PART2, letter=letter)
        ConfPatch.unset(KEK_PART2, letter=letter)
        check_that(ConfPatch.is_kek_in_config(KEK_PART2, letter=letter), not_none(),
                   step=u'Проверяем наличие KEKа в key_settings.cfg.xml',
                   error=u'KEK отсутствует в key_settings.cfg.xml!')
        for servant in SERVANTS:
            check_that(ConfPatch.is_servant_was_restarted(servant, letter=letter), not_none(),
                       step=u'Проверяем, что сервант {} рестартанулся'.format(servant['name']),
                       error=u'Сервант {} не рестартанулся!'.format(servant['name']))

    @pytest.mark.usefixtures('delete_test_kek_if_exists')
    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_cleanup(self, letter):
        # записываем два KEKа
        ConfPatch.set(KEK_PART2, letter=letter)
        ConfPatch.set(KEK_PART2_OTHER, letter=letter)
        # удаляем все кроме KEK_PART2
        ConfPatch.cleanup(KEK_PART2, letter=letter)
        # проверяем что KEK_PART2 остался, а KEK_PART2_OTHER - удалился
        check_that(ConfPatch.is_kek_in_config(KEK_PART2, letter=letter), not_none(),
                   step=u'Проверяем наличие первого KEKа в key_settings.cfg.xml',
                   error=u'Первый KEK отсутствует в key_settings.cfg.xml!')
        check_that(ConfPatch.is_kek_in_config(KEK_PART2_OTHER, letter=letter), not_none(),
                   step=u'Проверяем наличие второго KEKа в key_settings.cfg.xml',
                   error=u'Второй KEK отсутствует в key_settings.cfg.xml!')
        for servant in SERVANTS:
            check_that(ConfPatch.is_servant_was_restarted(servant, letter=letter), not_none(),
                       step=u'Проверяем, что сервант {} рестартанулся'.format(servant['name']),
                       error=u'Сервант {} не рестартанулся!'.format(servant['name']))

        ConfPatch.unset(KEK_PART2, letter=letter)

    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_set_noauth(self, letter):
        with check_mode(CheckMode.IGNORED):
            resp = ConfPatch.set(KEK_PART2, letter=letter, auth=None)
        check_that(ConfPatch.get_status_code(resp), is_(equal_to('missing_header')),
                   step=u'Проверяем корректность ошибки',
                   error=u'Ошибка некорректна!')

    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_unset_noauth(self, letter):
        with check_mode(CheckMode.IGNORED):
            resp = ConfPatch.unset(KEK_PART2, letter=letter, auth=None)
        check_that(ConfPatch.get_status_code(resp), is_(equal_to('missing_header')),
                   step=u'Проверяем корректность ошибки',
                   error=u'Ошибка некорректна!')

    @pytest.mark.parametrize('letter', defaults.PCIDSS.servant_pool, ids=ids_letter)
    def test_cleanup_noauth(self, letter):
        with check_mode(CheckMode.IGNORED):
            resp = ConfPatch.cleanup(KEK_PART2, letter=letter, auth=None)
        check_that(ConfPatch.get_status_code(resp), is_(equal_to('missing_header')),
                   step=u'Проверяем корректность ошибки',
                   error=u'Ошибка некорректна!')


@pytest.fixture(scope='class')
def check_state(request):
    if get_state() != defaults.PCIDSS.State.normal:
        pytest.skip('Not NORMAL status.')

    def fin():
        if get_state() == 'TEST1' or get_state() == 'TEST2':
            set_state(defaults.PCIDSS.State.normal)

    request.addfinalizer(fin)


@reporter.feature(features.PCIDSS.Scheduler)
@pytest.mark.usefixtures('check_state')
class TestSheduler(object):
    def test_task_with_ping(self):
        set_state('TEST1')
        task = 'task_with_ping'

        cur_time = time.localtime(time.time() - 5)
        Scheduler().wait_task_changes(task)

        Scheduler().wait_import_task(cur_time, task)

        Scheduler().wait_finish_task(cur_time, task)

        set_state(defaults.PCIDSS.State.normal)

    def test_task_without_ping(self):
        set_state('TEST2')
        task = 'task_without_ping'

        cur_time = time.localtime(time.time() - 5)

        Scheduler().wait_task_changes(task)

        Scheduler().wait_import_task(cur_time, task)

        Scheduler().wait_killed_task(cur_time)

        set_state(defaults.PCIDSS.State.normal)
