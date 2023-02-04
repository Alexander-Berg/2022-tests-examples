# coding: utf-8

import json
import time
from datetime import timedelta, datetime

import attr
import dateutil

import balance.balance_api as api
import balance.balance_db as db
import balance.balance_steps as steps
import balance.balance_web as web
import btestlib.data.defaults as defaults
import btestlib.reporter as reporter
from balance import balance_objects as obj
from btestlib import constants as c
from btestlib import environments as env
from btestlib import utils


# Принцип работы степов 1го уровня - принимают объект. Все параметры хранятся в объкте.
# Часто используемые параметры заданы явно в сигнатуре фактори метода, остальные принимаются как kwargs
# Метод считает что все зависимые сущности уже созданы - автоматизация их создания будет в ObjectFactory
# todo-igogor добавить информацию о типе во все методы, чтобы он нормально подсказывал
# todo-igogor в качестве passport_uid передавать пользователя или строку?
# Плюс строки что можно передать вообще кого угодно.
# Плюс User в том что данные о пользователях у нас передвигаются по сценарию в нем.
# Можно в контексте держать пользователя, а здесь принимать строку для гибкости
def create_or_update_client(client, passport_uid=defaults.PASSPORT_UID):
    # type: (obj.Client, int) -> obj.Client

    def client_params(client):
        mandatory = {'IS_AGENCY': client.is_agency}
        _validate_mandatory(mandatory)

        # Порядок мерджа словарей очень важен.
        # Сначала должны идти client.params т.к. в них содержатся значения шаблона, которые хотим переопределить
        return utils.merge_dicts([client.params,
                                  utils.remove_empty({'CLIENT_ID': client.id,
                                                      'AGENCY_ID': client.agency.id,
                                                      'REGION_ID': client.region.id,
                                                      'SERVICE_ID': client.service.id,
                                                      'CURRENCY': client.currency.iso_code,
                                                      'MIGRATE_TO_CURRENCY': client.migrate_to_currency}),
                                  mandatory,
                                  # todo-igogor это место протестировать похорошему.
                                  # Смысл в том, что те поля что передали ключевыми параметрами имеют наибольший приоритет,
                                  # чтобы через них можно было явно и быстро переопределить любой параметр вызова на случай если что-то идет не так.
                                  # Но при этом поля шаблона которые тоже передаются через params, но на шаг назад, должны иметь самый низкий приоритет
                                  # Проблема возникнет если кто-то нарушит схему ОбъкетШаблона(параметры шаблона).new(параметры),
                                  # а сделает сразy Объект().new(параметры, параметры шаблона)
                                  client.params_changes])

    is_create = client.id is None
    with reporter.step(u'{} {}:'.format(u'Создаем' if is_create else u'Редактируем', str(client))):
        code, status, client_id = api.medium().CreateClient(passport_uid, client_params(client))
        # todo-igogor надо делать валидацию code и status

        result = client.new(id=client_id) if is_create else client
        reporter.attach(u'Cоздан {} c параметрами'.format(str(result)) if is_create
                        else u'В {}, обновили'.format(str(result)), str_values(client.changes))
        # todo-igogor При создании было бы круто выводить имя шаблона но как его узнать хз, т.к. шаблон обычно изменен
        # можно ввести поле _template и заполнять его только в templates.Clients
        # todo-igogor После изменен хорошо бы выводить str клиента до изменения, но он сюда не передается
        # можно было бы его прикапывать в объекте, но имхо это ебля

        return result


def get_free_service_order_id(service):
    # type: (c.Services.Service) -> str
    with reporter.step(u"Получаем service_order_id для сервиса {0}".format(service)):
        seq_name = api.test_balance().server.GetTestSequenceNameForService(service.id)

        service_order_id = db.balance().execute(query="select {0}.nextval from dual".format(seq_name),
                                                single_row=True)['nextval']
        reporter.attach(u"service_order_id", utils.Presenter.pretty(service_order_id))
        return service_order_id


