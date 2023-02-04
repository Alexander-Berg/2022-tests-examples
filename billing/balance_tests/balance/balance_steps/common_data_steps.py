# coding=utf-8
__author__ = 'igogor'

import datetime
from decimal import Decimal

from dateutil.relativedelta import relativedelta

import btestlib.utils as utils
from btestlib.constants import Currencies, NdsNew, Nds, ActType

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class CommonData(object):
    @staticmethod
    def create_expected_act_data(amount, act_date, type=ActType.GENERIC, act_sum=None, addittional_params=None,
                                 context=None):
        if not act_sum:
            act_sum = amount

        act_data = {
            'type': type,
            'act_sum': act_sum,
            'amount': amount,
            'dt': act_date
        }

        if context is not None:
            act_data['amount_nds'] = utils.dround(
                amount / context.nds.koef_on_dt(act_date) * context.nds.pct_on_dt(act_date) / Decimal('100'),
                2
            )

        if addittional_params:
            act_data.update(addittional_params)

        return act_data

    @staticmethod
    def create_expected_act_data_with_contract(contract_id, amount, act_date, type, act_sum=None):
        expected_act_data = CommonData.create_expected_act_data(amount, act_date, type, act_sum)
        expected_act_data['contract_id'] = contract_id
        return expected_act_data

    @staticmethod
    def create_expected_invoice_data(contract_id, person_id, amount, invoice_type, paysys_id, firm,
                                     total_act_sum=None, currency=Currencies.RUB, nds=NdsNew.DEFAULT,
                                     dt=datetime.datetime.now()):

        nds_flag = 0 if nds.pct_on_dt(dt) == 0 and nds != NdsNew.ZERO else 1

        if total_act_sum is None:
            total_act_sum = amount

        return {
            'firm_id': firm.id,
            'type': invoice_type,
            'currency': currency.char_code,
            'paysys_id': paysys_id,

            'consume_sum': amount,
            'contract_id': contract_id,
            'person_id': person_id,
            'total_act_sum': total_act_sum,
            'nds_pct': 0 if nds.nds_id is None else nds.pct_on_dt(dt),
            'nds': nds_flag
        }

    @staticmethod
    def create_expected_invoice_data_by_context(context, contract_id, person_id, amount,
                                                total_act_sum=None, dt=datetime.datetime.now(),
                                                **additional_params):
        if total_act_sum is None:
            total_act_sum = amount

        expected_invoice_data = {
            'firm_id': context.firm.id,
            'type': context.invoice_type,
            'currency': context.currency.char_code,
            'paysys_id': context.paysys.id,

            'consume_sum': amount,
            'contract_id': contract_id,
            'person_id': person_id,
            'total_act_sum': total_act_sum,
            'nds_pct': context.nds.pct_on_dt(dt),
            'nds': 0 if context.nds.pct_on_dt(dt) == 0 and context.nds != NdsNew.ZERO and context.nds != NdsNew.ARMENIA else 1
        }

        expected_invoice_data.update(additional_params)

        return expected_invoice_data

    @staticmethod
    def create_expected_taxi_invoice_data(contract_id, person_id, amount, invoice_type, paysys_id, firm,
                                          total_act_sum=None, currency=Currencies.RUB, nds=NdsNew.DEFAULT,
                                          dt=datetime.datetime.now()
                                          ):

        nds_flag = 1 if nds.nds_id > 0 else 0
        # в Рымынии юр.лицо такси не платило НДС до 2020, 9, 1, тут костыль теперь, т.к. в счете НДС проставляется
        # на текущую дату
        if nds == NdsNew.ROMANIA and dt <= datetime.datetime(2020, 9, 1):
            dt = datetime.datetime(2020, 9, 1)

        if total_act_sum is None:
            total_act_sum = amount

        return {
            'firm_id': firm,
            'type': invoice_type,
            'currency': currency.char_code,
            'paysys_id': paysys_id,

            'consume_sum': amount,
            'contract_id': contract_id,
            'person_id': person_id,
            'total_act_sum': total_act_sum,
            'nds_pct': nds.pct_on_dt(dt),
            'nds': nds_flag
        }

    @staticmethod
    def create_expected_order_data(service_id, product_id, contract_id, consume_sum, completion_qty=None,
                                   consume_qty=None):
        if completion_qty is None:
            completion_qty = consume_sum

        if consume_qty is None:
            consume_qty = consume_sum

        return {
            'service_id': service_id,
            'service_code': product_id,
            'consume_sum': consume_sum,
            'consume_qty': consume_qty,
            'completion_qty': completion_qty,
            'contract_id': contract_id
        }

    @staticmethod
    def create_expected_consume_data(product_id, amount, invoice_type, current_qty=None, act_qty=None, act_sum=None,
                                     completion_qty=None, completion_sum=None):
        if current_qty is None:
            current_qty = amount

        if act_qty is None:
            act_qty = amount

        if act_sum is None:
            act_sum = amount

        if completion_qty is None:
            completion_qty = amount

        if completion_sum is None:
            completion_sum = amount

        return {'act_qty': act_qty,
                'act_sum': act_sum,
                'completion_qty': completion_qty,
                'completion_sum': completion_sum,
                'current_qty': current_qty,
                'service_code': product_id,
                'type': invoice_type}

    @staticmethod
    def create_expected_agent_report(contract_id, act_id, invoice_id, service, amount, dt, currency=Currencies.RUB):
        return {
            'service_id': service,
            'act_id': act_id,
            'act_qty': amount,
            'invoice_id': invoice_id,
            'act_amount': amount,
            'dt': dt,
            'currency': currency.char_code,
            'contract_id': contract_id
        }

    @staticmethod
    def create_expected_partner_act_data(client_id, contract_id, dt, description, page_id=None, partner_reward=None,
                                         currency=Currencies.RUB, nds=NdsNew.DEFAULT, place_id=None, place_type=None,
                                         act_reward=None, type_id=None, reference_currency=None,
                                         ref_partner_reward_wo_nds=None):
        return {
            'bucks': None,
            'clicks': None,
            'currency': currency.char_code,
            'description': description,
            'dt': utils.Date.first_day_of_month(dt),
            'end_dt': utils.Date.last_day_of_month(dt),
            'hits': None,
            'iso_currency': currency.iso_code,
            'nds': nds.pct_on_dt(dt),
            'owner_id': client_id,
            'page_id': page_id,
            'partner_contract_id': contract_id,
            'partner_reward': None,
            'partner_reward_wo_nds': partner_reward,
            'act_reward': None,
            'act_reward_wo_nds': act_reward,
            'place_id': place_id,
            'place_type': place_type,
            'product_price': None,
            'shows': None,
            'tag_id': None,
            'turnover': None,
            'type_id': type_id,
            'product_id': None,
            'reference_currency': reference_currency.char_code if reference_currency else None,
            'ref_partner_reward_wo_nds': ref_partner_reward_wo_nds
        }

    @staticmethod
    # в dt передавать первый месяц квартала в случае периода выплат = раз в квартал
    def create_expected_pad(context, client_id, contract_id, dt, partner_reward=None,
                            nds=NdsNew.DEFAULT, payment_type=1, **additional_params):
        if payment_type == 2:  # если период выплат = раз в квартал
            end_dt = utils.Date.last_day_of_month(dt + relativedelta(months=2))
        else:
            end_dt = utils.Date.last_day_of_month(dt)
        expected_data = {
            'bucks': None,
            'clicks': None,
            'currency': context.currency.char_code,
            'description': context.pad_description,
            'dt': utils.Date.first_day_of_month(dt),
            'end_dt': end_dt,
            'hits': None,
            'iso_currency': context.currency.iso_code,
            'nds': nds.pct_on_dt(dt),
            'owner_id': client_id,
            'page_id': context.page_id,
            'partner_contract_id': contract_id,
            'partner_reward': None,
            'partner_reward_wo_nds': partner_reward,
            'act_reward': None,
            'act_reward_wo_nds': None, #act_reward,
            'place_id': None, #place_id,
            'place_type': None, #place_type,
            'product_price': None,
            'shows': None,
            'tag_id': None,
            'turnover': None,
            'type_id': context.pad_type_id,
            'product_id': None,
            'reference_currency': None, #reference_currency.char_code if reference_currency else None,
            'ref_partner_reward_wo_nds': None, #ref_partner_reward_wo_nds
        }
        expected_data.update(additional_params)
        return expected_data
