# coding: utf-8
import ast
import time
from time import sleep

from hamcrest import equal_to, empty, is_not, is_

import btestlib.reporter as reporter
from btestlib import environments, secrets
from btestlib import utils as butils
from btestlib.utils import CheckMode
from simpleapi.common import logger
from simpleapi.common.utils import call_http, parse_xml
from simpleapi.data import defaults
from simpleapi.matchers.pcidss_matchers import keyapi_status_is, keykeeper_status_is, confpatch_status_is
from simpleapi.steps import balance_test_steps as balance_test
from simpleapi.steps import db_steps
from simpleapi.steps.check_steps import check_that

__author__ = 'slppls'

log = logger.get_logger()
cur_url = environments.SimpleapiEnvironment('DEV_BS', **environments.SimpleapiEnvironmentConfiguration[
    'DEV_BS'].value).pcidss_inside_url

AUTH_KEYKEEPER = {'CP-AUTH': secrets.get_secret(*secrets.TrustInfr.PCI_DSS_DEV_KEYKEEPER)}
AUTH_CONFPATCH = {'CP-AUTH': secrets.get_secret(*secrets.TrustInfr.PCI_DSS_DEV_CONFPATCH)}


def ignore_502(func):
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except Exception, exc:
            reporter.logger().error('ExpatError while parsing xml: {}'.format(exc))
            return None

    return wrapper


class KeyKeeper(object):
    @staticmethod
    @CheckMode.result_matches(keykeeper_status_is('success'))
    @parse_xml
    def get(letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keykeeper_python, auth=AUTH_KEYKEEPER):
        sleep(1)
        url = cur_url.format(letter, port) + 'key_keeper2/get'
        with reporter.step(u'Получаем содержимое KeyKeeper'):
            return call_http(url, {}, method='GET', headers=auth)

    @staticmethod
    @CheckMode.result_matches(keykeeper_status_is('success'))
    @parse_xml
    def set(kek, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keykeeper_python, auth=AUTH_KEYKEEPER):
        sleep(1)
        params = {
            'id': kek['id'],
            'data': kek['data'],
        }
        url = cur_url.format(letter, port) + 'key_keeper2/set?'
        with reporter.step(u'Устанавливаем в KeyKeeper часть ключа'):
            return call_http(url, params, method='GET', headers=auth)

    @staticmethod
    @CheckMode.result_matches(keykeeper_status_is('success'))
    @parse_xml
    def unset(kek, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keykeeper_python,
              auth=AUTH_KEYKEEPER):
        sleep(1)
        params = {
            'id': kek['id'],
        }
        url = cur_url.format(letter, port) + 'key_keeper2/unset?'
        with reporter.step(u'Удаляем часть ключа из KeyKeeper'):
            return call_http(url, params, method='GET', headers=auth)

    @staticmethod
    @CheckMode.result_matches(keykeeper_status_is('success'))
    @parse_xml
    def cleanup(kek, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keykeeper_python,
                auth=AUTH_KEYKEEPER):
        sleep(1)
        params = {
            'id': kek['id'],
        }
        url = cur_url.format(letter, port) + 'key_keeper2/cleanup?'
        with reporter.step(u'Удаляем все части ключа из KeyKeeper, кроме одной'):
            return call_http(url, params, method='GET', headers=auth)

    @staticmethod
    def get_status(resp):
        return resp['response']['status']

    @staticmethod
    def get_desc(resp):
        return resp['response']['desc']

    @staticmethod
    def get_status_desc(resp):
        return resp['response']['status_desc']

    @staticmethod
    def get_status_code(resp):
        return resp['response']['status_code']

    @staticmethod
    def get_item(resp):
        return resp['response']['items']['item']

    @staticmethod
    def get_id(resp):
        return KeyKeeper.get_item(resp)['@id']

    @staticmethod
    def get_data(resp):
        return KeyKeeper.get_item(resp)['@data']

    @staticmethod
    def get_create_ts(resp):
        return KeyKeeper.get_item(resp)['@create_ts']

    @staticmethod
    def is_kek_in_response(response, kek):
        keks_actual = KeyKeeper.parse_keks(response)

        return any(kek['id'] == KEK['id'] and kek['data'] == KEK['data']
                   for KEK in keks_actual)

    @staticmethod
    def parse_keks(response):
        keks = []
        items = KeyKeeper.get_item(response)
        if not isinstance(items, (list, tuple)):
            items = [items, ]

        for item in items:
            kek = {
                'id': item.get('@id'),
                'data': item.get('@data'),
                'create_ts': item.get('@create_ts'),
                'update_ts': item.get('@update_ts'),
            }
            keks.append(kek)

        return keks


