# coding=utf-8
__author__ = 'igogor'

import datetime
from decimal import Decimal
import decimal

import balance.balance_api as api
import balance.balance_db as db
from balance.balance_steps import common_steps, tarification_entity_steps
import btestlib.reporter as reporter
import btestlib.utils as utils
import client_steps
import common_data_steps
import contract_steps
import person_steps
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from btestlib.constants import Currencies, Firms, NdsNew as Nds, ActType, InvoiceType, Paysyses, Managers, \
    DistributionContractType
from btestlib.data import defaults
from btestlib.data.defaults import Distribution
from btestlib.utils import Date, dround, DROUND_PRECISION
from partner_steps import PartnerSteps

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class DistributionSteps(object):
    @staticmethod
    def create_distr_place(client_id, tag_id, result_page_ids, place_internal_type=Distribution.PLACE_INTERNAL_TYPE):
        with reporter.step(u'Создаём площадку: client_id: {}, tag_id: {}, page_ids: {}, place_internal_type: {}'
                                   .format(client_id, tag_id, result_page_ids, place_internal_type)):
            search_id = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']

            product_list = [{'id': result_page_id} for result_page_id in result_page_ids]

            place_id = PartnerSteps.create_partner_place(client_id, place_internal_type, Distribution.PLACE_TYPE,
                                                         tag_id, product_list, search_id)

            return place_id, search_id


    @staticmethod
    def create_distr_client_person_tag(passport_uid=defaults.PASSPORT_UID, person_type='ur',
                                       client_id=None, person_id=None):
        with reporter.step(u'Создаём связку "тег-клиент"'):
            client_id = client_id or client_steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
            # reporter.attach(u"Client ID", utils.Presenter.pretty(client_id))

            person_id = person_id or person_steps.PersonSteps.create(client_id, person_type, {'is-partner': '1'})
            # reporter.attach(u"Person ID", utils.Presenter.pretty(person_id))

            tag_id = DistributionSteps.create_distr_tag(client_id, passport_uid)
            reporter.attach(u'', u'Client ID: {} Person ID: {} Tag ID: {}'.format(client_id, person_id, tag_id))

            return client_id, person_id, tag_id

    @staticmethod
    def create_distr_tag(client_id, passport_uid=defaults.PASSPORT_UID):
        with reporter.step(u"Создаем тег для заданного клиента: client_id: {}".format(client_id)):
            tag_id = db.balance().execute("SELECT s_test_distribution_tag_id.nextval AS tag_id FROM dual")[0]['tag_id']
            reporter.attach(u"Tag ID", utils.Presenter.pretty(tag_id))
            # reporter.log('Tag_id: %d' % (tag_id))

            api.medium().CreateOrUpdateDistributionTag(passport_uid,
                                                       {'TagID': tag_id, 'TagName': 'CreatedByScript',
                                                        'ClientID': client_id})

            return tag_id

    @staticmethod
    def get_search_id(place_id):
        with reporter.step(u'Получаем search_id по place_id: {}'.format(place_id)):
            search_id = db.balance().execute("SELECT id, search_id, payment_type FROM t_place WHERE id = :place_id",
                                             {'place_id': place_id})[0]['search_id']

            reporter.step(u"Search id = {}".format(utils.Presenter.pretty(search_id)))

            return search_id


    @staticmethod
    def create_distr_completion_revshare(distribution_type, place_id, completion_dt, amount=None, vid=None,
                                         bucks_rs=None, completion_type=1):
        with reporter.step(u'Создаём открутки по дистрибуции с разделением прибыли: '
                           u'distribution_type: {}, place_id: {}, date: {}, vid: {}'
                                   .format(distribution_type.name, place_id, completion_dt, vid)):
            if not amount:
                amount = distribution_type.default_amount

            search_id = DistributionSteps.get_search_id(place_id)

            shows = 0
            orders = 0
            if distribution_type == DistributionType.MARKET_CPA:
                orders = Distribution.DEFAULT_REVSHARE_SHOWS
            else:
                shows = Distribution.DEFAULT_REVSHARE_SHOWS

            query = "INSERT INTO t_partner_tags_stat3 " \
                    "(DT,PLACE_ID,TAG_ID,PAGE_ID,SHOWS," \
                    "CLICKS,BUCKS,COMPLETION_TYPE,SOURCE_ID,VID,ORDERS,BUCKS_RS,CLICKSA) " \
                    "VALUES " \
                    "(:completion_dt,:place_id,:search_id,:page_id,:shows," \
                    ":clicks,:bucks,:completion_type,:source_id,:vid,:orders,:bucks_rs,:clicksa)"

            params = {'search_id': search_id, 'place_id': distribution_type.place_id,
                      'page_id': distribution_type.page_id, 'completion_dt': completion_dt,
                      'bucks': amount, 'source_id': distribution_type.source_id, 'vid': vid,
                      'shows': shows,
                      'clicks': Distribution.DEFAULT_REVSHARE_CLICKS,
                      'orders': orders,
                      'bucks_rs': bucks_rs,
                      'clicksa': Distribution.DEFAULT_REVSHARE_CLICKSA,
                      'completion_type': completion_type
                      }

            db.balance().execute(query, params)

    @staticmethod
    def create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type=DistributionType.VIDEO_HOSTING):
        distribution_types = [distribution_type for distribution_type in DistributionType
                              if distribution_type.subtype in [DistributionSubtype.REVSHARE,
                                                               DistributionSubtype.FIXED]
                              and distribution_type != exclude_revshare_type]

        places_ids, _ = DistributionSteps.create_places(client_id, tag_id, distribution_types)
        return places_ids


    @staticmethod
    @utils.cached
    def get_downloads_page_ids():
        with reporter.step(u'Получаем все page_ids для загрузок'):
            sql = "SELECT PAGE_ID FROM t_page_data WHERE contract_type = 'DISTRIBUTION' AND unit_id = 10"
            result = db.balance().execute(sql)

            page_ids = [row['page_id'] for row in result]
            reporter.attach(u"Page_ids", utils.Presenter.pretty(page_ids))

            return page_ids

    @staticmethod
    @utils.cached
    def get_all_page_ids():
        with reporter.step(u'Получаем все page_ids'):
            sql = "SELECT PAGE_ID FROM t_page_data WHERE contract_type = 'DISTRIBUTION' AND is_active = 1"
            result = db.balance().execute(sql)

            page_ids = [row['page_id'] for row in result]
            reporter.attach(u"Page_ids", utils.Presenter.pretty(page_ids))

            return page_ids

    @staticmethod
    def create_downloads_place(client_id, tag_id):
        with reporter.step(u'Создаем площадку для загрузок'):
            page_ids = DistributionSteps.get_downloads_page_ids()
            return DistributionSteps.create_distr_place(client_id, tag_id, page_ids)

    @staticmethod
    def create_places(client_id, tag_id, distribution_types):
        with reporter.step(u"Создаем площадки: client_id: {}, tag_id: {}".format(client_id, tag_id)):
            reporter.attach(u"Типы дистрибуции",
                            utils.Presenter.pretty(
                                [distribution_type.name for distribution_type in distribution_types]))

            clids_and_places_ids = {
                distribution_type: DistributionSteps.create_distr_place(client_id, tag_id,
                                                                        [distribution_type.result_page_id])
                for distribution_type in distribution_types}

            places_ids = {k: v[0] for k, v in clids_and_places_ids.iteritems()}
            clids = {k: v[1] for k, v in clids_and_places_ids.iteritems()}

            reporter.attach(u"Places IDs", utils.Presenter.pretty({k.name: v for k, v in places_ids.iteritems()}))
            reporter.attach(u"Clids", utils.Presenter.pretty({k.name: v for k, v in clids.iteritems()}))

            # distribution_subtypes = [distribution_type.subtype for distribution_type in distribution_types]
            # DistributionSteps.update_distr_views(distribution_subtypes)

            return places_ids, clids

    @staticmethod
    def create_revshare_completions(places_ids, act_date, completion_type=1):
        with reporter.step(u"Создаем открутки всех возможных типов дистрибуции"):
            for distribution_type, place_id in places_ids.iteritems():
                if distribution_type.subtype == DistributionSubtype.REVSHARE:
                    DistributionSteps.create_distr_completion_revshare(distribution_type, place_id, act_date,
                                                                       completion_type=completion_type)

    @staticmethod
    def get_metadata_from_db():
        import json
        metadata = db.balance().execute(''' select page_id, product_metadata from bo.t_page_data
                                            where product_metadata is not null ''')

        for row in metadata:
            row['product_metadata'] = json.loads(row['product_metadata'])

        field_mapping = {
            row['page_id']: row['product_metadata']['fields']
            for row in metadata
        }
        field_aliases = {
            row['page_id']: row['product_metadata'].get('distr_field_aliases', {})
            for row in metadata
        }
        product_to_src_map = {
            row['page_id']: row['product_metadata']['sources']
            for row in metadata
        }

        common_map = {}
        for product in field_mapping:
            common_map[product] = dict(field_mapping[product], **field_aliases[product])

        return field_mapping, field_aliases, product_to_src_map, common_map


    @staticmethod
    def create_entity_completions(places_ids, act_date,
                                  completion_type=1, bucks_rs=None, vid=None, amount=None, country_id=None,
                                  multiply=1, currency=Currencies.RUB):

        def get_first(dct, fields):
            for f in fields:
                if f in dct:
                    return dct[f]
            return None

        field_mapping, _, product_to_src_map, _ = DistributionSteps.get_metadata_from_db()

        with reporter.step(u"Создаем открутки в новой схеме для всех возможных типов дистрибуции"):
            for distribution_type, place_id in places_ids.iteritems():
                product_id = distribution_type.result_page_id
                mapping = field_mapping[product_id]
                key = {}

                if 'place_id' in mapping:
                    key['key_num_1'] = place_id
                else:
                    search_id = db.balance().execute('select search_id from t_place where id = :place_id',
                                                     dict(place_id=place_id)
                                                     )[0]['search_id']
                    key['key_num_1'] = search_id

                if 'completion_type' in mapping:
                    column = mapping['completion_type']
                    key[column] = completion_type

                if 'vid' in mapping:
                    column = mapping['vid']
                    key[column] = vid if vid is not None else -1

                if 'country_id' in mapping:
                    column = mapping['country_id']
                    key[column] = country_id if country_id is not None else -1

                if 'currency' in mapping:
                    column = mapping['currency']
                    key[column] = currency.iso_num_code

                eid = tarification_entity_steps.get_tarification_entity_id(product_id, **key)

                values = {('val_num_{}'.format(num)): 0 for num in range(1, 4+1)}
                shows_column = get_first(mapping,
                                         ['shows', 'hits', 'activations', 'count', 'orders', 'install_new', 'quantity'])
                if shows_column:
                    shows_value = (distribution_type.default_amount
                                   if distribution_type.subtype == DistributionSubtype.FIXED
                                   else defaults.Distribution.DEFAULT_REVSHARE_SHOWS
                                   )
                    values[shows_column] = (amount or shows_value) * multiply
                clicks_column = mapping.get('clicks')  # Для clicks альтернативных названий пока нет
                if clicks_column:
                    clicks_values = (0 if distribution_type.subtype == DistributionSubtype.FIXED
                                     else defaults.Distribution.DEFAULT_REVSHARE_CLICKS
                                     )
                    values[clicks_column] = (amount or clicks_values) * multiply
                if distribution_type.subtype == DistributionSubtype.REVSHARE:
                    bucks_column = get_first(mapping,
                                             ['bucks', 'commission_value', 'dsp_charge', 'money', 'commission'])
                    values[bucks_column] = (amount or Decimal(distribution_type.default_amount)) * multiply
                    if bucks_rs is not None:
                        values[bucks_column] = 0

                if bucks_rs is not None:
                    bucks_rs_column = mapping.get('bucks_rs')
                    if bucks_rs_column:
                        values[bucks_rs_column] = (amount or Decimal(bucks_rs)) * multiply

                src_id = product_to_src_map[product_id][0]

                db.balance().execute('''
                    insert into bo.t_entity_completion
                    (dt, product_id, entity_id, src_id, val_num_1, val_num_2, val_num_3, val_num_4)
                    values (:dt, :pid, :eid, :src_id, :val_num_1, :val_num_2, :val_num_3, :val_num_4) 
                ''',
                dict(dt=act_date, pid=product_id, eid=eid, src_id=src_id, **values)
                )


    @staticmethod
    def run_calculator_for_contract(contract_id):
        tasks = db.balance().execute('''
            select object_id
            from bo.t_export e
            join bo.t_contract_month cm on (cm.id = e.object_id)
            where type='ENTITY_CALC'
            and contract_id = :cid
        ''', dict(cid=contract_id))
        oids = [o['object_id'] for o in tasks]
        for oid in oids:
            common_steps.CommonSteps.export('ENTITY_CALC', 'ContractMonth', oid)


    @staticmethod
    def get_distribution_revenue_share_full_for_places(places_ids, dt):
        with reporter.step(u"Получаем полную информацию о разделении доходов для площадок за дату: {}".format(dt)):
            reporter.attach(u"Площадки", utils.Presenter.pretty({k.name: v for k, v in places_ids.iteritems()}))

            data_by_day = api.medium().GetDistributionRevenueShareFull(dt, dt)
            full_revshare_info = [row for place_id in places_ids.values()
                             for row in DistributionSteps.distribution_data_filtered_by_place(data_by_day, place_id)]

            for row in full_revshare_info:
                utils.round_dict_string_fields(row, ['PARTNER_WO_NDS', 'PARTNER',
                                                     'FULLPARTNER_WO_NDS', 'FULLPARTNER'])

            # full_revshare_info = [row for place_id in places_ids.values()
            #                       for row in DistributionSteps.get_distribution_revenue_share_full(dt, place_id)]

            reporter.attach(u"Информация о разделение доходов для площадок",
                            utils.Presenter.pretty(full_revshare_info))

            return full_revshare_info


    @staticmethod
    def get_distribution_acted(client_id, act_date):
        with reporter.step(u"Получаем информацию по актам для клиента: {} по первому дню месяца: {}"
                                   .format(client_id, act_date)):
            csv_data = api.medium().GetDistributionActed(act_date, client_id)
            acts_info = utils.csv_data_to_dict_list(csv_data, '\t')

            # a-vasin: округляем дробные значения
            for row in acts_info:
                utils.round_dict_string_fields(row, ['TURNOVER_WO_NDS', 'PARTNER_REWARD_WO_NDS'])

            reporter.attach(u"Информация о разделении доходов", utils.Presenter.pretty(acts_info))
            return acts_info


    @staticmethod
    def get_distribution_money(dt, places_ids):
        pruducts_ids = {distr_type.result_page_id for distr_type in places_ids}
        places_ids_u = {unicode(plid) for plid in places_ids.values()}
        res = []
        for product_id in pruducts_ids:
            product_data = utils.csv_data_to_dict_list(
                api.medium().GetDistributionMoney(dt, dt, product_id),
                '\t')
            place_data = [item for item in product_data if (item['PAGE_ID'] in places_ids_u)]
            res += place_data

        for row in res:
            utils.round_dict_string_fields(row, ['PARTNER_WO_NDS', 'PARTNER', 'FULLPARTNER_WO_NDS', 'FULLPARTNER'])

        return res


    @staticmethod
    def distribution_data_filtered_by_place(data, place_id):
        splitted = data.split('\n')
        header = tuple(splitted[0].split('\t'))
        filtered = []
        for line in splitted[1:]:
            row = tuple(line.split('\t'))
            if row[0] == str(place_id):
                filtered.append(dict(zip(header,row)))
        return filtered


    @staticmethod
    def get_discount_pct(distribution_type, discount_date=None):
        with reporter.step(u"Получаем процент скидки для типа дистрибуции {}".format(distribution_type.name)):
            if not discount_date:
                discount_date = datetime.datetime.now()

            query = 'SELECT * FROM t_partner_turnover_expense WHERE service_id=:service_id AND start_dt IN ' \
                    '(SELECT max(start_dt) FROM t_partner_turnover_expense ' \
                    'WHERE service_id=:service_id AND start_dt<=:discount_date)'
            params = {'service_id': distribution_type.service_id, 'discount_date': discount_date}

            discount_info = db.balance().execute(query, params)
            discount = Decimal(discount_info[0]['pct'] if discount_info else 0)

            return discount


    @staticmethod
    def create_full_contract(contract_type, client_id, person_id, tag_id, dt, service_start_dt, nds=Nds.DEFAULT,
                             contract_currency=Currencies.RUB, product_currency=Currencies.RUB,
                             parent_contract_id=None, firm=Firms.YANDEX_1, reward_type=1, multiplier=1,
                             supplements=None, exclude_revshare_type=DistributionType.VIDEO_HOSTING,
                             revshare_types=None, create_contract=None, end_dt=None, tail_time=None,
                             omit_currency=False, accounted_regions=None, ):
        with reporter.step(u'Создаем Дистрибуционный договор: {} со всеми возможными площадками'.format(contract_type)):
            if create_contract is None:
                create_contract = contract_steps.ContractSteps.create_offer \
                    if 'OFFER' in DistributionContractType.name(contract_type) \
                    else contract_steps.ContractSteps.create_common_contract

            if revshare_types is None:
                revshare_types = [distr_type for distr_type in DistributionType if
                                  distr_type.subtype == DistributionSubtype.REVSHARE]

            products_revshare = {str(distr_type.result_page_id):
                                 distr_type.default_price * (multiplier
                                                             if distr_type.partner_units != 'amount'
                                                             else 1)
                                 for distr_type in revshare_types if distr_type != exclude_revshare_type}

            products_fixed = {str(DistributionType.ADDAPPTER2_RETAIL.result_page_id):
                                  DistributionType.ADDAPPTER2_RETAIL.default_price * multiplier,
                              str(DistributionType.TAXI_NEW_PASSENGER.result_page_id):
                                  DistributionType.TAXI_NEW_PASSENGER.default_price * multiplier,
                              }
            params = {
                'client_id': client_id,
                'person_id': person_id,
                'firm_id': firm.id,
                'ctype': 'DISTRIBUTION',
                'manager_uid': Managers.VECHER.uid,
                'start_dt': dt,
                'service_start_dt': service_start_dt,
                'parent_contract_id': parent_contract_id,
                'distribution_tag': tag_id,
                'distribution_contract_type': contract_type,
                'install_price': DistributionType.INSTALLS.default_price * multiplier,
                'products_download': products_fixed,
                'search_price': DistributionType.SEARCHES.default_price * multiplier,
                'activation_price': DistributionType.ACTIVATIONS.default_price * multiplier,
                'products_revshare': products_revshare,
                'nds': str(nds.nds_id),
                'products_currency': product_currency.char_code,
                'search_currency': product_currency.char_code,
                'reward_type': reward_type,
                'supplements': [1, 2, 3] if supplements is None else supplements,
                'distribution_products': 'test',
                'partner_resources': 'test',
                'product_options': 'test',
                'product_search': 'test',
                'product_searchf': 'test',
                'download_domains': 'test-balance.ru',
                'install_soft': 'test-balance.ru',
                'signed': 1,
                'platform_type': None,
                'currency_calculation': 1,
                'manager_bo_code': 20431,
            }
            if end_dt:
                params['end_dt'] = end_dt
            if tail_time is not None:
                params['tail_time'] = tail_time
            if not omit_currency:
                params['currency'] = contract_currency.char_code
            if accounted_regions:
                params['use_geo_filter'] = 1
                params['accounted_regions'] = accounted_regions

            return create_contract(params)

