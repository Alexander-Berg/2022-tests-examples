# -*- coding: utf-8 -*-
import datetime
import decimal
import logging
import os
import pickle
import traceback
import time
import json
from collections import OrderedDict

import sqlalchemy as sa
import sqlalchemy.orm as orm
from dateutil.relativedelta import relativedelta

from balance.actions.cashback import auto_charge_client
from butils import rpcutil

from balance import constants as cst
from balance import core
from balance import discounts
from balance import exc
from balance import mapper
from balance import muzzle_util as ut
from balance import overdraft
from balance import scheme
from balance import deco
from billing.contract_iface import ContractTypeId
from balance.actions import close_month, process_completions
from balance.actions.request import RequestAction
from balance.actions import DistrCacheUpdate, ExportState
from balance.actions import invoice_refunds as a_ir
from balance.actions import invoice_transfer as a_it
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions import single_account
from balance.actions import test_actions
from balance.actions.fetch_restricted_domains import fetch_restricted_domains
from balance.application import getApplication
from balance.deco import retry_on_db_error
from balance.invoker import BalanceLogicBase
from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.utils import path as path_util
from mailer.balance_mailer import MessageData
from medium import medium_logic
from notifier import data_objects as notifier_objects


snout_method = deco.snout_method
log = logging.getLogger('xmlrpc.logic')


# Если в словаре ключом будет не строка, то он не сможет
# быть сериализован в xml.
# TODO: Сделать нормальную сериализацию.
def keys2strs(d):
    result = {}
    for k, v in d.iteritems():
        if isinstance(v, dict):
            result[str(k)] = keys2strs(v)
        elif isinstance(v, unicode):
            result[str(k)] = v
        else:
            result[str(k)] = str(v)

    return result


class SimpleServiceEx(Exception):
    pass