class ConfPatch(object):
    @staticmethod
    @CheckMode.result_matches(confpatch_status_is('success'))
    @parse_xml
    def set(kek, letter, port=defaults.PCIDSS.Port.confpatch, auth=AUTH_CONFPATCH):
        sleep(1)
        params = {
            'id': kek['id'],
            'data': kek['data'],
        }
        url = cur_url.format(letter, port) + 'conf_patch/set?'
        with reporter.step(u'Устанавливаем в ConfPatch часть ключа'):
            return call_http(url, params, method='GET', headers=auth)

    @staticmethod
    @CheckMode.result_matches(confpatch_status_is('success'))
    @parse_xml
    def unset(kek, letter, port=defaults.PCIDSS.Port.confpatch, auth=AUTH_CONFPATCH):
        sleep(1)
        params = {
            'id': kek['id']
        }
        url = cur_url.format(letter, port) + 'conf_patch/unset?'
        with reporter.step(u'Удаляем из ConfPatch часть ключа'):
            return call_http(url, params, method='GET', headers=auth)

    @staticmethod
    @CheckMode.result_matches(confpatch_status_is('success'))
    @parse_xml
    def cleanup(kek, letter, port=defaults.PCIDSS.Port.confpatch, auth=AUTH_CONFPATCH):
        sleep(1)
        params = {
            'id': kek['id']
        }
        url = cur_url.format(letter, port) + 'conf_patch/cleanup?'
        with reporter.step(u'Удаляем все части ключа из ConfPatch, кроме одной'):
            return call_http(url, params, method='GET', headers=auth)

    @staticmethod
    def get_from_config(letter=defaults.PCIDSS.base_servant):
        sleep(1)
        import xmltodict

        path = defaults.PCIDSS.Path.confpatch_conf_path.format(letter)
        resp = balance_test.find_config(path)

        config = ''
        for line in resp['lines']:
            config += line

        def _parse_kek_from_config(conf):
            keks = []
            conf_dict = xmltodict.parse(conf)
            conf_keks = conf_dict.get('KeySettings').get('items')
            if conf_keks:
                conf_keks = conf_keks.get('item')
            else:
                # no keks in config
                return ()

            if not isinstance(conf_keks, (list, tuple)):
                conf_keks = [conf_keks, ]

            for conf_kek in conf_keks:
                kek = {
                    'id': conf_kek['@id'],
                    'data': conf_kek['@data'],
                    'update_ts': conf_kek['@update_ts']
                }
                keks.append(kek)

            return keks

        return _parse_kek_from_config(config)

    @staticmethod
    def is_kek_in_config(kek, letter):
        sleep(1)
        return any(kek['id'] == KEK['id'] and kek['data'] == KEK['data']
                   for KEK in ConfPatch.get_from_config(letter))

    @staticmethod
    def is_servant_was_restarted(servant, letter):
        with reporter.step(u'Проверяем, был ли перезапущен сервант {}'.format(servant)):
            path = defaults.PCIDSS.Path.servant_restart_path.format(letter, servant['path'])
            # метод FindInLogByRegexp ищет в логах, стартуя с момента времени last_date и далее
            # реальный перезапуск серванта начинатеся ранее, чем мы в тестах зовем find_in_log
            # поэтому надо брать время слегка в прошлом
            # берем текущее_время - 10_секунд. 10 секунд выбрано эмпирически
            time_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(time.time() - 10))
            resp = balance_test.find_in_log(path, time_str, servant['restart_regexp'])
            # проверяем, что есть хотя бы одна запись о рестарте серванта
            return resp.get('lines') and len(resp['lines']) > 0

    @staticmethod
    def get_status(resp):
        return resp['response']['status']

    @staticmethod
    def get_status_code(resp):
        return resp['response']['status_code']


class Tokenizer(object):
    @staticmethod
    @parse_xml
    def check_kek(version, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.tokenizer):
        sleep(1)
        params = {
            'kek_version': version
        }
        url = cur_url.format(letter, port) + 'service/check_kek?'
        with reporter.step(u'Проверяем, успешно ли собрался kek версии {}'.format(version)):
            return call_http(url, params, method='GET')

    @staticmethod
    def get_status(resp):
        return resp['response']['status']

    @staticmethod
    def get_ts(resp):
        return resp['response']['ts']

    @staticmethod
    def get_check_kek(resp):
        return resp['response']['check_kek']


