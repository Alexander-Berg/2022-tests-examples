# coding: utf-8

__author__ = 'a-vasin'

import os
from datetime import datetime

from hamcrest import not_, equal_to, is_in, all_of, has_key, not_none

import btestlib.environments as env
import cashmachines.data.passwords as pwd
from btestlib import reporter
from btestlib import utils
from cashmachines.cashmachines_utils import call_http_raw_tvm, call_http_tvm
from cashmachines.data import defaults
from cashmachines.data.constants import CMNds, State, Group

FAILURE_MATCHER = all_of(not_(has_key(u'error')), not_(has_key(u'errors')))


# Swagger
# https://whitespirit-dev1f.balance.os.yandex.net:8080/

class StatusSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def get_all_statuses(include_offline=False):
        with reporter.step(u'Получаем статусы всех касс (include_offline: {})'.format(include_offline)):
            method_url = '{base_url}/v1/cashmachines'.format(base_url=env.get_ws_url())
            params = {
                'include_offline': str(include_offline).lower()
            }
            return call_http_tvm(method_url, params, method='GET')

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def get_status(serial_number):
        with reporter.step(u'Получаем статус кассы: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/status' \
                .format(base_url=env.get_ws_url(), sn=serial_number)
            return call_http_tvm(method_url, method='GET')

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def ping(inn=None, group=None, backlog_ratio=None):
        with reporter.step(u'Пингуем кассы (ИНН: {}, группа: {})'.format(inn, group)):
            method_url = '{base_url}/v1/ping'.format(base_url=env.get_ws_url())
            params = utils.remove_empty({
                'firm_inn': inn,
                'group': group,
                'backlog_ratio': backlog_ratio
            })
            return call_http_raw_tvm(method_url, params, method='GET')


class AdminSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def clear_debug_fn(serial_number):
        with reporter.step(u'Очищаем данные тестового ФН: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/clear_debug_fn?mysecret={mysecret}' \
                .format(base_url=env.get_ws_url(), sn=serial_number,
                        mysecret=pwd.mysecret(serial_number))
            return call_http_tvm(method_url, json_data={u"password": 111111})

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def clear_device_data(serial_number, ip=None):
        with reporter.step(u'Очищаем данные кассы: {}'.format(serial_number)):
            sn, mysecret = defaults.get_sn_and_mysecret(serial_number, ip)

            method_url = '{base_url}/v1/cashmachines/{sn}/clear_device_data?mysecret={mysecret}' \
                .format(base_url=env.get_ws_url(), sn=sn, mysecret=mysecret)
            return call_http_tvm(method_url, json_data=pwd.payload(serial_number))

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def close_fiscal_mode(serial_number):
        with reporter.step(u'Закрываем фискальный режим кассы: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/close_fiscal_mode?mysecret={mysecret}' \
                .format(base_url=env.get_ws_url(), sn=serial_number,
                        mysecret=pwd.mysecret(serial_number))
            return call_http_tvm(method_url, json_data=pwd.payload(serial_number))

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def configure(serial_number, config_params=None):
        with reporter.step(u'Конфигурируем кассу: {}'.format(serial_number)):
            if not config_params:
                config_params = defaults.config(serial_number)

            method_url = '{base_url}/v1/cashmachines/{sn}/configure' \
                .format(base_url=env.get_ws_url(), sn=serial_number)
            return call_http_tvm(method_url, json_data=config_params)

    # -А что делает ident?
    # -Включает синию лампочку на кассе или выключает. В общем идентифицирует.
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def ident(serial_number, on=False):
        with reporter.step(u'Идентифицируем кассу: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/ident?on_={on_}' \
                .format(base_url=env.get_ws_url(), sn=serial_number, on_=str(on).lower())
            return call_http_raw_tvm(method_url)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def log(serial_number, size=65535):
        with reporter.step(u'Получаем логи кассы: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/log' \
                .format(base_url=env.get_ws_url(), sn=serial_number)
            return call_http_tvm(method_url, params={'log_size': size}, method='GET')

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def maintenance(serial_number):
        with reporter.step(u'Техническое обслуживание кассы: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/maintenance' \
                .format(base_url=env.get_ws_url(), sn=serial_number)
            return call_http_tvm(method_url)

    # a-vasin: говорят, при hot (не cold) reboot что-то может пойти не так
    # по этому использовать его стоит только с веской причиной
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def reboot(serial_number, cold=False):
        with reporter.step(u'Перезагружаем кассу: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/reboot?cold={cold}' \
                .format(base_url=env.get_ws_url(), sn=serial_number, cold=str(cold).lower())
            return call_http_tvm(method_url)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def register(serial_number, reregister=False, fn_changed=False):
        with reporter.step(u'Регистрируем кассу: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/register?' \
                         'mysecret={mysecret}&reregister={reregister}&fn_changed={fn_changed}' \
                .format(base_url=env.get_ws_url(), sn=serial_number,
                        mysecret=pwd.mysecret(serial_number),
                        reregister=str(reregister).lower(), fn_changed=str(fn_changed).lower())
            return call_http_tvm(method_url, json_data=pwd.payload(serial_number))

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def set_datetime(serial_number, dt=None):
        if dt is None:
            dt = ''

        if isinstance(dt, datetime):
            dt = dt.strftime('%Y-%m-%dT%H:%M:%S')

        with reporter.step(u'Устанавливаем на кассе: {} дату: {}'.format(serial_number, dt)):
            method_url = '{base_url}/v1/cashmachines/{sn}/set_datetime?dt={dt}' \
                .format(base_url=env.get_ws_url(), sn=serial_number, dt=dt)
            return call_http_raw_tvm(method_url)


class ShiftSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def open_shift(serial_number):
        with reporter.step(u'Открываем смену для кассы: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/open_shift' \
                .format(base_url=env.get_ws_url(), sn=serial_number)
            return call_http_tvm(method_url)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def close_shift(serial_number):
        with reporter.step(u'Закрываем смену для кассы: {}'.format(serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/close_shift' \
                .format(base_url=env.get_ws_url(), sn=serial_number)
            return call_http_tvm(method_url)


class ReceiptSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def get_document(serial_number, document_number, with_ofd_ticket=False, with_printform=False, with_rawform=False,
                     with_fullform=False):
        with reporter.step(u'Получаем документ под номером: {} у кассы: {}'.format(document_number, serial_number)):
            method_url = '{base_url}/v1/cashmachines/{sn}/document/{dn}' \
                .format(base_url=env.get_ws_url(), sn=serial_number, dn=document_number)

            params = {
                'with_ofd_ticket': str(with_ofd_ticket).lower(),
                'with_printform': str(with_printform).lower(),
                'with_rawform': str(with_rawform).lower(),
                'with_fullform': str(with_fullform).lower()
            }

            return call_http_tvm(method_url, params=params, method='GET')

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def receipts(receipt_request, group=None, tvm_token=None, timeout=None, wait4free=0, retry_count=10):
        with reporter.step(u'Пробиваем чек'):
            method_url = '{base_url}/v1/receipts?wait4free={w4f}&retry_count={rc}' \
                .format(base_url=env.get_ws_url(), w4f=wait4free, rc=retry_count)
            if group:
                method_url += '&group={}'.format(group)
            return call_http_tvm(method_url, json_data=receipt_request, tvm_token=tvm_token, timeout=timeout)


class DebugSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def lost_receipts():
        with reporter.step(u'Получаем потерянные чеки'):
            method_url = '{base_url}/lost_receipts'.format(base_url=env.whitespirit_env().debug_url)
            return call_http_tvm(method_url, method='GET')


class BalancerSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def ping():
        with reporter.step(u'Пингуем балансер'):
            method_url = '{base_url}/ping'.format(base_url=env.whitespirit_env().balancer_url)
            return call_http_raw_tvm(method_url, method='GET')

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def hudsucker():
        with reporter.step(u'Получаем статус балансера'):
            method_url = '{base_url}/hudsucker'.format(base_url=env.whitespirit_env().balancer_url)
            return call_http_raw_tvm(method_url, method='GET')


class CMSteps(object):
    @staticmethod
    def make_receipts(prices, qtys, ndses=None):
        amounts = [sum([price * qty for price, qty in zip(prices, qtys)])]
        return ReceiptSteps.receipts(defaults.receipts(defaults.rows(prices, qtys, ndses), defaults.payments(amounts)))

    @staticmethod
    def make_single_receipt(price, qty, nds=CMNds.NDS_20):
        return CMSteps.make_receipts([price], [qty], [nds])

    @staticmethod
    def make_big_single_receipt(price=defaults.PRICE, qty=defaults.QTY, nds=CMNds.NDS_20):
        number_of_rows = 123

        return CMSteps.make_receipts(
            [price] * number_of_rows,
            [qty] * number_of_rows,
            [nds] * number_of_rows
        )

    @staticmethod
    def get_receipt_printed_form_html(price, qty, nds=CMNds.NDS_18):
        receipt = CMSteps.make_single_receipt(price, qty, nds)
        url = 'https://check.greed-tm1f.yandex.ru'
        return call_http_tvm(url, json_data=receipt)

    # a-vasin: "Ну если падает с эксепшном, тогда значит недоступна" (c) dimonb
    # охуительные дизайнерские решения от разработки
    @staticmethod
    def wait_for_cashmachine_online(serial_number):
        def try_get_status():
            with utils.check_mode(utils.CheckMode.IGNORED):
                response = StatusSteps.get_status(serial_number)
                return response.get('state', None)

        with reporter.step(u'Ждем, когда начнет отвечать касса: {}'.format(serial_number)):
            return utils.wait_until(lambda: try_get_status(), not_(is_in([None, State.OFFLINE])), timeout=300)

    @staticmethod
    def wait_for_cashmachine(serial_number, state):
        with reporter.step(u'Ждем, пока касса: {} не перейдет в ожидаемое состояние: {}'.format(serial_number, state)):
            last_state = CMSteps.wait_for_cashmachine_online(serial_number)
            if last_state != state:
                utils.wait_until(lambda: StatusSteps.get_status(serial_number)['state'], equal_to(state))

    @staticmethod
    def reset_cashmachine(serial_number):
        with reporter.step(u'Сбрасываем в начальное состояние кассу: {}'.format(serial_number)):
            AdminSteps.clear_debug_fn(serial_number)
            CMSteps.wait_for_cashmachine_online(serial_number)

            AdminSteps.clear_device_data(serial_number)
            CMSteps.wait_for_cashmachine_online(serial_number)

            AdminSteps.reboot(serial_number)
            CMSteps.wait_for_cashmachine(serial_number, State.NONCONFIGURED)

    @staticmethod
    def setup_cashmachine(serial_number):
        with reporter.step(u'Конфигурируем и регистрируем кассу: {}'.format(serial_number)):
            AdminSteps.configure(serial_number)
            # TODO: вот тут не должно быть этого ожидания, но вы поняли, Кирюха фиксит
            CMSteps.wait_for_cashmachine(serial_number, State.NONCONFIGURED)

            AdminSteps.reboot(serial_number)
            CMSteps.wait_for_cashmachine(serial_number, State.NONCONFIGURED)

            AdminSteps.register(serial_number)
            CMSteps.wait_for_cashmachine(serial_number, State.CLOSE_SHIFT)

    @staticmethod
    def prepare_cashmachine_for_receipts(serial_number):
        with reporter.step(u'Сбрасываем и настраиваем для пробития чеков кассу: {}'.format(serial_number)):
            CMSteps.reset_cashmachine(serial_number)
            CMSteps.setup_cashmachine(serial_number)
            ShiftSteps.open_shift(serial_number)

    @staticmethod
    @utils.CheckMode.result_matches(not_none())
    def restore_from_fatal_error_without_sn(ip):
        with reporter.step(u'Восстанавливаем кассу с ip: {}'.format(ip)):
            devices = StatusSteps.get_all_statuses()['devices'].values()
            online_sns = {device['sn'] for device in devices}
            potential_sns = defaults.CASHMACHINES_BY_HOST[env.whitespirit_env().name].difference(online_sns)

            AdminSteps.clear_debug_fn(defaults.ip_serial_number(ip))

            for serial_number in potential_sns:
                with utils.check_mode(utils.CheckMode.IGNORED):
                    response = AdminSteps.clear_device_data(serial_number, ip)

                if response is not None:
                    continue

                CMSteps.wait_for_cashmachine(serial_number, State.NONCONFIGURED)

                AdminSteps.reboot(serial_number)
                CMSteps.wait_for_cashmachine(serial_number, State.NONCONFIGURED)

                return serial_number

    @staticmethod
    def restore_from_fatal_error(serial_number):
        with reporter.step(u'Восстанавливаем кассу с sn: {}'.format(serial_number)):
            AdminSteps.clear_debug_fn(serial_number)
            AdminSteps.clear_device_data(serial_number)

            CMSteps.wait_for_cashmachine(serial_number, State.NONCONFIGURED)

            AdminSteps.reboot(serial_number)
            CMSteps.wait_for_cashmachine(serial_number, State.NONCONFIGURED)

            return serial_number

    # ток в один поток из-за этого =(
    @staticmethod
    def restore_cashmachines():
        proxy_to_balancer = os.getenv('proxy_to_balancer', '')
        os.environ['proxy_to_balancer'] = ''
        try:
            machines = StatusSteps.get_all_statuses()
            for machine in machines['devices'].values():
                if machine["proto"] == "mock" or not machine["sn"]:
                    continue

                serial_number = machine['registration_info'].get('sn', None)
                state = machine['state']

                if state == State.FATAL_ERROR:
                    serial_number = CMSteps.restore_from_fatal_error(serial_number)
                    CMSteps.setup_cashmachine(serial_number)
                    state = State.CLOSE_SHIFT

                if state == State.OVERDUE_OPEN_SHIFT:
                    ShiftSteps.close_shift(serial_number)
                    ShiftSteps.open_shift(serial_number)
                    continue

                if state == State.CLOSE_SHIFT:
                    ShiftSteps.open_shift(serial_number)
                    continue

                if set(machine['groups']) != set(Group.values()):
                    params = defaults.groups_config(serial_number)
                    AdminSteps.configure(serial_number, params)
                    continue

                if state == State.OPEN_SHIFT:
                    continue

                CMSteps.prepare_cashmachine_for_receipts(serial_number)
        finally:
            os.environ['proxy_to_balancer'] = proxy_to_balancer

    @staticmethod
    @utils.CheckMode.result_matches(not_none())
    def get_random_sn():
        with reporter.step(u"Получаем serial number любой рабочей кассы"):
            machines = StatusSteps.get_all_statuses()
            for machine in machines['devices'].values():
                serial_number = machine['registration_info'].get('sn', None)
                state = machine['state']

                if state == State.OPEN_SHIFT and machine["proto"] != "mock":
                    return serial_number

            return None
