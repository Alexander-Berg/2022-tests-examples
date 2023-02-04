import datetime
import pytest

from mock import mock

from review.compensations import logic, exceptions
from review.compensations.models.choices import REGULAR_PAYMENT_TYPES

from . import factories


@pytest.fixture(scope='module', autouse=True)
def mock_s3_storage():
    with mock.patch('review.lib.s3.S3Storage.save', return_value='mock_filename.xlsx'):
        yield


@pytest.fixture
def setup(db):
    payment_types = [
        factories.PaymentTypeFactory(),
        factories.PaymentTypeFactory(),
        factories.PaymentTypeFactory(),
        factories.PaymentTypeFactory(),
    ]
    regular_dates = {
        REGULAR_PAYMENT_TYPES.salary: datetime.date(2022, 5, 5),
        REGULAR_PAYMENT_TYPES.preadvance: datetime.date(2022, 5, 17),
        REGULAR_PAYMENT_TYPES.advance: datetime.date(2022, 5, 20),
    }
    payment_type_to_regular_payments = {
        payment_types[0].id: [REGULAR_PAYMENT_TYPES.salary, REGULAR_PAYMENT_TYPES.advance],
        payment_types[1].id: [REGULAR_PAYMENT_TYPES.advance],
        # Если для выбранного типа выплат в данной стране Элемент не предусматривает привязки к регулярным датам.
        payment_types[2].id: [],
        payment_types[3].id: [
            REGULAR_PAYMENT_TYPES.salary,
            REGULAR_PAYMENT_TYPES.advance,
            REGULAR_PAYMENT_TYPES.preadvance,
        ],
    }

    return locals()


# Примеры дат во всех промежутках между regular_dates из setup-фикстуры
RAW_DATES = [
    datetime.date(2022, 5, 3),
    datetime.date(2022, 5, 9),
    datetime.date(2022, 5, 18),
    datetime.date(2022, 6, 2),
]

ANSWERS = {  # (raw_date, payment_type) -> result
    (RAW_DATES[0], 0): exceptions.RegularPaymentDateNotFoundError,
    (RAW_DATES[0], 1): exceptions.RegularPaymentDateNotFoundError,
    (RAW_DATES[0], 2): RAW_DATES[0],
    (RAW_DATES[0], 3): exceptions.RegularPaymentDateNotFoundError,

    (RAW_DATES[1], 0): datetime.date(2022, 5, 5),
    (RAW_DATES[1], 1): exceptions.RegularPaymentDateNotFoundError,
    (RAW_DATES[1], 2): RAW_DATES[1],
    (RAW_DATES[1], 3): datetime.date(2022, 5, 5),

    (RAW_DATES[2], 0): datetime.date(2022, 5, 5),
    (RAW_DATES[2], 1): exceptions.RegularPaymentDateNotFoundError,
    (RAW_DATES[2], 2): RAW_DATES[2],
    (RAW_DATES[2], 3): datetime.date(2022, 5, 17),

    (RAW_DATES[3], 0): datetime.date(2022, 5, 20),
    (RAW_DATES[3], 1): datetime.date(2022, 5, 20),
    (RAW_DATES[3], 2): RAW_DATES[3],
    (RAW_DATES[3], 3): datetime.date(2022, 5, 20),
}


@pytest.mark.parametrize('raw_date', RAW_DATES)
@pytest.mark.parametrize('payment_type_no', [0, 1, 2, 3])
def test_get_last_possible_payment_date(setup, raw_date, payment_type_no):
    payment_type = setup['payment_types'][payment_type_no]

    payment = factories.PersonPaymentFactory(
        schedule=factories.PersonPaymentScheduleFactory(payment_type=payment_type),
        raw_date=raw_date,
    )

    expected_result = ANSWERS[(raw_date, payment_type_no)]

    if isinstance(expected_result, datetime.date):
        result_payment_date = logic.get_last_possible_payment_date(
            payment=payment,
            regulars=setup['regular_dates'],
            payment_type_to_regular_payments=setup['payment_type_to_regular_payments'],
        )
        assert result_payment_date == expected_result

    elif issubclass(expected_result, Exception):
        with pytest.raises(expected_result):
            logic.get_last_possible_payment_date(
                payment=payment,
                regulars=setup['regular_dates'],
                payment_type_to_regular_payments=setup['payment_type_to_regular_payments'],
            )


def test_get_last_possible_payment_date_fails_on_wrong_payment_type():
    payment_type = factories.PaymentTypeFactory()
    another_payment_type = factories.PaymentTypeFactory()
    payment = factories.PersonPaymentFactory(
        schedule=factories.PersonPaymentScheduleFactory(payment_type=payment_type),
        raw_date=datetime.date(2022, 5, 25),
    )

    with pytest.raises(exceptions.ElementNotFoundError):
        logic.get_last_possible_payment_date(
            payment=payment,
            regulars={REGULAR_PAYMENT_TYPES.salary: datetime.date(2022, 5, 25)},
            payment_type_to_regular_payments={another_payment_type.id: []},
        )
