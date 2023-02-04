# -*- coding: utf-8 -*-

from contextlib import contextmanager
import datetime
import pytest

import balance.exc as exc
from balance import mapper
from balance.constants import OebsOperationType

from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.utils.partner_processing.unit_dispatching_logic import UnitDispatcher
# noinspection PyUnresolvedReferences
from cluster_tools.partner_processing_enqueuer import ServiceEnqueueUnit  # импорт нужен для регистрации метаклассом

import tests.object_builder as ob

dt = datetime.datetime

MODULE_NAME = 'partner_processing_enqueuer'
UNIT_TYPE = 'enqueue_unit'


@contextmanager
def no_exception_ctx(*args, **kwargs):
    yield


def pytest_raises_ctx(*args, **kwargs):
    def wrapper():
        return pytest.raises(*args, **kwargs)
    return wrapper


class TestEnqueueUnitBase(object):
    unit_name = None

    def prepare_env(self, code, queue, on_dt, params, exception_context, expected_data, session):
        return None

    def check_results(self, code, queue, on_dt, params, exception_context, expected_data, enqueue_unit,
                      prepared_test_data, session):
        pass

    def execute(self, code, queue, on_dt, params, exception_context, expected_data, session):
        if exception_context is None:
            exception_context = no_exception_ctx
        prepared_test_data = self.prepare_env(code, queue, on_dt, params, exception_context, expected_data, session)
        session.flush()
        enqueue_unit = None
        with exception_context():
            enqueue_unit_cls = UnitDispatcher.get_unit(MODULE_NAME, UNIT_TYPE, self.unit_name)
            enqueue_unit = enqueue_unit_cls(session, code, queue, on_dt,
                                            params=params)
            enqueue_unit.enqueue()
        if exception_context is not no_exception_ctx:
            return
        session.flush()
        self.check_results(code, queue, on_dt, params, exception_context, expected_data, enqueue_unit,
                           prepared_test_data, session)


class TestNotExistingEnqueueUnit(TestEnqueueUnitBase):
    unit_name = 'unit_not_exists'

    @pytest.mark.parametrize(
        'code, queue, on_dt, params, exception_context, expected_data', [
            ('blue_market_netting', 'PARTNER_PROCESSING', dt.now(), {},
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_DISPATCHER_EXCEPTION,
                               match='Partner processing dispatcher error: module_name: PARTNER_PROCESSING_ENQUEUER '
                                     'unit_type: ENQUEUE_UNIT unit_name: UNIT_NOT_EXISTS not registered'),
             None),
        ],
        ids=[
            'unit_not_exists',
        ]
    )
    def test(self, code, queue, on_dt, params, exception_context, expected_data, session):
        self.execute(code, queue, on_dt, params, exception_context, expected_data, session)


