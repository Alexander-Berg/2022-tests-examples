# coding: utf-8

"""
Модуль для синхронизации объектов
между тестовыми базами balance и "Лёгенькой" outstaff

"""
from future.utils import raise_with_traceback
import sys
import copy
import json
import inspect
import argparse
from datetime import datetime
from balance.application import getApplication
from balance import mapper
from balance import scheme
from balance.payments import scheme_payments
from butils import logger
import sqlalchemy as sa
from sqlalchemy.testing import emits_warning
from sqlalchemy import exc as sa_exc


__author__ = 'anurmanov'

log = logger.get_logger()

emits_warning_1 = \
emits_warning("Usage of the 'collection append' operation is not currently" + \
" supported within the execution stage of the flush process")

emits_warning_2 = \
emits_warning("Attribute history events accumulated on 1 previously clean instances" + \
" within inner-flush event handlers have been reset, and will not result in database updates")

query_for_parent_payment = \
    "select p.*,  " \
    "r.trust_refund_id as r__trust_refund_id, " \
    "cb.trust_payment_id as cb__trust_payment_id " \
    "from t_payment p, t_ccard_bound_payment cb, t_refund r " \
    "where p.trust_payment_id = {trust_payment_id} " \
    "and p.id = cb.id " \
    "and p.id = r.id "

query_for_payment_by_id = \
    "select p.*,  " \
    "r.trust_refund_id as r__trust_refund_id, " \
    "cb.trust_payment_id as cb__trust_payment_id " \
    "from t_payment p, t_ccard_bound_payment cb, t_refund r " \
    "where p.id = {id} " \
    "and p.id = cb.id " \
    "and p.id = r.id "

query_for_t_ccard_bound_payment = \
    "select * " \
    "from t_ccard_bound_payment " \
    "where id = {}"

query_for_t_refund = \
    "select * " \
    "from t_refund " \
    "where id = {}"


class TestClientDoesNotExist_Exception(Exception):
    def __init__(self, test_client_name):
        msg = "Test client '{}' doesn't exist!".format(test_client_name)
        super(TestClientDoesNotExist_Exception, self).__init__(msg)


def copy_mapper_object(obj, mapper_class):
    new_obj = mapper_class()
    for key, value in obj.__dict__.items():
        if key[0] != '_':
            setattr(new_obj, key, copy.deepcopy(value))
    return new_obj


def create_db_sync_metadata_table():
    """Создать структуру таблицы t_test_db_sync_metadata

    :return: None
    """

    app = getApplication()
    session = app.new_session(database_id='balance')

    with session.begin():
        try:
            session.execute('''create table bo.t_test_db_sync_metadata(
                table_name varchar(512) not null,
                filter_rules varchar(4000) null,
                last_state_dt timestamp null,
                sequence_id varchar(128) null,
                CONSTRAINT t_test_db_sync_metadata_pk PRIMARY KEY (table_name)
            )''')
        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))


def _sequence_nextval(self, seq_name):
    return self.execute('select {}.nextval from dual'.\
                        format(seq_name)).fetchone()['nextval']