class KeyApi(object):
    @staticmethod
    @ignore_502
    @parse_xml
    def status(letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        sleep(1)
        url = cur_url.format(letter, port) + 'logic/status'
        with reporter.step(u'Получаем обобщенную информацию о системе KeyApi'):
            return call_http(url, {}, method='GET')

    @staticmethod
    @ignore_502
    @parse_xml
    def status_extended(letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        sleep(1)
        url = cur_url.format(letter, port) + 'logic/status?extended=1'
        with reporter.step(u'Получаем расширенную обобщенную информацию о системе KeyApi'):
            return call_http(url, {}, method='GET')

    @staticmethod
    def get_version_from_status(resp):
        return resp['response']['settings']['KEK_VERSION']

    @staticmethod
    def get_target_version_from_status(resp):
        return resp['response']['settings']['KEK_TARGET_VERSION']

    @staticmethod
    def get_state_from_status(resp):
        if resp:
            return resp['response']['settings']['STATE']

    @staticmethod
    def get_state_extension_from_status(resp):
        return resp['response']['settings']['STATE_EXTENSION']

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def generate_kek(password, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password
        }
        url = cur_url.format(letter, port) + 'logic/generate_kek?'
        with reporter.step(u'Запускаем генерацию нового kek '):
            return call_http(url, params, method='GET')

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def get_component(password, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password
        }
        url = cur_url.format(letter, port) + 'logic/get_component?'
        with reporter.step(u'Получаем новую компоненту kek'):
            return call_http(url, params, method='GET')

    @staticmethod
    def get_status(resp):
        return resp['response']['status']

    @staticmethod
    def get_component_from_get_comp(resp):
        return resp['response']['component']

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def confirm_component(password, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password
        }
        url = cur_url.format(letter, port) + 'logic/confirm_component?'
        with reporter.step(u'Подтверждаем получение компоненты kek'):
            return call_http(url, params, method='GET')

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def reencrypt_deks(password, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password
        }
        url = cur_url.format(letter, port) + 'logic/reencrypt_deks?'
        with reporter.step(u'Начинаем перешифрование deks'):
            return call_http(url, params, method='GET')

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def switch_kek(password, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password
        }
        url = cur_url.format(letter, port) + 'logic/switch_kek?'
        with reporter.step(u'Меняем старый kek на новый'):
            return call_http(url, params, method='GET')

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def cleanup(password, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password
        }
        url = cur_url.format(letter, port) + 'logic/cleanup?'
        with reporter.step(u'Откатываем все изменения до первоначального состояния'):
            return call_http(url, params, method='GET')

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def reset_target_version(password, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password
        }
        url = cur_url.format(letter, port) + 'logic/reset_target_version?'
        with reporter.step(u'Сбрасываем целевую версию kek до текущей'):
            return call_http(url, params, method='GET')

    @staticmethod
    @CheckMode.result_matches(keyapi_status_is('success'))
    @parse_xml
    def recover_component(password, kek_version, letter=defaults.PCIDSS.base_servant, port=defaults.PCIDSS.Port.keyapi):
        params = {
            'password': password,
            'version': kek_version,
        }
        url = cur_url.format(letter, port) + 'logic/recover_component?'
        with reporter.step(u'Проталкиваем новую компоненту kek'):
            return call_http(url, params, method='GET')

    @staticmethod
    def get_switch_ready(resp):
        if resp:
            return resp.get('response').get('extended').get('KEK_SWITCH_READY')

    @staticmethod
    def get_kek_part_check(resp, kek_version, kek_part):
        parts_status = []
        if resp:
            return resp.get('response').get('settings').get('KEK_VER{}_PART{}_CHECK'.format(kek_version, kek_part))

    def wait_reencrypt_deks_done(self):
        with reporter.step(u'Ждём, пока перешифруются deks...'):
            return butils.wait_until(lambda: self.get_switch_ready(self.status_extended()),
                                     success_condition=equal_to('True'),
                                     timeout=30 * 60)

    def wait_switch_kek_done(self):
        with reporter.step(u'Ждём, пока произойдет смена KEKа...'):
            return butils.wait_until(lambda: self.get_state_from_status(self.status()),
                                     success_condition=equal_to(defaults.PCIDSS.State.normal),
                                     timeout=15 * 60)

    def wait_generate_kek_done(self):
        with reporter.step(u'Ждём, пока завершиться генерация KEKа...'):
            return butils.wait_until(lambda: self.get_state_from_status(self.status()),
                                     success_condition=equal_to(defaults.PCIDSS.State.confirm_kek_parts),
                                     timeout=3 * 60)

    def wait_cleanup_done(self):
        with reporter.step(u'Ждём, пока произойдет смена KEKа...'):
            return butils.wait_until(lambda: self.get_state_from_status(self.status()),
                                     success_condition=equal_to(defaults.PCIDSS.State.normal),
                                     timeout=3 * 60)


def get_kek_part_1(kek_version):
    with reporter.step(u'Получаем первую компоненту'):
        get_resp = KeyKeeper.get()
        keks = KeyKeeper.parse_keks(get_resp)
        for kek in keks:
            if kek['id'] == 'KEK_VER{}_PART1'.format(str(kek_version)):
                return kek['data']


def get_kek_part_2(kek_version):
    with reporter.step(u'Получаем вторую компоненту'):
        keks = ConfPatch.get_from_config()
        for kek in keks:
            if kek['id'] == 'KEK_VER{}_PART2'.format(str(kek_version)):
                return kek['data']


def get_kek_part_3(kek_version):
    with reporter.step(u'Получаем третью компоненту'):
        query = "SELECT cvalue FROM t_config WHERE ckey = 'KEK_VER{}_PART3'".format(kek_version)
        return db_steps.pcidss().execute_query(query)['cvalue']


def get_kek_parts(kek_version):
    with reporter.step(u'Запрашиваем три компоненты'):
        return {1: get_kek_part_1(kek_version), 2: get_kek_part_2(kek_version), 3: get_kek_part_3(kek_version)}


def set_state(status):
    query = "UPDATE t_config SET cvalue = '{}' WHERE ckey = 'STATE'".format(status)
    db_steps.pcidss().execute_update(query)


def get_state():
    query = "SELECT cvalue FROM t_config WHERE ckey = 'STATE'"
    return db_steps.pcidss().execute_query(query)['cvalue']


class Scheduler(object):
    @staticmethod
    def check_task_status(cur_time, status, task):
        time_str = time.strftime("%Y-%m-%d %H:%M:%S", cur_time)
        result = []
        with reporter.step(u'Проверяем статус таска'):
            for letter in defaults.PCIDSS.servant_pool:
                path = defaults.PCIDSS.Path.scheduler_path.format(letter)
                regexp = '.+{} task <function {}+'.format(status, task)
                result.append(balance_test.find_in_log(path, time_str, regexp)['lines'])
            return result

    @staticmethod
    def search_killed_by_sigterm(cur_time):
        time_str = time.strftime("%Y-%m-%d %H:%M:%S", cur_time)
        result = []
        with reporter.step(u'Проверяем, убил ли SIGTERM таск'):
            for letter in defaults.PCIDSS.servant_pool:
                path = defaults.PCIDSS.Path.scheduler_path.format(letter)
                regexp = '.+card_pyproxy_scheduler: killed by SIGTERM+'
                result.append(balance_test.find_in_log(path, time_str, regexp)['lines'])
            return result

    @staticmethod
    def get_sch_params(letter=defaults.PCIDSS.base_servant):
        resp = ast.literal_eval(KeyApi.get_state_extension_from_status(KeyApi.status(letter)))
        return resp['sch_task']

    def wait_task_changes(self, task):
        with reporter.step(u'Ждём, пока задача сменится...'):
            return butils.wait_until(lambda: self.get_sch_params(),
                                     success_condition=equal_to('{}.{}'.format(task, task)),
                                     timeout=2 * 60)

    def wait_import_task(self, cur_time, task):
        with reporter.step(u'Ждём, пока задача заимпортится...'):
            return butils.wait_until(lambda: self.check_task_status(cur_time, 'imported', task),
                                     success_condition=is_not(empty()),
                                     timeout=2 * 60)

    def wait_finish_task(self, cur_time, task):
        with reporter.step(u'Ждём, пока задача завершится...'):
            return butils.wait_until(lambda: self.check_task_status(cur_time, 'finished', task),
                                     success_condition=is_not(empty()),
                                     timeout=2 * 60)

    def wait_killed_task(self, cur_time):
        with reporter.step(u'Ждём, пока задача завершится...'):
            return butils.wait_until(lambda: self.search_killed_by_sigterm(cur_time),
                                     success_condition=is_not(empty()),
                                     timeout=2 * 60)


class ChangeKEK(object):
    @staticmethod
    def process_data_prepare():
        with reporter.step(u'Подготовливаем данные для смены КЕКа'):
            current_kek_version = KeyApi().get_version_from_status(KeyApi().status())
            return current_kek_version, get_kek_parts(current_kek_version)

    @staticmethod
    def process_generate_kek(kek_parts):
        with reporter.step(u'Генерируем новый КЕК'):
            gen_kek = KeyApi.generate_kek(kek_parts[1])
            KeyApi().wait_generate_kek_done()

            new_kek_version = KeyApi.get_target_version_from_status(KeyApi.status())
            resp = Tokenizer.check_kek(new_kek_version)
            check_that(Tokenizer.get_check_kek(resp), is_(equal_to('true')),
                       step=u'Проверяем, что КЕК успешно проверен внутри pcidss',
                       error=u'Генерация КЕКа неуспешна!')

    @staticmethod
    def process_get_components(kek_parts):
        with reporter.step(u'Получаем три новые части КЕКа'):
            new_kek_parts = {}
            for num in kek_parts:
                current_comp = KeyApi.get_component(kek_parts[num])
                check_that(KeyApi.get_status(current_comp), is_(equal_to('success')),
                           step=u'Проверяем успешность получения компоненты {}'.format(num),
                           error=u'Ошибка в получении компоненты {}'.format(num))
                new_kek_parts.update({num: KeyApi.get_component_from_get_comp(current_comp)})
            return new_kek_parts

    @staticmethod
    def process_confirm_components(kek_parts):
        with reporter.step(u'Подтверждаем три новые части КЕКа'):
            for num in kek_parts:
                resp = KeyApi.confirm_component(kek_parts[num])
                check_that(KeyApi.get_status(resp), is_(equal_to('success')),
                           step=u'Проверяем статус вызова метода для {} компоненты'.format(num),
                           error=u'Ошибка в статусе вызова метода для {} компоненты'.format(num))
                cur_part_check = KeyApi.get_kek_part_check(KeyApi.status_extended(),
                                                           KeyApi.get_target_version_from_status(KeyApi.status()), num)
                check_that(cur_part_check, is_(equal_to('1')),
                           step=u'Проверяем успешность подтверждения компоненты в pcidss {}'.format(num),
                           error=u'Ошибка в подтверждении компоненты {}'.format(num))

    @staticmethod
    def process_reencrypt_deks(kek_parts):
        with reporter.step(u'Перешифровываем deks'):
            time.sleep(30)  # вынужденный слип, так как проблема кэширования в pci не приоритетна для починки
            KeyApi.reencrypt_deks(kek_parts[1])
            KeyApi().wait_reencrypt_deks_done()

    @staticmethod
    def process_switch_kek(kek_parts):
        with reporter.step(u'Меняем местами новый и старый КЕК'):
            time.sleep(30)  # вынужденный слип, так как проблема кэширования в pci не приоритетна для починки
            KeyApi.switch_kek(kek_parts[1])
            KeyApi().wait_switch_kek_done()

    @staticmethod
    def process_cleanup(kek_parts):
        with reporter.step(u'Отменяем все изменения в процессе смены КЕКа'):
            KeyApi.cleanup(kek_parts[1])
            KeyApi().wait_cleanup_done()

    @staticmethod
    def process_reset_target_version(kek_parts):
        with reporter.step(u'Сбрасываем ожидаемую версию КЕКа'):
            KeyApi.reset_target_version(kek_parts[1])
            KeyApi().wait_reencrypt_deks_done()

    @staticmethod
    def process_kek_change():
        with reporter.step(u'Запускаем процедуру смены КЕКа'):
            _, old_kek_parts = ChangeKEK.process_data_prepare()
            ChangeKEK.process_generate_kek(old_kek_parts)
            new_kek_parts = ChangeKEK.process_get_components(old_kek_parts)
            ChangeKEK.process_confirm_components(new_kek_parts)
            ChangeKEK.process_reencrypt_deks(new_kek_parts)
            ChangeKEK.process_switch_kek(new_kek_parts)