class TestServiceEnqueueUnit(TestEnqueueUnitBase):
    unit_name = 'service_enqueuer'

    @pytest.mark.parametrize(
        'code, queue, on_dt, params, exception_context, expected_data', [
            ('blue_market_netting', 'PARTNER_PROCESSING', dt.now(), {},
             pytest_raises_ctx(exc.COMMON_JSONSCHEMA_VALIDATION_EXCEPTION,
                               match="'service_ids' is a required property"),
             None
             ),
            ('blue_market_netting', 'PARTNER_PROCESSING', dt.now(), {'service_ids': [4849]}, None, None),
            ('blue_market_netting', 'PARTNER_PROCESSING', dt.now(),
             {'service_ids': [4849], 'tail_days': 3, 'priority': 23}, None, None),
        ],
        ids=[
            'no_service_ids',
            'sucsess_only_services_ids_param',
            'sucsess_only_services_ids_param_all_params',
        ]
    )
    def test(self, code, queue, on_dt, params, exception_context, expected_data, session):
        self.execute(code, queue, on_dt, params, exception_context, expected_data, session)

    def prepare_env(self, code, queue, on_dt, params, exception_context, expected_data, session):
        tail_days = params.get('tail_days', 45)
        unsuitable_contracts = [
            # не GENERAL договор
            ob.ContractBuilder(
                ctype='DISTRIBUTION',
                dt=on_dt,
                is_faxed=dt.now(),
                services={4849: 1}
            ).build(session).obj,
            # Неподписанный
            ob.ContractBuilder(
                ctype='GENERAL',
                dt=on_dt,
                is_faxed=None,
                is_signed=None,
                services={4849: 1}
            ).build(session).obj,
            # аннулированный
            ob.ContractBuilder(
                ctype='GENERAL',
                dt=on_dt,
                is_signed=on_dt,
                is_cancelled=on_dt,
                services={4849: 1}
            ).build(session).obj,
            # нет сервиса
            ob.ContractBuilder(
                ctype='GENERAL',
                dt=on_dt,
                is_signed=on_dt,
                services={4850: 1}
            ).build(session).obj,
            # tail_days
            ob.ContractBuilder(
                ctype='GENERAL',
                dt=on_dt - datetime.timedelta(days=50),
                finish_dt=on_dt - datetime.timedelta(days=tail_days + 1),
                is_signed=on_dt,
                services={4850: 1}
            ).build(session).obj,
        ]
        suitable_contracts = [
            ob.ContractBuilder(
                ctype='GENERAL',
                dt=dt.now(),
                is_faxed=dt.now(),
                services={4849: 1}
            ).build(session).obj,
            ob.ContractBuilder(
                ctype='GENERAL',
                dt=on_dt - datetime.timedelta(days=50),
                finish_dt=on_dt - datetime.timedelta(days=tail_days),
                is_signed=on_dt,
                services={4849: 1}
            ).build(session).obj,
        ]
        session.flush()
        return suitable_contracts, unsuitable_contracts

    def check_results(self, code, queue, on_dt, params, exception_context, expected_data, enqueue_unit,
                      prepared_test_data, session):
        priority = params.get('priority', 0)
        suitable_contracts, unsuitable_contracts = prepared_test_data
        suitable_contracts_ids = {c.id for c in suitable_contracts}
        unsuitable_contracts_ids = {c.id for c in unsuitable_contracts}
        # я так и не смог понять, почему актуальный стейт экспорта не подтягивается этим селектом,
        # а каждый объект экспорта надо рефрешить отдельно. Возможно, какие-то кеши/хаки
        # сессии в юнит тестах
        exports = session.query(mapper.Export)\
            .filter(mapper.Export.classname == 'Contract')\
            .filter(mapper.Export.type == queue)\
            .filter(mapper.Export.object_id.in_(suitable_contracts_ids | unsuitable_contracts_ids))\
            .all()
        for e in exports:
            session.refresh(e)
        enqueued = [e for e in exports if e.state == 0]
        exported_contracts_ids = {e.object_id for e in enqueued}
        assert exported_contracts_ids == suitable_contracts_ids, \
            (suitable_contracts, unsuitable_contracts, exported_contracts_ids)

        assert all([e.priority == priority for e in enqueued])
        assert all([e.input.get('code') == code for e in enqueued])