class DatabaseSynchronizer(object):

    def __init__(self, session_source, session_destin, test_mode=False):
        self.session_source = session_source
        self.session_destin = session_destin
        self.sync_metadata = None
        self.test_mode = test_mode
        self.sync_start_time = dict()
        #bind sequence_nextval method
        if not hasattr(self.session_destin, 'sequence_nextval'):
            setattr(self.session_destin, 'sequence_nextval',
                    lambda *args, **kwargs: _sequence_nextval(
                        self.session_destin, *args, **kwargs)
            )


    def prepare_sync_metadata(self):
        """Подготовить метаданные для синхронизации

        :return (dict): словарь метаданных, описывающий переносимые объекты данные
        """

        try:
            sql_text = "select table_name, " + \
                       "filter_rules, " + \
                       "last_state_dt, " + \
                       "ignore_columns, " + \
                       "sequence_id, " + \
                       "mapping_table, " + \
                       "search_columns " + \
                       "from t_test_db_sync_metadata"

            data = self.session_source.execute(sql_text).fetchall()

            metadata = dict()
            for row in data:
                table_name = row['table_name']
                metadata[table_name] = dict(row.items())

                ignore_columns = metadata[table_name]['ignore_columns']
                if ignore_columns:
                    ignore_columns = list(map(lambda s: s.lower().strip(),
                                              ignore_columns.split(',')))
                else:
                    ignore_columns = []
                metadata[table_name]['ignore_columns'] = ignore_columns

                search_columns = metadata[table_name]['search_columns']
                if search_columns:
                    search_columns = list(map(lambda s: s.lower().strip(),
                                              search_columns.split(',')))
                else:
                    search_columns = []
                metadata[table_name]['search_columns'] = search_columns

            self.sync_metadata = metadata
        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))


    def find_id_in_mapping_table(self, mapping_table, search_keys):
        try:
            if not search_keys:
                raise ValueError('search_keys is empty')

            query = "select id from {} where ".format(mapping_table)
            conditions = []
            for key in search_keys.keys():
                conditions.append("{}=:{}".format(key, key))
            query += " and ".join(conditions)

            data = self.session_destin.execute(query, search_keys).fetchall()
            number_of_rows = len(data)
            if number_of_rows == 0:
                return None
            elif number_of_rows == 1:
                return data[0]['id']
            else:
                raise ValueError('mapping table {} has >1 rows for search_keys: {}'\
                                 .format(mapping_table, str(search_keys)))
        except Exception:
            msg = 'Error in {}: {}'\
            .format(inspect.stack()[0][3], str(sys.exc_info()))

            log.error(msg)
            raise_with_traceback(RuntimeError(msg))


    def save_to_mapping_table(self, mapping_table, record):
        try:
            query = "insert into {}".format(mapping_table)
            query += "(" + ",".join(record.keys()) + ") "
            query += "values("
            query += ",".join([":"+key for key in record.keys()]) + ") "
            self.session_destin.execute(query, record)
        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))
            raise_with_traceback(
                sa_exc.DatabaseError('error in query: {} - params: {}'\
                              .format(query, record))
            )


    def set_sync_metadata(self, sync_metadata):
        self.sync_metadata = sync_metadata


    def get_test_client_id(self, client_name):
        try:
            respond = self.session_source.query(mapper.Client)\
                .filter_by(name=client_name)

            if respond.exists():
                return respond.one().id
            else:
                raise TestClientDoesNotExist_Exception(
                    test_client_name=client_name)
        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))


    def _update_last_state_dt_field(self, table_name):
        try:
            query = "update t_test_db_sync_metadata " + \
            "set last_state_dt = {} " + \
            "where table_name = '{}'"\
            .format(self.session_source.now(), table_name)

            with self.session_source.begin():
                self.session_source.execute(query)
        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))


    def _start_sync(self, table_name):
        """
        Код выполняемый перед началом цикла синхронизации таблицы
        :param table_name:
        :return: None
        """
        pass


    def _finish_sync(self, table_name):
        """
        Код выполняемый после окончания цикла синхронизации таблицы
        :param table_name:
        :return: None
        """
        pass


    @emits_warning_1
    @emits_warning_2
    def _sync_simple_table(self, table_name, filters, query, mapper_class, substitute_by):
        """Синхронизация простой таблицы без зависимостей

        :param table_name (str): имя таблицы
        :param filters (dict): фильтры в виде sql-текста условия where (без and в начале)
        :param query (str): строка sql запроса
        :param mapper_class (mapper.Object): mapper-класс соответствующий таблице
        :param substitute_by (dict): словарь полей для замены
        :return (dict): ключ id объекта в source-таблице, значение - новый id в destin-таблице
        """
        id_mapping = dict()

        try:
            ignore_columns = self.sync_metadata[table_name]['ignore_columns']
            mapping_table = self.sync_metadata[table_name]['mapping_table']
            search_columns = self.sync_metadata[table_name]['search_columns']
            sequence_id = self.sync_metadata[table_name]['sequence_id']
            if not sequence_id:
                raise ValueError('Empty sequence id for table {}' \
                                 .format(table_name))

            if search_columns:
                search_keys = {c: None for c in search_columns}
            else:
                search_keys = {'id': None}

            filter_rules = self.sync_metadata[table_name]['filter_rules']
            if filter_rules:
                if isinstance(filter_rules, str):
                    query += " and " + filter_rules
                else:
                    raise RuntimeError(
                        'column filter_rules is not str type for table {}'\
                        .format(table_name)
                    )

            if filters:
                if isinstance(filters, str):
                    query += " and " + filters
                else:
                    raise RuntimeError('param filters is not str type')

            data = self.session_source.execute(query).fetchall()
            data = [dict(c.items()) for c in data]

            for item in data:
                try:
                    for column in ignore_columns:
                        del item[column]

                    for column in search_keys:
                        search_keys[column] = item[column]

                    full_search = False
                    add_new_record = False
                    # попытаться найти в маппинг-таблице,
                    # иначе поиск по всей таблице
                    if mapping_table:
                        id = self.find_id_in_mapping_table(
                            mapping_table,
                            search_keys
                        )
                        if id:
                            # поиск по id, найденному в таблице-маппинге
                            respond = self.session_destin.query(mapper_class)\
                            .filter_by(id=item['id'])
                        else:
                            # если не нашли в маппинг-таблице,
                            # то считаем что имеем дело с новым объектом
                            add_new_record = True
                    else:
                        full_search = True

                    # поиск во всей таблице
                    if full_search:
                        respond = self.session_destin.query(mapper_class) \
                        .filter_by(**search_keys)

                    if not add_new_record and respond.exists():
                        record = copy_mapper_object(respond.one(), mapper_class)

                        for key, value in item:
                            record.__setattr__(key, value)
                    else:
                        item['id'] = None
                        record = mapper_class(**item)
                        record.id = self.session_destin\
                        .sequence_nextval(sequence_id)

                        add_new_record = True

                    # заменить атрибуты в объектах из словаря substitute_by,
                    # ключами являются идентификаторы атрибутов объекта
                    if substitute_by and isinstance(substitute_by, dict):
                        for key in substitute_by.keys():
                            if hasattr(record, key):
                                # атрибут объекта из базы источника
                                source_attr_value = record.__getattr__(key)
                                # поискать в словаре substitute_by
                                if source_attr_value in substitute_by[key]:
                                    # заменить на атрибут из базы назначения
                                    destin_attr_value = substitute_by[key][source_attr]
                                    record.__setattr__(key, destin_attr_value)

                    with self.session_destin.begin():
                        #если добавление записи,
                        # то надо создать запись в маппинг-таблице
                        if add_new_record and mapping_table:
                            mapping_record = {'id': record.id}
                            mapping_record.update(search_keys)
                            self.save_to_mapping_table(mapping_table, mapping_record)

                        self.session_destin.add(record)

                    id_mapping[item['id']] = record.id

                    self._update_last_state_dt_field(table_name)

                except Exception:
                    msg = 'Error in one iteration of {} with object {}: {} '\
                    .format(
                        inspect.stack()[0][3],
                        str(item),
                        str(sys.exc_info())
                    )
                    log.error(msg)
                    raise_with_traceback(
                        RuntimeError(msg)
                    )

            return id_mapping

        except Exception:
            msg = 'Error in {}: {}'\
            .format(
                inspect.stack()[0][3],
                str(sys.exc_info())
            )
            log.error(msg)
            raise_with_traceback(
                RuntimeError(msg)
            )

    def sync_clients(self, filters, substitute_by = None):
        """
        :param filters (dict): {'column_name': value}
        :param substitute_by (dict): словарь полей для замены
        :return (dict): ключ id объекта в source-таблице, значение - новый id в destin-таблице
        """
        table_name = 't_client'

        try:
            query = "select * from {table_name}  " \
                    "where update_dt_yt>to_timestamp('{dt}', '{dt_format}') " \
                .format(
                table_name=table_name,
                dt=self.sync_metadata[table_name]['last_state_dt'],
                dt_format='YYYY-MM-DD HH24:MI:SS'
            )

            return self._sync_simple_table(table_name,
                                           filters,
                                           query,
                                           mapper.Client,
                                           substitute_by)

        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))


    def sync_service_products(self, filters, substitute_by = None):
        """

        :param filters (dict): {'column_name': value}
        :param substitute_by (dict): словарь полей для замены
        :return (dict): ключ id объекта в source-таблице, значение - новый id в destin-таблице
        """
        table_name = 't_service_product'

        try:
            query = "select * " \
            "from {table_name} " \
            "where update_dt > to_timestamp('{dt}', '{dt_format}') " \
            .format(
                table_name=table_name,
                dt=self.sync_metadata[table_name]['last_state_dt'],
                dt_format='YYYY-MM-DD HH24:MI:SS'
            )

            return self._sync_simple_table(table_name,
                                           filters,
                                           query,
                                           mapper.ServiceProduct,
                                           substitute_by)

        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))


    @emits_warning_1
    @emits_warning_2
    def _sync_payment(self, item):
        """Данная функция занимается синхронизацией платежа и его зависимостей

        :param item (dict): словарь значений полей платежа из session-таблицы
        :return: None
        """
        try:
            trust_refund_id = item['r__trust_refund_id']
            trust_payment_id = item['cb__trust_payment_id']
            paysys_code = item['paysys_code']
            payment_id = item['id']

            del item['r__trust_refund_id']
            del item['cb__trust_payment_id']
            del item['id']

            # сначала переносим зависимости t_processing,
            # t_payment_method, t_terminal
            processing = None
            respond = self.session_source.query(mapper.Processing)\
                .filter_by(id=item['processing_id'])

            if respond.exists():
                processing = copy_mapper_object(respond.one(), mapper.Processing)

            payment_method = None
            respond = self.session_source.query(mapper.PaymentMethod)\
                .filter_by(id=item['payment_method_id'])

            if respond.exists():
                payment_method = copy_mapper_object(respond.one(),
                                                    mapper.PaymentMethod)

            terminal = None
            respond = self.session_source.query(mapper.Terminal)\
                .filter_by(id=item['terminal_id'])

            if respond.exists():
                terminal = copy_mapper_object(respond.one(), mapper.Terminal)

            # если terminal есть в базе-назначения,
            # тогда и договор уже есть (переносим один раз без обновления)
            try:
                with self.session_destin.begin():
                    # поиск terminal в базе-назначения
                    respond = self.session_destin.query(mapper.Terminal)\
                        .filter_by(id=item['terminal_id'])

                    if terminal and respond.exists():

                        # для таблицы t_terminal переносим договор (t_contract2)
                        # и его зависимости
                        respond = self.session_source.query(mapper.Contract)\
                            .filter_by(id=terminal.contract_id)

                        # если договор есть, то переносим его вместей с зависимостями
                        if respond.exists():

                            contract = copy_mapper_object(
                                respond.one(),
                                mapper.Contract
                            )

                            # переносим зависимость t_client
                            client = copy_mapper_object(
                                self.session_source.query(mapper.Client)\
                                .filter_by(id=contract.client_id)\
                                .one(),
                                mapper.Client
                            )
                            sequence_id = self.sync_metadata['t_client']['sequence_id']
                            client.id = self.session_destin.sequence_nextval(sequence_id)
                            # переносим t_client в базу-назначение
                            self.session_destin.add(client)

                            # переносим зависимость t_person
                            person = copy_mapper_object(
                                self.session_source.query(mapper.Person)\
                                .filter_by(id=contract.person_id)\
                                .one(),
                                mapper.Person
                            )
                            sequence_id = self.sync_metadata['t_person']['sequence_id']
                            person.id = self.session_destin.sequence_nextval(sequence_id)
                            # переносим t_person в базу-назначение
                            self.session_destin.add(person)

                            # переносим все записи t_contract_collateral,
                            # связанные с договором
                            contract_collateral_items = \
                            self.session_source.query(
                                mapper.ContractCollateral
                            )\
                            .filter_by(
                                contract2_id=contract.id
                            ).all()

                            # генерируем новый id для договора
                            sequence_id = self.sync_metadata['t_contract2']['sequence_id']
                            contract.id = self.session_destin.sequence_nextval(sequence_id)

                            contract_collateral_sequence_id = self.sync_metadata['t_contract_collateral']['sequence_id']
                            contract_attributes_sequence_id = self.sync_metadata['t_contract_attributes']['sequence_id']
                            for collateral_item in contract_collateral_items:
                                contract_collateral = copy_mapper_object(
                                    collateral_item,
                                    mapper.ContractCollateral
                                )

                                # переносим все t_contract_attributes
                                # для каждого t_contract_collaterals
                                data = self.session_source.execute(
                                    "select * from t_contract_attributes" + \
                                    "where collateral_id = {}"\
                                    .format(contract_collateral.id)
                                ).fetchall()

                                contract_attributes = [dict(row.items()) for row in data]

                                for contract_attribute_item in contract_attributes:
                                    # генерируем новый id для t_contract_attributes
                                    contract_attribute_item['id'] = self.session_destin.sequence_nextval(contract_attributes_sequence_id)
                                    # создаем новые t_contract_attributes
                                    # в базе-назначение
                                    self.session_destin.execute(
                                        scheme.contract_attributes\
                                        .insert()\
                                        .values(**contract_attribute_item)
                                    )

                                # генерируем новый id для t_contract_collateral
                                contract_collateral.id = self.session_destin.sequence_nextval(contract_collateral_sequence_id)
                                contract_collateral.contract2_id = contract.id
                                # переносим t_contract_collateral в базу-назначение
                                self.session_destin.add(contract_collateral)

                            # переносим договор в базу-назначение
                            self.session_destin.add(contract)

            except Exception:
                log.error(str(sys.exc_info()))

            if processing:
                with self.session_destin.begin():
                    self.session_destin.add(processing)

            if payment_method:
                with self.session_destin.begin():
                    self.session_destin.add(payment_method)

            if terminal:
                with self.session_destin.begin():
                    self.session_destin.add(terminal)

            # далее переносим саму запись t_payment и
            # ее t_ccard_bound_payment, либо t_refund

            # если paysys_code == 'TRUST', то имеем дело
            # с t_ccard_bound_payment
            if paysys_code == 'TRUST':
                item_t_ccard_bound_payment = self.session_source.execute( \
                    query_for_t_ccard_bound_payment.format(payment_id)).first()
                item_t_ccard_bound_payment = dict(item_t_ccard_bound_payment.items())
                del item_t_ccard_bound_payment['id']

                ignore_columns = ['update_dt_yt']
                for column in ignore_columns:
                    del item_t_ccard_bound_payment[column]

                # в t_ccard_bound_payment ищем по trust_payment_id
                found = False
                try:
                    respond = self.session_destin.query(mapper.TrustPayment) \
                    .filter_by(trust_payment_id=trust_payment_id)
                    found = respond.exists()
                except Exception:
                    log.error(str(sys.exc_info()))
                    respond = None
                    found = False

                if found:
                    trust_payment = respond.one()

                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.trust_payments\
                            .where(
                                scheme_payments\
                                .trust_payments\
                                .trust_payment_id==trust_payment_id
                            )\
                            .update(**item_t_ccard_bound_payment)
                        )
                    # затем по id из t_ccard_bound_payment находим сам t_payment
                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.payments\
                            .where(scheme_payments\
                                   .payments\
                                   .id==trust_payment.id
                            )\
                            .update(**item))
                else:
                    # создаем новый t_payment и t_ccard_bound_payment
                    sequence_id = self.sync_metadata['t_payment']['sequence_id']
                    if sequence_id:
                        # используем новый id из sequence
                        id = self.session_destin.sequence_nextval(sequence_id)
                    else:
                        id = item['id']

                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.payments.insert().values(
                                id=id, **item
                            )
                        )

                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.trust_payments.insert().values(
                                id=id, **item_t_ccard_bound_payment
                            )
                        )

            # иначе если paysys_code == 'REFUND', либо 'REFUND_GROUP'
            # то имеем дело с таблицей t_refund
            else:
                item_t_refund = self.session_source.execute( \
                    query_for_t_refund.format(payment_id)).first()
                item_t_refund = dict(item_t_refund.items())
                del item_t_refund['id']

                # переносим платеж, на который делается возврат
                orig_payment_id = item_t_refund['orig_payment_id']
                if orig_payment_id:
                    orig_payment_data = self.session_source.execute(
                        query_for_payment_by_id.format(
                            id=orig_payment_id
                        )
                    ).first()
                    orig_payment_item = dict(orig_payment_data.items())
                    # переносим
                    self._sync_payment(orig_payment_item)

                ignore_columns = ['update_dt_yt']
                for column in ignore_columns:
                    del item_t_refund[column]

                # в t_refund ищем по trust_refund_id
                found = False
                try:
                    if paysys_code == 'REFUND_GROUP':
                        respond = self.session_destin.query(mapper.RefundGroup)\
                            .filter_by(trust_refund_id=trust_refund_id)
                    else:
                        respond = self.session_destin.query(mapper.Refund)\
                            .filter_by(trust_refund_id=trust_refund_id)

                    found = respond.exists()
                except Exception:
                    log.error(str(sys.exc_info()))
                    found = False

                if found:
                    refund_payment = respond.one()

                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.refunds\
                            .where(
                                scheme_payments\
                                .refunds\
                                .trust_refund_id==trust_refund_id
                            )\
                            .update(**item_t_refund)
                        )

                    # затем по id из t_refund находим сам t_payment
                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.payments\
                            .where(
                                scheme_payments\
                                .payments\
                                .id==refund_payment.id
                            )\
                            .update(**item)
                        )
                else:
                    # создаем новый t_payment и t_refund
                    sequence_id = self.sync_metadata['t_payment']['sequence_id']
                    if sequence_id:
                        # используем новый id из sequence
                        id = self.session_destin.sequence_nextval(sequence_id)
                    else:
                        id = item['id']

                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.payments.insert().values(
                                id=id, **item
                            )
                        )

                    with self.session_destin.begin():
                        self.session_destin.execute(
                            scheme_payments.refunds.insert().values(
                                id=id, **item_t_refund)
                        )

            self._update_last_state_dt_field('t_payment')

        except Exception:
            log.error('Error in {} with object {}: {}'\
                      .format(inspect.stack()[0][3],
                              str(item),
                              str(sys.exc_info()))
            )


    def sync_payments(self, filters):
        """
        :param filters (dict): {'column_name': value}
        :return:
        """
        table_name = 't_payment'

        try:
            query = "select p.*,  " \
            "r.trust_refund_id as r__trust_refund_id, "\
            "cb.trust_payment_id as cb__trust_payment_id " \
            "from {table_name} p " \
            "left join t_ccard_bound_payment cb on " \
            "p.id = cb.id " \
            "left join t_refund r on " \
            "p.id = r.id " \
            "where p.update_dt_yt > to_timestamp('{dt}', '{dt_format}') " \
            .format(
                table_name=table_name,
                dt=self.sync_metadata[table_name]['last_state_dt'],
                dt_format='YYYY-MM-DD HH24:MI:SS'
            )

            filter_rules = self.sync_metadata[table_name]['filter_rules']
            if filter_rules:
                if isinstance(filter_rules, str):
                    query += " and " + filter_rules
                else:
                    raise RuntimeError(
                        'column filter_rules is not str type for table {}'\
                        .format(table_name)
                    )

            data = self.session_source.execute(query).fetchall()
            data = [dict(c.items()) for c in data]

            ignore_columns = self.sync_metadata[table_name]['ignore_columns']

            for item in data:
                for c in ignore_columns:
                    del item[c]

                # если у платежа заполнен trust_group_id -
                # это trust_payment_id родительского платежа.
                # нужно перенести сначала его
                if item['trust_group_id']:
                    parent_payment_data = self.session_source.execute(
                        query_for_parent_payment.format(
                            trust_payment_id=item['trust_group_id'])
                    ).first()
                    parent_item = dict(parent_payment_data.items())
                    # переносим родительский платеж
                    self._sync_payment(parent_item)
                # переносим платеж
                self._sync_payment(item)

        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))


    def sync_data(self, filters):
        """

        :param filters (dict): ключи - имена таблиц, значения - словарь-фильтр
        :return: None
        """
        try:
            if not (filters and isinstance(filters, dict)):
                raise ValueError('filters param must be dict')

            # self.sync_clients вернет словарь идентификаторов клиентов
            # в базе-источнике, соответствующий идентификатору в базе-назначения
            client_id_mapping = self.sync_clients(
                filters=filters.get('t_client', None)
            )
            # используем идентификаторы клиентов из предыдущего шага
            # для замены в новых сервис-продуктах поля partner_id
            self.sync_service_products(
                filters=filters.get('t_service_product', None),
                substitute_by={'partner_id': client_id_mapping}
            )

            self.sync_payments(
                filters=filters.get('t_payment', None)
            )

        except Exception:
            log.error('Error in {}: {}'\
                      .format(inspect.stack()[0][3], str(sys.exc_info())))

def _read_cmd_args():
    """
    В командную строку параметром filters можно передать фильтры
    для каждой таблицы синхронизации в виде SQL-текста
    условия секции where, без ' and 'в начале
    Пример:
    --filters='id in (2345, 4342)'

    :return:
    """
    args_dict = dict()

    parser = argparse.ArgumentParser()
    parser.add_argument('filters', type=str, required=False)

    args = parser.parse_args()
    if args.filters:
        args_dict = json.loads(value)

    return  args_dict


def main():
    try:
        app = getApplication()
        session_source = app.new_session(database_id='balance')
        session_destin = app.new_session(database_id='light_balance')

        synchronizer = DatabaseSynchronizer(session_source, session_destin)
        synchronizer.prepare_sync_metadata()

        args = _read_cmd_args()

        synchronizer.sync_data(args.get('filters', None))

    except Exception:
        log.error('Error in {}: {}'\
                  .format(inspect.stack()[0][3], str(sys.exc_info())))


if __name__ == '__main__':
    main()