def create_or_update_orders(orders, service_token=None, passport_uid=defaults.PASSPORT_UID):
    # type: (list[obj.Order], str, str) -> list[obj.Order]
    def order_params(order):
        # type: (obj.Order) -> dict
        mandatory = {'ClientID': order.client.id,
                     'ProductID': order.product.id,
                     'ServiceID': order.service.id,
                     'ServiceOrderID': order.service_order_id,
                     'TEXT': 'Py_Test order'
                     }
        _validate_mandatory(mandatory)

        return utils.merge_dicts([order.params,
                                  utils.remove_empty({'ManagerUID': order.manager.uid,
                                                      'AgencyID': order.agency.id,
                                                      'GroupServiceOrderID': order.group_order.service_order_id}),
                                  mandatory,
                                  order.params_changes])

    with reporter.step(u'Создаем/обновляем {}:'.format(u'заказ' if len(orders) == 1 else u'группу заказов')):
        # todo-igogor точно ли это должно быть здесь?
        for order in orders:
            if not order.service_order_id:
                order.service_order_id = get_free_service_order_id(order.service)

        request_params = [passport_uid, [order_params(order) for order in orders]]
        if service_token:
            request_params.append(service_token)
        response_list = api.medium().CreateOrUpdateOrdersBatch(*request_params)

        errors = [u'Параметры: {}, \nОшибка: {}'.format(order_params(order), order_response[1])
                  for order, order_response in zip(orders, response_list)
                  if order_response[0] == -1]
        if errors:
            reporter.attach(u'Ошибки создания/обновления заказов', reporter.pformat(errors))
            raise utils.TestsError('CreateOrUpdateOrdersBatch returned errors: {}'.format(errors))

        result_orders = []
        for order in orders:
            is_created = order.id is None
            if is_created:
                order_id = db.balance().execute(query="SELECT id FROM T_ORDER  WHERE SERVICE_ID = :service_id "
                                                      "AND SERVICE_ORDER_ID = :service_order_id",
                                                named_params={'service_id': order.service.id,
                                                              'service_order_id': order.service_order_id},
                                                single_row=True, fail_empty=True)['id']
                result_order = order.new(id=order_id)
            else:
                result_order = order
            reporter.attach(u'{} {} c параметрами'.format(u'Создали' if is_created else u'Обновили', result_order),
                            str_values(order.changes))
            result_orders.append(result_order)

        return result_orders[0] if len(result_orders) == 1 else result_orders


def create_or_update_person(person, passport_uid=defaults.PASSPORT_UID):
    def person_params(person):
        # type: (obj.Person) -> dict

        mandatory = {'client_id': person.client.id,
                     'type': person.type.code}
        _validate_mandatory(mandatory)
        return utils.merge_dicts([person.params,
                                  utils.remove_empty({'person_id': person.id}),
                                  mandatory,
                                  person.params_changes])

    is_create = person.id is None
    with reporter.step(u'Создаем плательщика с типом {} для клиента {}'.format(person.type, person.client) if is_create
                       else u'Обновляем плательщика {}'.format(person)):
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            response = api.medium().CreatePerson(passport_uid, person_params(person))
        person_id = response  # todo-igogor добавить валидацию ответа и обработку ошибки

        result_person = person.new(id=person_id) if is_create else person
        reporter.attach(u'{} {} с параметрами'.format(u'Создан' if is_create else u'Обновлен', result_person),
                        person.changes)
        return result_person


