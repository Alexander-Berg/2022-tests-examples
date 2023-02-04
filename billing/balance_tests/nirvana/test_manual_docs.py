# -*- coding: utf-8 -*-

"test_manual_docs"

from __future__ import unicode_literals

import contextlib
import decimal
from functools import partial

import mock
import pytest
import json
import datetime as dt

from balance import mapper
from balance import muzzle_util as ut
from balance.actions.nirvana.operations import manual_docs as md
from balance.constants import ServiceId, PermissionCode, SHOWS_1000_UNIT_ID, TAX_POLICY_RUSSIA_RESIDENT,\
                              RegionId, NUM_CODE_RUR, FirmId, PersonCategoryCodes, UNITS_1000_UNIT_ID
from balance.contractpage import ContractPage
from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.queue_processor import process_object

from tests import object_builder as ob
from tests.tutils import mock_transactions

OUTPUTS = {}
INPUTS = {}

PERSON_UR_TYPE = PersonCategoryCodes.russia_resident_legal_entity

dt_now = dt.datetime.now()


def create_nirvana_block(session, options):
    options['run_id'] = session.now().strftime('%Y-%m-%dT%H:%M:%S')
    nb = (
        ob.NirvanaBlockBuilder(
            operation='manual_docs',
            request={
                'data': {'options': options},
                'context': {'workflow': {'owner': 'autodasha'}},
            },
        )
        .build(session)
        .obj
    )

    return nb


def check_outputs(req_output):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        result = set(tuple(sorted(d.items())) for d in list_of_dicts)
        return result

    def rm_from_report(dict_list, names):
        return [{k: v for k, v in d.items() if k not in names} for d in dict_list]

    assert to_set(
        rm_from_report(OUTPUTS['success_report'], ['Акт', 'Заказ', 'Новый счёт'])
    ) == to_set(req_output['success_report'])
    assert to_set(rm_from_report(OUTPUTS['fail_report'], ['Traceback'])) == to_set(
        req_output['fail_report']
    )
    assert to_set(
        rm_from_report(OUTPUTS['exported_objects'], ['object_id'])
    ) == to_set(req_output['exported_objects'])


@pytest.fixture(autouse=True)
def clear_io():
    global INPUTS, OUTPUTS
    INPUTS = {'input_rows': []}
    OUTPUTS = {'success_report': [], 'fail_report': [], 'exported_objects': []}


@contextlib.contextmanager
def mock_patch_type(type_id):
    with mock.patch(
        'balance.actions.nirvana.operations.manual_docs.Context.type', type_id
    ), mock.patch(
        'balance.actions.nirvana.task.TASK_ITEM_OP_MAP', {type_id: md.process_task_item}
    ):
        yield


@pytest.fixture(autouse=True)
def task_type(session):
    res = ob.NirvanaTaskTypeBuilder.construct(session)
    with mock_patch_type(res.id):
        yield res


def async_execution(session, flag):
    if flag:
        session.config.__dict__['PARALLELIZED_NIRVANA_OPERATIONS'] = ['manual_docs']
    else:
        session.config.__dict__['PARALLELIZED_NIRVANA_OPERATIONS'] = []


@pytest.fixture(autouse=True)
def mock_batch_processor():
    patch_path = 'balance.util.ParallelBatchProcessor.process_batches'
    calls = []

    def _process_batches(_s, func, batches, **kw):
        calls.append(batches)
        return map(partial(func, **kw), batches)

    with mock.patch(patch_path, _process_batches):
        yield calls


@pytest.fixture
def shop_product(session):
    return ob.ProductBuilder.construct(
        session, engine_id=ServiceId.ONE_TIME_SALE, price=10, service_code=''
    )


@pytest.fixture
def shop_product2(session):
    return ob.ProductBuilder.construct(
        session, price=1, engine_id=ServiceId.ONE_TIME_SALE
    )


@pytest.fixture
def shop_product3(session):
    return ob.ProductBuilder.construct(
        session, price=0.01, engine_id=ServiceId.ONE_TIME_SALE
    )


