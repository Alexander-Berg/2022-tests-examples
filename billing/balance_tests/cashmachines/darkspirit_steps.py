# coding: utf-8

__author__ = 'a-vasin'

import requests
from hamcrest import not_, all_of, has_key

import btestlib.environments as env
from btestlib import reporter
from btestlib import utils
from btestlib.constants import Firms
from simpleapi.common.utils import call_http

FAILURE_MATCHER = all_of(not_(has_key(u'error')), not_(has_key(u'errors')))


# Swagger
# https://greed-tm1f.yandex.ru:8616/

# a-vasin: AdminSteps только для тестовых сред
class AdminSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def clean_cash_registers(serial_number):
        with reporter.step(u'Очищаем и удаляем все данные из DS тестового ФН: {}'.format(serial_number)):
            method_url = '{base_url}/admin/cash_registers/{sn}/clean' \
                .format(base_url=env.darkspirit_env().darkspirit_url, sn=serial_number)
            return call_http(method_url)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def test_configure(serial_numbers, firm_inn=Firms.YANDEX_1.inn, ofd_inn=u"7704358518"):
        with reporter.step(u'Конфигурируем кассы: {}'.format(serial_numbers)):
            method_url = '{base_url}/admin/test-configure' \
                .format(base_url=env.darkspirit_env().darkspirit_url)

            json_data = {
                "firm_inn": firm_inn,
                "ofd_inn": ofd_inn,
                "serial_numbers": serial_numbers
            }

            return call_http(method_url, json_data=json_data)


class ReceiptsSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def get_receipt(fn_sn, dn, fp):
        # TODO дополнить описание степа
        with reporter.step(u'Получаем чек'):
            method_url = '{base_url}/fiscal_storages/{fn_sn}/documents/{dn}/{fp}' \
                .format(base_url=env.darkspirit_env().darkspirit_url, fn_sn=fn_sn, dn=dn, fp=fp)
            return call_http(method_url, method='GET')

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def upload_receipt():
        # TODO дополнить описание степа и доработать сам степ
        with reporter.step(u'Получаем чек'):
            method_url = '{base_url}/receipts'.format(base_url=env.darkspirit_env().darkspirit_url)

            # json_data = defaults.ds_receipts()

            return call_http(method_url)


class RegistrationSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    # TODO не понятно, что должны быть внутри CVS пока что (отправлять будем строку, а не настоящий файл)
    def configure(cvs_data):
        with reporter.step(u'Конфигурируем кассу'):
            method_url = '{base_url}/registration/configure'.format(base_url=env.darkspirit_env().darkspirit_url)

            headers = {
                "accept": "application/json",
                "content-type": "multipart/form-data"
            }

            files = {'file': ('configure.csv', cvs_data, 'text/plain')}

            reporter.report_http_call_curl('POST', method_url, headers=headers)
            return requests.post(uri, headers=headers, files=files).content

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def create_fiscalization_xml(serial_numbers):
        with reporter.step(u'Генерируем отчет для налоговой: {}'.format(serial_numbers)):
            method_url = '{base_url}/registration/create-fiscalization-xml'.format(
                base_url=env.darkspirit_env().darkspirit_url)

            json_data = {
                "serial_numbers": serial_numbers
            }

            return call_http(method_url, json_data=json_data)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def create_registration_archive(serial_numbers, firm_inn=Firms.YANDEX_1.inn, ofd_inn=u"7704358518"):
        with reporter.step(u'Генерируем архив для регистрации в налоговой: {}'.format(serial_numbers)):
            method_url = '{base_url}/registration/create-xml'.format(base_url=env.darkspirit_env().darkspirit_url)

            json_data = {
                "firm_inn": firm_inn,
                "ofd_inn": ofd_inn,
                "serial_numbers": serial_numbers
            }

            return call_http(method_url, json_data=json_data)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def register():
        with reporter.step(u'Пробиваем документы о регистрации'):
            method_url = '{base_url}/registration/register'.format(base_url=env.darkspirit_env().darkspirit_url)
            return call_http(method_url)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def update_registration(serial_numbers):
        with reporter.step(u'Обновляем данные о регистрации: {}'.format(serial_numbers)):
            method_url = '{base_url}/registration/update-registration'.format(
                base_url=env.darkspirit_env().darkspirit_url)

            json_data = {
                "serial_numbers": serial_numbers
            }

            return call_http(method_url, json_data=json_data)

    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def upload_ofd(serial_numbers):
        with reporter.step(u'Загружаем данные о регистрации в ОФД: {}'.format(serial_numbers)):
            method_url = '{base_url}/registration/upload-cash-registers-to-ofd'.format(
                base_url=env.darkspirit_env().darkspirit_url)

            json_data = {
                "serial_numbers": serial_numbers
            }

            return call_http(method_url, json_data=json_data)


class WhitespiritsSteps(object):
    @staticmethod
    @utils.CheckMode.result_matches(FAILURE_MATCHER)
    def create_or_update(url, version):
        with reporter.step(u'Регистрируем инстанс WS'):
            method_url = '{base_url}/whitespirits'.format(base_url=env.darkspirit_env().darkspirit_url)

            json_data = {
                "url": url,
                "version": version
            }

            return call_http(method_url, json_data=json_data)