class ContractParams(object):
    MONTHS_NUMBERS_TO_STRING = {
        1: u'янв', 2: u'фев', 3: u'мар', 4: u'апр', 5: u'май', 6: u'июн', 7: u'июл', 8: u'авг',
        9: u'сен', 10: u'окт', 11: u'ноя', 12: u'дек'
    }

    @staticmethod
    def add_value(contract_params, contract_key, value):
        contract_params[contract_key] = value or ''

    @staticmethod
    def add_checkbox_set(contract_params, contract_key, values):
        for key in contract_params.keys():
            if key.startswith(contract_key):
                contract_params.pop(key)

        contract_params[contract_key] = u'1'

        for value in values:
            contract_params['{}-{}'.format(contract_key, value)] = value

    @staticmethod
    def add_checkbox_date(contract_params, contract_key, string_date):
        keys = contract_key.split(',')

        if string_date is None:
            for key in keys:
                contract_params.pop(key, None)
            return

        # setting flags
        contract_params[keys[0]] = ''
        contract_params[keys[0] + '-checkpassed'] = u'1'

        # ...-date - date for display in format: 'DD MON YYYY г.'
        datetime_value = dateutil.parser.parse(string_date)
        contract_params[keys[1]] = u'{0} {1} {2} г.'.format(
            datetime_value.day,
            ContractParams.MONTHS_NUMBERS_TO_STRING[datetime_value.month],
            datetime_value.year
        )

        contract_params[keys[2]] = string_date  # real date

    @staticmethod
    def add_checkbox(contract_params, contract_key, value):
        contract_params.pop(contract_key, None)

        if value == 1:
            contract_params[contract_key] = ''

    @staticmethod
    def add_advisor_price(contract_params, contract_key, price):
        contract_params['supplements-2'] = 2
        contract_params[contract_key] = price

    @staticmethod
    def set_price(contract_params, contract_key, prices):
        prices_list = json.loads(contract_params[contract_key])
        not_found_products = [product_id for product_id, _ in prices]

        for product_id, price in prices:
            for price_dict in prices_list:
                if price_dict['id'] == product_id:
                    price_dict['price'] = price
                    not_found_products.remove(product_id)

        contract_params[contract_key] = json.dumps(prices_list)

        if not_found_products:
            raise KeyError('Product with IDs {} were not found'.format(not_found_products))

    @staticmethod
    def get_processor(db_key):
        special_processors = {'SERVICES': ContractParams.add_checkbox_set,
                              'SUPPLEMENTS': ContractParams.add_checkbox_set,

                              'IS_SIGNED': ContractParams.add_checkbox_date,
                              'IS_FAXED': ContractParams.add_checkbox_date,
                              'IS_CANCELLED': ContractParams.add_checkbox_date,
                              'IS_SUSPENDED': ContractParams.add_checkbox_date,
                              'SENT_DT': ContractParams.add_checkbox_date,
                              'DEAL_PASSPORT': ContractParams.add_checkbox_date,

                              'NON_RESIDENT_CLIENTS': ContractParams.add_checkbox,
                              'REPAYMENT_ON_CONSUME': ContractParams.add_checkbox,
                              'PERSONAL_ACCOUNT': ContractParams.add_checkbox,
                              'LIFT_CREDIT_ON_PAYMENT': ContractParams.add_checkbox,
                              'PERSONAL_ACCOUNT_FICTIVE': ContractParams.add_checkbox,
                              'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': ContractParams.add_checkbox,
                              'IS_BOOKED': ContractParams.add_checkbox,
                              'TEST_MODE': ContractParams.add_checkbox,

                              'ADVISOR_PRICE': ContractParams.add_advisor_price,

                              'PRODUCTS_DOWNLOAD': ContractParams.set_price,
                              'PRODUCTS_REVSHARE': ContractParams.set_price}
        return special_processors.get(db_key, ContractParams.add_value)

    @staticmethod
    def value_to_contract_format(value):
        if isinstance(value, datetime):
            return utils.Date.date_to_iso_format(value)
        return value

    @staticmethod
    def contract_key_to_contract_format(key):
        special_keys = {'IS_SIGNED': 'is-signed,is-signed-date,is-signed-dt',
                        'IS_FAXED': 'is-faxed,is-faxed-date,is-faxed-dt',
                        'IS_CANCELLED': 'is-cancelled,is-cancelled-date,is-cancelled-dt',
                        'IS_SUSPENDED': 'is-suspended,is-suspended-date,is-suspended-dt',
                        'SENT_DT': 'sent-dt,sent-dt-date,sent-dt-dt',
                        'DEAL_PASSPORT': 'deal-passport,deal-passport-date,deal-passport-dt',
                        'SCALE': 'wholesale-agent-premium-awards-scale-type'}
        return special_keys.get(key, utils.String.to_kebab_case(upper_underscore=key))

    @staticmethod
    def collateral_key_to_contract_format(key, collateral_type_id):
        special_keys = {'CONTRACT2_ID': 'id',
                        'XXX': 'col-new-print-form-type',
                        'DT': 'col-new-dt',
                        'IS_SIGNED': 'col-new-is-signed,col-new-is-signed-date,col-new-is-signed-dt',
                        'IS_FAXED': 'col-new-is-faxed,col-new-is-faxed-date,col-new-is-faxed-dt',
                        'IS_BOOKED': 'col-new-is-booked,col-new-is-booked-date,col-new-is-booked-dt',
                        'UFS_PAYMENT': 'col-new-group02-grp-{}-partner-commission-sum'.format(collateral_type_id),
                        'UFS_REFUND': 'col-new-group02-grp-{}-partner-commission-sum2'.format(collateral_type_id)
                        }
        # todo-igogor не уверен что это формат для всех параметров
        return special_keys.get(key,
                                'col-new-group02-grp-{}-{}'.format(collateral_type_id,
                                                                   utils.String.to_kebab_case(upper_underscore=key)))

    @staticmethod
    def _make_params(params, special_params_data):
        # type: (dict, list) -> dict
        for db_key, contract_key, value in special_params_data:
            processor = ContractParams.get_processor(db_key=db_key)
            processor(params, contract_key, ContractParams.value_to_contract_format(value))
            params.pop(db_key)
        return params

    @staticmethod
    def make_contract_params(contract):
        # type: (obj.Contract) -> dict
        attr_params = utils.remove_empty({
            'ID': contract.id,
            'EXTERNAL_ID': contract.external_id,
            'CLIENT_ID': contract.client.id,
            'PERSON_ID': contract.person.id,
            'commission': contract.type.id,  # ебаный биллинг
            'SERVICES': [service.id for service in contract.services] or None,
            'PAYMENT_TYPE': contract.payment_type,
            # todo-igogor что делать когда в поле кладется объект не того типа
            'FIRM': contract.firm if isinstance(contract.firm, (str, unicode)) else contract.firm.id,
            'COUNTRY': contract.country.id,
            'CURRENCY': contract.currency.num_code,
            'DT': contract.start_dt,
            'FINISH_DT': contract.finish_dt,
            'IS_SIGNED': contract.signed_dt,
            'IS_FAXED': contract.faxed_dt,
            'IS_CANCELLED': contract.cancelled_dt,
            'IS_SUSPENDED': contract.suspended_dt,
            'SENT_DT': contract.sent_dt,
            'IS_BOOKED': contract.booked_dt,
        })
        contract_params = utils.merge_dicts([contract.params, attr_params])
        special_params_data = [(db_key, ContractParams.contract_key_to_contract_format(db_key), value)
                               for db_key, value in contract_params.iteritems() if db_key.isupper()]

        return ContractParams._make_params(contract_params, special_params_data)

    @staticmethod
    def make_collateral_params(collateral):
        # type: (obj.Collateral) -> dict
        attr_params = utils.remove_empty({
            'ID': collateral.id,
            'CONTRACT2_ID': collateral.contract.id,
            'DT': collateral.start_dt,
            'FINISH_DT': collateral.finish_dt,
            'IS_SIGNED': collateral.signed_dt,
            'IS_FAXED': collateral.faxed_dt,
            'IS_BOOKED': collateral.booked_dt,
        })
        collateral_params = utils.merge_dicts([collateral.params, attr_params])
        special_params_data = [(db_key,
                                ContractParams.collateral_key_to_contract_format(db_key, collateral.type_id),
                                value)
                               for db_key, value in collateral_params.iteritems() if db_key.isupper()]

        creation_params = ContractParams._make_params(collateral_params, special_params_data)
        if collateral.id is None:
            return creation_params
        else:
            return {key.replace('col-new', 'col-' + str(collateral.id)): value
                    for key, value in creation_params.iteritems()}