# ----------- obsolete (?) addapter steps
# по адаптеру давно в проде нет данных, но подтвердения об отключении получить не можем

    @staticmethod
    def create_addapter_places(client_id, tag_id):
        distribution_types = [distribution_type for distribution_type in DistributionType
                              if distribution_type.subtype == DistributionSubtype.ADDAPTER]

        places_ids, clids = DistributionSteps.create_places(client_id, tag_id, distribution_types)
        return places_ids, clids


    @staticmethod
    def update_distr_views():
        with reporter.step(u'Обновляем матвью'):
            db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); END;")
            db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DIST_CONTRACT_DOWNLOAD_PROD','C'); END;")
            db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DIST_CONTRACT_REVSHARE_PROD','C'); END;")
            # db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_ADDAPTER_COMM_PLACES','C'); END;")

    @staticmethod
    def create_addapter_completion_row(clid, linked_clid, distribution_type, completion_dt, amount=None, price=None,
                                       amount_multiplier=Decimal('1')):
        with reporter.step(u"Добавляем одну строку открутки для Addapter: "
                           u"clid: {}, linked_clid: {}, distribution_type: {}, date: {}"
                                   .format(clid, linked_clid, distribution_type.name, completion_dt)):
            if amount is None:
                amount = distribution_type.default_amount
            if price is None:
                price = distribution_type.default_price

            amount *= amount_multiplier

            query = "INSERT INTO t_partner_addapter_stat (DT,CLID,LINKED_CLID,PAGE_ID,MONEY,INSTALLS,SOURCE_ID) " \
                    "VALUES (:dt,:clid,:linked_clid,:page_id,:money,:installs,:src_id)"

            params = {
                'dt': completion_dt,
                'clid': clid,
                'linked_clid': linked_clid,
                'page_id': distribution_type.page_id,
                'money': amount * price,
                'installs': amount,
                'src_id': distribution_type.source_id
            }

            db.balance().execute(query, params)

    @staticmethod
    def get_addapter_stat(completion_dt, contract_subtype, contract_id=None):
        with reporter.step(u"Вызываем метод GetAddapterStat для типа договора: {} за дату: {}"
                                   .format(contract_subtype.name, completion_dt)):
            csv_data = api.medium().GetAddapterStat(completion_dt, contract_subtype.name,
                                                    utils.remove_empty({'contract_id': contract_id}))
            addapter_stat = utils.csv_data_to_dict_list(csv_data, '\t')

            # a-vasin: округляем дробные значения
            for row in addapter_stat:
                utils.round_dict_string_fields(row, ['SUM_WO_NDS', 'SUM'])

            return addapter_stat

    @staticmethod
    def get_addapter_stat_by_contract_ids(comletion_dt, contract_subtype, contract_ids):
        with reporter.step(u"Вызываем метод GetAddapterStat и фильтруем по ID договоров"):
            reporter.attach(u"ID договоров", utils.Presenter.pretty(contract_ids))

            addapter_stat = [row for contract_id in contract_ids
                             for row in
                             DistributionSteps.get_addapter_stat(comletion_dt, contract_subtype, contract_id)]

            reporter.attach(u"Фильтрованный AddapterStat", utils.Presenter.pretty(addapter_stat))

            return addapter_stat