class TestOfferActivationEnqueueUnit(TestServiceEnqueueUnit):
    unit_name = 'offer_activation_enqueuer'

    @pytest.mark.parametrize(
        'code, queue, on_dt, params, exception_context, expected_data', [
            ('offer_activation', 'OFFER_ACTIVATION', dt.now(), {},
             pytest_raises_ctx(exc.COMMON_JSONSCHEMA_VALIDATION_EXCEPTION,
                               match="'service_ids' is a required property"),
             None
             ),
            ('offer_activation', 'OFFER_ACTIVATION', dt.now(), {'service_ids': [4849]}, None, None),
        ],
        ids=[
            'no_service_ids',
            'success_only_services_ids_param',
        ]
    )
    def test(self, code, queue, on_dt, params, exception_context, expected_data, session):
        self.execute(code, queue, on_dt, params, exception_context, expected_data, session)

    def prepare_env(self, code, queue, on_dt, params, exception_context, expected_data, session):
        def build_contract(offer_accepted=None,
                           ctype='GENERAL',
                           contract_dt=on_dt - datetime.timedelta(days=2),
                           offer_confirmation_type='min-payment',  # type: Optional[str]
                           is_deactivated=None,
                           finish_dt=None,
                           service_id=4849,
                           with_payment=False,
                           payment_type=OebsOperationType.INSERT,
                           service_code=None,
                           payment_dt=None,
                           update_dt=None):
            c = ob.ContractBuilder(
                ctype=ctype or 'GENERAL',
                dt=contract_dt,
                is_faxed=dt.now(),
                services={service_id: 1},
                finish_dt=finish_dt
            ).build(session).obj

            if offer_accepted is not None:
                c.offer_accepted = offer_accepted
            if is_deactivated is not None:
                c.col0.is_deactivated = is_deactivated
            c.col0.offer_confirmation_type = offer_confirmation_type

            if with_payment:
                paysys = ob.Getter(mapper.Paysys, 1003).build(session).obj
                personal_account = (
                    PersonalAccountManager(session)
                        .for_contract(c)
                        .for_service_code(service_code)
                        .for_paysys(paysys)
                        .get(auto_create=True)
                )
                receipt_date = payment_dt or (on_dt - datetime.timedelta(days=1))
                last_update_date = update_dt or receipt_date
                ob.OebsCashPaymentFactBuilder(
                    amount=100,
                    operation_type=payment_type,
                    invoice=personal_account,
                    receipt_date=receipt_date,
                    last_update_date=last_update_date).build(session)

            return c

        tail_days = params.get('tail_days', 45)
        unsuitable_contracts = [
            build_contract(offer_accepted=1),  # уже принята оферта
            build_contract(ctype='DISTRIBUTION'),  # не GENERAL-договор
            build_contract(service_id=4999),  # контракт не содержит нужные сервис
            build_contract(contract_dt=on_dt + datetime.timedelta(days=100)),  # контракт в будущем
            build_contract(offer_confirmation_type=None),  # не min-payment
            build_contract(finish_dt=on_dt - datetime.timedelta(days=100)),  # давно расторгнут
            build_contract(is_deactivated=1),  # уже деактивирован
            # деактивирован, платеж был, но неподходящего типа
            build_contract(is_deactivated=1,
                           with_payment=True,
                           payment_type=OebsOperationType.CORRECTION_NETTING),
            # деактивирован, платеж был, но неподходящего service_code
            build_contract(is_deactivated=1,
                           with_payment=True,
                           service_code='APIKEYS_MARKET'),
            # деактивирован, платеж был, но дата прихода выписки из ОЕБС не попадает под условия фильтра
            build_contract(is_deactivated=1,
                           with_payment=True,
                           payment_dt=(on_dt - datetime.timedelta(days=181)),
                           update_dt=(on_dt - datetime.timedelta(days=181))),
        ]
        suitable_contracts = [
            build_contract(),  # is_deactivated=None
            build_contract(is_deactivated=0),  # то же, что is_deactivated=None
            build_contract(offer_accepted=0),  # то же, что offer_accepted=None
            build_contract(finish_dt=on_dt - datetime.timedelta(days=tail_days)),  # недавно завершенный контракт

            # деактивирован, но был платеж (type=ONLINE)
            build_contract(is_deactivated=1,
                           with_payment=True,
                           payment_type=OebsOperationType.ONLINE),
            # деактивирован, но был платеж (type=INSERT)
            build_contract(is_deactivated=1,
                           with_payment=True,
                           payment_type=OebsOperationType.INSERT),
            # деактивирован, но был платеж (service_code=None)
            build_contract(is_deactivated=1,
                           with_payment=True,
                           service_code=None),
            # деактивирован, но был платеж (service_code=YANDEX_SERVICE)
            build_contract(is_deactivated=1,
                           with_payment=True,
                           service_code='YANDEX_SERVICE'),
            # деактивирован, но был платеж, и дата прихода выписки из ОЕБС попадает под условия фильтра
            build_contract(is_deactivated=1,
                           with_payment=True,
                           payment_dt=(on_dt - datetime.timedelta(days=181)),
                           update_dt=(on_dt - datetime.timedelta(days=1))),
        ]
        session.flush()
        return suitable_contracts, unsuitable_contracts