def create_or_update_contract(contract, passport_uid=defaults.PASSPORT_UID):
    # type: (obj.Contract, str) -> obj.Contract
    is_create = contract.id is None
    with reporter.step(u"{} договор {}".format(u'Создаем' if is_create else u'Обновляем',
                                               str(contract))):
        contract_params = ContractParams.make_contract_params(contract)
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            response = api.medium().CreateContract(passport_uid, contract_params)
        # todo-igogor валидация респонса
        if is_create:
            result_contract = contract.new(id=response['ID'], external_id=response['EXTERNAL_ID'])
            # todo-igogor убираем параметры которые не могут быть созданы вызовом создания, только отдельно. Гемор
            # перенести внутрь объекта?
            result_contract = result_contract.new(collaterals=[], cancelled_dt=None)
        else:
            result_contract = contract.new()

        reporter.attach(u'{} {} с параметрами'.format(u'Создан' if is_create else u'Обновлен',
                                                      str(result_contract)),
                        contract.changes)  # todo-igogor здесь интересны только кастомно заданные
        reporter.attach(u'Ссылка на договор', web.AdminInterface.ContractEditPage.url(contract_id=result_contract.id))
        # if ignored_keys:   # todo-igogor надо бы как-то прикрутить
        #     reporter.attach(u'Игнорированные параметры', ignored_keys)
        return result_contract


def get_contract_collateral_ids(contract_id):
    result = db.balance().execute(
        'SELECT * FROM t_contract_collateral WHERE contract2_id = :contract_id',
        {'contract_id': contract_id})
    return [col['id'] for col in result]


def create_or_update_collateral(collateral, passport_uid=defaults.PASSPORT_UID):
    # type: (obj.Collateral, str) -> obj.Collateral
    is_create = collateral.id is None
    with reporter.step(u"{} допсоглашение {} для договора {}".format(u'Создаем' if is_create else u'Обновляем',
                                                                     str(collateral), str(collateral.contract))):
        collateral_params = ContractParams.make_collateral_params(collateral)
        if is_create:
            contract_collaterals_before = get_contract_collateral_ids(collateral.contract.id)  # todo-igogor не очень
            with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
                response = api.medium().CreateContract(passport_uid, collateral_params)
            # todo-igogor валидация респонса
            contract_collaterals_after = get_contract_collateral_ids(collateral.contract.id)
            collateral_id = set(contract_collaterals_after).difference(contract_collaterals_before).pop()
            result_collateral = collateral.new(id=collateral_id)
        else:
            with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
                response = api.medium().CreateContract(passport_uid, collateral_params)
            # todo-igogor валидация респонса
            result_collateral = collateral.new()

        collateral.contract.on_collateral(result_collateral)

        reporter.attach(u'{} допсоглашение {} с параметрами'.format(u'Создан' if is_create else u'Обновлен',
                                                                    str(result_collateral)),
                        collateral_params)  # todo-igogor здесь интересны только кастомно заданные
        reporter.attach(u'Ссылка на договор и ДС',
                        web.AdminInterface.ContractEditPage.url(contract_id=collateral.contract.id))
        # if ignored_keys:
        #     reporter.attach(u'Игнорированные параметры', ignored_keys)  # todo-igogor надо бы как-то прикрутить
        return result_collateral