class Logic(BalanceLogicBase):
    name = 'test_xmlrpc'

    def Ping(self):
        return True

    @rpcutil.call_description(rpcutil.arg_array(int))
    def UpdateContractCache(self, contract_ids):
        if len(contract_ids) == 0:
            raise exc.INVALID_PARAM('list of ids should not be empty')
        session = self._new_session()
        contracts = session.query(mapper.Contract)\
            .options(sa.orm.joinedload(mapper.Contract.collaterals))\
            .filter(mapper.Contract.id.in_(contract_ids))\
            .all()
        if len(contracts) < len(contract_ids):
            missing_contracts = set(contract_ids) - set(c.id for c in contracts)
            raise exc.INVALID_PARAM('contracts with id %s were not found' % (missing_contracts,))
        for contract in contracts:
            contract.on_before_flush()
        return 0

    # Метод устарел. После BALANCE-21828 дату можно передавать прямо в Balance.CreateRequest
    @rpcutil.call_description(
        int,
        int,
        rpcutil.arg_array(
            {
                ('ServiceID', 'EngineID'): int,
                ('ServiceOrderID', 'OrderID'): int,
                ('Price', 'Quantity', 'Qty'): rpcutil.arg_money(),
                'Discount': rpcutil.arg_money(default=decimal.Decimal(0)),
                'Rate': rpcutil.arg_money(default=decimal.Decimal(1)),
                'NDS': rpcutil.arg_int(default=1),
            }),
        rpcutil.arg_date,
        rpcutil.arg_variant(
            int,
            {
                'Overdraft': int,
                'PromoCode': str,
                'Region': str,
                'UiType': str,
                'ForceUnmoderated': int,
                'AdjustQty': int,
                'ReturnPath': str,
            }
        )
    )
    def CreateRequestOnDate(self, operator_uid, client_id, orders, desired_dt, props=None):
        overdraft = 0
        promo_code = None
        region = None
        ui_type = None
        force_unmoderated = 0
        adjust_qty = 0
        return_path = None
        if props.__class__ == int:
            overdraft = props
        elif props:
            overdraft = props.get('Overdraft', 0)
            promo_code = props.get('PromoCode', None)
            region = props.get('Region', None)
            ui_type = props.get('UiType', None)
            force_unmoderated = props.get('ForceUnmoderated', 0)
            adjust_qty = props.get('AdjustQty', 0)
            return_path = props.get('ReturnPath', None)
        log.info('overdraft=%(overdraft)s promo_code=%(promo_code)s '
                 'region=%(region)s ui_type=%(ui_type)s '
                 'force_unmoderated=%(force_unmoderated)s '
                 'adjust_qty=%(adjust_qty)s return_path=%(return_path)s' %
                 locals())
        session = self._new_session(operator_uid)
        (request_id, ref_service_id) = RequestAction.create(
            session,
            client_id, orders, desired_dt=desired_dt,
            overdraft=overdraft,
            promo_code=promo_code,
            force_unmoderated=force_unmoderated,
            adjust_qty=adjust_qty,
        )
        (user_path, admin_path) = path_util.get_paystep_url(
            self, request_id, ref_service_id, region, ui_type, return_path
        )
        return 0, 'SUCCESS', user_path, admin_path

    @rpcutil.call_description(
        rpcutil.arg_str,
        rpcutil.arg_struct(strict=False)
    )
    def CreateServiceProduct(self, service_token, params):
        def raise_error(error_type, code, desc):
            raise SimpleServiceEx(
                {
                    "status": "error",
                    "error_type": error_type,
                    "status_code": code,
                    "status_desc": desc,
                }
            )

        def invalid_request(code, desc):
            raise_error("invalid_request", code=code, desc=desc)

        session = self._new_session()
        service = session.query(mapper.Service).filter_by(token=service_token).one()
        service_id = service.id
        product_id = None
        products = (
            session.query(mapper.Product)
                .filter_by(engine_id=service_id, common=1, hidden=0)
                .all()
        )
        if len(products) not in (0, 1):
            raise_error("wrong_product_configuration", code='invalid_product',
                        desc='More than 1 product with "common=1"')

        if len(products) == 1:
            product_id = products[0].id
        service_product_eid = params["service_product_id"]
        name = params["name"]
        partner_id = int(params.get("partner_id") or 0) or None
        prices = params.get("prices")
        ymshop = params.get("ymshop")
        local_names = params.get("local_names")
        parent_service_product_eid = params.get("parent_service_product_id")
        product_type = params.get("product_type")
        package_name = params.get("package_name")
        inapp_name = params.get("inapp_name", params.get("sku"))
        single_purchase = params.get("single_purchase")
        subs_period = params.get("subs_period", "").upper()
        subs_trial_period = params.get("subs_trial_period", "").upper()
        subs_intro_period = params.get("subs_introductory_period", "").upper()
        subs_intro_period_prices = params.get("subs_introductory_period_prices")
        active_until_dt = params.get("active_until_dt")
        bonuses = params.get("bonuses")
        product_qty = params.get("product_qty")
        service_fee = params.get("service_fee")
        fiscal_title = params.get("fiscal_title")
        fiscal_nds = params.get("fiscal_nds")

        if bool(fiscal_nds) ^ bool(fiscal_title):
            invalid_request(
                "missing_fiscal_data",
                "fiscal_nds and fiscal_title should either both be present or None",
            )

        if product_type == "subs" and not subs_period:
            invalid_request(
                "missing_subs_period", "Subs product type requires subs_period"
            )

        if subs_trial_period and subs_intro_period:
            invalid_request(
                "invalid_sub_trial_period",
                "Should be only subs_trial_period or subs_introductory_period",
            )

        if (subs_intro_period or subs_intro_period_prices) and product_type != "subs":
            invalid_request(
                "invalid_product_type",
                "Only sub should have subs_introductory_period and subs_introductory_period_prices",
            )

        if subs_intro_period_prices and not subs_intro_period:
            invalid_request(
                "missing_subs_introductory_period",
                "Should have both subs_introductory_period and subs_introductory_period_prices",
            )

        if subs_intro_period and not subs_intro_period_prices:
            invalid_request(
                "missing_subs_introductory_period_prices",
                "Should have both subs_introductory_period and subs_introductory_period_prices",
            )

        if subs_intro_period and not single_purchase:
            invalid_request(
                "invalid_single_purchase",
                "Products with subs_introductory_period should be single_purchase",
            )

        def check_period_pattern(period):
            if period and not bool(re.match("^[0-9]+[YMWDS]$", period)):
                invalid_request(
                    "invalid_request", "Invalid period pattern: %s" % period
                )

        check_period_pattern(subs_period)
        check_period_pattern(subs_trial_period)
        check_period_pattern(subs_intro_period)

        if partner_id:
            # Check partner exists
            session.query(mapper.Client).getone(partner_id)

        service_product = (
            session.query(mapper.ServiceProduct)
                .filter_by(service_id=service_id)
                .filter_by(external_id=service_product_eid)
                .first()
        )

        if not service_product:
            service_product = mapper.ServiceProduct(
                service_id=service_id,
                external_id=service_product_eid,
                name=name,
                partner_id=partner_id,
                product_id=product_id,
                product_type=product_type,
                package_name=package_name,
                inapp_name=inapp_name,
                subs_period=subs_period,
                service_fee=service_fee,
                fiscal_title=fiscal_title,
                fiscal_nds=fiscal_nds,
            )
            if subs_intro_period:
                service_product.subs_intro_period = subs_intro_period
            session.add(service_product)
        else:
            if partner_id != service_product.partner_id:
                if service.via_oebs:
                    raise ut.INVALID_PARAM("partner_id: cannot be changed")
                thirdparty_service = session.query(mapper.ThirdPartyService).get(
                    service_id
                )
                if thirdparty_service:
                    any_payment_exists = (
                        session.query("1")
                            .select_from(mapper.Order)
                            .join(mapper.RequestOrder)
                            .join(mapper.Request)
                            .join(mapper.TrustPayment)
                            .filter(mapper.Order.service_product == service_product)
                            .exists()
                    )
                    if any_payment_exists:
                        invalid_request(
                            "already_has_payments",
                            "service product already has payments",
                        )
            service_product.name = name
            service_product.partner_id = partner_id
            service_product.package_name = package_name
            service_product.inapp_name = inapp_name
            if fiscal_title:
                service_product.fiscal_title = fiscal_title
            if fiscal_nds:
                service_product.fiscal_nds = fiscal_nds
            if service_product.product_id != product_id:
                invalid_request("invalid_product_id", "product_id can not be changed")
            service_product.service_fee = service_fee

        service_product.single_purchase = single_purchase
        if parent_service_product_eid:
            if package_name:
                invalid_request(
                    "invalid_package_name", "package_name not for sub-products"
                )
            parent_service_product = (
                session.query(mapper.ServiceProduct)
                    .filter_by(
                    service_id=service_id, external_id=parent_service_product_eid
                )
                    .one()
            )
            if parent_service_product.parent is not None:
                raise ut.INVALID_PARAM(
                    "service product %s has parent service product"
                    % parent_service_product.external_id
                )
            service_product.parent = parent_service_product
        else:
            if inapp_name:
                invalid_request(
                    "invalid_inapp_name", "inapp_name only for sub-products"
                )
            if product_type == "inapp":
                invalid_request(
                    "invalid_product_type", "inapp must have a parent product"
                )
            if service_product.parent is not None:  # warm up from lazyness
                service_product.parent = None

        if prices is not None:
            for p in prices:
                dt_ = p.get('dt')
                if dt_:
                    p['dt'] = datetime.datetime.fromtimestamp(dt_)
            service_product.set_service_prices(prices)

        if subs_intro_period_prices is not None:
            service_product.set_service_subs_intro_period_prices(
                subs_intro_period_prices
            )

        if ymshop is not None:
            service_product.set_product_shop(ymshop["shop_id"], ymshop["article_id"])

        if local_names is not None:
            service_product.local_names = [
                dict(local_name) for local_name in local_names
            ]

        if subs_trial_period:
            service_product.subs_trial_period = subs_trial_period

        if active_until_dt == "0":
            service_product.active_until_dt = None
        elif active_until_dt:
            service_product.active_until_dt = rpcutil.arg_date.type_instance(
                active_until_dt
            )

        session.flush()
        if bonuses:
            service_product.set_bonuses(bonuses)
        if service_product.service.signature_is_needed and not service_product.parent:
            gen_product_key = True
            if "_gen_product_key" in params:
                gen_product_key = int(params["_gen_product_key"]) != 0
            product_key = params.get("_product_key") or ""
            if gen_product_key:
                if product_key:
                    service_product.private_key = product_key
                else:
                    service_product.ensure_private_key_exists()

        if product_qty:
            service_product.set_qty(product_qty)

        return {"status": "success", "_internal_id": str(service_product.id)}

    @rpcutil.call_description(
        {
            'order_id': rpcutil.arg_int(mandatory=True),
            'paysys_code': rpcutil.arg_str(mandatory=True),
            'qty': rpcutil.arg_decimal(mandatory=True)
        }
    )
    def PayOrderCC(self, params):
        session = self._new_session()
        core_ = core.Core(session)
        with session.begin():
            core_.pay_order_cc(
                order_id=params['order_id'],
                paysys_cc=params['paysys_code'],
                sum=params['qty']
            )
        return 0

    ##
    # Включение счета.
    # Параметры передаются в хеше, где:
    #   'invoice_id' - id счета
    #   'sum' - сумма
    @rpcutil.call_description(
        {
            'invoice_id': rpcutil.arg_int(mandatory=True),
            'sum': rpcutil.arg_str(default=None)
        }
    )
    def TurnOn(self, param):
        session = self._new_session()
        with session.begin():
            invoice = session.query(mapper.Invoice).get(param['invoice_id'])
            if param['sum'] is None:
                InvoiceTurnOn(invoice, manual=True).do()
            else:
                InvoiceTurnOn(invoice, decimal.Decimal(param['sum']), manual=True).do()

            session.flush()
        return 0

    @rpcutil.call_description(
        rpcutil.arg_int(),
        rpcutil.arg_money(default=0, mandatory=False),
    )
    @snout_method('/assessor/invoice/make-payment', 'BALANCE-32158')
    def MakePayment(self, invoice_id, payment_sum=None):
        session = self._new_session()
        with session.begin():
            invoice = session.query(mapper.Invoice).getone(invoice_id)
            payment_sum = payment_sum or invoice.total_sum

            session.execute(
                """
                insert into t_correction_payment (dt, doc_date, sum, memo, invoice_eid)
                values (:dt, :dt, :total_sum, 'Testing', :external_id)
                """,
                {
                    'dt': datetime.datetime.now(),
                    'total_sum': decimal.Decimal(payment_sum),
                    'external_id': invoice.external_id,
                }
            )
        return 0

    @rpcutil.call_description(
        int,
    )
    def OEBSPayment(self, invoice_id):
        session = self._new_session()
        with session.begin():
            invoice = session.query(mapper.Invoice).getone(invoice_id)
            invoice.update_on_payment_or_patch()
            invoice.on_payment()
        return 0

    @rpcutil.call_description(
        rpcutil.arg_struct({
            'InvoiceID': rpcutil.arg_str(mandatory=True),
            'PaymentSum': rpcutil.arg_money(default=0, mandatory=False),
        }),
    )
    def MakeOEBSPayment(self, params):
        invoice_eid = params['InvoiceID']
        payment_sum = params.get('PaymentSum')

        session = self._new_session()
        invoice_id = session.query(mapper.Invoice.id).filter(mapper.Invoice.external_id == invoice_eid).scalar()

        self.MakePayment(invoice_id, payment_sum)
        self.OEBSPayment(invoice_id)

        return 0

    @rpcutil.call_description(
        rpcutil.arg_struct({
            'InvoiceID': rpcutil.arg_int(mandatory=True),
            'PaymentSum': rpcutil.arg_money(default=0, mandatory=False),
        }),
    )
    @deco.session_transaction
    @snout_method('/assessor/charge-note/pay', 'BALANCE-32709')
    def PayWithChargeNote(self, session, params):
        invoice_id = params['InvoiceID']
        invoice = session.query(mapper.Invoice).getone(invoice_id)
        payment_sum = params.get('PaymentSum')
        session.execute(
            """
            insert into t_correction_payment (dt, doc_date, sum, memo, invoice_eid)
            values (:dt, :dt, :total_sum, 'Testing', :external_id)
            """,
            {
                'dt': datetime.datetime.now(),
                'total_sum': decimal.Decimal(payment_sum),
                'external_id': invoice.external_id,
            }
        )
        InvoiceTurnOn(invoice, decimal.Decimal(payment_sum), manual=True).do()
        return 0



    @rpcutil.call_description(
        rpcutil.arg_struct({
            'ContractID': rpcutil.arg_int(mandatory=True),
        }),
    )
    def MakeOfferAccepted(self, params):
        session = self._new_session()

        contract = session.query(mapper.Contract).getone(params['ContractID'])
        current = contract.current_state()

        # ничего не делаем с не офертами, с офертами, не требующими активации, а также с уже актифированными офертами
        if (
                contract.type != 'GENERAL'
                or current.commission != ContractTypeId.OFFER
                or current.offer_confirmation_type != 'min-payment'
                or contract.offer_accepted
        ):
            return 0

        # собираем все ЛС договора
        pa_eid = []
        for service_code in mapper.ServiceCode.trackable_service_codes(session):
            try:
                personal_account = PersonalAccountManager(session) \
                    .for_contract(contract) \
                    .for_service_code(service_code) \
                    .get(auto_create=False)
            except exc.INVOICE_NOT_FOUND:
                pass
            else:
                pa_eid.append(personal_account.external_id)

        # ищем текущую сумму платежей для активации
        current_sum = session \
            .query(sa.func.nvl(sa.func.sum(mapper.OebsCashPaymentFact.amount), 0)) \
            .filter(mapper.OebsCashPaymentFact.operation_type.in_([cst.OebsOperationType.INSERT,
                                                                   cst.OebsOperationType.ONLINE]),
                    mapper.OebsCashPaymentFact.receipt_number.in_(pa_eid)) \
            .scalar()
        # и сколько не хватает
        delta = current.offer_activation_payment_amount - current_sum

        # создадим платёж на нехватающую сумму
        if delta > 0:
            # нам необходим хотя бы один ЛС, чтобы сделать по нему
            assert pa_eid, 'Contract id = %s has no personal accounts' % contract.id

            with session.begin():
                session.add(mapper.OebsCashPaymentFact(
                    amount=delta,
                    created_by=-1,
                    creation_date=session.now(),
                    last_updated_by=-1,
                    last_update_date=session.now(),
                    operation_type=cst.OebsOperationType.INSERT,
                    receipt_date=ut.trunc_date(session.now()),
                    receipt_number=pa_eid[0],  # ЛС подойдёт любой
                    source_id=sa.func.next_value(sa.Sequence('s_oebs_cpf_source_id_test')),
                    xxar_cash_fact_id=sa.func.next_value(sa.Sequence('s_oebs_cash_payment_fact_test')),
                ))

        return 0

    @rpcutil.call_description(
        {
            'service_id': rpcutil.arg_int(mandatory=True, alias='ServiceID'),
            'service_order_id': rpcutil.arg_int(mandatory=True, alias='ServiceOrderID'),
            'do_stop': rpcutil.arg_int(default=0, alias='Stop'),
            ('Days', 'Shows', 'Clicks', 'Units', 'Bucks'): rpcutil.arg_decimal(default=-1, group='shipment'),
            'Money': rpcutil.arg_decimal(default=0, group='shipment'),
            'use_current_shipment': rpcutil.arg_bool(default=False),
        }
    )
    @snout_method('/assessor/order/do-campaigns', 'BALANCE-32042')
    def Campaigns(self, param):
        session = self._new_session()
        use_current_shipment = param.pop('use_current_shipment')

        with session.begin():
            order = session.query(mapper.Order).filter_by(service_id=param['service_id'],
                                                          service_order_id=param['service_order_id']).one()

            if use_current_shipment:
                shipment = {k: getattr(order.shipment, k.lower()) for k in param.group('shipment').keys()}
            else:
                shipment = dict(param.group('shipment'))

            # order.calculate_consumption(dt=dt.datetime.today(), stop=param['do_stop'], shipment_info=shipment)
            order.calculate_consumption_fair(dt=datetime.datetime.today(), stop=param['do_stop'],
                                             shipment_info=shipment)

            session.flush()

        return 0

    @rpcutil.call_description(
        {
            'service_id': rpcutil.arg_int(mandatory=True, alias='ServiceID'),
            'service_order_id': rpcutil.arg_int(mandatory=True, alias='ServiceOrderID'),
            'do_stop': rpcutil.arg_int(default=0, alias='Stop'),
            ('Days', 'Shows', 'Clicks', 'Units', 'Bucks'): rpcutil.arg_decimal(default=-1, group='shipment'),
            'Money': rpcutil.arg_decimal(default=0, group='shipment'),
        }
    )
    def Campaigns2(self, param):
        session = self._new_session()

        shipment = dict((key, val) for key, val in param.group('shipment').iteritems())

        with session.begin():
            order = session.query(mapper.Order).filter_by(service_id=param['service_id'],
                                                          service_order_id=param['service_order_id']).one()
            pr_compl = process_completions.ProcessCompletions(order, on_dt=datetime.datetime.today())
            pr_compl.calculate_consumption(shipment, stop=param['do_stop'])
            session.flush()
        return 0

    @rpcutil.call_description(
        {
            'service_id': rpcutil.arg_int(mandatory=True, alias='ServiceID'),
            'service_order_id': rpcutil.arg_int(mandatory=True, alias='ServiceOrderID'),
            'do_stop': rpcutil.arg_int(default=0, alias='Stop'),
            ('Days', 'Shows', 'Clicks', 'Units', 'Bucks', 'Months'): rpcutil.arg_decimal(default=-1, group='shipment'),
            'Money': rpcutil.arg_decimal(default=0, group='shipment'),
        },
        rpcutil.arg_date,
    )
    def OldCampaigns(self, param, createDate):
        session = self._new_session()

        shipment = dict((key, val) for key, val in param.group('shipment').iteritems())

        with session.begin():
            order = session.query(mapper.Order).filter_by(service_id=param['service_id'],
                                                          service_order_id=param['service_order_id']).one()
            order.calculate_consumption_fair(dt=createDate, stop=param['do_stop'], shipment_info=shipment)

            session.flush()

        return 0

    @rpcutil.call_description(
        int,
    )
    def Act(self, invoice_id):
        session = self._new_session()

        with session.begin():
            acts = core.Core(session).generate_act(invoice_id, force=1, backdate=datetime.datetime.today())

        if acts:
            return [act.id for act in acts]
        else:
            return -1

    @rpcutil.call_description(
        int,
        rpcutil.arg_date,
    )
    def OldAct(self, invoice_id, createDate):
        session = self._new_session()

        with session.begin():
            acts = core.Core(session).generate_act(invoice_id, force=1, backdate=createDate)
        if acts:
            return [act.id for act in acts]
        else:
            return -1

    @rpcutil.call_description(
        rpcutil.arg_array(int),
    )
    def BanOverdraft(self, clients):
        session = self._new_session()
        overdraft.Overdraft(session).ban_clients(clients)
        return 0

    @rpcutil.call_description(
        rpcutil.arg_array(int),
    )
    def EnqueueOverdraft(self, client_ids):
        session = self._new_session()
        overdraft.enqueue_clients(session)
        return 0

    @rpcutil.call_description(
        rpcutil.arg_array(int)
    )
    def ManualSuspect(self, clients):
        session = self._new_session()
        overdraft.Overdraft(session).manual_suspect_worker(clients)
        return 0

    @rpcutil.call_description(
        rpcutil.arg_array(int),
    )
    def ResetOverdraftInvoices(self, clients):
        session = self._new_session()
        overdraft.Overdraft(session).reset_overdraft_invoices(clients)
        return 0

    @rpcutil.call_description(
        rpcutil.arg_array(int),
    )
    def RefundOrders(self, clients):
        session = self._new_session()
        overdraft.Overdraft(session).refund_orders(clients)
        return 0

    @rpcutil.call_description(
        int,
        rpcutil.arg_date,
        rpcutil.arg_date,
    )
    def GetPartnerReward(self, client_id, start_dt, end_dt):
        data = [r.split('\t') for r in self.GetPagesStat(start_dt,
                                                         end_dt).splitlines()]
        return [dict(zip(data[0], r)) for r in data[1:] if int(dict(zip(data[0],
                                                                        r))['CLIENT_ID']) == client_id]

    @rpcutil.call_description(
        int,
    )
    def HideAct(self, act_id):
        session = self._new_session()
        with session.begin():
            act = session.query(mapper.Act).getone(act_id)
            act.hide()
        return 0

    @rpcutil.call_description(
        int,
    )
    def UnhideAct(self, act_id):
        session = self._new_session()
        with session.begin():
            act = session.query(mapper.Act).getone(act_id)
            act.unhide()
        return 0

    # just run core.ActAccounter.generate_act() for our client
    @rpcutil.call_description(
        int,
        int,
        rpcutil.arg_date,
        rpcutil.arg_bool(default=False),
    )
    @snout_method('/assessor/act/generate', 'BALANCE-32633')
    def ActAccounter(self, client_id, force_value, mnth, with_coverage=False, split_act_creation=None):
        import balance.actions.acts as a_a

        session = self._new_session()

        if split_act_creation is None:
            split_act_creation = session.config.get('ACT_SPLIT_ACT_CREATION', False)

        cov = None
        if with_coverage:
            import balance.actions.acts.coverage_util as cov_util
            cov = cov_util.CovUtil()
            cov.start()

        with session.begin():
            client = session.query(mapper.Client).getone(client_id)
            if force_value:
                op_type = cst.OperationTypeIDs.enqueue_monthly_acts
            else:
                op_type = cst.OperationTypeIDs.enqueue_daily_acts
            operation = mapper.Operation(op_type)
            session.add(operation)
            session.flush()
            act_accounter = a_a.ActAccounter(client, a_a.ActMonth(for_month=mnth), force=force_value,
                                             enq_operation_id=operation.id,
                                             split_act_creation=split_act_creation)
            res = act_accounter.do()
            if with_coverage:
                cov.stop_get_data()
                if act_accounter.operation:
                    act_accounter.operation.coverage = cov.mk_op_data()

        return [act.id for act in res]

    def ActEnqueuer(self, client_ids, dt=None, force=1):
        session = self._new_session()
        act_month = mapper.ActMonth(for_month=dt or datetime.datetime.today())

        import balance.actions.acts as a_a

        op_type = cst.OperationTypeIDs.enqueue_monthly_acts if force else cst.OperationTypeIDs.enqueue_daily_acts
        act_enq = a_a.ActEnqueuer(session, act_month, force=force, client_ids=client_ids, op_type=op_type)
        act_enq.enqueue_acts()

    @rpcutil.call_description(
        rpcutil.arg_date,
    )
    def CloseFirms(self, mnth):
        session = self._new_session()
        import balance.actions.acts as a_a
        with session.begin():
            close_month.MonthCloser(session, a_a.ActMonth(for_month=mnth)).close_firms()
        return 0

    def ClearGEOFF(self):
        from balance.actions import linked_geo
        session = self._new_session()
        linked_geo.LinkedGeoFreefundsCleaner().do(session)

    @rpcutil.call_description(
        int,
        rpcutil.arg_date,
    )
    def CalcDsp(self, contract_id, month_dt, service_id):
        from balance.reverse_partners import ReversePartnerCalc
        session = self._new_session()
        import balance.actions.acts as a_a
        month = a_a.ActMonth(for_month=month_dt)
        c = session.query(mapper.Contract).get(contract_id)
        with session.begin():
            ReversePartnerCalc(c, service_id, month).process_and_enqueue_act()

    @rpcutil.call_description(
        int,
        rpcutil.arg_date,
        rpcutil.arg_bool(default=False),
    )
    def UpdateDistributionBudget(self, contract_id, month_dt):
        from balance.actions.acts import ActMonth

        session = self._new_session()
        DistrCacheUpdate().do(
            session,
            month=ActMonth(for_month=month_dt),
            contract_id=contract_id)

    @rpcutil.call_description(
        int,
        rpcutil.arg_date,

    )
    def GeneratePartnerAct(self, contract_id, month_dt):
        from cluster_tools import generate_partner_acts as gpa
        session = self._new_session()

        from balance.actions.acts import ActMonth
        contract = session.query(mapper.Contract) \
            .get(contract_id)

        with session.begin():
            am = ActMonth(for_month=month_dt)
            generator = gpa.get_generator(contract, act_month=am)
            generator.generate(am)

    @rpcutil.call_description(
        rpcutil.arg_date,
    )
    def GeneratePlusActs(self, month_dt):
        from cluster_tools import generate_partner_acts as gpa
        session = self._new_session()

        from balance.actions.acts import ActMonth
        with session.begin():
            am = ActMonth(for_month=month_dt)
            gpa.generate_plus_2_0_acts(session, am)

    # return test sequence name depending on service id
    @rpcutil.call_description(
        int,
    )
    def GetTestSequenceNameForService(self, service_id):
        if service_id == 7:
            seq_name = 'S_TEST_SERVICE_ORDER_ID_7'
        elif service_id == 116:
            seq_name = 'S_TEST_SERVICE_ORDER_ID_116'
        elif service_id == 97:
            seq_name = 'S_TEST_SERVICE_ORDER_ID_97'
        else:
            seq_name = 'S_TEST_SERVICE_ORDER_ID'

        return seq_name

    # Метод используется для синхронизации значений service_order_id после переналивки БД сервиса XX
    # (чтобы их новые заказы не пересекались с нашими тестовыми заказми и заказми сделанными ими вчера)
    # TESTBALANCE-348 + изменения BALANCE-24324
    def GetMaxServiceOrderID(self, service_id):
        seq_name = self.GetTestSequenceNameForService(service_id)
        session = self._new_session()

        if service_id == 7:
            with session.begin():
                sql_seq_max_value = 'select nvl(max(max_value), 0) from user_sequences where sequence_name = :seq_name'
                seq_max_value = session.execute(sql_seq_max_value, {'seq_name': seq_name}).fetchone()[0]
                # ищем первый свободный диапазон размера > empty_range_size и service_order_id > sequence.max_value
                sql = '''select s_o_id, next_s_o_id, (next_s_o_id - s_o_id) as diff
                         from (select
                                 lag(service_order_id, 1, :seq_max_value) over (partition by service_id order by service_order_id) as s_o_id,
                                 service_order_id as next_s_o_id
                               from t_order
                               where service_id = :service_id
                               and service_order_id > :seq_max_value
                               order by SERVICE_ORDER_ID)
                         where (next_s_o_id - s_o_id) > :empty_range_size
                         and rownum = 1'''
                res = session.execute(sql, {'service_id': service_id, 'seq_max_value': seq_max_value,
                                            'empty_range_size': 1000000}).fetchone()
                # если ничего не нашлось, вернем просто max(service_order_id) > sequence.max_value
                if not res:
                    sql = '''select nvl(max(service_order_id), :seq_max_value)
                             from t_order
                             where service_id = :service_id
                             and id != 2336199
                             and service_order_id > :seq_max_value'''
                    res = session.execute(sql, {'service_id': service_id, 'seq_max_value': seq_max_value}).fetchone()
            return long(res[0])
        else:
            sql = 'select nvl(max(service_order_id),0) from t_order where service_id = :service_id'
            with session.begin():
                sql_seq_min_value = 'select nvl(max(min_value), 0) from user_sequences where sequence_name = :seq_name'
                seq_min_value = session.execute(sql_seq_min_value, {'seq_name': seq_name}).fetchone()[0]
                if seq_min_value != 0:
                    sql += ' and service_order_id < {seq_min_value}'.format(seq_min_value=seq_min_value)
                res = session.execute(sql, {'service_id': service_id}).fetchone()
            return long(res[0])

    @rpcutil.call_description()
    def GetMaxClientID(self):
        seq_name = 'S_CLIENT_ID'
        session = self._new_session()
        with session.begin():
            sql = """select nvl(LAST_NUMBER, 0) from user_sequences where sequence_name = :seq_name"""
            res = session.execute(sql, {'seq_name': seq_name, }).fetchone()
        if res[0] == 0:
            raise exc.INVALID_PARAM('Sequence %s hasn\'t been created' % seq_name)
        return long(res[0])

    @rpcutil.call_description(int, )
    def UpdateClientID(self, number):
        seq_name = 'S_CLIENT_ID'
        session = self._new_session()
        if session.config.get('DISABLE_TESTXMLRPC_UPDATE_CLIENT_ID', None):
            raise exc.INVALID_PARAM('UpdateClientID is disabled. Contact Balance duty')
        with session.begin():
            sql = """select nvl(LAST_NUMBER, 0) from user_sequences where sequence_name = :seq_name"""
            res = session.execute(sql, {'seq_name': seq_name, }).fetchone()
        if res[0] == 0:
            raise exc.INVALID_PARAM('Sequence %s hasn\'t been created' % seq_name)
        if res[0] > number:
            raise exc.INVALID_PARAM('Cannot turn sequence %s back to %s, last_number %s' % (seq_name, number, res[0]))
        with session.begin():
            sql = """ALTER SEQUENCE %s INCREMENT BY %s""" % (seq_name, number - res[0] + 20)  # 20 - sequence cache
            session.execute(sql)
            sql = """SELECT %s.nextval FROM dual""" % seq_name
            num = session.execute(sql).fetchone()[0]
            log.debug('Got number %s', num)
            sql = """ALTER SEQUENCE %s INCREMENT BY 1""" % seq_name
            session.execute(sql)
        return num

    @rpcutil.call_description(
        rpcutil.arg_date,
        int,
        rpcutil.arg_array(int),
    )
    def UpdateLimits(self, mnth, force_value, client_ids):
        import balance.actions.acts as a_a

        # Если передали пустой спсиок, меняем его на None, потому что
        # MonthCloser проверяет на равенство None
        if len(client_ids) == 0:
            client_ids = None

        session = self._new_session()
        with session.begin():
            month_closer = close_month.MonthCloser(session, a_a.ActMonth(for_month=mnth), client_ids=client_ids)
            month_closer.update_limits()
        return 0

    @rpcutil.call_description(
        {
            'ClientID': rpcutil.arg_int(mandatory=True)
        }
    )
    @deco.session_transaction
    @snout_method('/assessor/client/migrate-to-els', 'BALANCE-32195')
    def MigrateClientToEls(self, session, params):
        els_start_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT

        client_id = params['ClientID']
        client = session.query(mapper.Client).getone(client_id)
        client.creation_dt = els_start_dt
        session.flush()
        output = single_account.prepare.process_client(client)

        if client.has_single_account:
            return client.single_account_number
        else:
            raise exc.INVALID_PARAM(output)

    @rpcutil.call_description(
        rpcutil.arg_array(rpcutil.arg_str())
    )
    def DCSRunCheckNew(self, check_args):
        check_args = list(check_args)
        log.debug(u'[dcs] Starting check with args: %s', ' '.join(check_args))

        try:
            from dcs.main import run_check
        except ImportError:
            import xmlrpclib

            import requests

            response = requests.post(
                ('%s/api/legacy/test_xmlrpc_proxy'
                 % getApplication().get_component_cfg('yb-dcs')['URL']),
                json=check_args, verify=cst.REQUESTS_SSL_CERTS
            )
            response.raise_for_status()
            response = response.json()
            if response['status'] == 'error':
                raise xmlrpclib.Fault(-1, response['value'])
            elif response['value'] is not None:
                return response['value']
        else:
            return run_check(check_args)

    @rpcutil.call_description(
        str,
        rpcutil.arg_date,
        str
    )
    def FindInLogByRegexp(self, log_file, last_date, regexp):
        import re
        from balance.utils.log_find_dt import binary_search_by_date
        max_read_size = 10000000  # magic limit from my head
        resp = {
            'lines': [],
            'status': 'success'
        }
        try:
            regex_object = re.compile(regexp)
            log_size = os.path.getsize(log_file)
        except OSError:
            resp['status'] = 'error'
            resp['status_desc'] = 'log file not found'
            return resp
        except re.error:
            resp['status'] = 'error'
            resp['status_desc'] = 'invalid regexp'
            return resp
        read_size = 0
        with open(log_file, 'r') as fd:
            offset = binary_search_by_date(fd, last_date, 0, log_size)
            fd.seek(offset)
            log.info('offset: %s', offset)
            for line in fd:
                if read_size >= max_read_size:
                    break
                if regex_object.match(line):
                    resp['lines'].append(line)
                read_size += len(line)
        return resp

    @rpcutil.call_description(
        str
    )
    def FindConfig(self, conf_file):
        max_read_size = 10000000  # magic limit from @buyvich head
        resp = {
            'lines': [],
            'status': 'success'
        }
        try:
            conf_size = os.path.getsize(conf_file)
        except OSError:
            resp['status'] = 'error'
            resp['status_desc'] = 'config file not found'
            return resp
        with open(conf_file, 'r') as fd:
            for line in fd:
                resp['lines'].append(line)
        return resp

    @rpcutil.call_description(
        int,
    )
    def CheckPartialConfigProcessCompletion(self, order_id):
        session = self._new_session()
        order = session.query(mapper.Order).getone(id=order_id)
        if not order:
            return 0

        session.config.load_partions_config('USE_PROCESS_COMPLETIONS_666', order=order)
        return long(session.config.is_use('USE_PROCESS_COMPLETIONS_666'))

    @rpcutil.call_description(
        rpcutil.arg_str(mandatory=True)
    )
    def GetConfigItem(self, item):
        '''
        Получение значения произвольного t_config.item
        :param item:
        :return:
        '''
        session = self._new_session()
        return session.config.get(key=item)

    def BalanceCommonVersion(self):
        import balance.version as v
        return v.__version__

    def ExecuteOEBS(self, firm_id, sql_query, sql_params=None):
        from balance.processors.oebs import get_firm_cursor
        session = self._new_session()
        firm = session.query(mapper.Firm).getone(firm_id)

        def strip_null_byte(value):
            if isinstance(value, basestring):
                value = value.strip('\00')
            return value

        with get_firm_cursor(firm, backend_id='oebs_qa') as (cursor, org_suffix):
            log.debug('Executing sql in OEBS: %r, params %r' % (sql_query, sql_params))
            con = cursor.execute(sql_query, sql_params or {})
            if con:
                raw_result = con.fetchall()
                description = zip(*con.description)[0]
                result = [{d.lower(): strip_null_byte(v) for d, v in zip(description, row)} for row in raw_result]
                return result
            else:
                return cursor.rowcount

    def ExecuteSQL(self, dbname, sql_query, sql_params=None, dict_list=True, key=None):
        session = self._new_session(database_id=dbname)
        return test_actions.execute_sql(session, sql_query, sql_params, dict_list, key)

    def GetNotification(self, opcode, object_id):
        session = self._new_session()
        res = []

        def int_to_str(value):
            if isinstance(value, (int, long)):
                value = str(value)
            return value

        for row in session.query(mapper.NotificationLog) \
                .filter_by(opcode=opcode) \
                .filter_by(object_id=object_id) \
                .order_by(mapper.NotificationLog.dt):
            info = {k: getattr(row, k) for k in row.get_props()}
            info['info'] = {k: int_to_str(w) for k, w in info.get('info', {}).iteritems()}
            res.append(info)
        return res

    def Enqueue(self, classname, object_id, type, rate=0,
                state=None, priority=0, input=None, next_export=None, enqueue_dt=None):
        """
        У метода неправильное название. На самом деле, он должен называться как-то так:
        UpdateOrInsertExport, потому что здесь можно установить любые значения колонкам
        в t_export.
        """
        session = self._new_session()
        input = pickle.dumps(input)

        if state is None:
            state = cst.ExportState.enqueued

        # TODO: Check also that next_export >= now, after migration to new fetch query
        assert state == cst.ExportState.enqueued or next_export is None, \
            'Cannot set next_export for state != %d' % cst.ExportState.enqueued

        if state == cst.ExportState.enqueued:
            enqueue_dt = enqueue_dt or datetime.datetime.now()

        cls = mapper.Exportable._exports.get(classname)
        if cls:
            priority = cls._fix_priority(priority, type)
        q_text = """
            merge into t_export exp
            using (select :object_id id from dual) objs
            on (objs.id = exp.object_id and exp.type = :type and exp.classname = :classname)
            when matched then
            update set
              state = :state, rate = :rate, priority = :priority,
              update_dt = sysdate, next_export = :next_export, input = nvl(:input, input),
              enqueue_dt = nvl(:enqueue_dt, enqueue_dt)
            when not matched then
            insert (classname, object_id, rate, state, type, priority, input, next_export, enqueue_dt)
            values (:classname, :object_id, :rate, :state, :type, :priority, :input, :next_export, :enqueue_dt)
        """
        q = sa.text(q_text, bindparams=[sa.bindparam('input', type_=sa.PickleType.impl)])
        with session.begin():
            session.execute(q, locals())

    def GetExportObject(self, queue_name, class_name, object_id):
        return self.ExportObject(queue_name, class_name, object_id, get_only=True)

    @snout_method('/assessor/export/export_object', 'BALANCE-31741')
    def ExportObject(self, queue_name, class_name, object_id,
                     priority=0, input_=None, next_export=None, get_only=False, retry_count=3, spoof_task=None):
        from balance.queue_processor import process_object, get_export_object, nil_transform

        export_obj = None
        session = self._new_session()
        if get_only:
            export_obj = get_export_object(session, queue_name, class_name, object_id)
            spoofer = None
        else:
            spoof_context, spoof_arg = nil_transform, None
            if spoof_task is not None:
                import oebs_spoof
                spoof_context, spoof_arg = oebs_spoof.spoof, spoof_task['patches']

            with spoof_context(spoof_arg) as spoofer:
                for i in range(retry_count):
                    export_obj = process_object(session, queue_name, class_name, object_id, priority, input_,
                                                next_export, object_transform=spoofer and spoofer.object_transform)
                    if export_obj.traceback is not None and (
                            exc.DEFER_LOCK_WITH_NOWAIT.__name__ in export_obj.traceback
                            or exc.DEFER_LOCKED_OBJECT.__name__ in export_obj.traceback
                    ):
                        log.debug("Got exception '{}' (attempt {}/{})".format(export_obj.error, i + 1, retry_count))
                        if i < retry_count - 1:
                            time.sleep(2 ** (i + 1))
                    else:
                        break

        response = {str(k): getattr(export_obj, k) for k in mapper.Export.__table__.c.keys()}
        response = keys2strs(response)
        if spoofer is not None:
            response['export_log'] = spoofer.transformed_result

        return response

    def ExportObjectWithSpoof(self, export_task, spoof_task, retry_count=3):
        queue_name = export_task['queue']
        class_name = export_task['class']
        object_id = export_task['object_id']
        priority = export_task.get('priority', 0)
        input_ = export_task.get('input', None)
        next_export = export_task.get('next_export', None)

        if queue_name != 'OEBS':
            raise ValueError('Can spoof only for OEBS')

        res = self.ExportObject(queue_name, class_name, object_id, priority=priority, input_=input_,
                                next_export=next_export, get_only=False, retry_count=retry_count,
                                spoof_task=spoof_task)

        if spoof_task.get('format_export_log', False):
            import oebs_spoof.sql_format
            res['export_log_formatted'] = oebs_spoof.sql_format.format(res['export_log'])

        return res

    def UATransfer(self, client_id, input_):
        from balance.actions.unified_account import handle_client
        session = self._new_session()
        client = session.query(mapper.Client).getone(client_id)
        return handle_client(client, input_)

    def UATransferQueue(self, client_ids=None, for_dt=None):
        import balance.actions.unified_account as a_ua
        session = self._new_session()
        for_dt = for_dt or (ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(seconds=1))
        a_ua.UnifiedAccountEnqueuer.enqueue(session, for_dt=for_dt, client_ids=client_ids)

    def AutoOverdraftEnqueue(self, service_id, person_id, for_dt=None):
        session = self._new_session()
        with session.begin():
            overdraft_param = (
                session.query(mapper.OverdraftParams)
                    .filter_by(person_id=person_id, service_id=service_id)
                    .one()
            )

            input_ = {'for_dt': for_dt} if for_dt else None
            overdraft_param.enqueue(mapper.OverdraftParams.AUTO_OVERDRAFT_EXPORT_TYPE, input_=input_)

    def PartnerActsEnqueue(self, params):
        from balance.actions import partners_enqueuer
        contract_ids = params['contract_ids']
        month = mapper.ActMonth(for_month=params['month'])
        priority = params.get('priority', 0)
        session = self._new_session()
        contracts = session. \
            query(mapper.Contract) \
            .filter(mapper.Contract.id.in_(contract_ids)) \
            .options(orm.load_only(mapper.Contract.id, mapper.Contract.client_id)) \
            .all()
        enqueuer = partners_enqueuer.PartnerActsEnqueuer(contracts, month, priority=priority)
        enqueuer.enqueue(session)

    def GetOfferRules(self):
        from balance import offer
        return offer.PARSER.rules

    @rpcutil.call_description(
        {
            'invoice_id': rpcutil.arg_int,
            'request_id': rpcutil.arg_int,
            'paysys_id': rpcutil.arg_int,
        }
    )
    def GetOffer(self, params):
        from balance import offer
        invoice_id = params.get('invoice_id')
        session = self._new_session()
        if invoice_id:
            invoice = session.query(mapper.Invoice).getone(invoice_id)
            return offer.from_invoice(invoice)
        else:
            request_id = params.get('request_id')
            paysys_id = params.get('paysys_id')
            request = session.query(mapper.Request).getone(request_id)
            paysys = session.query(mapper.Paysys).getone(paysys_id)
            return offer.process_rules(offer.PaystepNS(offer.RequestNS(request), paysys))

    @rpcutil.call_description(
        int,
    )
    def CacheBalance(self, contract_id):
        from balance.processors.cache_partner_balances import cache_balance
        session = self._new_session()
        with session.begin():
            cache_balance(session, contract_id)
        return 0

    @rpcutil.call_description(
        int,
    )
    def ProcessFastBalance(self, contract_id):
        from balance.processors.process_fast_balances import process_balance
        session = self._new_session()
        with session.begin():
            process_balance(session, contract_id)
        return 0

    @rpcutil.call_description(
        rpcutil.arg_str(mandatory=True),
        rpcutil.arg_str(mandatory=True),
        rpcutil.arg_str(mandatory=True))
    @deco.with_rosession
    def GetPartnerFastBalance(self, session, object_id, object_type, lb_topic):
        from balance.son_schema.partners import PartnerFastBalanceSchema
        fast_balance = session.query(mapper.PartnerFastBalance) \
            .filter_by(object_id=object_id,
                       object_type=object_type,
                       lb_topic=lb_topic) \
            .one_or_none()
        return PartnerFastBalanceSchema().dump(fast_balance).data

    def _processNgExportQueue(self, session, queue, object_ids):
        from balance.queue_processor_ng import QueueProcessorNg
        queue_processor_ng = QueueProcessorNg(queue)
        app = getApplication()
        return 0, 'SUCCESS', queue_processor_ng.process_batch(session, app, object_ids)

    @rpcutil.call_description(
        rpcutil.arg_str(mandatory=True),
        rpcutil.arg_array(rpcutil.arg_int, mandatory=False))
    @deco.with_rosession
    def RoProcessNgExportQueue(self, session, queue, object_ids=None):
        return self._processNgExportQueue(session, queue, object_ids)

    @rpcutil.call_description(
        rpcutil.arg_str(mandatory=True),
        rpcutil.arg_array(rpcutil.arg_int, mandatory=False))
    @deco.with_session
    def ProcessNgExportQueue(self, session, queue, object_ids=None):
        return self._processNgExportQueue(session, queue, object_ids)

    @rpcutil.call_description(
        rpcutil.arg_int(mandatory=True),
        rpcutil.arg_struct(mandatory=True))
    @deco.with_session
    def UpdateContractExtpropFields(self, session, contract_id, params):
        params = dict(params)
        with session.begin():
            contract = session.query(mapper.Contract).get(contract_id)
            for field in params:
                if field in contract.extprops_dict:
                    setattr(contract, field, params[field])


    @rpcutil.call_description({
        'start_dt': rpcutil.arg_date,
        'end_dt': rpcutil.arg_date,
        'completion_source': rpcutil.arg_str,
        'completion_filter': rpcutil.arg_str,
    })
    def GetPartnerCompletions(self, params):
        from balance.completions_fetcher.configurable_partner_completion import ProcessorFactory, CompletionConfig
        session = self._new_session()
        start_dt, end_dt = params['start_dt'], params['end_dt']
        completion_filter = json.loads(params.get('completion_filter', '{}')) or None
        config = CompletionConfig(params['completion_source'], 'PARTNER_COMPL', session)
        processor = ProcessorFactory.get_instance(session, config, start_dt, end_dt,
                                                  completion_filter=completion_filter)
        processor.process(is_chained=False)

    @rpcutil.call_description({
        'start_dt': rpcutil.arg_date,
        'end_dt': rpcutil.arg_date,
        'completion_source': rpcutil.arg_str,
        'completion_filter': rpcutil.arg_str,
    })
    def GetEntityCompletions(self, params):
        session = self._new_session()
        from balance.completions_fetcher.partner_entity_completions import CompletionBase
        cbase = CompletionBase
        getter = cbase._comp_getters[params['completion_source']]

        start_dt = params['start_dt']

        completion_filter = json.loads(
            params.get('completion_filter', '{}'))

        if not completion_filter:
            completion_filter = None

        comp_getter = getter(session, start_dt)
        comp_getter.cluster_get(False)

    @rpcutil.call_description(
        rpcutil.arg_struct
    )
    def BatchThirdpartyTransactons(self, params):
        session = self._new_session()
        from cluster_tools.batch_thirdparty_transactions import BatchThirdparty
        from balance.batcher import BatchSet
        batch_tp = BatchThirdparty(session)
        tp_scheme = scheme.thirdparty_transactions

        where = sa.text('')
        for k, v in params.iteritems():
            if hasattr(v, '__iter__'):
                where = sa.and_(where, getattr(tp_scheme.c, k).in_(v))
            else:
                where = sa.and_(where, getattr(tp_scheme.c, k) == v)

        assert where != sa.text(''), "no where clause setted"

        session = self._new_session()
        bs = BatchSet(session=session,
                      scheme=tp_scheme,
                      additional_where=where)
        b_count = batch_tp.batch(bs)

        return 'OK, {} batches created'.format(b_count)

    def ResolveMncloseTask(self, task_name):
        from balance.mncloselib import get_mnclose, resolve_task
        mnclose = get_mnclose(self.cfg)
        resolve_task(mnclose, task_name)
        return 'OK, task {} resolved'.format(task_name)

    def GetHost(self):
        from balance import util
        return util.HOSTNAME

    @rpcutil.call_description(int)
    def GetEmailMessageData(self, message_id):
        session = self._new_session()
        email = session.query(mapper.EmailMessage).getone(message_id)
        msg = MessageData.from_message_mapper(email, cfg=getApplication().cfg, strict=False)
        response = {str(k): getattr(msg, k) for k in msg.__dict__}
        return response

    @rpcutil.call_description(
        rpcutil.arg_int,
        rpcutil.arg_int,
        rpcutil.arg_int,
        rpcutil.arg_int,
        rpcutil.arg_decimal,
        rpcutil.arg_decimal,
    )
    def TransferFromInvoice(self, invoice_id, service_id, service_order_id,
                            mode=cst.TransferMode.all, sum=None, discount_pct=0):
        """Трансфер средств с беззаказья.
           sum: сумма для перевода в фишках

        """
        sum = str(sum) if sum is not None else None
        if not 0 <= discount_pct <= 100:
            raise exc.INVALID_PARAM('invalid discount for a transfer')

        session = self._new_session()
        with session.begin():

            if mode not in (cst.TransferMode.all, cst.TransferMode.src):
                raise exc.INVALID_PARAM('invalid mode for a transfer')

            invoice = session.query(mapper.Invoice).with_lockmode('update').getone(invoice_id)
            dst_order = (
                session.query(mapper.Order)
                    .with_lockmode('update')
                    .getone(service_id=service_id, service_order_id=service_order_id)
            )
            invoice.transfer(dst_order, mode=int(mode), sum=sum, discount_pct=discount_pct)

        return 'OK, transfer unused fund in invoice {} to order {} done'.format(invoice_id, dst_order.id)

    # noinspection PyBroadException
    @rpcutil.call_description(
        rpcutil.arg_array({
            'ObjectID': rpcutil.arg_int(),
            'ObjectType': rpcutil.arg_str(),
        }),
    )
    def MigrateToBatchStructure(self, objects):
        result = []
        session = self._new_session()

        for obj_data in objects:
            error = None
            migration_status = None
            obj_id = obj_data['ObjectID']
            obj_type = obj_data['ObjectType']

            try:
                if obj_type.lower() == 'contractcollateral':
                    model = mapper.ContractCollateral
                elif obj_type.lower() == 'person':
                    model = mapper.Person
                else:
                    raise ValueError('ObjectType should be either ContractCollateral or Person, not %s' % obj_type)

                obj = session.query(model).getone(obj_id)
                with session.begin():
                    migration_status = obj.migrate_new_structure()
            except Exception:
                error = traceback.format_exc()

            if error is not None:
                status = error
            elif migration_status is True:
                status = 'object has been successfully migrated'
            else:
                status = 'object was migrated before'

            result.append({
                'ObjectID': obj_id,
                'ObjectType': obj_type,
                'Status': status,
            })

        return result

    @rpcutil.call_description(
        rpcutil.arg_int,
        rpcutil.arg_int,
    )
    @snout_method('/assessor/notification/info', 'BALANCE-31208')
    def GetNotificationInfo(self, opcode, object_id):
        """Возвращает информацию по нотификации
        """
        session = self._new_session()
        info = notifier_objects.BaseInfo.get_notification_info(session, opcode, object_id)[1]
        return info

    @rpcutil.call_description(
        rpcutil.arg_int,
    )
    def CreatePersonalAccounts(self, contract_id):
        session = self._new_session()
        with session.begin():
            from balance import contractpage
            cp = contractpage.ContractPage(session, contract_id)
            cp.create_personal_accounts()

        return 'OK'

    @rpcutil.call_description(
        rpcutil.arg_int,
    )
    def CreateBarcode(self, collateral_id):
        session = self._new_session()
        session_oebs = self._new_session(database_id='oebs')

        query = 'select XXCMN_INTAPI_CONTRACTS_PKG.get_barcode(%s) from dual'

        collateral = session.query(mapper.ContractCollateral).getone(collateral_id)

        with session.begin():
            oebs_barcode, = session_oebs.execute(query % collateral.id).fetchone()
            if oebs_barcode:
                collateral.print_tpl_barcode = oebs_barcode  # записываем значение из ОЕБС
                collateral._do_not_export = True  # экспортировать нет смысла
                log.info('Barcode for collateral id = %s is overridden with value %s from OEBS'
                         % (collateral.id, oebs_barcode))
            else:
                # новое значение из последовательности
                collateral.print_tpl_barcode = sa.func.next_value(sa.Sequence('s_barcode_values'))

        return 'OK'

    @rpcutil.call_description(
        rpcutil.arg_int,
        rpcutil.arg_struct,
    )
    def EnqueuePrintFormEmail(self, passport_id, params):
        from balance.publisher.wiki_handler import WikiHandler
        session = self._new_session(passport_id)
        objects = [(params['object_type'], params['object_id'])]
        extra_data = params.get('extra_data')
        with session.begin():
            res = WikiHandler.enqueue_print_form_email(session, objects, extra_data)
        return res

    @rpcutil.call_description(
        rpcutil.arg_int,
    )
    def GetBarcodeOEBS(self, collateral_id):
        session_oebs = self._new_session(database_id='oebs')

        query = 'select XXCMN_INTAPI_CONTRACTS_PKG.get_barcode(%s) from dual'
        oebs_barcode, = session_oebs.execute(query % collateral_id).fetchone()

        return oebs_barcode

    @rpcutil.call_description(
        rpcutil.arg_str,
        rpcutil.arg_bool,
        rpcutil.arg_str
    )
    def SetPassportAdmsubscribe(self, login, subscribe, subscription):
        # низкоуровневый аналог butils.passport.passport_admsubscribe
        import requests
        from butils.passport import PassportCfg

        q = {
            'mode': 'admsubscribe',
            'login': login,
            'from': subscription
        }
        if not subscribe:
            q['unsubscribe'] = 'true'

        pass_cfg = PassportCfg().get_default_passport()
        internal_url = pass_cfg['InternalURL']
        timeout = float(pass_cfg['BlackBoxTimeout'])

        url = internal_url.strip('/') + '/passport'
        resp = requests.get(url, params=q, timeout=timeout, verify=False)
        resp.raise_for_status()
        return resp.content

    @rpcutil.call_description(
        {
            'invoice_id': rpcutil.arg_int(mandatory=True),
            'amount': rpcutil.arg_decimal,
            'unused_funds_lock': rpcutil.arg_int,
            'order_id': rpcutil.arg_int,
        }
    )
    def InvoiceRollback(self, params):
        from balance import core
        session = self._new_session()
        core_obj = core.Core(session)
        return core_obj.invoice_rollback(**params)

    def ProcessUncomposedPayments(self):
        from balance.compose_side_payments_registry import process_uncomposed_payments
        process_uncomposed_payments(self._new_session())

    @deco.with_session_transaction
    def CreateInvoiceWithDiscount(self, session, operator_uid, invoice_params, discounts_info=None):
        if discounts_info:
            discounts_info = list(discounts_info)
            discounts_info = discounts_info + [None] * (3 - len(discounts_info))
        old_func = discounts.calc_from_ns

        def patch_func(ns):
            return [
                discounts.DiscountProof('mock', discount=di[0], adjust_quantity=bool(di[1])) if di else None
                for di in discounts_info
            ]

        try:
            if discounts_info:
                discounts.calc_from_ns = patch_func
            invoices = core.Core(session, self.dbhelper).create_invoice(
                invoice_params['RequestID'],
                invoice_params['PaysysID'],
                invoice_params['PersonID'],
                credit=invoice_params['Credit'],
                contract_id=invoice_params['ContractID'],
                overdraft=invoice_params['Overdraft']
            )
        finally:
            discounts.calc_from_ns = old_func

        return invoices[0].id

    @rpcutil.call_description(
        rpcutil.arg_int,
        rpcutil.arg_service_id,
        rpcutil.arg_money,
    )
    @snout_method('/assessor/client/set-overdraft', 'BALANCE-31400')
    def SetClientOverdraft(self, client_id, service_id, overdraft_limit):
        session = self._new_session()
        client = session.query(mapper.Client).getone(client_id)

        firm_ids = [params.firm_id for params in mapper.ServiceFirmOverdraftParams.get(session, service_id)]

        if firm_ids:
            with session.begin():
                for firm_id in firm_ids:
                    iso_currency = client.get_overdraft_currency(service_id, firm_id)
                    for c in client.overdraft_brand_clients:
                        c.set_overdraft_limit(service_id, firm_id, overdraft_limit, iso_currency)

    @retry_on_db_error(code=8103, retry=3)
    @rpcutil.call_description(
        rpcutil.arg_int(),
        rpcutil.arg_date()
    )
    def RunPartnerCalculator(self, contract_id, month_dt):
        from balance.partner_calculator import ContractCalculator, ScaleCache
        ScaleCache().clear_cache()  # Singleton. Cache in tests may cause problems.
        cc = ContractCalculator(self._new_session(), contract_id, mapper.ActMonth(for_month=month_dt))
        cc.run()

    @rpcutil.call_description(
        int,  # client_id
    )
    def SingleAccountProcessClient(self, client_id):
        # TODO: switch to ExportObject when SINGLE_ACCOUNT queue will be enabled
        session = self._new_session()
        client = session.query(mapper.Client).getone(client_id)
        with session.begin():
            output = single_account.prepare.process_client(client)

        if client.has_single_account:
            return client.single_account_number
        else:
            raise exc.INVALID_PARAM(output)

    def CheckInvoiceRefundAvailability(self, passport_id, cpf_id, additional_requisites=cst.SENTINEL):
        session = self._new_session(passport_id)
        with session.begin():
            cpf = session.query(mapper.OebsCashPaymentFact).get(cpf_id)
            return a_ir.InvoiceRefundManager(cpf, additional_requisites).check_availability(strict=True)

    def CreateInvoiceRefund(self, passport_id, cpf_id, amount, payload=None):
        session = self._new_session(passport_id)
        with session.begin():
            cpf = session.query(mapper.OebsCashPaymentFact).get(cpf_id)
            ref = a_ir.InvoiceRefundManager(cpf, payload).create(amount)
            return ref.id

    def GetInvoiceRefundAvailableAmount(self, passport_id, cpf_id):
        session = self._new_session(passport_id)
        with session.begin():
            cpf = session.query(mapper.OebsCashPaymentFact).get(cpf_id)
            return a_ir.InvoiceRefundManager(cpf).get_refundable_amount(skip_availability_check=True)

    def CheckInvoiceRefundStatus(self, refund_id):
        from cluster_tools.invoice_refunds_status_check import process_batch

        session = self._new_session()
        with session.begin():
            process_batch(session, [refund_id])
            refund = session.query(mapper.InvoiceRefund).get(refund_id)
            return refund.status_code, refund.status_descr

    def CheckInvoiceTransferAvailability(self, passport_id, src_invoice_id, dst_invoice_id):
        session = self._new_session(passport_id)
        src_invoice = session.query(mapper.Invoice).get(src_invoice_id)
        dst_invoice = session.query(mapper.Invoice).get(dst_invoice_id)
        return a_it.InvoiceTransferManager(src_invoice, dst_invoice).check_availability(strict=True)

    def CreateInvoiceTransfer(self, passport_id, src_invoice_id, dst_invoice_id, amount, all_):
        session = self._new_session(passport_id)
        with session.begin():
            src_invoice = session.query(mapper.Invoice).get(src_invoice_id)
            dst_invoice = session.query(mapper.Invoice).get(dst_invoice_id)
            invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(amount, all_)
            return invoice_transfer.id

    def GetInvoiceTransferAvailableSum(self, passport_id, src_invoice_id):
        session = self._new_session(passport_id)
        src_invoice = session.query(mapper.Invoice).get(src_invoice_id)
        return a_it.InvoiceTransferManager(src_invoice, None).get_available_invoice_transfer_sum()

    def CheckInvoiceTransferStatus(self, passport_id, invoice_transfer_id):
        session = self._new_session(passport_id)
        invoice_transfer = session.query(mapper.InvoiceTransfer).get(invoice_transfer_id)
        return invoice_transfer.status_code

    def UnlockInvoiceTransfer(self, passport_id, invoice_transfer_id):
        session = self._new_session(passport_id)
        with session.begin():
            invoice_transfer = session.query(mapper.InvoiceTransfer).get(invoice_transfer_id)
            if invoice_transfer.check_unlock_allowed(strict=True):
                invoice_transfer.set_status(cst.InvoiceRefundStatus.failed_unlocked)
            return invoice_transfer.status_code

    @rpcutil.call_description(
        rpcutil.arg_int,
    )
    def MigrateContractToDecoupling(self, contract_id):
        session = self._new_session()
        with session.begin():
            import balance.constants as const
            import balance.reverse_partners as rp
            from balance.actions.unified_account import UnifiedAccount

            contract = session.query(mapper.Contract).getone(contract_id)
            cs = contract.current_signed()
            assert const.ServiceId.TAXI_CORP in cs.services
            assert const.ServiceId.TAXI_CORP_CLIENTS not in cs.services

            services = contract.col0.services
            assert const.ServiceId.TAXI_CORP in services
            assert const.ServiceId.TAXI_CORP_CLIENTS not in services

            services[const.ServiceId.TAXI_CORP_CLIENTS] = 1
            contract.col0.services = services
            session.flush()

            cs = contract.current_signed()
            assert const.ServiceId.TAXI_CORP in cs.services
            assert const.ServiceId.TAXI_CORP_CLIENTS in cs.services

            sid = rp.get_taxi_eff_sid(contract)
            assert sid == const.ServiceId.TAXI_CORP_CLIENTS

            rp.PartnersUnifiedAccountLinker(cs).link_unified_orders(sid)

            product = rp.get_product(sid, contract, ua_root=True)
            main_order = rp.get_order(sid, contract, product=product)

            ua = UnifiedAccount(session, main_order)
            ua.transfer2main()

        return 'OK'

    @rpcutil.call_description(
        rpcutil.arg_int(),
    )
    def KillStager(self, request_id):
        import os, signal
        from balance.processors.stager.launcher import launcher

        def check_kill_process(pstring):
            for line in os.popen("ps ax | grep " + pstring + " | grep -v grep"):
                fields = line.split()
                pid = fields[0]
                os.kill(int(pid), signal.SIGKILL)

        check_kill_process('request-id={}'.format(request_id))

        session = self._new_session()
        tasks = session.query(mapper.StagerTask).distinct(mapper.StagerTask.task_date).filter(
            mapper.StagerTask.request_id == request_id).all()

        with session.begin():
            for task in tasks:
                if task.status not in ('failed', 'success'):
                    launcher.log_task(session, task, 'killed')

    @rpcutil.call_description(
        rpcutil.arg_int(),
    )
    def CheckStager(self, request_id):
        session = self._new_session()

        task = session \
            .query(mapper.StagerTask) \
            .filter_by(request_id=request_id) \
            .order_by(mapper.StagerTask.dt.desc(),
                      mapper.StagerTask.task_id.desc()) \
            .limit(1) \
            .one()

        return {
            'status': task.status,
            'task_id': task.task_id,
            'date': task.task_date,
            'error': task.error,
            'nodes': task.nodes
        }

    @rpcutil.call_description(
        rpcutil.arg_str(),  # project
        rpcutil.arg_str(),  # start_dt
        rpcutil.arg_str(default=None),  # end_dt
        rpcutil.arg_str(default='{}'),  # custom paths json
        rpcutil.arg_str(default='hahn'),  # cluster
        rpcutil.arg_bool(default=False),  # wait?
    )
    def RunStager(self, project_name,
                  start_dt, end_dt=None,
                  custom_paths=None, proxy='hahn',
                  wait=False):
        try:
            import sys
            import threading, multiprocessing, subprocess
            from balance.processors.stager.lib import everyday

            custom_paths = custom_paths or '{}'
            end_dt = end_dt or start_dt
            session = self._new_session()

            seq = sa.Sequence('s_stager_req_id')
            request_id, = session.execute(seq.next_value()).fetchone()
            with session.begin():
                for day in everyday(start_dt, end_dt):
                    session.add(
                        mapper.StagerTask(
                            request_id=request_id,
                            project_name=project_name,
                            yt_cluster=proxy,
                            task_date=day,
                            status='init',
                            nodes=custom_paths,
                            dt=datetime.datetime.now()))

            if request_id:
                # since Application uses env config first, unset
                # for stager to be able pick its own file in application/stager.py
                clean_env = os.environ.copy()
                clean_env.pop('YANDEX_XML_CONFIG', None)
                proc = subprocess.Popen(
                    [
                        'yb-python',
                        '-pysupport',
                        'balance/processors/stager/launcher/launcher.py',
                        '--request-id={}'.format(request_id)
                    ],
                    close_fds=False, env=clean_env)

                if wait:
                    proc.wait()

                return {
                    'status': 'ok',
                    'request_id': request_id
                }
        except Exception as exc:
            traceback_ = traceback.format_exc()
            return {'status': 'error', 'exc': str(exc), 'traceback': str(traceback_)}
        return {'status': 'error'}

    @rpcutil.call_description(
        {
            'contract_eid': rpcutil.arg_str(alias='ContractEID', mandatory=True)
        }
    )
    def GetContractID(self, params):
        '''
        Возвращаем id всех существующих договоров по внешнему id
        :param params:
        :return:
        '''
        contract_eid = params.get('contract_eid')

        session = self._new_session()
        rows = session.query(mapper.Contract.id).filter(mapper.Contract.external_id == contract_eid).all()

        if not rows:
            raise exc.INVALID_PARAM('No contracts with external_id {}'.format(contract_eid))

        # Для одного external_id может возвращаться несколько id из-за:
        # - переездов договоров в БЮ \ другие фирмы
        # - продлений\перезаведений договоров с сохранением external_id
        return [row.id for row in rows]

    @rpcutil.call_description(
        {
            'act_id': rpcutil.arg_int(alias='ActID', mandatory=True)
        }
    )
    def GetAct(self, params):
        act_id = params.get('act_id')

        session = self._new_session()
        act = session.query(mapper.Act).getone(act_id)

        res = {x: getattr(act, x) for x in
               ('id', 'dt', 'external_id', 'client_id', 'payment_term_dt')}
        res['cancelled'] = int(act.hidden == 4)

        return res

    @rpcutil.call_description(
        {
            'invoice_id': rpcutil.arg_int(alias='InvoiceID', mandatory=True)
        }
    )
    def GetInvoice(self, params):
        invoice_id = params.get('invoice_id')

        session = self._new_session()
        inv = session.query(mapper.Invoice).getone(invoice_id)

        res = {x: getattr(inv, x) for x in ('id', 'dt', 'external_id', 'contract_id',
                                            'client_id', 'person_id', 'currency',
                                            'currency_rate', 'total_sum', 'payment_term_dt')}
        res['cancelled'] = int(inv.hidden == 2)

        return res

    @rpcutil.call_description(
        {
            'invoice_id': rpcutil.arg_int(alias='InvoiceID', mandatory=True),
            'payment_term_dt': rpcutil.arg_date(alias='PaymentTermDT', mandatory=True)
        }
    )
    @snout_method('/assessor/invoice/update-payment-term', 'BALANCE-31739')
    def UpdateInvoicePaymentTerm(self, params):
        '''
        :param params:
        :return:
        '''
        invoice_id = params.get('invoice_id')
        payment_term_dt = params.get('payment_term_dt')

        session = self._new_session()
        obj = session.query(mapper.Invoice).getone(invoice_id)

        if obj.payment_term_dt is None:
            raise exc.INVALID_PARAM('Payment term dt is not supported for this invoice')

        with session.begin():
            obj.payment_term_dt = payment_term_dt

    @rpcutil.call_description(
        {
            'act_id': rpcutil.arg_int(alias='ActID', mandatory=True),
            'payment_term_dt': rpcutil.arg_date(alias='PaymentTermDT', mandatory=True)
        }
    )
    @snout_method('/assessor/act/update-payment-term', 'BALANCE-31739')
    def UpdateActPaymentTerm(self, params):
        '''
        :param params:
        :return:
        '''
        act_id = params.get('act_id')
        payment_term_dt = params.get('payment_term_dt')

        session = self._new_session()
        obj = session.query(mapper.Act).getone(act_id)

        if obj.payment_term_dt is None:
            raise exc.INVALID_PARAM('Payment term dt is not supported for this act')

        with session.begin():
            obj.payment_term_dt = payment_term_dt

    @rpcutil.call_description(
        {
            'client_id': rpcutil.arg_int(alias='ClientID', mandatory=True)
        }
    )
    def NotifyClient(self, params):
        client_id = params.get('client_id')

        session = self._new_session()
        client = session.query(mapper.Client).getone(client_id)
        with session.begin():
            client.notify(force=True)

    @rpcutil.call_description(
        {
            'client_id': rpcutil.arg_int(alias='ClientID', mandatory=True),
            'firm_id': rpcutil.arg_int(alias='FirmID', mandatory=False),
            'service_id': rpcutil.arg_int(alias='ServiceID', mandatory=False)
        }
    )
    def NotifyOverdraft(self, params):
        client_id = params.get('client_id')
        firm_id = params.get('firm_id')
        service_id = params.get('service_id')

        session = self._new_session()
        client = session.query(mapper.Client).getone(client_id)
        with session.begin():
            client.notify_overdraft(force=True, firm_id=firm_id, service_id=service_id)

    @rpcutil.call_description(
        {
            'client_id': rpcutil.arg_int(alias='ClientID', mandatory=True),
            'service_id': rpcutil.arg_int(alias='ServiceID', mandatory=False),
            'product_id': rpcutil.arg_int(alias='ProductID', mandatory=False),
        }
    )
    def NotifyClientOrders(self, params):
        client_id = params.get('client_id')
        service_id = params.get('service_id')
        product_id = params.get('product_id')

        session = self._new_session()
        client = session.query(mapper.Client).getone(client_id)
        with session.begin():
            client.notify_orders(service_id=service_id, product_id=product_id, force=True)

    @rpcutil.call_description(
        {
            'service_id': rpcutil.arg_int(alias='ServiceID', mandatory=True),
            'service_order_id': rpcutil.arg_str(alias='ServiceOrderID', mandatory=True)
        }
    )
    def NotifyOrder(self, params):
        service_id = params.get('service_id')
        service_order_id = params.get('service_order_id')

        session = self._new_session()
        order = session.query(mapper.Order).getone(service_id=service_id, service_order_id=service_order_id)
        with session.begin():
            order.notify(force=True)

    @rpcutil.call_description(
        {
            'service_id': rpcutil.arg_int(alias='ServiceID', mandatory=True),
            'service_order_id': rpcutil.arg_str(alias='ServiceOrderID', mandatory=True)
        }
    )
    def NotifyPromocode(self, params):
        service_id = params.get('service_id')
        service_order_id = params.get('service_order_id')

        session = self._new_session()
        order = session.query(mapper.Order).getone(service_id=service_id, service_order_id=service_order_id)
        with session.begin():
            order.notify_promocode_bonus(force=True)

    @rpcutil.call_description(
        {
            'client_id': rpcutil.arg_int(alias='ClientID', mandatory=True),
            'service_id': rpcutil.arg_int(alias='ServiceID', mandatory=True),
            'iso_currency': rpcutil.arg_str(alias='IsoCurrency'),
            'bonus': rpcutil.arg_decimal(alias='Bonus', mandatory=True),
        },
    )
    def TopUpClientCashback(self, params):
        session = self._new_session()
        with session.begin():
            try:
                client_cashback = (
                    session
                    .query(mapper.ClientCashback)
                    .filter(
                        mapper.ClientCashback.client_id == params['client_id'],
                        mapper.ClientCashback.service_id == params['service_id'],
                        mapper.ClientCashback.iso_currency == params['iso_currency'],
                    )
                    .one()
                )
                client_cashback.bonus += params['bonus']

            except orm.exc.NoResultFound:
                client_cashback = mapper.ClientCashback(**params)
                session.add(client_cashback)

            session.flush()
            client_cashback.notify()
        return client_cashback.id

    @rpcutil.call_description(
        {
            'invoice_id': rpcutil.arg_int(alias='InvoiceID', mandatory=True)
        }
    )
    def NotifyInvoice(self, params):
        invoice_id = params.get('invoice_id')

        session = self._new_session()
        invoice = session.query(mapper.Invoice).getone(invoice_id)
        with session.begin():
            invoice.notify_payments()

    @rpcutil.call_description(
        rpcutil.arg_service_id
    )
    @deco.with_rosession
    def GetNotificationParams(self, session, service_id):
        service_params = session.query(
            mapper.ServiceNotifyParams
        ).getone(service_id)

        return {
            name: getattr(service_params, name)
            for name in ('test_url', 'protocol', 'version')
        } if not service_params.hidden else {}

    @rpcutil.call_description(
        rpcutil.arg_int,
        rpcutil.arg_int,
    )
    @deco.with_rosession
    def GetNotificationInfo(self, session, opcode, object_id):
        """Возвращает информацию по нотификации:
        url - url сервиса для нотификации
        protocol - json-rest или xmlrpc
        path - путь запроса, разбитый на элементы
        args и kwargs - передаваемые параметры
        """
        info = notifier_objects.BaseInfo.get_notification_info(session, opcode, object_id)[1]
        return info

    @rpcutil.call_description(
        rpcutil.arg_service_id,
        rpcutil.arg_struct({
            'url': rpcutil.arg_str,
            'protocol': rpcutil.arg_str,
        })
    )
    @deco.with_session_transaction
    def UpdateNotificationParams(self, session, service_id, params):
        service_params = session.query(mapper.ServiceNotifyParams).getone(service_id)

        url = params.get('url', None)
        protocol = params.get('protocol', None)

        if url:
            service_params.test_url = url
        if protocol:
            from notifier.data_objects import SUPPORTING_PROTOCOLS
            if protocol not in SUPPORTING_PROTOCOLS:
                raise exc.INVALID_PARAM(
                    'Unknown protocol: %s. Possible values: %s' % (protocol, SUPPORTING_PROTOCOLS))
            service_params.protocol = protocol

        return 0, 'SUCCESS'

    @rpcutil.call_description(
        rpcutil.arg_service_id,
        rpcutil.arg_str
    )
    def UpdateNotificationUrl(self, service_id, url):
        self.UpdateNotificationParams(service_id, {'url': url})
        return 0, 'SUCCESS'

    @deco.with_session
    def FetchRestrictedDomains(self, session):
        fetch_restricted_domains(session)

    @deco.with_rosession
    def CheckOebsConfig(self, session):
        from balance.oebs_config import OebsConfig
        from balance.oebs_config.checks import CHECKS
        oebs_config = OebsConfig(session)
        escape_ch = '/'
        like_op = oebs_config.CONFIG_PREFIX.replace('_', escape_ch + '_') + '%'
        config_items = session.query(mapper.Config).filter(mapper.Config.item.like(like_op, escape=escape_ch))
        config_items = [c.item[len(oebs_config.CONFIG_PREFIX):] for c in config_items]
        result = {}
        for config_item, check_func in CHECKS.items():
            log.info('trying to check OebsConfig (prefix=%s): %s' % (oebs_config.CONFIG_PREFIX, config_item))
            res = check_func(oebs_config)
            log.info('OebsConfig (prefix=%s): %s, result: %s' % (oebs_config.CONFIG_PREFIX, config_item, res))
            result[config_item] = res

        if not frozenset(CHECKS.keys()).issubset(frozenset(config_items)):
            raise ValueError('set(balance.oebs_config.checks.items) is not subset of set(bo.t_config.oebs_config__)')

        return str(result)

    @rpcutil.call_description(
        {
            "client_id": rpcutil.arg_int(alias="ClientID", mandatory=True),
            "service_id": rpcutil.arg_int(alias="ServiceID", mandatory=True),
            "currency": rpcutil.arg_str(alias="Currency", mandatory=True),
            "reward": rpcutil.arg_money(alias="Reward", mandatory=True),
            "cashback_months_to_live": rpcutil.arg_int(alias="CashbackMonthsToLive", mandatory=False),
            "auto_consume_enabled": rpcutil.arg_bool(alias="AutoConsumeEnabled", mandatory=False),
        }
    )
    @deco.with_session_transaction
    def CreateClientCashback(self, session, params):
        client_id = params['client_id']
        service_id = params['service_id']
        iso_currency = mapper.Currency.fix_iso_code(params['currency'])
        bonus = params['reward']
        cashback_months_to_live = params.get('cashback_months_to_live')
        is_auto_charge_enabled_new = params.get('auto_consume_enabled', False)

        if cashback_months_to_live:
            start_dt = ut.trunc_date(datetime.datetime.now())
            finish_dt = start_dt + relativedelta(months=cashback_months_to_live)
        else:
            start_dt = finish_dt = None

        cashbacks = OrderedDict((
            ((row.client_id, row.service_id, row.iso_currency, row.start_dt, row.finish_dt), row)
            for row in (
                session.query(mapper.ClientCashback)
                .with_for_update()
                .filter(
                    mapper.ClientCashback.client_id == client_id,
                    mapper.ClientCashback.service_id == service_id,
                )
                .options(orm.subqueryload('client').joinedload('cashback_settings'))
                # ASC, чтобы при добавлении нового кешбека тот встал в конец,
                # уже после этого произойдет reversed() и получится DESC NULLS LAST.
                .order_by(mapper.ClientCashback.start_dt.asc().nullsfirst())
            )
        ))

        cashback = cashbacks.get((client_id, service_id, iso_currency, start_dt, finish_dt))
        if cashback:
            cashback.bonus += bonus
        else:
            cashback = mapper.ClientCashback(
                client_id=client_id,
                service_id=service_id,
                iso_currency=iso_currency,
                bonus=bonus,
                start_dt=start_dt,
                finish_dt=finish_dt,
            )
            session.add(cashback)
            cashback.client = (
                session.query(mapper.Client)
                .filter_by(id=client_id)
                .options(orm.joinedload('cashback_settings'))
                .one()
            )
            cashbacks[(client_id, service_id, iso_currency, start_dt, finish_dt)] = cashback

        current_cashback_settings = cashback.client.cashback_settings.get(service_id)
        if current_cashback_settings is None:
            if is_auto_charge_enabled_new:
                settings = mapper.ClientCashbackSettings(
                    service_id=service_id,
                    client_id=client_id,
                    is_auto_charge_enabled=True
                )
                session.add(settings)
                cashback.client.cashback_settings[service_id] = settings
        elif current_cashback_settings.is_auto_charge_enabled != is_auto_charge_enabled_new:
            current_cashback_settings.is_auto_charge_enabled = is_auto_charge_enabled_new

        return {
            "client_id": client_id,
            "service_id": service_id,
            "auto_consume_enabled": is_auto_charge_enabled_new,
            "cashbacks": [
                {
                    "currency": cb.iso_currency,
                    "reward": cb.bonus,
                    "start_dt": cb.start_dt,
                    "finish_dt": cb.finish_dt,
                } for cb in reversed(cashbacks.values())    # Получается DESC NULLS LAST
            ]
        }

    @rpcutil.call_description(
        {
            "client_id": rpcutil.arg_int(alias="ClientID", mandatory=True),
            "service_id": rpcutil.arg_int(alias="ServiceID", mandatory=True),
        }
    )
    @deco.with_session_transaction
    def ChargeClientCashback(self, session, params):
        client = session.query(mapper.Client).filter_by(id=params["client_id"]).one()

        auto_charge_client(client, params["service_id"])

        return 'OK'


class LogicWithMedium(Logic, medium_logic.Logic):
    pass
