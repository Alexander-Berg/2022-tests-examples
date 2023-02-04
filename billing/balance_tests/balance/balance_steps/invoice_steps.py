# coding=utf-8
__author__ = 'igogor'

import datetime
import urlparse
import random
import export_steps
import order_steps
import request_steps
import common_steps
import campaigh_steps
import acts_steps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
import btestlib.utils as utils
import btestlib.config as balance_config
from btestlib.constants import Users, Export
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


class InvoiceSteps(object):
    @staticmethod
    def create(request_id, person_id, paysys_id, credit=0, contract_id=None, overdraft=0, endbuyer_id=None,
               passport_uid=defaults.PASSPORT_UID, agency_discount=0, adjust_qty=0, turn_on=None):
        request_params = {'PaysysID': paysys_id,
                          'PersonID': person_id,
                          'RequestID': str(request_id),
                          'Credit': credit,
                          'ContractID': contract_id,
                          'Overdraft': overdraft
                          }
        if turn_on is not None:
            request_params['TurnOn'] = turn_on
        # TODO add endbuyer info => step
        with reporter.step(
                u'Выставляем {0} счёт по реквесту (request_id: {1}) на плательщика (person_id: {2}) {3} {4} с способом оплаты (paysys_id: {5})'.format(
                    u'предоплатный' if request_params['Credit'] == 0 else u'постоплатный',
                    request_params['RequestID'],
                    request_params['PersonID'],
                    u'без договора' if request_params['ContractID'] is None \
                            else u'по договору (contract_id: {0})'.format(request_params['ContractID']),
                    u'в овердрафт' if request_params['Overdraft'] == 1 else '',
                    request_params['PaysysID'])):
            if agency_discount:
                invoice_id = api.test_balance().CreateInvoiceWithDiscount(passport_uid, request_params,
                                                                          [(0, 0), (agency_discount, adjust_qty)])
            else:
                invoice_id = api.medium().CreateInvoice(passport_uid, request_params)
            if endbuyer_id:
                db.balance().insert_extprop(object_id=invoice_id, classname="'Invoice'", attrname="'endbuyer_id'",
                                            value_num=endbuyer_id, passport_uid=passport_uid)
            query = "SELECT external_id, total_sum, consume_sum, type FROM t_invoice WHERE id = :invoice_id"
            result = db.balance().execute(query, {'invoice_id': invoice_id})
            external_id, total_sum = result[0]['external_id'], result[0]['total_sum']
            invoice_url = '{base_url}/invoice.xml?invoice_id={invoice_id}'.format(base_url=env.balance_env().balance_ai,
                                                                                  invoice_id=invoice_id)
            reporter.attach(u'invoice_id = {} external_id = {} endbuyer_id = {}'.format(invoice_id, external_id,
                                                                                        endbuyer_id))
            reporter.report_url(u'Ссылка на счет', invoice_url)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            invoice_type = result[0]['type']
            if invoice_type not in ['charge_note', 'fictive', 'fictive_personal_account']:
                export_steps.ExportSteps.extended_oebs_invoice_export(invoice_id, person_id)

        return invoice_id, external_id, total_sum

    @staticmethod
    def create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                             invoice_dt=None, agency_id=None, credit=0, contract_id=None,
                             overdraft=0, manager_uid=None, endbuyer_id=None, passport_uid=defaults.PASSPORT_UID,
                             prevent_oebs_export=False):
        with reporter.step(u'Выставляем счёт быстрым методом:'):
            order_owner = client_id
            invoice_owner = agency_id or client_id
            service_order_id = []
            order_id = []
            orders_list = []
            # reporter.log('------ Orders list: ------')
            for campaign in campaigns_list:

                additional_params = {'AgencyID': agency_id, 'ManagerUID': manager_uid}
                additional_params.update(campaign.get('additional_params', {}))

                service_order_id.append(order_steps.OrderSteps.next_id(campaign['service_id']))
                order_id.append(
                    order_steps.OrderSteps.create(
                        campaign['client_id'] if 'client_id' in campaign else order_owner,
                        product_id=campaign['product_id'],
                        service_id=campaign['service_id'],
                        service_order_id=service_order_id[-1],
                        params=additional_params
                    )
                )
                orders_list.append({'ServiceID': campaign['service_id'],
                                    'ServiceOrderID': service_order_id[-1],
                                    'Qty': campaign['qty']}
                                   )
                if 'begin_dt' in campaign:
                    orders_list[-1].update({'BeginDT': campaign['begin_dt']})

            request_id = request_steps.RequestSteps.create(invoice_owner, orders_list,
                                                           additional_params=dict(InvoiceDesireDT=invoice_dt))
            invoice_id, external_id, total_sum = InvoiceSteps.create(request_id, person_id, paysys_id, credit=credit,
                                                                     contract_id=contract_id, overdraft=overdraft,
                                                                     endbuyer_id=endbuyer_id)

        for i, campaign in enumerate(campaigns_list):
            orders_list[i].update({'ProductID': campaign['product_id']})
            ##    reporter.log(('{0:<'+str(log_align)+'} | ...').format('create_force_invoice done'))

        if prevent_oebs_export:
            export_steps.ExportSteps.prevent_auto_export(invoice_id, Export.Classname.INVOICE)

        return invoice_id, external_id, total_sum, orders_list

    @staticmethod
    def create_prepay_http(request_id, person_id, paysys_id, contract_id='', endbuyer_id='', prevent_oebs_export=False):
        user = Users.YB_ADM
        session = passport_steps.auth_session(user)
        issue_invoice_url = urlparse.urljoin(env.balance_env().balance_ci, '/issue-invoice.xml')
        sk = utils.get_secret_key(user.uid)
        params = dict(request_id=request_id, paysys_id=paysys_id, person_id=person_id, endbuyer_id=endbuyer_id,
                      contract_id=contract_id, sk=sk)
        resp = utils.call_http(session, issue_invoice_url, params)
        invoice_id = utils.get_url_parameter(resp.url, param='invoice_id')[0]

        if prevent_oebs_export:
            export_steps.ExportSteps.prevent_auto_export(invoice_id, Export.Classname.INVOICE)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            export_steps.ExportSteps.extended_oebs_invoice_export(invoice_id, person_id)

        return invoice_id

    @staticmethod
    def pay_with_certificate_or_compensation(order_id, sum, type='ce'):

        # ce - сертификат, co - компенсация
        api.test_balance().PayOrderCC({'order_id': order_id, 'paysys_code': type, 'qty': sum})
        type = type + '%'
        query = "SELECT i.id FROM t_invoice i INNER JOIN t_consume c ON c.INVOICE_ID = i.id INNER JOIN t_paysys p ON i.PAYSYS_ID = p.ID WHERE c.PARENT_ORDER_ID = :order_id AND p.cc LIKE :type ORDER BY i.dt DESC"
        sql_params = {'order_id': order_id, 'type': type + '%'}
        invoice_id = db.balance().execute(query, sql_params)[0]['id']
        return invoice_id

    @staticmethod
    def create_invoice_with_act(context, client_id, person_id, dt, qty):
        service_order_id = order_steps.OrderSteps.next_id(service_id=context.service.id)
        order_steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                                      service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
        ]
        request_id = request_steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                                       additional_params=dict(InvoiceDesireDT=dt))

        invoice_id, _, _ = InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                               paysys_id=context.paysys.id,
                                               credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        InvoiceSteps.pay(invoice_id)
        campaigh_steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
                                                   {context.product.type.code: qty}, 0,
                                                   dt)
        act_id = acts_steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
        return invoice_id, act_id

    @staticmethod
    def pay(invoice_id, payment_sum=None, payment_dt=None):
        with reporter.step(u'Оплачиваем "быстро" счёт {0} {1} {2}'.format(invoice_id,
                                                                          u'на всю сумму' if not payment_sum else u'на сумму: {0}'.format(
                                                                              payment_sum),
                                                                          u'на текущую дату' if not payment_dt else u'на дату: {0}'.format(
                                                                              payment_dt))):
            query = "SELECT total_sum, external_id FROM t_invoice WHERE id = :invoice_id"
            result = db.balance().execute(query, {'invoice_id': invoice_id})[0]
            external_id, total_sum = result['external_id'], result['total_sum']
            p_sum = payment_sum or total_sum
            p_date = payment_dt or datetime.datetime.today()
            # TODO: how to work with dates? no need to do to_date(dt, <format>)
            db.balance().execute(
                """INSERT INTO t_correction_payment (dt, doc_date, sum, memo, invoice_eid) VALUES (:paymentDate,
                :paymentDate, :total_sum, 'Testing', :external_id)""",
                {'paymentDate': p_date, 'total_sum': p_sum, 'external_id': external_id})

            api.test_balance().OEBSPayment(invoice_id)

    @staticmethod
    def pay_with_charge_note(invoice_id):
        api.test_balance().PayWithChargeNote(invoice_id)

    @staticmethod
    def pay_force(invoice_id, receipt_sum=None, receipt_sum_1c=None):
        if receipt_sum:
            query = "UPDATE t_invoice SET receipt_sum= :receipt_sum WHERE id = :invoice_id"
            db.balance().execute(query, {'invoice_id': invoice_id, 'receipt_sum': receipt_sum})
        elif receipt_sum_1c:
            query = "UPDATE t_invoice SET receipt_sum_1c= :receipt_sum_1c WHERE id = :invoice_id"
            db.balance().execute(query, {'invoice_id': invoice_id, 'receipt_sum_1c': receipt_sum_1c})
        else:
            return None

    @staticmethod
    def pay_fair(invoice_id, payment_sum=None, payment_dt=None, enqueue_only=False, orig_id=None, operation_type=None,
                 source_id=None, inn=None, bik=None, account=None, customer_name=None, cash_receipt_number=None):
        query = "SELECT total_sum, external_id FROM t_invoice WHERE id = :invoice_id"
        result = db.balance().execute(query, {'invoice_id': invoice_id}, single_row=True)
        external_id, total_sum = result['external_id'], result['total_sum']
        p_sum = payment_sum or total_sum
        p_date = payment_dt or datetime.datetime.today()

        with reporter.step(
                u'Оплачиваем "честно" счёт {0} на {1} с датой платежа {2}'.format(invoice_id, p_sum, p_date)):
            query = "INSERT INTO t_oebs_cash_payment_fact \
                     (XXAR_CASH_FACT_ID,AMOUNT,RECEIPT_NUMBER,RECEIPT_DATE,LAST_UPDATED_BY,LAST_UPDATE_DATE," \
                    "LAST_UPDATE_LOGIN,CREATED_BY,CREATION_DATE,CASH_RECEIPT_NUMBER,ORIG_ID," \
                    " OPERATION_TYPE, SOURCE_ID, INN, BIK, ACCOUNT_NAME, CUSTOMER_NAME) \
                     VALUES (S_OEBS_CASH_PAYMENT_FACT_TEST.nextval,:payment_sum,:external_id,:payment_dt,'-1'," \
                    "trunc(sysdate),'-1','-1',trunc(sysdate),:cash_receipt_number,:orig_id, :operation_type, :source_id, :inn, :bik, :account, :customer_name)"
            query_params = {'payment_sum': p_sum,
                            'external_id': external_id,
                            'payment_dt': p_date,
                            'cash_receipt_number': cash_receipt_number or u'{0}-99999'.format(external_id),
                            'orig_id': orig_id,
                            'operation_type': operation_type,
                            'source_id': source_id,
                            'inn': inn,
                            'bik': bik,
                            'account': account,
                            'customer_name': customer_name

                            }
            db.balance().execute(query, query_params)

        if enqueue_only:
            api.test_balance().Enqueue(Export.Classname.INVOICE, invoice_id, Export.Type.PROCESS_PAYMENTS)
        else:
            common_steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    @staticmethod
    def create_oebs_cash_payment_fact(amount, receipt_number, operation_type, receipt_dt=NOW):
        xxar_cash_fact_id = api.test_balance().ExecuteOEBS(1, 'SELECT   XXAR_CASH_PAYMENT_FACT_SEQ.NEXTVAL FROM dual',
                                                           {})[0]['nextval']
        source_id = random.randint(1, 100000)
        cash_receipt_number = random.randint(1, 100000)
        query = '''insert into xxfin.xxar_cash_payment_fact (AMOUNT, RECEIPT_NUMBER, RECEIPT_DATE,
        LAST_UPDATED_BY, LAST_UPDATE_DATE, CREATED_BY, CREATION_DATE, XXAR_CASH_FACT_ID, CASH_RECEIPT_NUMBER,
        SOURCE_ID
        ) values (:amount, :receipt_number, :receipt_dt, :last_updated_by,
        :last_update_date, :created_by, :creation_date, :xxar_cash_fact_id, :cash_receipt_number,
        :source_id)'''
        params = {'amount': 100, 'receipt_number': receipt_number, 'receipt_dt': receipt_dt, 'last_updated_by': 0,
                  'last_update_date': NOW, 'created_by': 0, 'creation_date': NOW,
                  'xxar_cash_fact_id': xxar_cash_fact_id, 'source_id': source_id,
                  'cash_receipt_number': cash_receipt_number}
        result = api.test_balance().ExecuteOEBS(1, query, params)
        return xxar_cash_fact_id, source_id, cash_receipt_number

    @staticmethod
    def create_fast_payment(service_id, client_id, paysys_id, items, passport_uid=defaults.PASSPORT_UID, login=None,
                            overdraft=0, qty_is_amount=0, payment_token=None):
        token = db.get_service_by_id(service_id)[0]['token']
        request_params = {'service_token': token,
                          'passport_id': passport_uid,
                          'login': login,
                          'client_id': client_id,
                          'paysys_id': paysys_id,
                          'items': items,
                          'overdraft': overdraft,
                          'qty_is_amount': qty_is_amount,
                          'payment_token': payment_token
                          }
        return api.medium().CreateFastPayment(request_params)

    @staticmethod
    def turn_on(invoice_id, sum=None):
        with reporter.step(u'Включаем счёт {0} на сумму {1}'.format(invoice_id, sum or 'full sum')):
            api.test_balance().TurnOn({'invoice_id': invoice_id, 'sum': sum})
            # log.debug('Invoice {0} is turned on {1}'.format(invoice_id, sum if sum else 'full sum'))

    @staticmethod
    def turn_on_ai(invoice_id):
        with reporter.step(u'Включаем счет {0} через админский интерфейс'.format(invoice_id)):
            session = passport_steps.auth_session()
            turn_on_invoice_url = '{base_url}/confirm-payment.xml'.format(base_url=env.balance_env().balance_ai)
            params = dict(invoice_id=invoice_id)
            headers = {'X-Requested-With': 'XMLHttpRequest'}
            utils.call_http(session, turn_on_invoice_url, params, headers)
            return invoice_id

    @staticmethod
    def change_dt_ai(invoice_id, dt):
        with reporter.step(u'Меняем дату счета {0} через админский интерфейс на {1}'.format(invoice_id, dt)):
            session = passport_steps.auth_session()
            change_invoice_dt_url = '{base_url}/patch-invoice-date.xml'.format(base_url=env.balance_env().balance_ai)
            params = dict(dt=dt.isoformat(), invoice_id=invoice_id)
            utils.call_http(session, change_invoice_dt_url, params)
            return invoice_id

    @staticmethod
    def patch_sum_ai(invoice_id, invoice_sum):
        with reporter.step(u'Меняем сумму счета {0} через админский интерфейс на {1}'.format(invoice_id, invoice_sum)):
            session = passport_steps.auth_session()
            change_invoice_sum_url = '{base_url}/patch-invoice-sum.xml'.format(base_url=env.balance_env().balance_ai)
            params = dict(new_rur_sum=invoice_sum, invoice_id=invoice_id)
            utils.call_http(session, change_invoice_sum_url, params)
            return invoice_id

    @staticmethod
    def make_rollback_ai(invoice_id, unused_funds_lock=1, amount=None, order_id=0, user=None):
        with reporter.step(u'Переносим средства на беззаказье {0}'.format(invoice_id)):
            if not user:
                user = Users.YB_ADM
            session = passport_steps.auth_session(user=user)
            gen_sk = utils.get_secret_key(passport_id=user.uid)
            make_rollback_url = '{base_url}/make-rollback.xml'.format(base_url=env.balance_env().balance_ai)
            params = dict(amount=amount, invoice_id=invoice_id, order_id=order_id, unused_funds_lock=unused_funds_lock,
                          sk=gen_sk)
            headers = {'X-Requested-With': 'XMLHttpRequest'}
            utils.call_http(session, make_rollback_url, params, headers)
            return invoice_id

    @staticmethod
    def get_invoice_data_by_client(client_id):
        sql = "SELECT paysys_id, contract_id, person_id, consume_sum, total_act_sum, " \
              "currency, nds, nds_pct, type, firm_id FROM t_invoice WHERE client_id = :client_id ORDER BY dt, type"
        params = {'client_id': client_id}
        data = db.balance().execute(sql, params, descr='Ищем данные в t_invoice по клиенту',
                                    fail_empty=False)
        return data

    @staticmethod
    def get_invoice_data_by_client_and_contract(client_id, contract_id):
        sql = "SELECT paysys_id, contract_id, person_id, consume_sum, total_act_sum, " \
              "currency, nds, nds_pct, type, firm_id FROM t_invoice " \
              "WHERE client_id = :client_id and contract_id = :contract_id " \
              "ORDER BY dt, type"
        params = {'client_id': client_id, 'contract_id': contract_id}
        data = db.balance().execute(sql, params, descr='Ищем данные в t_invoice по клиенту и договору',
                                    fail_empty=False)
        return data

    @staticmethod
    def get_invoice_id_by_client_and_contract(client_id, contract_id):
        sql = "SELECT id, contract_id FROM t_invoice " \
              "WHERE client_id = :client_id and contract_id = :contract_id " \
              "ORDER BY dt, type"
        params = {'client_id': client_id, 'contract_id': contract_id}
        data = db.balance().execute(sql, params, descr='Ищем id счета в t_invoice по клиенту и договору',
                                    fail_empty=False)
        return data

    @staticmethod
    def get_invoice_data_by_client_and_dt(client_id, dt):
        sql = "SELECT paysys_id, contract_id, person_id, consume_sum, total_act_sum, " \
              "currency, nds, nds_pct, type, firm_id, amount FROM t_invoice WHERE client_id = :client_id AND dt = :dt"
        params = {'client_id': client_id, 'dt': dt}
        data = db.balance().execute(sql, params, descr='Ищем данные в t_invoice по клиенту',
                                    fail_empty=False)
        return data

    @staticmethod
    def get_invoice_data_by_client_with_ids(client_id):
        sql = "SELECT paysys_id, contract_id, person_id, consume_sum, total_act_sum, " \
              "currency, nds, nds_pct, type, firm_id, id, external_id FROM t_invoice WHERE client_id = :client_id ORDER BY dt"
        params = {'client_id': client_id}
        data = db.balance().execute(sql, params, descr='Ищем данные в t_invoice по клиенту',
                                    fail_empty=False)
        return data

    @staticmethod
    def get_invoice_eid(contract_id, client_id, currency, vat=1):
        invoices = [inv for inv in InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)
                    if inv['currency'] == currency and inv['contract_id'] == contract_id]
        invoices = {inv['nds']: inv for inv in invoices}
        return invoices.get(vat)['external_id']

    # TODO: refactor it!
    @staticmethod
    def get_invoice_ids(client_id, type='personal_account', expected_inv_count=1):
        with reporter.step(u'Получаем id и external id лицевого счета для клиента: {}'.format(client_id)):
            query = "SELECT ID, EXTERNAL_ID FROM T_INVOICE WHERE CLIENT_ID=:client_id AND TYPE=:type"
            params = {'client_id': client_id, 'type': type}
            result = db.balance().execute(query, params)

            invoice_id = result[0]['id']
            external_invoice_id = result[0]['external_id']

            if expected_inv_count > 1:
                invoice_id = []
                external_invoice_id = []
                for i in range(expected_inv_count):
                    invoice_id.append(result[i]['id'])
                    external_invoice_id.append(result[i]['external_id'])
                # invoice_id = [result[0]['id'], result[1]['id']]
                # external_invoice_id = [result[0]['external_id'], result[1]['external_id']]

            reporter.attach(u'ID лицевого счета', utils.Presenter.pretty(invoice_id))
            reporter.attach(u'External ID лицевого счета', utils.Presenter.pretty(external_invoice_id))

            return invoice_id, external_invoice_id

    @staticmethod
    def get_personal_account_external_id(contract_id, service):
        with reporter.step(u'Находим eid для лицевого счета для сервиса "{}" и договора: {}'
                                   .format(service.name, contract_id)):
            query = '''SELECT DISTINCT inv.EXTERNAL_ID FROM T_INVOICE inv
                              JOIN T_EXTPROPS prop ON inv.ID = prop.OBJECT_ID
                              JOIN T_PRODUCT prod ON prod.SERVICE_CODE = prop.VALUE_STR
                            WHERE inv.CONTRACT_ID=:contract_id
                              AND prop.CLASSNAME='PersonalAccount'
                              AND prop.ATTRNAME='service_code'
                              AND prod.ENGINE_ID=:service_id
                              AND prod.COMMON = 0
                              AND prod.HIDDEN = 0'''
            params = {'contract_id': contract_id, 'service_id': service.id}
            eid = db.balance().execute(query, params, single_row=True)['external_id']
            return eid

    @staticmethod
    def get_personal_account_external_id_with_service_code(contract_id, service_code):
        query_for_get_invoice_external_id = "SELECT i.external_id, i.id FROM t_invoice i, t_extprops ext " \
                                            "WHERE i.id = ext.object_id " \
                                            "AND i.contract_id = :contract_id " \
                                            "AND i.type = 'personal_account' " \
                                            "AND ext.classname = 'PersonalAccount' " \
                                            "AND ext.attrname = 'service_code' " \
                                            "AND value_str = :service_code"
        params = {'contract_id': contract_id, 'service_code': service_code}
        invoice_data = db.balance().execute(query_for_get_invoice_external_id, params)
        return invoice_data[0]['external_id'], invoice_data[0]['id']

    @staticmethod
    def get_personal_accounts_with_service_codes(client_id):
        with reporter.step(u'Находим данные ЛС с сервис кодом для клиента: {}'.format(client_id)):
            query = "SELECT paysys_id, contract_id, person_id, consume_sum, total_act_sum, " \
                    "currency, nds, nds_pct, type, firm_id, prop.VALUE_STR service_code " \
                    "FROM T_INVOICE inv LEFT JOIN T_EXTPROPS prop ON " \
                    "inv.ID = prop.OBJECT_ID AND prop.CLASSNAME='PersonalAccount' AND prop.ATTRNAME='service_code' " \
                    "WHERE inv.CLIENT_ID=:client_id"

            params = {'client_id': client_id}
            return db.balance().execute(query, params)

    @staticmethod
    def get_personal_account_by_client_and_paysys(client_id, paysys_id):
        with reporter.step(u'Находим данные ЛС для клиента {} с paysys_id {}'.format(client_id, paysys_id)):
            query = "SELECT id, external_id FROM t_invoice WHERE client_id=:client_id and paysys_id=:paysys_id"
            params = {'client_id': client_id, 'paysys_id': paysys_id}
            return db.balance().execute(query, params)

    @staticmethod
    def get_invoice_by_service_or_service_code(contract_id, service=None, service_code=None,
                                               is_service_code_exist=True):
        # is_service_code_exist = False, если у продукта нет service_code и нужен такой лс без service_code
        with reporter.step(u'Находим eid для лицевого счета договора: {}'.format(contract_id)):
            if not service_code:
                query_to_get_service_code = "select distinct service_code from bo.t_product where engine_id = :service_id " \
                                            "and rownum = 1 and hidden=0"
                service_code = \
                    db.balance().execute(query_to_get_service_code, {'service_id': service.id}, single_row=True)[
                        'service_code']

            query = "SELECT inv.id, inv.external_id FROM T_INVOICE inv LEFT OUTER JOIN T_EXTPROPS prop ON inv.ID = prop.OBJECT_ID " \
                    "and prop.ATTRNAME='service_code' AND prop.CLASSNAME='PersonalAccount'" \
                    "WHERE inv.CONTRACT_ID=:contract_id"
            if service_code:
                query += " AND prop.VALUE_STR = :service_code"
            if not is_service_code_exist:
                query += " AND prop.VALUE_STR is null"

            invoice_data = db.balance().execute(query, {'contract_id': contract_id, 'service_code': service_code},
                                                single_row=True)

            if invoice_data == {}:
                raise Exception("No personal accounts by params")
            return invoice_data['id'], invoice_data['external_id'], service_code

    @staticmethod
    def get_payment_id_for_charge_note(invoice_id):
        query = "SELECT id FROM t_payment WHERE invoice_id = :invoice_id"
        params = {'invoice_id': invoice_id}
        payment_id = db.balance().execute(query, params)[0]['id']
        return payment_id

    @staticmethod
    def is_invoice_currency(invoice_id):
        orders = db.get_orders_by_invoice(invoice_id)
        products_list = []
        for order in orders:
            product_id = order['product_id']
            product = db.get_product_by_id(product_id)[0]
            products_list.append(product)
        currency_set = set()
        for product in products_list:
            unit_id = product['unit_id']
            unit = db.get_unit_by_id(unit_id)[0]['iso_currency']
            currency_set.add(unit)
        return False if currency_set == {None} else True

    @staticmethod
    def make_repayment_invoice(invoice_id, with_confirmation=True, prevent_oebs_export=False):
        with reporter.step(u'Создаем счет на погашение для фиктивного счета: {}'.format(invoice_id)):
            session = passport_steps.auth_session()
            deferpays_action_url = '{base_url}/deferpays-action.xml'.format(base_url=env.balance_env().balance_ci)
            deferpay = db.get_deferpays_by_invoice(invoice_id)[0]
            deferpay_id = deferpay['id']
            deferpay_key = 'deferpay_{0}'.format(deferpay_id)
            # выставляем предварительный счет
            params = {deferpay_key: deferpay_id, 'action': 'repayment-invoice'}
            utils.call_http(session, deferpays_action_url, params, method='GET')
            repayment_invoice_ids = [x['repayment_invoice_id'] for x in db.get_repayment_by_invoice(invoice_id)]
            if repayment_invoice_ids:
                if with_confirmation:
                    # подтверждаем счет
                    params = {deferpay_key: deferpay_id, 'action': 'confirm-invoices'}
                    utils.call_http(session, deferpays_action_url, params, method='GET')

                    if prevent_oebs_export:
                        for id in repayment_invoice_ids:
                            export_steps.ExportSteps.prevent_auto_export(id, Export.Classname.INVOICE)

                invoice_url = env.balance_env().balance_ai + '/invoice.xml?invoice_id={}'
                reporter.report_urls(u'Счета на погашение',
                                     *[(u'repayment_invoice_id: {}'.format(invoice_id), invoice_url.format(invoice_id))
                                       for invoice_id in repayment_invoice_ids])
                return repayment_invoice_ids
            else:
                reporter.attach(u'Счетов на погашение создано не было')
                return

    @staticmethod
    def set_turn_on_dt(invoice_id, dt):
        db.balance().execute('UPDATE t_invoice SET turn_on_dt = :dt WHERE id = :invoice_id',
                             {'invoice_id': invoice_id, 'dt': dt})

    @staticmethod
    def make_invoice_extern(invoice_id):
        db.balance().execute('UPDATE t_invoice SET extern = 1 WHERE id = :invoice_id',
                             {'invoice_id': invoice_id})

    @staticmethod
    def pay_ls(invoice_id, payment_sum):
        api.test_balance().PayChargeNote({'InvoiceID': invoice_id, 'PaymentSum': payment_sum})

    @staticmethod
    def set_dt(invoice_id, dt):
        db.balance().execute('UPDATE t_invoice SET dt = :dt WHERE id = :invoice_id',
                             {'invoice_id': invoice_id, 'dt': dt})

    @staticmethod
    def set_payment_term_dt(invoice_id, dt):
        db.balance().execute('UPDATE t_invoice SET payment_term_dt = :dt WHERE id = :invoice_id',
                             {'invoice_id': invoice_id, 'dt': dt})

    @staticmethod
    def free_funds_to_order(invoice_id, service_id, service_order_id, mode=0, sum=0, discount_pct=0):
        print invoice_id, service_id, service_order_id, mode, sum, discount_pct
        return api.test_balance().TransferFromInvoice(invoice_id, service_id, service_order_id, mode, sum, discount_pct)

    @staticmethod
    def patch_invoice_contract(invoice_id, contract_id):
        with reporter.step(
                u'Привязываем счет {0}  к договору {1} через админский интерфейс'.format(invoice_id, contract_id)):
            session = passport_steps.auth_session()
            turn_on_invoice_url = '{base_url}/patch-invoice-contract.xml'.format(base_url=env.balance_env().balance_ai)
            params = dict(invoice_id=invoice_id, contract_id=contract_id)
            headers = {'X-Requested-With': 'XMLHttpRequest'}
            utils.call_http(session, turn_on_invoice_url, params, headers)
            return invoice_id, contract_id

    @staticmethod
    def create_cash_payment_fact(invoice_eid, amount, dt, type, orig_id=None, invoice_id=None):
        with reporter.step(u'Вставляем запись об оплате в T_OEBS_CASH_PAYMENT_FACT для счета: {}'.format(invoice_eid)):
            source_id = db.balance().execute("SELECT S_OEBS_CPF_SOURCE_ID_TEST.nextval val FROM dual")[0]['val']
            cash_fact_id = db.balance().execute("SELECT S_OEBS_CASH_PAYMENT_FACT_TEST.nextval val FROM dual")[0]['val']

            query = "INSERT INTO T_OEBS_CASH_PAYMENT_FACT" \
                    "(XXAR_CASH_FACT_ID, AMOUNT, RECEIPT_NUMBER, RECEIPT_DATE, OPERATION_TYPE, " \
                    "LAST_UPDATED_BY, LAST_UPDATE_DATE, CREATED_BY, CREATION_DATE, SOURCE_ID, ORIG_ID) VALUES " \
                    "(:cash_fact_id, :amount, :invoice_eid, :dt, :type, " \
                    "-1, :dt, -1, :dt, :source_id, :orig_id)"
            params = {
                'invoice_eid': invoice_eid,
                'amount': amount,
                'dt': dt,
                'type': type,
                'source_id': source_id,
                'cash_fact_id': cash_fact_id,
                'orig_id': orig_id
            }
            db.balance().execute(query, params)

        if invoice_id:
            common_steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

        return cash_fact_id, source_id

    @staticmethod
    def get_invoice_receipt_data(invoice_id):
        query = "SELECT inv.RECEIPT_SUM, inv.RECEIPT_SUM_1C " \
                "FROM T_INVOICE inv WHERE ID=:invoice_id"

        params = {'invoice_id': invoice_id}
        return db.balance().execute(query, params)

    @staticmethod
    def get_all_invoice_data_by_id(invoice_id):
        query = "SELECT * FROM T_INVOICE inv WHERE ID=:invoice_id"

        params = {'invoice_id': invoice_id}
        return db.balance().execute(query, params)[0]