def create_request(request, passport_uid=defaults.PASSPORT_UID):
    def request_order_params(request_order):
        # type: (obj.RequestOrder) -> dict

        mandatory = {'ServiceID': request_order.order.service.id,
                     'ServiceOrderID': request_order.order.service_order_id,
                     'Qty': request_order.qty}
        _validate_mandatory(mandatory)

        return utils.merge_dicts([
            request_order.params,
            utils.remove_empty({'BeginDT': utils.Date.date_to_iso_format(request_order.begin_dt, pass_none=True)}),
            mandatory,
            request_order.params_changes])

    # todo-igogor надо еще даты выводить?
    with reporter.step(u'Создаем реквест на клиента {} на заказы: {}'.format(
            request.client, u', '.join(map(str, request.lines)))):
        orders_params_list = [request_order_params(request_order) for request_order in request.lines]
        response = api.medium().CreateRequest(passport_uid, request.client.id, orders_params_list, request.params)
        # todo-igogor обработка ошибки в респонсе
        request_id = utils.get_url_parameter(response[3], param='request_id')[0]

        created_request = request.new(id=request_id)

        # todo-igogor здесь не выводятся параметры которые были переопределены в RequestOrder
        reporter.attach(u'Создан {} с параметрами'.format(created_request), request.changes)
        # todo-igogor ссылки надо добавить во все остальные сущности
        # todo-igogor ссылки должны формироваться в PageObjects - RequestPage.url(request_id=request.id)
        reporter.report_url(u'Ссылка на реквест'.format(created_request),
                            '{base_url}/paypreview.xml?request_id={request_id}'.format(
                                base_url=env.balance_env().balance_ci,
                                request_id=created_request.id))
        return created_request


# todo-igogor добавление endbuyer_id надо не забыть. И еще обработку total_sum если она нужна
def create_invoice(invoice, passport_uid=defaults.PASSPORT_UID):
    # todo-igogor плохо, что ключи словарей xmlrpc живут как в самих объектах, так и здесь. Лучше бы только здесь.
    def invoice_params(invoice):
        mandatory = {'RequestID': invoice.request.id,
                     'PersonID': invoice.person.id,
                     'PaysysID': invoice.paysys.id}
        _validate_mandatory(mandatory)

        return utils.merge_dicts([
            invoice.params,
            utils.remove_empty(
                # todo-igogor здесь можно будет убрать условную логику когда будет нормально инициализирован Contract
                {'ContractID': invoice.contract.id if invoice.contract else None,
                 # todo-igogor слезы
                 'Credit': None if invoice.is_credit is None else 1 if invoice.is_credit else 0,
                 'Overdraft': None if invoice.is_overdraft is None else 1 if invoice.is_overdraft else 0}),
            mandatory,
            invoice.params_changes])

    # todo-igogor можно выставить постоплатный счет в овердрафт? По тексту старого степа.
    with reporter.step(u'Создаем {invoice_type} счет: {request}, {person}, {paysys}{contract}'.format(
            invoice_type=u'овердрафтный' if invoice.is_overdraft else u'постоплатный' if invoice.is_credit \
                    else u'предоплатный',
            request=invoice.request, person=invoice.person, paysys=invoice.paysys,
            contract=u', {}'.format(invoice.contract) if invoice.contract else u'')):
        response = api.medium().CreateInvoice(passport_uid, invoice_params(invoice))
        invoice_id = response  # todo-igogor обработка ошибок

        with reporter.step(u'Поднимаем данные о счете из базы'):
            result = db.balance().execute(query="SELECT external_id, total_sum, consume_sum "
                                                "FROM t_invoice WHERE id = :invoice_id",
                                          named_params={'invoice_id': invoice_id},
                                          single_row=True, fail_empty=True)

        created_invoice = invoice.new(id=invoice_id, external_id=result['external_id'],
                                      total=obj.QtyMoney(qty=invoice.request.qty, money=result['total_sum']))
        reporter.attach(u'Создан {} с параметрами'.format(created_invoice), str_values(invoice.changes))
        reporter.report_url(u'Ссылка на счет',
                            '{base_url}/invoice.xml?invoice_id={invoice_id}'.format(
                                base_url=env.balance_env().balance_ai,
                                invoice_id=invoice_id))

        return created_invoice


def pay(invoice, payment_sum=None, payment_dt=None, consumes_from_db=True):
    with reporter.step(u'Оплачиваем "быстро" счёт {} на сумму: {}, на дату: {}'.format(
            invoice.id, payment_sum or u'полностью', payment_dt or u'текущую')):
        payment_sum = payment_sum or invoice.total.money  # todo-igogor переименовать money в sum чтоб более привычно
        date = payment_dt or datetime.today()

        with reporter.step(u'Вставляем данные о платеже в базу'):  # todo-igogor недостаточно информативно
            # TODO: how to work with dates? no need to do to_date(dt, <format>)
            db.balance().execute(
                'INSERT INTO t_correction_payment (dt, doc_date, sum, memo, invoice_eid) '
                'VALUES (to_date(:paymentDate, \'DD.MM.YYYY HH24:MI:SS\'), '
                'to_date(:paymentDate,\'DD.MM.YYYY HH24:MI:SS\'), :total_sum, \'Testing\', :external_id)',
                {'paymentDate': date, 'total_sum': payment_sum, 'external_id': invoice.external_id})
        with reporter.step(u'Запускаем тестовый синхронный разборщик платежа'):
            api.test_balance().OEBSPayment(invoice.id)

        if consumes_from_db:
            with reporter.step(u'Поднимаем данные о консьюмах счета из базы'):
                # todo-igogor как определить какие консьюмы уже были прикручены.
                # Кажется что по id проще всего но бля. Не будет работать для случая когда делаем второй платеж.
                result = db.balance().execute('SELECT PARENT_ORDER_ID, CONSUME_QTY, CONSUME_SUM'
                                              ' FROM BO.T_CONSUME WHERE INVOICE_ID=:invoice_id',
                                              dict(invoice_id=invoice.id),
                                              descr=u'Получаем консьюмы счета {}'.format(invoice.id))
                consumes = [obj.Consume(order=obj.Order().new(id=line['parent_order_id']),
                                        qty=line['consume_qty'], money=line['consume_sum'])
                            for line in result]
                invoice.on_payment(consumes, clear_consumes=True)

            reporter.attach(u'Зачисления по заказам', map(unicode, consumes))

        reporter.attach(u'Оплатили счет {} на сумму {} на дату {}'.format(invoice.id, payment_sum, date))


