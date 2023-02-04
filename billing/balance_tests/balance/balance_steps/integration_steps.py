# coding=utf-8
__author__ = 'sfreest'

import uuid

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from btestlib.data import defaults


class CommonIntegrationSteps(object):
    DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT = {
        'create_integration': 1,
        'create_configuration': 1,
        'link_integration_to_client': 1,
        'set_integration_to_contract': 1,
    }

    @staticmethod
    def create_integration(params=None, passport_uid=defaults.PASSPORT_UID):
        integration_default_cc = unicode(uuid.uuid1())
        request_params = {
            'cc': integration_default_cc,
            'display_name': u'Отображаемое имя: {integration_default_cc}'
                .format(integration_default_cc=integration_default_cc),
        }
        if params:
            request_params.update(params)
        with reporter.step(
                u'Создаем интеграцию:\n'
                u'  cc: {request_params[cc]}\n'
                u'  display_name: {request_params[display_name]}'.format(request_params=request_params)):
            code, status = api.medium().CreateIntegration(passport_uid, request_params)
        return request_params, code, status

    @staticmethod
    def create_integrations_configuration(integration_cc, params=None, passport_uid=defaults.PASSPORT_UID):
        configuration_default_cc = unicode(uuid.uuid1())
        request_params = {
            'integration_cc': integration_cc,
            'cc': configuration_default_cc,
            'display_name': u'Отображаемое имя: {configuration_default_cc}'
                .format(configuration_default_cc=configuration_default_cc),
        }
        if params:
            request_params.update(params)
        with reporter.step(
                u'Создаем конфигурацию для интеграции {integration_cc}\n:'
                u'  cc: {request_params[cc]}\n'
                u'  display_name: {request_params[display_name]}\n'
                u'  scheme:\n'
                u'    {scheme}'.format(integration_cc=integration_cc, request_params=request_params,
                                       scheme=params['scheme'])):
            code, status = api.medium().CreateIntegrationsConfiguration(passport_uid, request_params)
        return request_params, code, status

    @staticmethod
    def link_integration_configuration_to_client(integration_cc, configuration_cc, client_id, passport_uid=defaults.PASSPORT_UID):
        request_params = {
            'integration_cc': integration_cc,
            'client_id': client_id,
            'configuration_cc': configuration_cc,
        }
        with reporter.step(
                u'Привязываем интеграцию {integration_cc} к клиенту {client_id}'
                        .format(integration_cc=integration_cc, client_id=client_id)):
            code, status = api.medium().LinkIntegrationToClient(passport_uid, request_params)
        return request_params, code, status
