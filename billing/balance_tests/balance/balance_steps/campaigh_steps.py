# coding=utf-8
__author__ = 'igogor'

import datetime

from decimal import Decimal
import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class CampaignsSteps(object):
    @staticmethod
    def do_campaigns(service_id, service_order_id, campaigns_params, do_stop=0, campaigns_dt=None):
        '''
        Sent fair campaigns to order
        '''
        # todo можно генерить список из ProductTypes
        unit_names = ['Bucks', 'Shows', 'Clicks', 'Units', 'Days', 'Money']
        campaigns_defaults = dict.fromkeys(unit_names, 0)
        params = campaigns_defaults.copy()
        params.update(campaigns_params)
        params.update({'service_id': service_id, 'service_order_id': service_order_id, 'do_stop': do_stop})
        with reporter.step(u'Откручиваем на заказе {0} {1} на {2}'.format(
                '{0}-{1}'.format(params['service_id'], params['service_order_id']),
                ', '.join(u'в {0}, количество {1}'.format(k, v) for k, v in params.iteritems() if
                          params[k] != 0 and k in unit_names),
                u'текущую дату' if campaigns_dt == None else campaigns_dt)):
            # with reporter.step(u"Создаём заказ c параметрами: {0}".format(', '.join('{0}: {1}'.format(k, v) for k, v in main_params.items()))):
            if campaigns_dt:
                result = api.test_balance().OldCampaigns(params, campaigns_dt)
            else:
                result = api.test_balance().Campaigns(params)

            # reporter.log(('{0:<' + str(log_align) + '} | {1}, {2}').format('do_campaigns: done', params,
            #                                                                "CampaignsDT: {0}".format(campaigns_dt)))
            return result

    @staticmethod
    def add_campaigns(service_id, service_order_id, campaigns_params={}, do_stop=0, campaigns_dt=None):
        '''
        Sent some additional campaigns to order
        '''
        current_campaigns = CampaignsSteps.get_campaigns(service_id, service_order_id)
        for key, value in campaigns_params.iteritems():
            if value and current_campaigns:
                campaigns_params[key] = float(Decimal(value) + current_campaigns)
        return CampaignsSteps.do_campaigns(service_id, service_order_id, campaigns_params, do_stop, campaigns_dt)

    @staticmethod
    def get_campaigns(service_id, service_order_id):
        '''
        Returns completion_qty value for order
        '''
        result = db.balance().execute(
            'SELECT completion_qty FROM t_order'
            '  WHERE service_id = :service_id AND service_order_id = :service_order_id',
            {'service_id': service_id, 'service_order_id': service_order_id},
            single_row=True, fail_empty=True)
        return Decimal(result['completion_qty'])

    @staticmethod
    def update_campaigns(service_id, service_order_id, campaign_params={}, do_stop=0, campaigns_dt=None):
        with reporter.step(u'Делаем открутки для заказа {}-{}'.format(service_id, service_order_id)):
            if not campaigns_dt:
                campaigns_dt = datetime.datetime.now()
            unit_names = ['Bucks', 'Shows', 'Clicks', 'Units', 'Days', 'Money']
            campaigns_defaults = dict.fromkeys(unit_names, 0)
            params = campaigns_defaults.copy()
            params.update(campaign_params)
            params.update({'dt': campaigns_dt})
            params.update({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'stop': do_stop})
            # reporter.log(params)
            result = api.medium().UpdateCampaigns([params])
            # reporter.log(result)