def do_completions(order, bucks=None, shows=None, clicks=None, units=None, days=None, money=None,
                   do_stop=None, campaigns_dt=None, completion_from_db=True):
    params = {'service_id': order.service.id, 'service_order_id': order.service_order_id}
    params.update(utils.remove_empty({'Bucks': bucks,
                                      'Shows': shows,
                                      'Clicks': clicks,
                                      'Units': units,
                                      'Days': days,
                                      'Money': money,
                                      'do_stop': do_stop}))

    with reporter.step(u'Откручиваем {} на {} на заказе {}'.format(params, campaigns_dt or u'текущую дату',
                                                                   str(order))):

        if campaigns_dt:
            response = api.test_balance().OldCampaigns(params, campaigns_dt)
        else:
            response = api.test_balance().Campaigns(params)
        # todo-igogor валидация респонса

        if completion_from_db:
            with reporter.step(u'Поднимаем данные об открутках из базы'):
                db_completion = db.balance().execute('SELECT COMPLETION_SUM, COMPLETION_QTY FROM BO.T_CONSUME'
                                                     ' WHERE PARENT_ORDER_ID = :order_id', {'order_id': order.id})
            order.on_completion(qty=db_completion['completion_qty'], money=db_completion['completion_sum'])


def create_act(act):
    with reporter.step(u'Выставляем акт по счёту {0} на {1}'.format(act.invoice.id, act.dt or u'текущую дату')):
        response = api.test_balance().OldAct(act.invoice.id, act.dt)
        # todo-igogor валидация респонса
        created_act = act.new(id=response)
        # todo-igogor нужно ли хранить акты счета в счете?
        # todo-igogor поднимать external_id из базы?
        # todo-igogor поднимать информацию о строчках из базы?

        return created_act


def prepare(balance_object, skippable=False, descr=u'пустой объект'):
    if obj.is_empty(balance_object):
        if skippable:
            return balance_object
        else:
            raise utils.TestsError(u'Неверно заполнен объект - ' + descr)
    elif balance_object.id:
        return balance_object  # todo-igogor как быть с обновлением? Что если человек хочет создать используя существующие объекты?

    if isinstance(balance_object, obj.Client):
        agency = prepare(balance_object.agency, skippable=True)
        return create_or_update_client(client=balance_object.new(agency=agency))

    elif isinstance(balance_object, obj.Person):
        client = prepare(balance_object.client, skippable=False, descr=u'в плательщике не задан клиент')
        return create_or_update_person(person=balance_object.new(client=client))

    elif isinstance(balance_object, obj.Contract):
        client = prepare(balance_object.client, skippable=False, descr=u'в договоре не задан клиент')
        person = prepare(balance_object.person.new(client=client), skippable=False,
                         descr=u'В договоре не задан плательщик')
        created_contract = create_or_update_contract(contract=balance_object.new(client=client, person=person))

        if balance_object.collaterals:
            created_collaterals = []
            for collateral in balance_object.collaterals:
                created_collateral = create_or_update_collateral(collateral.new(contract=created_contract))
                created_collaterals.append(created_collateral)
            created_contract.collaterals = created_collaterals

        if balance_object.cancelled_dt:
            created_contract = create_or_update_contract(created_contract.new(cancelled_dt=balance_object.cancelled_dt))

        return created_contract

    elif isinstance(balance_object, obj.Order):
        client = prepare(balance_object.client, skippable=False, descr=u'в заказе не задан клиент')
        agency = prepare(balance_object.agency, skippable=True)
        group_order = prepare(balance_object.group_order, skippable=True)
        return create_or_update_orders([balance_object.new(client=client, agency=agency, group_order=group_order)])

    elif isinstance(balance_object, obj.Request):
        client = prepare(balance_object.client, skippable=False, descr=u'в реквесте не задан клиент')
        request_orders = []
        for line in balance_object.lines:
            order_client = line.order.client or balance_object.lines[0].order.client or client
            order = prepare(line.order.new(client=order_client), skippable=False,
                            descr=u'в реквесте не задан заказ')
            request_orders.append(line.new(order=order))
        return create_request(request=balance_object.new(client=client, lines=request_orders))

    elif isinstance(balance_object, obj.Invoice):
        client = balance_object.contract.client or balance_object.request.client or balance_object.client
        request = prepare(balance_object.request.new(client=client), skippable=False, descr=u'в счете не задан реквест')
        person = prepare(balance_object.person.new(client=request.client), skippable=False,
                         descr=u'в счете не задан плательщик')
        contract = prepare(balance_object.contract.new(client=request.client, person=person),
                           skippable=True)
        invoice = create_invoice(invoice=balance_object.new(request=request, contract=contract, person=person))
        if balance_object.consumes:
            pay(invoice=balance_object, payment_sum=balance_object.total.sum)
            for consume in balance_object.consumes:
                if consume.completion_qty:
                    # todo-igogor передавать дату и do_stop
                    # todo-igogor как быть когда на заказе уже есть открутки?
                    do_completions(consume.order, **{consume.order.product.type.code.lower(): consume.completion_qty})
        return invoice

    elif isinstance(balance_object, obj.Act):
        invoice = prepare(balance_object.invoice, skippable=False, descr=u'в акте не задан счет')
        return create_act(act=balance_object.new(invoice=invoice))

    else:
        raise utils.TestsError(u'В методе передан объект неверного типа')


