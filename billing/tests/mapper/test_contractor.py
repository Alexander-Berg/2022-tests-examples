from datetime import datetime, timedelta
from unittest import mock
import pytz

from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.balance2apikeys import Balance2Apikeys
from billing.apikeys.apikeys.mapper import contractor


def test_tarifficator_cache_hit(mongomock, empty_tariff):
    tarrificator = contractor.Tarifficator.get_tariffication(empty_tariff.cc)
    cached = contractor.Tarifficator.get_tariffication(empty_tariff.cc)

    assert tarrificator is cached


def test_tarrificator_cache_expire(mongomock, monkeypatch, empty_tariff):
    import time

    now = time.time()
    tarrificator = contractor.Tarifficator.get_tariffication(empty_tariff.cc)

    # rewind time
    monkeypatch.setattr(time, 'time', lambda: now + 3601)
    cached = contractor.Tarifficator.get_tariffication(empty_tariff.cc)

    assert tarrificator is not cached


class PatchedTarifficatorTask:

    @classmethod
    def enqueue(cls, link):
        return


class TestContractor:

    def test_schedule_tariff_change_duplicate_audit_trail(self, mongomock, link_with_fake_tariff):
        """Проверяем, что при смене текущего тарифа не создаются дубли в логах.

        Метод `get_actual_tariffs` внутри `schedule_tariff_change` возвращает новый тариф.

        В некоторых случаях тарификатор может не отработать после работы контактора (ненормальное поведение),
        в таком случае при повторном запуске контрактора не нужно создавать дубль записи в логах AuditTrail.
        """
        project = link_with_fake_tariff.project
        service = link_with_fake_tariff.service
        contractor = mapper.Contractor(user=project.user, contracts_data=[mapper.ContractData(
            project_id=link_with_fake_tariff.project_id,
            date=datetime.now(pytz.utc) - timedelta(days=7),
            balance_contract_id=1,
            flow_type=mapper.ContractFlowTypes.classic,
            collaterals=[mapper.CollateralData(
                date=datetime.now(pytz.utc) - timedelta(days=7),
                tariffs={service.cc: link_with_fake_tariff.config.tariff + '_new'}
            )]
        )])

        start_audit_trail_count = mapper.AuditTrail.objects.filter(project_id=project.id, service_id=service.id).count()

        with mock.patch('billing.apikeys.apikeys.mapper.task.TarifficatorTask.enqueue', new=PatchedTarifficatorTask.enqueue):
            # "инициализирующая" смена тарифа - установка old_state у audit_trail
            contractor.schedule_tariff_change()
            # "тестовая" смена тарифа - проверка, создаются ли дубли
            contractor.schedule_tariff_change()

        end_audit_trail_count = mapper.AuditTrail.objects.filter(project_id=project.id, service_id=service.id).count()

        assert end_audit_trail_count - start_audit_trail_count == 1

    def test_schedule_tariff_change_duplicate_audit_trail_during_tariff_detaching(self, mongomock,
                                                                                  link_with_fake_tariff):
        """Проверяем, что при отрывании текущего тарифа не создаются дубли в логах.

        Метод `get_actual_tariffs` внутри `schedule_tariff_change` возвращает пустой словарь.

        В некоторых случаях тарификатор может не отработать после работы контактора (ненормальное поведение),
        в таком случае при повторном запуске контрактора не нужно создавать дубль записи в логах AuditTrail.
        """
        project = link_with_fake_tariff.project
        service = link_with_fake_tariff.service
        contractor = mapper.Contractor(user=project.user, contracts_data=[])

        start_audit_trail_count = mapper.AuditTrail.objects.filter(project_id=project.id, service_id=service.id).count()

        with mock.patch('billing.apikeys.apikeys.mapper.task.TarifficatorTask.enqueue', new=PatchedTarifficatorTask.enqueue):
            contractor.schedule_tariff_change()
            contractor.schedule_tariff_change()

        end_audit_trail_count = mapper.AuditTrail.objects.filter(project_id=project.id, service_id=service.id).count()

        assert end_audit_trail_count - start_audit_trail_count == 1

    def test_schedule_tariff_change_tariff_detaching(self, mongomock, link_with_fake_tariff):
        """Проверяем, что текущий договорной тариф отрывается при прекращении договора."""
        project = link_with_fake_tariff.project
        contractor = mapper.Contractor(user=project.user, contracts_data=[])

        with mock.patch('billing.apikeys.apikeys.mapper.task.TarifficatorTask.enqueue', new=PatchedTarifficatorTask.enqueue):
            contractor.schedule_tariff_change()

        link_with_fake_tariff.reload()

        assert (link_with_fake_tariff.config.scheduled_tariff is None and
                link_with_fake_tariff.config.scheduled_tariff_date < datetime.now(pytz.utc))

    def test_schedule_tariff_change_tariff_detaching_for_contractless(self, mongomock,
                                                                      link_with_fake_contractless_tariff):
        """Проверяем, что если текущий тариф офертный, то отрывание не происходит."""
        project = link_with_fake_contractless_tariff.project
        contractor = mapper.Contractor(user=project.user, contracts_data=[])

        scheduled_tariff = link_with_fake_contractless_tariff.config.scheduled_tariff
        scheduled_tariff_date = link_with_fake_contractless_tariff.config.scheduled_tariff_date

        with mock.patch('billing.apikeys.apikeys.mapper.task.TarifficatorTask.enqueue', new=PatchedTarifficatorTask.enqueue):
            contractor.schedule_tariff_change()

        link_with_fake_contractless_tariff.reload()

        assert (scheduled_tariff == link_with_fake_contractless_tariff.config.scheduled_tariff
                and scheduled_tariff_date == link_with_fake_contractless_tariff.config.scheduled_tariff_date)

    def test_schedule_tariff_change_contract_info_clearing(self, mongomock, link_with_fake_tariff):
        """
        Проверяем, что при прекращении договора из линки
        стирается информация о договоре, тариф оторван и связка заблокирована.
        """
        balance_contract_info = dict(balance_contract_id='1', contract_id='123/1', contract_dt=datetime.now(pytz.utc))
        link_with_fake_tariff.update(**balance_contract_info)
        link_with_fake_tariff.save()

        project = link_with_fake_tariff.project
        contractor = mapper.Contractor(user=project.user, contracts_data=[])

        with mock.patch('billing.apikeys.apikeys.mapper.task.TarifficatorTask.enqueue', new=PatchedTarifficatorTask.enqueue):
            contractor.schedule_tariff_change()

        link_with_fake_tariff.reload()
        with mock.patch.object(Balance2Apikeys, 'get_personal_account', lambda *args: None):
            with mock.patch.object(mapper.User, 'get_client', lambda x: {'NAME': 'Fake'}):
                mapper.TarifficatorTask(link=link_with_fake_tariff, dt=datetime.now(pytz.utc))._do_task()

        assert (link_with_fake_tariff.balance_contract_id is None
                and link_with_fake_tariff.contract_id is None
                and link_with_fake_tariff.contract_dt is None
                and link_with_fake_tariff.expire_dt is None
                and link_with_fake_tariff.config.tariff is None
                and link_with_fake_tariff.config.banned)

    def test_flow_is_contractless_1(self, mongomock, simple_link):
        assert simple_link.get_current_flow_type() == mapper.ContractFlowTypes.contractless

    def test_flow_is_contractless_2(self, mongomock, link_with_fake_contractless_tariff):
        assert link_with_fake_contractless_tariff.get_current_flow_type() == mapper.ContractFlowTypes.contractless

    def test_flow_is_classic(self, mongomock, link_with_fake_tariff):
        assert link_with_fake_tariff.get_current_flow_type() == mapper.ContractFlowTypes.classic

    def test_flow_change_contractless_to_classic(self, mongomock, simple_link, empty_tariff):
        project = simple_link.project
        service = simple_link.service
        contractor = mapper.Contractor(user=project.user, contracts_data=[mapper.ContractData(
            project_id=simple_link.project_id,
            date=datetime.now(pytz.utc) - timedelta(days=7),
            balance_contract_id='123',
            flow_type=mapper.ContractFlowTypes.classic,
            collaterals=[mapper.CollateralData(
                date=datetime.now(pytz.utc) - timedelta(days=7),
                tariffs={service.cc: empty_tariff.cc}
            )]
        )])

        contractor.schedule_tariff_change()

        simple_link.reload()
        assert simple_link.config.scheduled_tariff_date is not None
        assert simple_link.config.scheduled_tariff == empty_tariff.cc
        assert simple_link.scheduled_contract_data['contract'] == '123'

    def test_flow_change_contractless_to_tariffless(self, mongomock, link_with_fake_contractless_tariff):
        project = link_with_fake_contractless_tariff.project
        service = link_with_fake_contractless_tariff.service
        contractor = mapper.Contractor(user=project.user, contracts_data=[mapper.ContractData(
            date=datetime.now(pytz.utc) - timedelta(days=7),
            balance_contract_id='123',
            flow_type=mapper.ContractFlowTypes.tariffless,
            tariffless_services=[service.cc]
        )])

        contractor.schedule_tariff_change()

        link_with_fake_contractless_tariff.reload()
        assert link_with_fake_contractless_tariff.config.scheduled_tariff_date is not None
        assert link_with_fake_contractless_tariff.config.scheduled_tariff is None
        assert link_with_fake_contractless_tariff.scheduled_contract_data['contract'] == '123'

    def test_flow_change_contractless_to_tariffless_to_tariffless(
            self, mongomock, simple_link, empty_tariff):
        project = simple_link.project
        service = simple_link.service
        contractor = mapper.Contractor(user=project.user, contracts_data=[mapper.ContractData(
            project_id=simple_link.project_id,
            date=datetime.now(pytz.utc) - timedelta(days=7),
            balance_contract_id='123',
            flow_type=mapper.ContractFlowTypes.classic,
            collaterals=[mapper.CollateralData(
                date=datetime.now(pytz.utc) - timedelta(days=7),
                tariffs={service.cc: empty_tariff.cc}
            )]
        )])

        contractor.schedule_tariff_change()

        simple_link.reload()
        assert simple_link.config.scheduled_tariff_date is not None
        assert simple_link.config.scheduled_tariff == empty_tariff.cc
        assert simple_link.scheduled_contract_data['contract'] == '123'

        contractor = mapper.Contractor(user=project.user, contracts_data=[
            mapper.ContractData(
                project_id=simple_link.project_id,
                date=datetime.now(pytz.utc) - timedelta(days=7),
                balance_contract_id='123',
                flow_type=mapper.ContractFlowTypes.classic,
                collaterals=[mapper.CollateralData(
                    date=datetime.now(pytz.utc) - timedelta(days=7),
                    tariffs={service.cc: empty_tariff.cc}
                )],
            ), mapper.ContractData(
                date=datetime.now(pytz.utc) - timedelta(days=6),
                balance_contract_id='1234',
                flow_type=mapper.ContractFlowTypes.tariffless,
                tariffless_services=[service.cc]
            )
        ])

        contractor.schedule_tariff_change()

        simple_link.reload()
        assert simple_link.config.scheduled_tariff_date is not None
        assert simple_link.config.scheduled_tariff == empty_tariff.cc
        assert simple_link.scheduled_contract_data['contract'] == '123'

        contractor = mapper.Contractor(user=project.user, contracts_data=[
            mapper.ContractData(
                project_id=simple_link.project_id,
                date=datetime.now(pytz.utc) - timedelta(days=7),
                finish_date=datetime.now(pytz.utc) - timedelta(days=1),
                balance_contract_id='123',
                flow_type=mapper.ContractFlowTypes.classic,
                collaterals=[mapper.CollateralData(
                    date=datetime.now(pytz.utc) - timedelta(days=7),
                    tariffs={service.cc: empty_tariff.cc}
                )],
            ), mapper.ContractData(
                date=datetime.now(pytz.utc) - timedelta(days=6),
                balance_contract_id='1234',
                flow_type=mapper.ContractFlowTypes.tariffless,
                tariffless_services=[service.cc]
            )
        ])

        contractor.schedule_tariff_change()

        simple_link.reload()
        assert simple_link.config.scheduled_tariff_date is not None
        assert simple_link.config.scheduled_tariff is None
        assert simple_link.scheduled_contract_data['contract'] == '1234'

    def test_contractor_gets_actual_collateral_correctly(self, mongomock, link_with_fake_tariff, simple_service):
        """
        Как правило доп.соглашения идут в порядке их создания. Но в для примера нарушили этот порядок.
        Получается самый актуальный доп. - предпоследний.
        """
        now = datetime.utcnow()
        contract_data = mapper.ContractData(
            project_id=link_with_fake_tariff.project_id,
            date=now - timedelta(days=7),
            balance_contract_id=1,
            flow_type=mapper.ContractFlowTypes.classic,
        )
        contract_data.collaterals = [
            mapper.CollateralData(date=now-timedelta(days=500),
                                  finish_date=now-timedelta(days=301),
                                  tariffs={simple_service.cc: link_with_fake_tariff.config.tariff + '_1'}),
            mapper.CollateralData(date=now-timedelta(days=300),
                                  tariffs={simple_service.cc: link_with_fake_tariff.config.tariff + '_2'}),
            mapper.CollateralData(date=now - timedelta(days=7),
                                  finish_date=now + timedelta(days=1),
                                  tariffs={simple_service.cc: link_with_fake_tariff.config.tariff + '_4'}),
            mapper.CollateralData(date=now-timedelta(days=100),
                                  tariffs={simple_service.cc: link_with_fake_tariff.config.tariff + '_3'}),
        ]
        assert contract_data.get_actual_collateral(now) == contract_data.collaterals[-2]