class DistributionData(object):
    @staticmethod
    def get_revshare_reward(percent, amount, exchange_rate=Decimal(1), discount=Decimal(0),
                            dt=datetime.datetime.now(), nds_dt=None):
        return Decimal(percent) / 100 * DistributionData.get_revshare_turnover(amount, exchange_rate, discount, dt, nds_dt=nds_dt)

    @staticmethod
    def get_revshare_turnover(amount, exchange_rate=Decimal(1), discount=Decimal(0),
                              dt=datetime.datetime.now(), nds_dt=None):
        turnover = Decimal(amount) * 30 / Nds.DEFAULT.koef_on_dt(nds_dt or dt)
        turnover *= Decimal(exchange_rate)
        turnover *= Decimal(1) - Decimal(discount) / 100
        return turnover

    @staticmethod
    def create_basic_data_row(distribution_type, contract_id, client_id, tag_id, place_id, start_dt, reward,
                              nds=Nds.DEFAULT, currency=Currencies.RUB):
        end_dt = Date.last_day_of_month(start_dt)
        nds_flag = '1' if nds else '0'

        basic_data_row = {'partner_contract_id': contract_id, 'place_id': place_id,
                          'page_id': distribution_type.result_page_id,
                          'owner_id': client_id, 'clicks': 0, 'shows': 0, 'hits': 0, 'bucks': 0,
                          'description': distribution_type.description,
                          'dt': start_dt, 'nds': nds_flag, 'partner_reward_wo_nds': reward,
                          'turnover': None, 'end_dt': end_dt, 'place_type': Distribution.PLACE_TYPE,
                          'currency': currency.char_code,
                          'type_id': distribution_type.type_id,
                          'tag_id': tag_id, 'act_reward': None, 'act_reward_wo_nds': None}

        return basic_data_row

    @staticmethod
    def create_amount_data_row(distribution_type, contract_id, client_id, tag_id, place_id, start_dt):

        data_row = DistributionData.create_basic_data_row(
            distribution_type, contract_id, client_id, tag_id, place_id, start_dt,
            reward=distribution_type.default_amount
        )
        upd = {
            'turnover': None,
            'bucks': 0,
            'clicks': 0,
            'shows': 0,
        }
        data_row.update(upd)
        return data_row

    @staticmethod
    def create_revshare_data_row(distribution_type, contract_id, client_id, tag_id, place_id, start_dt, acts_number=1,
                                 nds=Nds.DEFAULT, currency=Currencies.RUB,
                                 exchange_rate=Decimal(1), discount=Decimal(0), amount_rs=None, price_multiplier=1,
                                 nds_dt=None):

        if distribution_type.partner_units == 'amount':
            data_row = DistributionData.create_basic_data_row(
                distribution_type, contract_id, client_id, tag_id, place_id, start_dt, distribution_type.default_amount,
                nds=nds, currency=currency
            )
            data_row['partner_reward_wo_nds'] *= acts_number
            return data_row

        amount = Decimal(distribution_type.default_amount) * acts_number / distribution_type.units_type_rate
        percent = distribution_type.default_price * price_multiplier

        if amount_rs is not None:
            amount_rs = Decimal(amount_rs) * acts_number / distribution_type.units_type_rate
            reward = dround(DistributionData.get_revshare_turnover(amount_rs, exchange_rate, discount, dt=start_dt, nds_dt=nds_dt), rounding=decimal.ROUND_HALF_UP)
        else:
            reward = dround(DistributionData.get_revshare_reward(percent, amount, exchange_rate, discount, dt=start_dt, nds_dt=nds_dt), rounding=decimal.ROUND_HALF_UP)

        turnover = dround(DistributionData.get_revshare_turnover(amount, exchange_rate, discount, dt=start_dt, nds_dt=nds_dt), rounding=decimal.ROUND_HALF_UP)

        revshare_data_row = DistributionData.create_basic_data_row(distribution_type, contract_id, client_id, tag_id,
                                                                   place_id, start_dt, reward, nds, currency)
        # ставим значения открутки
        revshare_data_row.update({'turnover': turnover, 'bucks': Decimal(amount)})

        # ставим захардкоженые значения для revshare =(
        revshare_data_row.update({
            'clicks': Distribution.DEFAULT_REVSHARE_CLICKS * acts_number if distribution_type != DistributionType.TAXI_LUCKY_RIDE else 0,
            'shows': Distribution.DEFAULT_REVSHARE_SHOWS * acts_number,
            'hits': 0})

        return revshare_data_row

    @staticmethod
    def create_fixed_data_row(distribution_type, contract_id, client_id, tag_id, place_id, start_dt,
                              acts_number=1, nds=Nds.DEFAULT, currency=Currencies.RUB,
                              exchange_rate=Decimal(1), price_multiplier=1):
        amount = distribution_type.default_amount * acts_number
        price = distribution_type.default_price * price_multiplier / distribution_type.units_type_rate

        reward = dround(price * amount * exchange_rate, DROUND_PRECISION)

        data_row = DistributionData.create_basic_data_row(distribution_type, contract_id, client_id, tag_id,
                                                          place_id, start_dt, reward, nds, currency)
        # ставим значение открутки
        data_row[distribution_type.partner_units] = amount

        return data_row


    @staticmethod
    def create_expected_full_partner_act_data(contract_id, client_id, tag_id, places_ids, start_dt,
                                              acts_number=1, nds=Nds.DEFAULT, currency=Currencies.RUB,
                                              product_exchange_rate=Decimal(1), rub_exchange_rate=Decimal(1),
                                              revshare_discount=None, price_multiplier=1, nds_dt=None):
        if not revshare_discount:
            revshare_discount = {}

        # создаем предзаполненные варианты функций, где мы проставили даты и часть id
        create_fixed_data_row_short = lambda distribution_type: \
            DistributionData.create_fixed_data_row(distribution_type, contract_id, client_id, tag_id,
                                                   places_ids[distribution_type], start_dt, acts_number, nds,
                                                   currency, product_exchange_rate, price_multiplier=price_multiplier)

        create_revshare_data_row_short = lambda distribution_type: \
            DistributionData.create_revshare_data_row(distribution_type, contract_id, client_id, tag_id,
                                                      places_ids[distribution_type], start_dt,
                                                      acts_number, nds, currency, rub_exchange_rate,
                                                      revshare_discount.get(distribution_type, Decimal(0)),
                                                      price_multiplier=price_multiplier, nds_dt=nds_dt)

        expected_rows = []

        for distribution_type in places_ids.keys():
            if distribution_type.subtype == DistributionSubtype.REVSHARE:
                expected_rows.append(create_revshare_data_row_short(distribution_type))
            elif distribution_type.subtype == DistributionSubtype.FIXED:
                expected_rows.append(create_fixed_data_row_short(distribution_type))

        return expected_rows

    @staticmethod
    def filter_not_tail_data_rows(partner_act_data):
        return [row for row in partner_act_data
                if row['description'] in
                [distribution_type.description for distribution_type in DistributionType if distribution_type.has_tail]]

    @staticmethod
    def create_serp_hits_completion_row(contract_id, client_id, tag_id, place_id, act_date, acts_number=1,
                                        nds=Nds.DEFAULT):
        amount = DistributionType.SEARCHES.default_amount * acts_number
        price = DistributionType.SEARCHES.default_price / DistributionType.SEARCHES.units_type_rate

        reward_wo_nds = dround(amount * price, DROUND_PRECISION)
        reward = dround(amount * price * nds.koef_on_dt(act_date), DROUND_PRECISION)

        return {'PARTNER_WO_NDS': reward_wo_nds, 'HITS': amount,
                'CONTRACT_ID': contract_id, 'PLACE_ID': DistributionType.SEARCHES.result_page_id,
                'CURRENCY': Currencies.RUB.char_code, 'PARTNER': reward,
                'DT': act_date.isoformat(' '),
                'PAGE_ID': place_id, 'FULLPARTNER': u'',
                'FULLPARTNER_WO_NDS': u'',
                'CLIENT_ID': client_id}

    @staticmethod
    def create_fixed_completion_row(common_map, distribution_type, contract_id, client_id,
                                    tag_id, place_id, act_date, acts_number=1, nds=Nds.DEFAULT, vid=u''):
        amount = distribution_type.default_amount * acts_number
        price = distribution_type.default_price / distribution_type.units_type_rate

        reward_wo_nds = dround(amount * price, DROUND_PRECISION)
        reward = dround(amount * price * nds.koef_on_dt(act_date), DROUND_PRECISION)

        expected_data_row = {'PARTNER_WO_NDS': reward_wo_nds, 'CONTRACT_ID': contract_id,
                'PLACE_ID': distribution_type.result_page_id,
                'CLIENT_ID': client_id, 'PARTNER': reward,
                'PAGE_ID': place_id, 'SHOWS': amount,
                'DT': act_date.isoformat(' '),
                'CURRENCY': Currencies.RUB.char_code,
                'FULLPARTNER': u'',
                'FULLPARTNER_WO_NDS': u''}

        if 'vid' in common_map[distribution_type.result_page_id]:
            expected_data_row.update({'VID': vid if vid else -1})

        return expected_data_row

    @staticmethod
    def create_revshare_completion_row(common_map, distribution_type, contract_id, client_id,
                                       tag_id, place_id, act_date, acts_number=1, nds=Nds.DEFAULT, vid=u'',
                                       currency=Currencies.RUB, exchange_rate=Decimal('1'), bucks_rs=None, percent=None,
                                       clicksa=0):

        if distribution_type.partner_units == 'amount':
            reward_wo_nds = distribution_type.default_amount * acts_number
            reward = reward_wo_nds * nds.koef_on_dt(act_date)
            expected_data_row = {'PARTNER_WO_NDS': reward_wo_nds, 'CONTRACT_ID': contract_id,
                'PLACE_ID': distribution_type.result_page_id,
                'CLIENT_ID': client_id,
                'PARTNER': reward,
                'PAGE_ID': place_id,
                'DT': act_date.isoformat(' '),
                'CURRENCY': Currencies.RUB.char_code
                }
            return expected_data_row


        amount = distribution_type.default_amount * acts_number / distribution_type.units_type_rate
        percent = percent or distribution_type.default_price

        if bucks_rs is None:
            reward_wo_nds = dround(DistributionData.get_revshare_reward(percent, amount, exchange_rate, dt=act_date))
            reward = dround(DistributionData.get_revshare_reward(percent, amount, exchange_rate, dt=act_date)
                            * nds.koef_on_dt(act_date))
        else:
            bucks_rs = Decimal(bucks_rs) * acts_number / distribution_type.units_type_rate
            reward_wo_nds = dround(DistributionData.get_revshare_turnover(bucks_rs, exchange_rate, dt=act_date))
            reward = dround(DistributionData.get_revshare_turnover(bucks_rs, exchange_rate, dt=act_date)
                            * nds.koef_on_dt(act_date))

        turnover_wo_nds = dround(DistributionData.get_revshare_turnover(amount, exchange_rate, dt=act_date))
        turnover = dround(DistributionData.get_revshare_turnover(amount, exchange_rate, dt=act_date)
                          * nds.koef_on_dt(act_date))

        expected_data_row = {'PARTNER_WO_NDS': reward_wo_nds, 'CONTRACT_ID': contract_id,
                'PLACE_ID': distribution_type.result_page_id,
                'CLIENT_ID': client_id,
                'FULLPARTNER_WO_NDS': turnover_wo_nds,
                'FULLPARTNER': turnover,
                'PARTNER': reward,
                'PAGE_ID': place_id,
                'DT': act_date.isoformat(' '),
                'CURRENCY': Currencies.RUB.char_code
                }

        if 'vid' in common_map[distribution_type.result_page_id]:
            expected_data_row.update({'VID': vid if vid else -1})
        if 'clicks' in common_map[distribution_type.result_page_id]:
            expected_data_row.update({'CLICKS': Distribution.DEFAULT_REVSHARE_CLICKS * acts_number})
        if 'shows' in common_map[distribution_type.result_page_id]:
            expected_data_row.update({'SHOWS': Distribution.DEFAULT_REVSHARE_SHOWS * acts_number})
        if 'bucks' in common_map[distribution_type.result_page_id]:
            expected_data_row.update({'BUCKS': Decimal(amount)})
        if 'bucks_rs' in common_map[distribution_type.result_page_id]:
            expected_data_row.update({'BUCKS_RS': bucks_rs or 0})
        if 'clicksa' in common_map[distribution_type.result_page_id]:
            expected_data_row.update({'CLICKSA': clicksa})
        return expected_data_row

    @staticmethod
    def create_expected_full_completion_rows(contract_id, client_id, tag_id, places_ids, act_date,
                                             acts_number=1, nds=Nds.DEFAULT, percent=None):
        _, _, _, common_map = DistributionSteps.get_metadata_from_db()

        create_fixed_completion_row_short = lambda distribution_type: \
            DistributionData.create_fixed_completion_row(common_map, distribution_type, contract_id, client_id, tag_id,
                                                         places_ids[distribution_type], act_date, acts_number, nds, u'')

        create_revshare_completion_row_short = lambda distribution_type: \
            DistributionData.create_revshare_completion_row(common_map, distribution_type, contract_id, client_id, tag_id,
                                                            places_ids[distribution_type], act_date, acts_number, nds,
                                                            u'', percent=percent)

        expected_rows = []

        for distribution_type in places_ids.keys():
            if distribution_type == DistributionType.SEARCHES:
                expected_rows.append(
                    DistributionData.create_serp_hits_completion_row(contract_id, client_id, tag_id,
                                                                     places_ids[DistributionType.SEARCHES],
                                                                     act_date, acts_number, nds))
            elif distribution_type.subtype == DistributionSubtype.REVSHARE:
                expected_rows.append(create_revshare_completion_row_short(distribution_type))
            elif distribution_type.subtype == DistributionSubtype.FIXED:
                expected_rows.append(create_fixed_completion_row_short(distribution_type))

        return expected_rows

    @staticmethod
    def create_expected_revshare_full(client_id, places_ids, act_date, nds=Nds.DEFAULT, acts_number=1):
        expected_full_revshare_info = []

        for distribution_type, place_id in places_ids.iteritems():
            reward_wo_nds = Decimal(0)
            reward = Decimal(0)

            amount = distribution_type.default_amount * acts_number / distribution_type.units_type_rate

            turnover_wo_nds = dround(DistributionData.get_revshare_turnover(amount, dt=act_date), DROUND_PRECISION)
            turnover = dround(DistributionData.get_revshare_turnover(amount, dt=act_date) * nds.koef_on_dt(act_date),
                              DROUND_PRECISION)

            expected_full_revshare_info.append(
                {'PARTNER_WO_NDS': reward_wo_nds, 'PLACE_ID': distribution_type.result_page_id,
                 'CLIENT_ID': client_id, 'FULLPARTNER_WO_NDS': turnover_wo_nds,
                 'BUCKS': Decimal(amount), 'FULLPARTNER': turnover, 'PARTNER': reward,
                 'DT': act_date.isoformat(' '),
                 'PAGE_ID': place_id,
                 'CLICKS': Distribution.DEFAULT_REVSHARE_CLICKS * acts_number,
                 'SHOWS': Distribution.DEFAULT_REVSHARE_SHOWS * acts_number
                 })

        return expected_full_revshare_info

    @staticmethod
    def create_basic_distribution_acted_row(distribution_type, contract_id, external_id, client_id, tag_id, place_id,
                                            act_date, reward):
        end_dt = Date.last_day_of_month(act_date)

        return {'HITS': 0, 'DESCRIPTION': distribution_type.description,
                'CONTRACT_CURRENCY': Currencies.RUB.char_code,
                'SHOWS': 0, 'PAGE_ID': distribution_type.result_page_id,
                'TURNOVER_WO_NDS': u'', 'PLACE_ID': place_id, 'DOMAIN': u'pytest.com', 'TAG_ID': tag_id, 'CLICKS': 0,
                'PARTNER_REWARD_WO_NDS': reward, 'CLIENT_ID': client_id, 'ACT_DT': act_date.isoformat(' '),
                'PARTNER_REWARD': u'', 'BUCKS': 0, 'ACT_END_DT': end_dt.isoformat(' '), 'EXTERNAL_ID': external_id,
                'ACT_CURRENCY': Currencies.RUB.char_code, 'CONTRACT_ID': contract_id}

    @staticmethod
    def create_fixed_distribution_acted_row(distribution_type, contract_id, external_id, client_id, tag_id, place_id,
                                            act_date):
        amount = distribution_type.default_amount
        price = distribution_type.default_price / distribution_type.units_type_rate

        reward = dround(price * amount, DROUND_PRECISION)

        data_row = DistributionData.create_basic_distribution_acted_row(distribution_type, contract_id, external_id,
                                                                        client_id, tag_id, place_id, act_date, reward)
        # ставим значение открутки
        data_row[distribution_type.partner_units.upper()] = amount

        return data_row

    @staticmethod
    def create_revshare_distribution_acted_row(distribution_type, contract_id, external_id, client_id, tag_id, place_id,
                                               act_date):

        if distribution_type.partner_units == 'amount':
            data_row = (DistributionData
                        .create_basic_distribution_acted_row
                        (distribution_type, contract_id, external_id, client_id, tag_id,
                         place_id, act_date, distribution_type.default_amount
                         ))
            return data_row

        amount = distribution_type.default_amount / distribution_type.units_type_rate
        percent = distribution_type.default_price

        reward = dround(DistributionData.get_revshare_reward(percent, amount, dt=act_date), DROUND_PRECISION)
        turnover = dround(DistributionData.get_revshare_turnover(amount, dt=act_date), DROUND_PRECISION)

        revshare_data_row = DistributionData.create_basic_distribution_acted_row(distribution_type, contract_id,
                                                                                 external_id, client_id, tag_id,
                                                                                 place_id, act_date, reward)
        # ставим значения открутки
        revshare_data_row.update({'TURNOVER_WO_NDS': turnover, 'BUCKS': Decimal(amount)})

        # ставим захардкоженые значения для revshare =(
        revshare_data_row.update({
            'CLICKS': Distribution.DEFAULT_REVSHARE_CLICKS if distribution_type != DistributionType.TAXI_LUCKY_RIDE else 0,
            'SHOWS': Distribution.DEFAULT_REVSHARE_SHOWS
        })

        return revshare_data_row

    @staticmethod
    def create_expected_distribution_acted(contract_id, external_id, client_id, tag_id, places_ids, act_date):
        # создаем предзаполненные варианты функций, где мы проставили даты и часть id
        create_fixed_distribution_acted_row_short = lambda distribution_type: \
            DistributionData.create_fixed_distribution_acted_row(distribution_type, contract_id, external_id, client_id,
                                                                 tag_id, places_ids[distribution_type], act_date)

        create_revshare_distribution_acted_row_short = lambda distribution_type: \
            DistributionData.create_revshare_distribution_acted_row(distribution_type, contract_id, external_id,
                                                                    client_id, tag_id, places_ids[distribution_type],
                                                                    act_date)

        expected_rows = []

        for distribution_type in places_ids.keys():
            if distribution_type.subtype == DistributionSubtype.REVSHARE:
                expected_rows.append(create_revshare_distribution_acted_row_short(distribution_type))
            elif distribution_type.subtype == DistributionSubtype.FIXED:
                expected_rows.append(create_fixed_distribution_acted_row_short(distribution_type))

        return expected_rows