def put_consume_on_order(context, order, consume_qty):
    with reporter.step(u'Добавляем на заказ {} консьюм с qty = {}'.format(order, consume_qty)):
        person_id = steps.PersonSteps.create(order.client.id, context.person_type.code)
        orders_list = [{'ServiceID': order.service.id,
                        'ServiceOrderID': order.service_order_id,
                        'Qty': float(consume_qty),
                        'BeginDT': datetime.now()}]
        request_id = steps.RequestSteps.create(order.client.id, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0, overdraft=0)
        steps.InvoiceSteps.pay(invoice_id)

        qty, money = consume_qty, context.money(consume_qty)
        if order.is_converted:
            qty, money = context.qty(consume_qty), consume_qty
        order.on_consume(qty=qty, money=money)
        return order


def put_completion_on_order(context, order, completion_qty):
    with reporter.step(u'Откручиваем заказ {} на qty = {}'.format(order, completion_qty)):
        total_completion_qty = order.completion_qty + completion_qty
        steps.CampaignsSteps.do_campaigns(service_id=order.service.id,
                                          service_order_id=order.service_order_id,
                                          campaigns_params={order.product.type.code: float(total_completion_qty)},
                                          campaigns_dt=datetime.now())
        order.on_completion(qty=completion_qty, money=context.money(completion_qty))
        return order


# todo-igogor а ведь тут может быть случай для нескольких заказов
# todo-igogor нет возможности вызвать с катомными невалидными параметрами для получения ошибки
def transfer(context, from_order, to_order, qty, conversion_context=None, from_consume=None, to_consume=None):
    with reporter.step(u'Перенос с заказа {} на заказ {} , qty = {}'.format(from_order, to_order, qty)):
        # #balance_logic
        consume_old_qty = from_order.consume_money if from_order.is_converted else from_order.consume_qty

        api.test_balance().CreateTransferMultiple(defaults.PASSPORT_UID,
                                                  [{'ServiceID': from_order.service.id,
                                                    'ServiceOrderID': from_order.service_order_id,
                                                    'QtyOld': float(consume_old_qty),
                                                    'QtyNew': float(consume_old_qty - qty)}],
                                                  [{'ServiceID': to_order.service.id,
                                                    'ServiceOrderID': to_order.service_order_id,
                                                    'QtyDelta': 1}])

        # #balance_logic
        # todo-igogor я сожалею о всем далее написаном, наверное не стоило и ненавижу мультивалютность.
        if not from_consume:
            if from_order.is_converted:
                # для конвертированного заказа переносимый qty считается деньгами, но фишечный qty надо обновить
                from_consume = obj.QtyMoney(qty=conversion_context.qty(qty), money=qty)
            else:
                # дефолтный консьюм переноса для однообразных заказов
                from_consume = obj.QtyMoney(qty=qty, money=context.money(qty))
        # используется в качестве приходящего значения, в переносах между денежным и конвертированным заказами
        rounded_qty = utils.dround(qty, 2)
        if not to_consume:
            if to_order.is_converted:
                # для конвертированного заказа переносимый qty считается деньгами, но фишечный qty надо обновить
                to_consume = obj.QtyMoney(qty=conversion_context.qty(qty), money=qty)
            elif from_order.is_converted and to_order.is_multicurrency:
                to_consume = obj.QtyMoney(qty=qty, money=rounded_qty)

        from_order.on_transfer(to_order, transfer_consume=from_consume, to_order_consume=to_consume)


def group_transfer(parent):
    with reporter.step(u'Перенос по общему счету для клиента {}'.format(parent.client)):
        steps.CommonSteps.export('UA_TRANSFER', 'Client', parent.client.id)
        parent.on_group_transfer()