@pytest.fixture
def shop_product_w_tax(session):
    tax_policy = session.query(mapper.TaxPolicy).getone(TAX_POLICY_RUSSIA_RESIDENT)
    return ob.ProductBuilder.construct(
        session, price=500, engine_id=ServiceId.ONE_TIME_SALE,
        taxes=[tax_policy], unit=ob.Getter(mapper.ProductUnit, SHOWS_1000_UNIT_ID)
    )


@pytest.fixture
def shop_product_w_tax2(session):
    """ Особый случай для для продукта с НДС 20% и ценой, имеющих по половине копейки """
    tax_policy = session.query(mapper.TaxPolicy).getone(TAX_POLICY_RUSSIA_RESIDENT)
    product_unit = ob.Getter(mapper.ProductUnit, UNITS_1000_UNIT_ID)

    # Расчет цены с включенным НДС, у которой 20% НДС - 1.5 копеек, а цена без НДС - 7.5 копеек.
    price = decimal.Decimal('0.06') * 1 + decimal.Decimal('0.03')

    return ob.ProductBuilder.construct(
        session, price=price, engine_id=ServiceId.ONE_TIME_SALE, taxes=[tax_policy],
        unit=product_unit
    )


def patched_process(nirvana_block):
    session = nirvana_block.session

    with mock_transactions():
        nirvana_status = md.process(nirvana_block)

        if nirvana_block.real_operation in session.config.get('PARALLELIZED_NIRVANA_OPERATIONS', []):
            task = session.query(mapper.NirvanaTask).getone(block_id=nirvana_block.id)
            for item in task.items:
                process_object(session, 'NIRVANA_TASK_ITEM', 'NirvanaTaskItem', item.id)

            nirvana_status = md.process(nirvana_block)

    return nirvana_status


def patcher(f):
    def output_patch(self, name, data):
        OUTPUTS[name] = json.loads(data)

    patch_funcs = [
        (
            'balance.mapper.nirvana_processor.NirvanaBlock.download',
            lambda nb, name: json.dumps(INPUTS[name], cls=mapper.BalanceJSONEncoder),
        ),
        ('balance.mapper.nirvana_processor.NirvanaBlock.upload', output_patch),
    ]

    for target, func in patch_funcs:
        f = mock.patch(target, func)(f)
    return f