# ----------- obsolete (?) addapter steps
# по адаптеру давно в проде нет данных, но подтвердения об отключении получить не можем
    @staticmethod
    def create_addapter_data_row(distribution_type, contract_id, client_id, tag_id, place_id, start_dt,
                                 acts_number=1, nds=Nds.DEFAULT, currency=Currencies.RUB,
                                 exchange_rate=Decimal(1)):
        return DistributionData.create_fixed_data_row(distribution_type, contract_id, client_id, tag_id, place_id,
                                                      start_dt, acts_number, nds, currency, exchange_rate)


    @staticmethod
    def create_addapter_stat_row(clid, linked_clid, contract_id, distribution_type, completion_dt, nds=Nds.DEFAULT,
                                 currency=Currencies.RUB, exchange_rate=Decimal('1')):
        return {
            'DT': completion_dt.isoformat(' '),
            'CONTRACT_ID': contract_id,
            'PRODUCT_ID': distribution_type.page_id,
            'CLID': clid,
            'SUM': utils.dround(distribution_type.default_amount * distribution_type.default_price * exchange_rate
                                * nds.koef_on_dt(completion_dt)),
            'SUM_WO_NDS': utils.dround(
                distribution_type.default_amount * distribution_type.default_price * exchange_rate),
            'INSTALLS': distribution_type.default_amount,
            'LINKED_CLID': linked_clid,
            'CURRENCY': currency.char_code
        }

    @staticmethod
    def create_act_data(distribution_type, act_date, contract_id, nds=Nds.DEFAULT, exchange_rate=Decimal('1'),
                        amount_multiplier=Decimal('1')):
        amount = distribution_type.default_amount * amount_multiplier
        price = (exchange_rate * distribution_type.default_price * nds.get_pct()
                 / distribution_type.units_type_rate)

        return common_data_steps.CommonData.create_expected_act_data_with_contract(contract_id, amount * price,
                                                                                   utils.Date.last_day_of_month(
                                                                                       act_date),
                                                                                   ActType.GENERIC)

    @staticmethod
    def create_invoice_data(distribution_type, person_id, contract_id, nds=Nds.DEFAULT, currency=Currencies.RUB,
                            firm=Firms.YANDEX_1, exchange_rate=Decimal('1'), paysys=Paysyses.BANK_UR_RUB,
                            amount_multiplier=Decimal('1')):
        amount = distribution_type.default_amount * amount_multiplier
        price = (exchange_rate * distribution_type.default_price * nds.get_pct()
                 / distribution_type.units_type_rate)

        return common_data_steps.CommonData.create_expected_invoice_data(contract_id, person_id, amount * price,
                                                                         InvoiceType.PERSONAL_ACCOUNT, paysys.id, firm,
                                                                         currency=currency,
                                                                         nds=nds)
