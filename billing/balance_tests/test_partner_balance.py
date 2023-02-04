# coding: utf-8

import mock
import sqlalchemy as sa
import datetime
import json
from decimal import Decimal as D

from balance.constants import ExportState
from balance.processors.process_fast_balances import handle_fast_balance_contract
from balance.processors.logbroker_proc import process_logbroker_fast_balance, LBKX
from balance.son_schema import partners
from butils.decimal_unit import DecimalUnit as DU
from butils import logger
from balance import mapper, scheme
from balance import partner_balance
from balance import constants as const
from balance import reverse_partners as rp
from balance.processors.cache_partner_balances import cache_balance, cache_balance_processor
from balance.processors.process_payments import handle_invoice
from balance.processors.logbroker_proc import process_logbroker_full_balance
from balance.partner_balance import BalanceContractsManager
from medium import medium_logic
from tests import object_builder as ob
from tests.balance_tests import test_revpartners_old as trp
from tests.balance_tests.rev_partners import common as rp_common
from balance.mapper import PartnerBalanceCache

log = logger.get_logger()


def get_taxi_service_pa(contract):
    product = rp.get_product(rp.get_taxi_eff_sid(contract), contract, ua_root=True)
    return partner_balance.PartnerBalanceUnitMediator.get_pa(contract, None, service_code=product.service_code)