@pytest.mark.parametrize('async', [False, True])
@pytest.mark.parametrize(
    'input_data',
    [
        [
            {'Отчетный период': 1},
            {'ID продукта': '1'},
            {'ID продукта': '1', 'Отчетный период': 1},
            {'ID продукта': '1', 'Отчетный период': 1, 'Факт без НДС': 1},
            {
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 'На все!',
                'Выставить': 1,
            },
            {
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 'На все!',
                'Выставить': 1,
                'Признак типа оплаты': 'Предоплата',
            },
            {
                'ID продукта': '1',
                'Отчетный период': 'За первое число',
                'Факт без НДС': 1,
                'Выставить': 1,
                'Признак типа оплаты': 'Предоплата',
                '№ Договора': 'Большой',
            },
            {
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 'На все!',
                'Выставить': 1,
                'Признак типа оплаты': 'Предоплата',
                '№ Договора': 'Большой',
            },
            {
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': '25 миллиардов актов',
                'Признак типа оплаты': 'Предоплата',
                '№ Договора': 'Большой',
            },
            {
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                'Признак типа оплаты': 'Хочу заплатить потом',
                '№ Договора': 'Большой',
            },
            {
                'ID продукта': None,
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                'Признак типа оплаты': 'Хочу заплатить потом',
                '№ Договора': 'Большой',
            },
        ]
    ],
)
@patcher
def test_parse_file(session, input_data, async):
    async_execution(session, async)

    INPUTS['input_rows'] = input_data

    required_outputs = {
        'success_report': [],
        'fail_report': [
            {'Отчетный период': 1, 'Ошибка': 'Отсутствует поле "id продукта".'},
            {'ID продукта': '1', 'Ошибка': 'Отсутствует поле "отчетный период".'},
            {
                'ID продукта': '1',
                'Отчетный период': 1,
                'Ошибка': 'Отсутствует поле "факт без ндс".',
            },
            {
                'ID продукта': '1',
                'Отчетный период': 1,
                'Факт без НДС': 1,
                'Ошибка': 'Отсутствует поле "выставить".',
            },
            {
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 'На все!',
                'Выставить': 1,
                'Ошибка': 'Отсутствует поле "признак типа оплаты".',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 'На все!',
                'Выставить': 1,
                'Ошибка': 'Недостаточно данных для выставления документов.',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': 'За первое число',
                'Факт без НДС': 1,
                'Выставить': 1,
                'Ошибка': 'Неверный формат поля "отчетный период".',
                '№ Договора': 'Большой',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 'На все!',
                'Выставить': 1,
                '№ Договора': 'Большой',
                'Ошибка': 'Невозможно определить "факт без ндс".',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': '25 миллиардов актов',
                '№ Договора': 'Большой',
                'Ошибка': 'Невалидное значение поля "выставить" - 25 миллиардов актов',
            },
            {
                'Признак типа оплаты': 'Хочу заплатить потом',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                '№ Договора': 'Большой',
                'Ошибка': 'Невалидное значение поля "признак типа оплаты" - Хочу заплатить потом',
            },
            {
                'Признак типа оплаты': 'Хочу заплатить потом',
                'ID продукта': None,
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                '№ Договора': 'Большой',
                'Ошибка': 'Отсутствует поле "id продукта".',
            },
        ],
        'exported_objects': [],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@pytest.mark.parametrize(
    'input_data',
    [
        [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                '№ Договора': 'Нет',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                '№ Договора': 'TEST/TEST',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': 'Нет',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 1,
                'Выставить': 'Счёт + акт',
                '№ Договора': 'TEST/666',
                'ID Субклиента': '666',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '-1',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 1,
                'Выставить': 'Счёт + акт',
                '№ Договора': 'TEST/666',
            },
        ]
    ],
)
@patcher
def test_get_data(session, input_data, async):
    async_execution(session, async)

    INPUTS['input_rows'] = input_data

    contract1 = ob.ContractBuilder.construct(
        session,
        external_id='TEST/TEST',
        dt=dt_now - dt.timedelta(1),
        is_signed=dt_now,
    )
    contract2 = ob.ContractBuilder.construct(
        session,
        external_id='TEST/TEST',
        dt=dt_now - dt.timedelta(1),
        is_signed=dt_now,
    )
    contract3 = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
    )

    required_outputs = {
        'success_report': [],
        'fail_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                '№ Договора': 'Нет',
                'Ошибка': 'Действующий договор не найден.',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 1,
                'Выставить': 'Счёт',
                '№ Договора': 'TEST/TEST',
                'Ошибка': 'Найдено больше одного действующего договора.',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': '2021-01-01',
                'Факт без НДС': 1,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': 'Нет',
                'Ошибка': 'Счёт не найден.',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '1',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 1,
                'Выставить': 'Счёт + акт',
                '№ Договора': 'TEST/666',
                'ID Субклиента': '666',
                'Ошибка': 'Клиент не найден.',
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': '-1',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 1,
                'Выставить': 'Счёт + акт',
                '№ Договора': 'TEST/666',
                'Ошибка': 'Продукт не найден.',
            },
        ],
        'exported_objects': [],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [True])