# todo-igogor переделать на новый степ
def group_orders(parent, members, without_transfer=True):
    # type: (OrderInfo, List[OrderInfo]) -> OrderInfo
    with reporter.step(u'Объединяем в заказы в группу. Главный: {}, дочерние: {}'
                               .format(parent, [str(member) for member in members])):
        steps.OrderSteps.merge(parent_order=parent.id, sub_orders_ids=[member.id for member in members],
                               group_without_transfer=1 if without_transfer else 0)
        parent.on_grouping(parent, members)
        for member in members:
            member.on_grouping(parent, members)
        return parent


def trigger_notification(trigger):
    with reporter.step(u'Инициируем нотификацию:'):
        return trigger()


def get_order_notifications(order):
    with reporter.step(u'Получаем нотификации для заказа {}'.format(str(order))):
        order_opcode = 1
        notifications = api.test_balance().GetNotification(order_opcode, order.id)
        notifications = [notification['args'][0] for notification in notifications]
        reporter.attach(u'Нотификации', utils.Presenter.pretty(notifications))
        return notifications


# todo-igogor перенести в utils, в репортер? Сделать в pformat чтобы делался по опции str вместо repr?
def str_values(objects_dict):
    return {key: unicode(object) for key, object in objects_dict.iteritems()}


# todo-igogor внедрить во все методы формирования параметров
def _validate_mandatory(mandatory):
    missing_fields = set(mandatory.keys()).difference(set(utils.remove_empty(mandatory).keys()))
    if missing_fields:
        raise utils.TestsError('Missing mandatory parameters: {}'.format(', '.join(missing_fields)))


# В таком виде коллбэки создания переиспользуемы в разных модулях
# Здесь отделена логика преподготовки сущностей для создания заказа любого типа, каждый метод можно вызвать отдельно
# Класс нужен для хранения дефолтов, чтобы можно было их переопределить в каждом тесте один раз, а не в каждом вызове
class ObjectFactory(object):
    def __init__(self, consume_qty, completion_qty):
        self.consume_qty = consume_qty
        self.completion_qty = completion_qty
        # todo-igogor использовать переменную в reporting.step() для исключения всех вложенных степов кроме нижних
        # вывести степ верхнего уровня, заменить значение на False и тогда не будет многоуровневых степов
        self.report = True

    @staticmethod
    def _optional(arg):
        return str(arg) if arg else u'Новый'

    def new_client(self, context, is_agency=False, agency=None, migrate_to_currency=None):
        # todo-igogor как тут понять что надо пресоздать агентство?
        with reporter.step(u'Создаем клиента в контексте: {}'.format(context)):
            client_template = obj.Client.for_creation(context=context, agency=agency)
            if migrate_to_currency:
                client_template.migrate_to_currency = migrate_to_currency
            return create_or_update_client(client=client_template)

    def new_order(self, context, client=None):
        with reporter.step(u'Создаем заказ на клиента: {} в контексте: {}'.format(self._optional(client), context)):
            if not client:
                client = self.new_client(context)

            return create_or_update_orders(orders=[obj.Order.for_creation(context=context, client=client)])[0]

    def order_with_consume(self, context, consume_qty=None, client=None):
        consume_qty = consume_qty or self.consume_qty

        with reporter.step(u'Создаем заказ с консьюмом qty={} на клиента: {} в контексте {}'
                                   .format(consume_qty, self._optional(client), context)):
            order = self.new_order(context, client)
            return put_consume_on_order(context=context, order=order, consume_qty=consume_qty)

    def order_with_several_consumes(self, context, consume_qty=None, client=None):
        consume_qty = consume_qty or self.consume_qty
        with reporter.step(u'Создаем заказ с несколькими консьюмами qty = {} на клиента: {} в контексте {}'
                                   .format(consume_qty, self._optional(client), context)):
            order = self.order_with_consume(context=context, consume_qty=consume_qty or self.consume_qty, client=client)
            put_consume_on_order(context=context, order=order, consume_qty=consume_qty)
            return order

    def order_with_consume_and_completion(self, context, consume_qty=None, completion_qty=None,
                                          client=None):
        consume_qty = consume_qty or self.consume_qty
        completion_qty = completion_qty or self.completion_qty
        with reporter.step(u'Создаем заказ с консьюмом qty = {} и откруткой qty = {} на клиента: {} в контексте {}'
                                   .format(consume_qty, completion_qty, self._optional(client), context)):
            order = self.order_with_consume(context=context, consume_qty=consume_qty, client=client)

            return put_completion_on_order(context=context, order=order, completion_qty=completion_qty)

    def order_with_several_consumes_and_completions(self, context, consume_qty=None, completion_qty=None, client=None):
        consume_qty = consume_qty or self.consume_qty
        completion_qty = completion_qty or self.completion_qty
        with reporter.step(u'Создаем заказ с несколькими консьюмами и открутками на клиента: {} в контексте {}'
                                   .format(self._optional(client), context)):
            order = self.order_with_consume_and_completion(context=context, consume_qty=consume_qty,
                                                           completion_qty=completion_qty, client=client)
            put_consume_on_order(context=context, order=order, consume_qty=consume_qty)
            put_completion_on_order(context=context, order=order, completion_qty=completion_qty)
            # todo-igogor надо аттачить возвращаемые значения?
            return order