class TestPartnerBalance(trp.ReversePartnersBase):

    def setUp(self):
        super(TestPartnerBalance, self).setUp()
        self.id = (x for x in range(666))
        self.service_id = const.ServiceId.ZAXI
        self.balance_info = {'balance': 666}
        self.contract_id = 777

    def test_taxi_prepay(self):
        """
        Стартовый баланс для предоплаты такси
        """
        factory = partner_balance.partner_balance_factory_unit_processing
        min_cost = D(1000)
        contract = self.gen_contract(postpay=False, personal_account=True,
                                     con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, min_cost))
        pb = factory(self.session, const.ServiceId.TAXI_CASH)

        expecting = {
            'Currency': 'RUB',
            'BonusLeft': D('0'),
            'CurrMonthCharge': 0,
            'CurrMonthBonus': D('0'),
            'SubscriptionBalance': 0,
            'SubscriptionRate': 0,
            'DT': self.session.now().isoformat(),
            'Balance': DU('0', 'FISH'),
            'ClientID': contract.client.id,
            'ContractID': contract.id,
            'PersonalAccountExternalID': get_taxi_service_pa(contract).external_id,
            'ActSum': 0
        }

        self.assertDictEqual(expecting, pb.get_info(contract.id))

    def test_taxi_postpay(self):
        """
        Стартовый баланс для постаплаты такси
        """
        factory = partner_balance.partner_balance_factory_unit_processing
        min_cost = D(1000)
        contract = self.gen_contract(postpay=True, personal_account=True,
                                     con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, min_cost))
        pb = factory(self.session, const.ServiceId.TAXI_CASH)
        expecting = {
            'BonusLeft': D('0'),
            'Currency': 'RUB',
            'CurrMonthBonus': D('0'),
            'DT': self.session.now().isoformat(),
            'ClientID': contract.client.id,
            'ContractID': contract.id,
            'CommissionToPay': 0,
            'ActSum': 0
        }

        self.assertDictEqual(expecting, pb.get_info(contract.id))

    def test_cloud(self):
        """
        Стартовый баланс Яндекс.Облака
        """
        factory = partner_balance.partner_balance_factory_unit_processing
        sid = const.ServiceId.CLOUD
        con_func = lambda c, services=[sid]: rp_common.generic_terms(c, services)
        contract = self.gen_contract(postpay=False,
                                     personal_account=True,
                                     con_func=con_func)
        pb = factory(self.session, sid)

        expecting = {
            'ActSum': D('0'),
            'Currency': 'RUB',
            'ReceiptSum': DU('0', 'FISH'),
            'DT': self.session.now().isoformat(),
            'ContractID': contract.id,
            'ConsumeSum': DU('0', 'FISH'),
            'Amount': 0
        }

        self.assertDictEqual(expecting, pb.get_info(contract.id))

    def test_taxi_medium_unit_logic(self):
        """
        Проверка ручки GetTaxiBalance
        """
        min_cost = D(1000)
        contract = self.gen_contract(postpay=False, personal_account=True,
                                     con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, min_cost))

        expecting = [{
            'Currency': 'RUB',
            'BonusLeft': D('0'),
            'CurrMonthCharge': 0,
            'CurrMonthBonus': D('0'),
            'SubscriptionBalance': 0,
            'SubscriptionRate': 0,
            'DT': self.session.now().isoformat(),
            'Balance': DU('0', 'FISH'),
            'ClientID': contract.client.id,
            'ContractID': contract.id,
            'PersonalAccountExternalID': get_taxi_service_pa(contract).external_id,
            'ActSum': 0
        }]
        from medium.medium_logic import Logic
        medium = Logic()
        self.assertEqual(medium.GetTaxiBalance([contract.id]), expecting)

    def create_cert_paysys(self, contract, service_id):
        """
        Создает пейсис с сертификатом для теста
        """
        paysys_id = next(self.id)
        paysys = mapper.Paysys(id=paysys_id,
                               currency='RUR',
                               instant=0,
                               certificate=1,
                               cc='test_paysys',
                               firm=contract.firm,
                               category=contract.person.type)
        log.debug('Creating new paysys {}'.format(paysys))

        paysys_service = mapper.PaysysService(service_id=service_id,
                                              paysys_id=paysys_id,
                                              weight=1,
                                              extern=1)
        log.debug('creating new paysys_service'.format(paysys_service))

        self.session.add_all([paysys, paysys_service])
        self.session.flush()

    def test_taxi_field_OfferAccepted(self):
        factory = partner_balance.partner_balance_factory_unit_processing
        min_cost = D(1000)
        contract = self.gen_contract(postpay=False, personal_account=True,
                                     con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, min_cost))
        contract.offer_accepted = True
        pb = factory(self.session, const.ServiceId.TAXI_CASH)
        self.assertEqual(pb.get_info(contract.id)['OfferAccepted'], 1)

    def test_taxi_field_NettingLastDt(self):
        factory = partner_balance.partner_balance_factory_unit_processing
        for SET_NETTING_LAST_DT_IN_PROCESS_PAYMENTS in (0, 1):
            min_cost = D(1000)
            self.session.config.__dict__['HOLD_TAXI_NETTING'] = datetime.datetime(2020, 6, 16, 0, 0)
            self.session.config.__dict__['SET_NETTING_LAST_DT_IN_PROCESS_PAYMENTS'] = \
                SET_NETTING_LAST_DT_IN_PROCESS_PAYMENTS
            contract = self.gen_contract(postpay=False, personal_account=True,
                                         con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, min_cost))
            contract.col0.netting = True
            net_dt = datetime.datetime(2017, 1, 1, 0, 0)
            contract.daily_state = net_dt

            pb = factory(self.session, const.ServiceId.TAXI_CASH)
            pb.dt = datetime.datetime(2019, 1, 1)
            self.assertEqual(pb.get_info(contract.id)['NettingLastDt'], net_dt.isoformat())

            pa = get_taxi_service_pa(contract)
            now = datetime.datetime.now().replace(microsecond=0)
            ob.OebsCashPaymentFactBuilder(
                dt=now,
                amount=min_cost,
                operation_type=const.OebsOperationType.INSERT_YA_NETTING,
                invoice=pa).build(self.session)
            # self.session.expire_all()
            handle_invoice(pa)

            pb = factory(self.session, const.ServiceId.TAXI_CASH)
            pb.dt = datetime.datetime(2020, 6, 17)
            if SET_NETTING_LAST_DT_IN_PROCESS_PAYMENTS:
                self.assertEqual(contract.cpf_netting_last_dt, now)
                self.assertEqual(pb.get_info(contract.id)['NettingLastDt'], now.replace(microsecond=0).isoformat())
            else:
                self.assertEqual(contract.cpf_netting_last_dt, None)
                self.assertEqual(pb.get_info(contract.id)['NettingLastDt'], now.replace(microsecond=0).isoformat())

    def test_cache(self):
        factory = partner_balance.partner_balance_factory_unit_processing
        now = datetime.datetime.now()

        # valid_cache
        less_than_day_ago = now - datetime.timedelta(hours=23)
        ob.PartnerBalanceCache(dt=less_than_day_ago,
                               service_id=self.service_id,
                               contract_id=self.contract_id,
                               balance_info=self.balance_info).build(self.session)
        pb = factory(self.session, self.service_id)
        self.assertEqual(pb.get_cached_info(self.contract_id), self.balance_info)

        # invalid cache
        contract_id2 = 778
        more_than_day_ago = now - datetime.timedelta(hours=25)
        ob.PartnerBalanceCache(dt=more_than_day_ago,
                               service_id=self.service_id,
                               contract_id=contract_id2,
                               balance_info=self.balance_info).build(self.session)
        pb = factory(self.session, self.service_id)
        self.assertIsNone(pb.get_cached_info(contract_id2))

    def test_GetPartnerBalance_unit_logic(self):
        now = datetime.datetime.now()
        less_than_day_ago = now - datetime.timedelta(hours=23)

        ob.PartnerBalanceCache(dt=less_than_day_ago,
                               service_id=self.service_id,
                               contract_id=self.contract_id,
                               balance_info=self.balance_info).build(self.session)

        logic = medium_logic.Logic()
        self.assertEqual(logic.GetPartnerBalance(self.service_id, [self.contract_id]), [self.balance_info])

    def test_zaxi(self):
        factory = partner_balance.partner_balance_factory_unit_processing

        min_cost = D(1000)
        taxi_contract = self.gen_contract(postpay=False, personal_account=True,
                                          con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, min_cost))
        contract = self.gen_contract(postpay=True, personal_account=True,
                                     con_func=lambda c: rp_common.zaxi_terms(c, taxi_contract.id))

        pb = factory(self.session, const.ServiceId.ZAXI)

        expecting = {
            'ActSum': D('0'),
            'Balance': DU('0', 'FISH'),
            'ConsumeSum': DU('0', 'FISH'),
            'ContractEID': taxi_contract.external_id,
            'ContractID': taxi_contract.id,
            'Currency': 'RUB',
            'DT': self.session.now().isoformat(),
            'InvoiceEID': contract.invoices[0].external_id,
            'ReceiptSum': DU('0', 'FISH'),
            'TotalCharge': 0
        }

        self.assertDictEqual(expecting, pb.get_info(contract.id))

        contract_corp = self.gen_contract(postpay=True, personal_account=True,
                                          con_func=lambda c: rp_common.zaxi_corp_terms(c))

        pb = factory(self.session, const.ServiceId.ZAXI)

        expecting_corp = {
            'ActSum': D('0'),
            'Balance': DU('0', 'FISH'),
            'ConsumeSum': DU('0', 'FISH'),
            'ContractEID': contract_corp.external_id,
            'ContractID': contract_corp.id,
            'Currency': 'RUB',
            'DT': self.session.now().isoformat(),
            'InvoiceEID': contract_corp.invoices[0].external_id,
            'ReceiptSum': DU('0', 'FISH'),
            'TotalCharge': 0,
        }

        self.assertDictEqual(expecting_corp, pb.get_info(contract_corp.id))

    def test_fast_balance_enqueue(self):
        taxi_contract = self.gen_contract(postpay=False, personal_account=True,
                                          con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, D(1000)))
        contract = self.gen_contract(postpay=True, personal_account=True,
                                     con_func=lambda c: rp_common.zaxi_terms(c, taxi_contract.id),
                                     client=taxi_contract.client)

        pa = get_taxi_service_pa(taxi_contract)

        # Enqueues contract to PARTNER_FAST_BALANCE
        handle_invoice(pa)

        export_object_query = self.session \
            .query(mapper.Export) \
            .filter((mapper.Export.type == 'PARTNER_FAST_BALANCE') &
                    (mapper.Export.classname == 'Contract') &
                    (mapper.Export.state == ExportState.enqueued))

        export_object = export_object_query.filter(mapper.Export.object_id == contract.id).one_or_none()
        self.assertIsNotNone(export_object)
        handle_fast_balance_contract(contract, {})
        fast_balance_object = self.session.query(mapper.PartnerFastBalance) \
            .filter_by(object_id=export_object.object_id,
                       object_type='Contract',
                       lb_topic='partner-fast-balance-zaxi') \
            .one_or_none()
        self.assertIsNotNone(fast_balance_object)

    def test_fast_balance_logbroker_enqueue(self):
        export_ng = scheme.export_ng

        taxi_contract = self.gen_contract(postpay=False, personal_account=True,
                                          con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, D(1000)))
        export_contract = self.gen_contract(postpay=True, personal_account=True,
                                            con_func=lambda c: rp_common.zaxi_terms(c, taxi_contract.id),
                                            client=taxi_contract.client)

        # Create export object for logbroker
        handle_fast_balance_contract(export_contract, {})

        fast_balance_object = self.session.query(mapper.PartnerFastBalance) \
            .filter_by(object_id=export_contract.id,
                       object_type='Contract',
                       lb_topic='partner-fast-balance-zaxi') \
            .one_or_none()

        export_ng_object = self.session.execute(sa.select(
            [export_ng],
            (export_ng.c.type == 'LOGBROKER-PARTNER-FAST-BALANCE')
            & (export_ng.c.object_id == fast_balance_object.id)
            & (export_ng.c.state == ExportState.enqueued)
            & (export_ng.c.in_progress == None)
            & (export_ng.c.next_export <= sa.sql.functions.sysdate()),
            order_by=export_ng.c.priority
        )).fetchone()

        # Check export objects exists
        self.assertIsNotNone(export_ng_object)

        c = self.session.query(mapper.PartnerFastBalance).get(export_ng_object.object_id)
        expected_lb_message = json.dumps({
            'obj': partners.PartnerFastBalanceSchema().dump(c).data,
            'classname': 'PartnerFastBalance',
            'version': c.version_id,
        }, ensure_ascii=False).encode("UTF-8")

        # Check lb writes a message
        with mock.patch('balance.api.logbroker.LogbrokerProducer.write') as lb_mock:
            process_logbroker_fast_balance([export_ng_object])
            lb_mock.assert_called_once_with(expected_lb_message)