@patcher
def test_no_35_service(session, shop_product, async):
    async_execution(session, async)

    product = shop_product
    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        is_signed=dt_now,
    )

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 1,
            'Выставить': 'Счёт + акт',
            '№ Договора': 'TEST/666',
        }
    ]

    required_outputs = {
        'success_report': [],
        'fail_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 1,
                'Выставить': 'Счёт + акт',
                '№ Договора': 'TEST/666',
                'Ошибка': 'В договоре отсутствует 35 сервис.',
            }
        ],
        'exported_objects': [],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_paysys(session, shop_product, async):
    async_execution(session, async)

    product = shop_product
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(
        session, client=client, type='yt', country_id=RegionId.LATVIA
    )
    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
        currency=NUM_CODE_RUR,
        deal_passport=dt_now,
        firm=1,
    )

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
        }
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '100',
                'Сумма нового счёта': '100',
                'Комментарий': None,
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_get_contract_with_client(session, shop_product, async):
    async_execution(session, async)

    product = shop_product
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)

    client2 = ob.ClientBuilder.construct(session)
    person2 = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)

    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
    )

    contract2 = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client2,
        person=person2,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
    )

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
            'ID Клиента': str(client.id),
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт',
            '№ Договора': contract2.external_id,
        },
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'ID Клиента': str(client.id),
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            }
        ],
        'fail_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт',
                '№ Договора': contract2.external_id,
                'Ошибка': 'Найдено больше одного действующего договора.',
            },
        ],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_prepayment(session, shop_product, shop_product3, async):
    async_execution(session, async)

    product = shop_product
    product3 = shop_product3
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)
    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
    )

    order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=product.id,
        service_id=ServiceId.ONE_TIME_SALE,
    )
    request = ob.RequestBuilder.construct(
        session,
        firm_id=FirmId.YANDEX_OOO,
        basket=ob.BasketBuilder(
            client=client, rows=[ob.BasketItemBuilder(order=order, quantity=120)]
        ),
    )
    invoice = ob.InvoiceBuilder.construct(
        session, client=client, person=person, request=request, paysys_id=1003
    )
    invoice.turn_on_rows()

    client2 = ob.ClientBuilder.construct(session, is_agency=True)
    person2 = ob.PersonBuilder.construct(session, client=client2, type=PERSON_UR_TYPE)
    contract2 = ob.create_credit_contract(
        session,
        client=client2,
        person=person2,
        services={ServiceId.ONE_TIME_SALE, ServiceId.TAXI_CASH, ServiceId.TAXI_CARD},
        commission=0,
        personal_account_fictive=0,
        firm=13,
    )

    cp = ContractPage(session, contract2.id)
    cp.create_personal_accounts()
    pa = contract2.invoices[0]
    session.refresh(contract2)

    assert pa.type == 'personal_account'

    order2 = ob.OrderBuilder.construct(
        session,
        client=client2,
        product_id=product.id,
        service_id=ServiceId.ONE_TIME_SALE,
    )
    request2 = ob.RequestBuilder.construct(
        session,
        firm_id=FirmId.TAXI,
        basket=ob.BasketBuilder(
            client=client2, rows=[ob.BasketItemBuilder(order=order2, quantity=1)]
        ),
    )
    invoice2 = ob.InvoiceBuilder.construct(
        session, client=client2, person=person2, request=request2, paysys_id=1301003
    )
    invoice2.turn_on_rows()

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт',
            '№ Договора': contract.external_id,
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Акт к счёту',
            'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice.external_id,
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract2.external_id,
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Акт к счёту',
            'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice2.external_id,
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product3.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 34915.375,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract2.external_id,
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product3.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 34915.38,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract2.external_id,
        },
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Дата акта': 'Нет',
                'Факт без НДС': 100,
                'Выставить': 'Счёт',
                '№ Договора': contract.external_id,
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': 'Нет',
                'Сумма акта': 'Нет',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': 'Нет',
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': 'Нет',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract2.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product3.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 34915.375,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract2.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '4189845',
                'Сумма акта': '41898.45',
                'Сумма нового счёта': '41898.45',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product3.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 34915.38,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract2.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '4189845.6',
                'Сумма акта': '41898.46',
                'Сумма нового счёта': '41898.46',
                'Комментарий': None,
            },
        ],
        'fail_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice2.external_id,
                'Ошибка': 'Недостаточно зачислений на заказе %s.' % order2.eid,
            }
        ],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_personal_account(session, shop_product, shop_product2, async):
    async_execution(session, async)

    product = shop_product
    product2 = shop_product2
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)

    contract = ob.create_credit_contract(
        session,
        client=client,
        person=person,
        services={ServiceId.ONE_TIME_SALE, 111, 128},
        commission=0,
        external_id='TEST/666',
        personal_account_fictive=0,
        firm=13,
    )

    cp = ContractPage(session, contract.id)
    cp.create_personal_accounts()
    pa = contract.invoices[0]
    session.refresh(contract)

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Постоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': 'TEST/666',
        },
        {
            'Признак типа оплаты': 'Постоплата',
            'ID продукта': str(product2.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт',
            '№ Договора': 'TEST/666',
        },
        {
            'Признак типа оплаты': 'Постоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Акт к счёту',
            'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': pa.external_id,
        },
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Постоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': 'TEST/666',
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': 'Нет',
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': 'Нет',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Постоплата',
                'ID продукта': str(product2.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт',
                '№ Договора': 'TEST/666',
                'Дата нового счёта': 'Нет',
                'Количество по акту': '120',
                'Сумма акта': '120',
                'Сумма нового счёта': 'Нет',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Постоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': pa.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': 'Нет',
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': 'Нет',
                'Комментарий': None,
            },
        ],
        'fail_report': [],
        'exported_objects': [
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_fictive_personal_account(session, shop_product, async):
    async_execution(session, async)

    product = shop_product
    client = ob.ClientBuilder.construct(session, is_agency=True)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)
    contract = ob.create_credit_contract(
        session,
        client=client,
        person=person,
        services={ServiceId.ONE_TIME_SALE},
        commission=0,
        external_id='TEST/666',
    )
    paysys = session.query(mapper.Paysys).getone(1003)
    pa = (
        PersonalAccountManager(session)
        .for_contract(contract)
        .for_paysys(paysys)
        .get(auto_create=True, dt=contract.col0.dt)
    )

    manager = ob.SingleManagerBuilder.construct(session, domain_login='ya_krevedko')

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Постоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
            'Текущий ответственный': 'ya_krevedko',
        },
        {
            'Признак типа оплаты': 'Постоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': pa.external_id,
            'Текущий ответственный': 'i_ya_krevedko',
        },
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Постоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'Текущий ответственный': 'ya_krevedko',
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Постоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': pa.external_id,
                'Текущий ответственный': 'i_ya_krevedko',
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': 'Не удалось найти менеджера i_ya_krevedko. Документы выставили без менеджера.',
            },
        ],
        'fail_report': [],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    # assert pa.consumes[0].order.manager == manager
    # assert pa.consumes[1].order.manager is None

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_empty_data(session, shop_product, async):
    async_execution(session, async)

    product = shop_product
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)
    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
    )

    order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=product.id,
        service_id=ServiceId.ONE_TIME_SALE,
    )
    request = ob.RequestBuilder.construct(
        session,
        firm_id=FirmId.YANDEX_OOO,
        basket=ob.BasketBuilder(
            client=client, rows=[ob.BasketItemBuilder(order=order, quantity=120)]
        ),
    )
    invoice = ob.InvoiceBuilder.construct(
        session, client=client, person=person, request=request, paysys_id=1003
    )
    invoice.turn_on_rows()

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
        },
        {
            'Признак типа оплаты': None,
            'ID продукта': None,
            'Отчетный период': None,
            'Факт без НДС': None,
            'Выставить': None,
            '№ Договора': None,
            'Текущий ответственный': None,
        },
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
@mock.patch(
    'balance.actions.nirvana.operations.manual_docs.processor.can_close_invoice',
    lambda x, y: False,
)
@pytest.mark.parametrize('async', [False, True])
@pytest.mark.parametrize(
    'force_acts, need_perms, exp_res',
    [
        (True, True, True),
        (True, False, False),
        (False, True, False),
        (False, False, False),
    ],
)
def test_force_acts(session, shop_product, force_acts, need_perms, exp_res, async):
    async_execution(session, async)

    if need_perms:
        role1 = ob.create_role(session, PermissionCode.BILLING_SUPPORT)
        role2 = ob.create_role(session, PermissionCode.CREATE_REQUESTS_SHOP)
        roles = [role1, role2]
        ob.create_passport(session, *roles, patch_session=True)
    else:
        ob.create_passport(session, patch_session=True)

    product = shop_product
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)
    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
    )

    order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=product.id,
        service_id=ServiceId.ONE_TIME_SALE,
    )
    request = ob.RequestBuilder.construct(
        session,
        firm_id=FirmId.YANDEX_OOO,
        basket=ob.BasketBuilder(
            client=client, rows=[ob.BasketItemBuilder(order=order, quantity=120)]
        ),
    )
    invoice = ob.InvoiceBuilder.construct(
        session, client=client, person=person, request=request, paysys_id=1003
    )
    invoice.turn_on_rows()

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Акт к счёту',
            'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice.external_id,
        }
    ]

    if exp_res:
        success_report = [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': 'Нет',
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': 'Нет',
                'Комментарий': None,
            }
        ]
        fail_report = []
        exported_objects = [
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ]
    else:
        success_report = []
        fail_report = [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice.external_id,
                'Ошибка': 'Запрещено выставлять документы в закрытом периоде.',
            }
        ]
        exported_objects = []

    required_outputs = {
        'success_report': success_report,
        'fail_report': fail_report,
        'exported_objects': exported_objects,
    }

    nb = create_nirvana_block(session, {'force_acts': force_acts})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_subclient(session, shop_product, async):
    async_execution(session, async)

    product = shop_product
    client = ob.ClientBuilder.construct(session, is_agency=True)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)
    contract = ob.create_credit_contract(
        session,
        client=client,
        person=person,
        services={ServiceId.ONE_TIME_SALE},
        commission=0,
        external_id='TEST/666',
    )

    order = ob.OrderBuilder.construct(
        session, client=subclient1, agency=client, product_id=product.id, service_id=ServiceId.ONE_TIME_SALE
    )

    paysys = session.query(mapper.Paysys).getone(1003)

    pa = (
        PersonalAccountManager(session)
        .for_contract(contract, subclients=[subclient1])
        .for_paysys(paysys)
        .get(auto_create=True, dt=contract.col0.dt)
    )
    pa.transfer(order, mode=2, sum=200, skip_check=True)
    order.calculate_consumption(
        ut.trunc_date(dt_now), {order.shipment_type: 200}
    )
    session.flush()

    pa.generate_act(force=1)

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Постоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
            'ID субклиента': str(subclient1.id),
        },
        {
            'Признак типа оплаты': 'Постоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
            'ID субклиента': str(subclient2.id),
        },
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Постоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'ID субклиента': str(subclient1.id),
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            },
            {
                'Признак типа оплаты': 'Постоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'ID субклиента': str(subclient2.id),
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': '120',
                'Комментарий': None,
            },
        ],
        'fail_report': [],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert (
        (pa.consumes[0].order.client_id, pa.consumes[1].order.client_id)
        in ((subclient1.id, subclient2.id), (subclient2.id, subclient1.id))
    )

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_no_product(session, shop_product, shop_product3, async):
    async_execution(session, async)

    product = shop_product
    product3 = shop_product3
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type=PERSON_UR_TYPE)
    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
    )

    order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=product.id,
        service_id=ServiceId.ONE_TIME_SALE,
    )
    request = ob.RequestBuilder.construct(
        session,
        firm_id=FirmId.YANDEX_OOO,
        basket=ob.BasketBuilder(
            client=client, rows=[ob.BasketItemBuilder(order=order, quantity=120)]
        ),
    )
    invoice = ob.InvoiceBuilder.construct(
        session, client=client, person=person, request=request, paysys_id=1003
    )
    invoice.turn_on_rows()

    order2 = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=product.id,
        service_id=ServiceId.ONE_TIME_SALE,
    )
    order3 = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=product3.id,
        service_id=ServiceId.ONE_TIME_SALE,
    )
    request2 = ob.RequestBuilder.construct(
        session,
        firm_id=FirmId.YANDEX_OOO,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=order2, quantity=1),
                ob.BasketItemBuilder(order=order3, quantity=1),
            ],
        ),
    )
    invoice2 = ob.InvoiceBuilder.construct(
        session, client=client, person=person, request=request2, paysys_id=1003
    )
    invoice2.turn_on_rows()

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Акт к счёту',
            'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice.external_id,
        },
        {
            'Признак типа оплаты': 'Предоплата',
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 100,
            'Выставить': 'Акт к счёту',
            'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice2.external_id,
        },
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': 'Нет',
                'Количество по акту': '12',
                'Сумма акта': '120',
                'Сумма нового счёта': 'Нет',
                'Комментарий': None,
            }
        ],
        'fail_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 100,
                'Выставить': 'Акт к счёту',
                'Заполнить номер счета, если вы выбрали вариант выставить \'акт к счету\'': invoice2.external_id,
                'Ошибка': 'Невозможно определить продукт по счету.',
            }
        ],
        'exported_objects': [{'classname': 'Act', 'queue': 'OEBS_API'}],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_product_with_non_single_type_rate_and_tax(session, shop_product_w_tax):
    async_execution(session, False)

    product = shop_product_w_tax
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(
        session, client=client, type=PERSON_UR_TYPE, country_id=RegionId.LATVIA
    )

    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
        currency=NUM_CODE_RUR,
        deal_passport=dt_now,
        firm=1,
    )

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 10000,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
        }
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 10000,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '24000',
                'Сумма акта': '12000',
                'Сумма нового счёта': '12000',
                'Комментарий': None,
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_product_with_tax_on_half_of_min_currency_value(session, shop_product_w_tax2):
    """ Проверяет корректность формирования документов,
        когда при расчетах у значений НДС и общей суммы без НДС есть по половине копейки"""

    async_execution(session, False)

    product = shop_product_w_tax2
    client = ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(
        session, client=client, type=PERSON_UR_TYPE, country_id=RegionId.LATVIA
    )

    contract = ob.ContractBuilder.construct(
        session,
        external_id='TEST/666',
        dt=dt_now - dt.timedelta(1),
        client=client,
        person=person,
        is_signed=dt_now,
        services=[ServiceId.ONE_TIME_SALE],
        currency=NUM_CODE_RUR,
        deal_passport=dt_now,
        firm=1,
    )

    INPUTS['input_rows'] = [
        {
            'Признак типа оплаты': 'Предоплата',
            'ID продукта': str(product.id),
            'Отчетный период': dt_now.strftime('%Y-%m-%d'),
            'Факт без НДС': 0.82,
            'Выставить': 'Счёт + акт',
            '№ Договора': contract.external_id,
        }
    ]

    required_outputs = {
        'success_report': [
            {
                'Признак типа оплаты': 'Предоплата',
                'ID продукта': str(product.id),
                'Отчетный период': dt_now.strftime('%Y-%m-%d'),
                'Факт без НДС': 0.82,
                'Выставить': 'Счёт + акт',
                '№ Договора': contract.external_id,
                'Дата акта': dt_now.strftime('%d-%m-%Y'),
                'Дата нового счёта': dt_now.strftime('%d-%m-%Y'),
                'Количество по акту': '11',
                'Сумма акта': '0.99',
                'Сумма нового счёта': '0.99',
                'Комментарий': None,
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {'classname': 'Invoice', 'queue': 'OEBS_API'},
            {'classname': 'Act', 'queue': 'OEBS_API'},
        ],
    }

    nb = create_nirvana_block(session, {})

    res = patched_process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)
