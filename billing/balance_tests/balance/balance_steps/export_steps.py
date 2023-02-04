# coding=utf-8
__author__ = 'igogor'

import sys
import json
import datetime
import time
from hamcrest import not_, equal_to
from tenacity import retry, retry_if_exception, stop_after_attempt, wait_random

from btestlib import config as balance_config
import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Export
from common_steps import CommonSteps
from contract_steps import ContractSteps

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class ExportSteps(object):
    @staticmethod
    def export_oebs(client_id=None, contract_id=None, collateral_id=None, invoice_id=None, invoice_transfer_id=None,
                    person_id=None, endbuyer_id=None, act_id=None, transaction_id=None, correction_id=None,
                    manager_id=None, product_id=None, firm_id=None):
        """ Выгружаем объекты в ОЕБС в порядке их выгрузки """

        def is_resource_busy(e):
            exceptions_to_retry = ['ORA-30006: resource busy', u'Строка уже обрабатывается параллельной сессией',
                                   'Could not lock with nowait']
            return isinstance(e, utils.XmlRpc.XmlRpcError) and isinstance(e.response, dict) \
                   and e.response.get('traceback') is not None \
                   and any(exc in e.response.get('traceback') for exc in exceptions_to_retry)

        def before_try(func, trial_number):
            reporter.log('Try to execute {} (attempt {})'.format(func.__name__, trial_number))

        def after_try(func, trial_number, trial_time_taken):
            reporter.log('Failed to {} (attempt {})'.format(func.__name__, trial_number))
            reporter.log("Exception: {}".format(sys.exc_info()[1]))

        @retry(retry=retry_if_exception(is_resource_busy),
               stop=stop_after_attempt(5), wait=wait_random(min=2, max=5), reraise=True,
               before=before_try, after=after_try)
        def export_with_retry(func):
            return func()

        if product_id:
            export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.PRODUCT, product_id))

        if client_id:
            client_w_api = ExportSteps.get_oebs_export_type('Client')

            if client_w_api:
                export_with_retry(lambda: ExportSteps.export_w_oebs_api('Client', client_id))
            else:
                export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.CLIENT, client_id))

        if person_id:
            person_w_api = ExportSteps.get_oebs_export_type('Person')
            if person_w_api:
                export_with_retry(lambda: ExportSteps.export_w_oebs_api('Person', person_id))
            else:
                export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.PERSON, person_id))

        if endbuyer_id:
            export_with_retry(lambda: ExportSteps.export_w_oebs_api('Person', endbuyer_id))

        if manager_id:
            manager_w_api = ExportSteps.get_oebs_export_type('Manager')
            if manager_w_api:
                export_with_retry(lambda: ExportSteps.export_w_oebs_api('Manager', manager_id))
            else:
                export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.MANAGER, manager_id))

        if contract_id:
            contract_w_api = ExportSteps.get_oebs_export_type('Contract')

            if contract_w_api:
                export_with_retry(lambda: ExportSteps.export_w_oebs_api('Contract', contract_id))

            else:
                export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.CONTRACT, contract_id))
                export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.CONTRACT_COLLATERAL,
                                                             ContractSteps.get_collateral_id(contract_id)))

        if collateral_id:
            export_with_retry(
                lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.CONTRACT_COLLATERAL, collateral_id))

        if invoice_id:
            invoice_w_api = ExportSteps.get_oebs_export_type('Invoice')
            if invoice_w_api:
                export_with_retry(lambda: ExportSteps.export_w_oebs_api('Invoice', invoice_id))
            else:
                export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.INVOICE, invoice_id))

        if invoice_transfer_id:
            export_with_retry(lambda: ExportSteps.export_w_oebs_api('InvoiceTransfer', invoice_transfer_id))

        if act_id:
            act_w_api = ExportSteps.get_oebs_export_type('Act')
            if act_w_api:
                export_with_retry(lambda: ExportSteps.export_w_oebs_api('Act', act_id))
            else:
                export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.ACT, act_id))

        if transaction_id:
            export_with_retry(
                lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.TRANSACTION, transaction_id))

        if correction_id:
            export_with_retry(lambda: CommonSteps.export(Export.Type.OEBS, Export.Classname.CORRECTION, correction_id))

    @staticmethod
    def get_json_data(classname, object_id):
        with reporter.step(u'Получаем актуальный json для объекта {0}: {1}'.format(classname, object_id)):
            query = "select * from t_oebs_api_export_log where method = 'billingImport' and classname = :classname and object_id = :object_id"
            result = db.balance().execute(query, {'object_id': object_id, 'classname': classname})

            data = result[0]['data']
            json_data = json.loads(json.loads(data))
            reporter.log(json_data)

            return json_data

    @staticmethod
    def log_json_contract_actions(json_contracts_repo_path, json_contract_path, json_file_name, fix_contract):
        json_path = json_contracts_repo_path + json_contract_path + json_file_name
        if fix_contract:
            reporter.log(u'Фиксируем эталон: {}'.format(json_path))
        else:
            reporter.log(u'Сравниваем с эталоном: {}'.format(json_path))

    @staticmethod
    def init_oebs_api_export(classname, object_id):
        with reporter.step(u'Инициализируем выгрузку через OEBS_API без ожидания для {0} {1}'
                                   .format(classname, object_id)):
            try:
                CommonSteps.export('OEBS_API', classname, object_id)
            except Exception as exc:
                pass

    @staticmethod
    def set_fictive_export_status(classname, object_id):
        db.balance().execute('''update t_export set state = 1, export_dt = sysdate 
                                where type = 'OEBS_API' and object_id = :object_id and classname = :classname''',
                             {'object_id': object_id, 'classname': classname})

    @staticmethod
    def extended_oebs_export(classname, object_id, queue='OEBS', patches=None, format_export_log=True):
        with reporter.step(u'Принудительная выгрузка в OEBS: {0} {1}'.format(classname, object_id)):
            if patches is None:
                patches = []
            CommonSteps.export(queue_=queue, classname=classname, object_id=object_id)

    @staticmethod
    def extended_oebs_contract_export(contract_id, person_id=None):

        if person_id:
            ExportSteps.export_oebs(person_id=person_id)
        ExportSteps.export_oebs(contract_id=contract_id)

        #  colls = db.get_collaterals_by_contract(contract_id)
        #  for coll in colls:
        #      ExportSteps.export_oebs(collateral_id=coll['id'])

    @staticmethod
    def extended_oebs_invoice_export(invoice_id, person_id):

        # Для выгрузки плательщика без ошибок нужно немного подождать:
        time.sleep(5)

        ExportSteps.export_oebs(person_id=person_id)
        ExportSteps.export_oebs(invoice_id=invoice_id)

    @staticmethod
    def extended_oebs_payment_export(payment_id, service_id=None):
        # проверяем платеж это или возврат
        paysys_code = db.balance().execute("select paysys_code from t_payment where id = {}".format(payment_id))[0][
            'paysys_code']

        # получаем транзакции по payment_id (только выгружаемые в оебс)
        if paysys_code == 'REFUND':
            payment_id = \
                db.balance().execute("select orig_payment_id from t_refund where id = {}".format(payment_id))[0][
                    'orig_payment_id']
            query = "select id from t_thirdparty_transactions where payment_id = {}  and internal is null and transaction_type = 'refund'"
            if service_id:
                query += " and service_id = {}".format(service_id)
            transactions = db.balance().execute(query.format(
                payment_id))
        else:
            query = "select id from t_thirdparty_transactions where payment_id = {}  and internal is null"
            if service_id:
                query += " and service_id = {}".format(service_id)
            transactions = db.balance().execute(query.format(payment_id))

        for transaction in transactions:
            ExportSteps.extended_oebs_export(Export.Classname.TRANSACTION,
                                             transaction['id'])

    @staticmethod
    def export_w_oebs_api(classname, object_id):
        try:
            CommonSteps.export(queue_='OEBS', classname=classname, object_id=object_id)
        except Exception:
            pass

        db.balance().execute('''update t_export set input = Null where classname = :classname
                                                 and object_id = :object_id''',
                             {'object_id': object_id, 'classname': classname})

        def get_error():
            try:
                CommonSteps.export(queue_='OEBS_API', classname=classname, object_id=object_id)
            except Exception as exc:
                return exc.response

        utils.wait_until(lambda: get_error(), not_(equal_to('Retrying OEBS_API processing')), timeout=300)

        try:
            ExportSteps.get_export_data(object_id, classname, 'OEBS_API')
        except Exception as exc:
            raise utils.TestsError(exc.response.encode('utf-8'))

    @staticmethod
    def get_oebs_api_log(classname, object_id):
        person_logs = db.balance().execute('''SELECT data
                                                FROM bo.t_oebs_api_export_log
                                                WHERE object_id = :object_id
                                                and classname = :classname
                                                and method = 'billingImport'
                                                order by dt''', {'object_id': object_id,
                                                                 'classname': classname})
        result = []
        for log in person_logs:
            result.append(json.loads(log['data']))
        return result


    @staticmethod
    def get_oebs_api_response(classname, object_id):
        person_logs = db.balance().execute('''SELECT response
                                                FROM bo.t_oebs_api_export_log
                                                WHERE object_id = :object_id
                                                and classname = :classname
                                                and method = 'getStatusBilling'
                                                order by dt desc''', {'object_id': object_id,
                                                                 'classname': classname})
        result = []
        for log in person_logs:
            result.append(json.loads(log['response']))
        return result

    # Убираем объект из очереди стандартного разборщика оебс, чтобы он выгружался только вручную
    # Для предотвращения конфликтов TESTBALANCE-1493
    @staticmethod
    def prevent_auto_export(object_id, classname, queue_type=Export.Type.OEBS):
        if object_id is not None:
            with reporter.step("Предотвращаем выгрузку объекта {object_id} ({classname}) "
                               "стандартным разборщиком оебс".format(object_id=object_id, classname=classname)):
                query = "UPDATE t_export SET state = 2 " \
                        "WHERE object_id = :object_id AND classname = :classname AND type = :queue_type"
                db.balance().execute(query, {'object_id': object_id,
                                             'classname': classname,
                                             'queue_type': queue_type})

    @staticmethod
    def get_export_data(object_id, classname, queue_type):
        return api.test_balance().GetExportObject(queue_type, classname, object_id)

    @staticmethod
    def get_export_output(object_id, classname, queue_type):
        return db.balance().execute(
            "SELECT TO_CHAR(output) AS output FROM t_export "
            "WHERE type = :queue_type AND object_id = :object_id AND classname = :classname",
            {'classname': classname, 'queue_type': queue_type, 'object_id': object_id}, single_row=True)['output']

    @staticmethod
    def create_export_record(object_id, classname=Export.Classname.BALALAYKA_PAYMENT,
                             type=Export.Type.THIRDPARTY_TRANS):
        with reporter.step(
                u"Добавляем запись в T_EXPORT с типом {} объекта {} с id: {}".format(type, classname, object_id)):
            query = "INSERT INTO t_export (classname, object_id, type) " \
                    "VALUES (:classname, :side_payment_id, :type)"
            params = {
                'classname': classname,
                'side_payment_id': object_id,
                'type': type
            }
            db.balance().execute(query, params)

    @staticmethod
    def create_export_ng_record(object_id, type):
        with reporter.step(
                u"Добавляем запись в T_EXPORT_NG с типом {} и id: {}".format(type, object_id)):
            query = "INSERT INTO t_export_ng (object_id, type) " \
                    "VALUES (:object_id, :type)"
            params = {
                'object_id': object_id,
                'type': type
            }
            db.balance().execute(query, params)

    @staticmethod
    def create_export_record_and_export(object_id, type, classname, service_id=None, with_export_record=True):
        if with_export_record:
            ExportSteps.create_export_record(object_id, classname=classname)

        export_result = CommonSteps.export(type, classname, object_id)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA and \
                (classname == Export.Classname.SIDE_PAYMENT or classname == Export.Classname.BALALAYKA_PAYMENT):
            ExportSteps.extended_oebs_payment_export(object_id, service_id)

        return export_result

    @staticmethod
    def get_oebs_export_type(classname):
        config_dict = json.loads(db.balance().execute("""SELECT *
                                    FROM t_Config
                                    WHERE ITEM = 'CLASSNAMES_EXPORTED_WITH_OEBS_API'""")[0]['value_json'])
        classname_conf = config_dict.get(classname)
        if classname_conf == 1:
            return True
        else:
            return False