class TestPartnerFullBalanceLogBroker(trp.ReversePartnersBase):

    def setUp(self):
        super(TestPartnerFullBalanceLogBroker, self).setUp()
        self.service_id = const.ServiceId.ZAXI

    def test_set_topic_during_balace_cache_calculating(self):
        """Если в v_partner_balance_source в поле PROCESSING_UNITS_METADATA прописан юнит place_metadata_lb_topic,
        то при расчете контракта его баланс должен выгрузиться в PartnerBalanceCache c этим топиком"""

        self._check_topic_set_during_balance_cache_calculating(
            processing_units_metadata=["enrich_contract", "enrich_current_signed",
                                       {"unit": "place_metadata_lb_topic", "params": {"topic": 'fake_topic'}}],
            expected_topic='fake_topic')

    def test_null_topic_during_balace_cache_calculating(self):
        """Если в v_partner_balance_source в поле PROCESSING_UNITS_METADATA не прописан юнит place_metadata_lb_topic,
        то при расчете контракта его баланс должен выгрузиться в PartnerBalanceCache с топиком = None"""

        self._check_topic_set_during_balance_cache_calculating(
            processing_units_metadata=["enrich_contract", "enrich_current_signed"],
            expected_topic=None)

    def _check_topic_set_during_balance_cache_calculating(self, processing_units_metadata, expected_topic):
        """Выполняет расчет кэша баланса по заданным юнитам,
        проверяет добавление записи в PartnerBalanceCache и правильность установки названия топика"""

        taxi_contract = self.gen_contract(postpay=False, personal_account=True,
                                          con_func=lambda c: rp_common.taxi_ccard_combo_contract(c, D(1000)))
        contract = self.gen_contract(postpay=True, personal_account=True,
                                     con_func=lambda c: rp_common.zaxi_terms(c, taxi_contract.id),
                                     client=taxi_contract.client)

        # рассчитываем баланс, должен сохраниться в кэше
        with mock.patch('balance.partner_balance.partner_balance_factory_unit_processing') as factory_mock:
            factory_mock.return_value = BalanceContractsManager(self.session,
                                                                service_id=self.service_id,
                                                                processing_units=processing_units_metadata)
            cache_balance_processor(contract, {})

        pbc = mapper.PartnerBalanceCache
        balance_cache = self.session \
            .query(pbc) \
            .filter(pbc.contract_id == contract.id,
                    pbc.service_id == self.service_id) \
            .one()
        assert balance_cache.lb_topic == expected_topic

    def test_send_balance_cache_to_logbroker(self):
        """При добавлении строки PartnerBalanceCache должен появиться элемент в очереди ExportNg
        и выгрузитьс в логброкер"""

        # добавляем строку PartnerBalanceCache
        topic_name = u'partner-full-balance'
        pbc = self._add_partner_balance_cache(topic_name=topic_name)

        # убеждаемся, что в ExportNg присутствует задача на выгрузку
        export_ng_object = self._find_export_ng_task(pbc)
        assert export_ng_object is not None

        # проверяем выгрузку в логброкер в заданный топик
        expected_lb_message = json.dumps({
            'obj': partners.PartnerFullBalanceSchema().dump(pbc).data,
            'classname': 'PartnerFullBalance',
            'version': pbc.version_id,
        }, ensure_ascii=False).encode("UTF-8")

        with mock.patch('balance.api.logbroker.LogbrokerProducer.write') as lb_mock:
            process_logbroker_full_balance([export_ng_object])
            lb_mock.assert_called_once_with(expected_lb_message)

    def test_full_balance_version_increases_on_insert(self):
        """При добавлении строки PartnerBalanceCache version_id должна увеличиваться"""

        # добавляем первый расчет в PartnerBalanceCache
        pbc1 = self._add_partner_balance_cache()
        # добавляем более поздний расчет в PartnerBalanceCache
        later_dt = pbc1.dt + datetime.timedelta(seconds=1)
        pbc2 = self._add_partner_balance_cache(dt=later_dt)

        # убеждаемся, что version_id увеличивается
        assert pbc2.version_id > pbc1.version_id

    def test_dont_export_balance_cache_with_null_topic_name(self):
        """Если у объекта PartnerBalanceCache не установлен lb_topic, то в очередь ExportNg его помещать не надо"""

        # добавдяем объект, у которого не указан topic_name
        pbc = self._add_partner_balance_cache(topic_name=None)

        # убеждаемся, что выгрузки в export_ng не было
        export_ng_object = self._find_export_ng_task(pbc)
        assert export_ng_object is None

    def _add_partner_balance_cache(self, topic_name=None, dt=None):
        """Создает новый объект PartnerBalanceCache и добавляет в базу"""

        dt = dt or datetime.datetime.now() - datetime.timedelta(hours=23)
        dt = dt.replace(microsecond=0)
        service_id = const.ServiceId.ZAXI
        balance_info = {'balance': 123}
        contract_id = 777
        pbc = PartnerBalanceCache(dt=dt,
                                  service_id=service_id,
                                  contract_id=contract_id,
                                  balance_info=balance_info,
                                  lb_topic=topic_name)
        self.session.add(pbc)
        # чтобы сработали хуки sqlalchemy и отработала выгрузка в ExportNg
        self.session.flush()

        return pbc

    def _find_export_ng_task(self, pbc):
        """Находит в очереди ExportNg задачу для выгрузки заданного объекта PartnerBalanceCache"""

        export_ng = mapper.ExportNg
        return self.session \
            .query(export_ng) \
            .filter((export_ng.type == 'LOGBROKER-PARTNER-FULL-BALANCE')
                    & (export_ng.object_id == pbc.enqueue_object_id)
                    & (export_ng.state == ExportState.enqueued)
                    & (export_ng.in_progress == None)
                    & (export_ng.next_export <= datetime.datetime.now())) \
            .one_or_none()
