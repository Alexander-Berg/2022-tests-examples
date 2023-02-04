# coding=utf-8
__author__ = 'igogor'

import collections
import datetime
import urlparse

import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Users, Services, Firms, Products
from btestlib.data import defaults

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class RequestSteps(object):
    # TODO params???
    @staticmethod
    def create(client_id, orders_list, additional_params=None,
               passport_uid=defaults.PASSPORT_UID):  # TODO для реквеста с несколькими заказами
        # to define invoice dt set additional_params['InvoiceDesireDT']
        with reporter.step(
                u'Создаём недовыставленный счёт на заказ(ы): {0} клиента (client_id: {1}), счета будут выставляться {2}'.format(
                    (', '.join('{0}-{1}, QTY: {2}'.format(order['ServiceID'], order['ServiceOrderID'], order['Qty'])
                               for order in orders_list)),
                    client_id,
                    u' на текущее время' if not additional_params or additional_params.get('InvoiceDesireDT',
                                                                                           0) == 0 else u'на дату ' + str(
                        additional_params['InvoiceDesireDT']))):
            # (', '.join('{0}={1}'.format(k, v) for k, v in dict.items())), result[0]['id']))
            result = api.medium().CreateRequest(passport_uid, client_id, orders_list, additional_params)
            request_id = utils.get_url_parameter(result[3], param='request_id')[0]

            reporter.attach(u'request_id', request_id)
            req_url = '{base_url}/paypreview.xml?request_id={request_id}'.format(base_url=env.balance_env().balance_ci,
                                                                                 request_id=request_id)
            reporter.report_url(u'Ссылка на реквест', req_url)
            # log.debug(('{0:<' + str(log_align) + '} | {1}, {2}, {3}, {4}').format("Request_id: {0}".format(request_id),
            #                                                                       req_url,
            #                                                                       "'Owner': {0}".format(client_id),
            #                                                                       orders_list,
            #                                                                       additional_params))

            return int(request_id)

    @staticmethod
    def get_url(request_id):
        return '{base_url}/paypreview.xml?request_id={request_id}'.format(base_url=env.balance_env().balance_ci,
                                                                          request_id=request_id)

    @staticmethod
    def create2(client_id, orders_list, additional_params=None, passport_uid=defaults.PASSPORT_UID):
        result = api.medium().CreateRequest2(passport_uid, client_id, orders_list, additional_params)
        request_id = result['RequestID']
        # reporter.log(('{0:<' + str(log_align) + '} | {1}, {2}, {3}, {4}').format("Request_id: {0}".format(request_id),
        #                                                                          result['AdminPath'],
        #                                                                          "'Owner': {0}".format(client_id),
        #                                                                          orders_list,
        #                                                                          additional_params))
        # reporter.log(('{0:<' + str(log_align) + '} | CI {1}').format("Request_id: {0}".format(request_id),
        #                                                              result['UserPath']))
        reporter.attach(u'request_id', request_id)
        reporter.report_url(u'Реквест в админе', result['AdminPath'])
        reporter.report_url(u'Реквест в клиенте', result['UserPath'])

        return request_id

    @staticmethod
    def create_from_shop(client_id, firm_id=Firms.YANDEX_1.id, orders_list=None):
        order_defaults = {'product_id': Products.DIRECT_FISH.id, 'qty': 10, 'discount': 0}
        orders_list = orders_list or [order_defaults]
        orders_params_list = [utils.merge_dicts([order_defaults, order]) for order in orders_list]

        session = passport_steps.auth_session(user=Users.YB_ADM)
        create_request_url = urlparse.urljoin(env.balance_env().balance_ai, '/create-request.xml')
        params = {'client_id': unicode(client_id), 'firm_id': firm_id}
        orders_params_dict = {}
        for order_num, order in enumerate(orders_params_list):
            num = order_num + 1
            orders_params_dict['id_{}'.format(num)] = order.get('product_id')
            orders_params_dict['quantity_{}'.format(num)] = order.get('qty')
            orders_params_dict['discount_{}'.format(num)] = order.get('discount')
            orders_params_dict['memo_{}'.format(num)] = 'Py_Test order {}-{}'.format(Services.SHOP.id,
                                                                                     order.get('product_id'))
            orders_params_dict['order_client_id_{}'.format(num)] = client_id
        params.update(orders_params_dict)
        response = utils.call_http(session, create_request_url, params)
        return utils.get_url_parameter(response.url, param='request_id')[0]

    @staticmethod
    def get_request_choices(request_id, passport_uid=defaults.PASSPORT_UID, show_disabled_paysyses=False,
                            person_id=None):
        params = {'RequestID': request_id, 'OperatorUid': passport_uid, 'ShowDisabledPaysyses': show_disabled_paysyses}
        if person_id:
            params.update({'PersonID': person_id})
        result = api.medium().GetRequestChoices(params)
        return result

    @staticmethod
    def format_request_choices(request_choices):
        def rec_dd():
            return collections.defaultdict(rec_dd)

        def ddict2dict(d):
            for k, v in d.items():
                if isinstance(v, dict):
                    d[k] = ddict2dict(v)
                if isinstance(v, list):
                    d[k] = sorted(v)
            return dict(d)

        def format_collateral_attrs(collateral_attrs):
            result = collections.defaultdict()
            for attr in collateral_attrs:
                if attr['code'] in ['FIRM', 'CURRENCY']:
                    result[attr['code'].lower()] = attr['value_num']
            return result

        result = rec_dd()
        pcp_list = request_choices['pcp_list']
        paysyses_wo_contract_list = []
        for pcp in pcp_list:
            contract = pcp['contract']
            paysyses_list = pcp['paysyses']
            if contract is None:
                paysyses_wo_contract_list.extend([paysys['id'] for paysys in paysyses_list])
            else:
                contract_id = contract['id']
                person_id = db.get_contract_by_id(contract_id)[0]['person_id']
                batch_id = db.get_collaterals_by_contract(contract_id)[0]['attribute_batch_id']
                collateral_attrs = db.get_attributes_by_batch_id(batch_id)
                collateral_attrs_dict = format_collateral_attrs(collateral_attrs)
                collateral_attrs_dict['currency'] = db.get_currency(collateral_attrs_dict['currency'])[0]['char_code']
                collateral_attrs_dict['category'] = db.get_person_by_id(person_id)[0]['type']
                result['with_contract'][str(collateral_attrs_dict['firm'])][collateral_attrs_dict['currency']][
                    collateral_attrs_dict['category']] = [paysys['id'] for paysys in paysyses_list]

        if paysyses_wo_contract_list:
            query = '''SELECT id, category, currency, firm_id
                        FROM t_paysys
                        WHERE t_paysys.id IN ({0}) ORDER BY id'''
            query_format = ', '.join([str(paysys) for paysys in paysyses_wo_contract_list])
            paysyses_by_ids = db.balance().execute(query.format(query_format))

            for paysys in paysyses_by_ids:
                if not result['without_contract'][str(paysys['firm_id'])][paysys['currency']][paysys['category']]:
                    result['without_contract'][str(paysys['firm_id'])][paysys['currency']][paysys['category']] = [
                        paysys['id']]
                else:
                    result['without_contract'][str(paysys['firm_id'])][paysys['currency']][paysys['category']].append(
                        paysys['id'])
        # избавляемся от defaultdict
        result = ddict2dict(result)
        return result
