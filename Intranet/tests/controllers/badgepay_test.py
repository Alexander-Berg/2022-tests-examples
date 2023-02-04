import datetime as dt
from decimal import Decimal

import pytest
from mock import Mock

from staff.lib import attr_ext
from staff.person_profile.controllers import badgepay
from staff.person_profile.controllers.badgepay import (
    BadgepayAnswerError,
    BadgepayReport,
    FOOD_ACCOUNT,
    FullFoodReport,
    ShortFoodReport,
    get_badgepay_report,
    get_full_food_report,
    get_short_food_report,
    Month,
    ReportOrder,
    requests,
    tvm2,
)


CUR_DATE = dt.date(2015, 9, 3)
LAST_DAY = dt.date(2015, 9, 30)
HOLIDAYS = [
    {'date': dt.date(2015, 9, 5)},
    {'date': dt.date(2015, 9, 6)},
    {'date': dt.date(2015, 9, 12)},
    {'date': dt.date(2015, 9, 13)},
    {'date': dt.date(2015, 9, 19)},
    {'date': dt.date(2015, 9, 20)},
    {'date': dt.date(2015, 9, 25)},
    {'date': dt.date(2015, 9, 26)},
]
NUM_OF_WORKING_DAYS_LEFT = 20
NUM_OF_WORKING_DAYS_PASSED = 2


DAY_SUM = Decimal(450)
MONTH_SUM = Decimal(9700)
SPENT_SUM = Decimal(8000)
SPENT_DAY_SUM = Decimal(300)
REMAINED_SUM = MONTH_SUM - SPENT_SUM
RECOMMENDED_PER_DAY = REMAINED_SUM / NUM_OF_WORKING_DAYS_LEFT
REMAINED_DAY_SUM = DAY_SUM - SPENT_DAY_SUM

TAVERN = 'The Boiled Cat'
SPENT_IN_TAVERN = Decimal(800)
SPENT_BREAKFAST = Decimal(200)
SPENT_SNACK = Decimal(100)
DATE_AT_TAVERN = dt.datetime(2015, 9, 2)
DATE_AT_TAVERN_BREAKFAST = dt.datetime(2015, 9, 1)
OVERRUN = Decimal(5430.0)
FOOD_BALLANCE_COMPENSATION_DAYS = 21
DAYS_USED = 3


@pytest.fixture
def badgepay_json():
    return {
        'login': 'blablabla',
        'benefits': [
            {
                'account': 'Food',
                'ptype': 'Month',
                'days': FOOD_BALLANCE_COMPENSATION_DAYS,
                'sum': MONTH_SUM,
            },
            {
                'account': 'Food',
                'ptype': 'Day',
                'days': FOOD_BALLANCE_COMPENSATION_DAYS,
                'sum': 0,
            },
            {
                'account': 'Breakfast',
                'ptype': 'Day',
                'days': None,
                'sum': 200,
            }
        ],
        'limits': [
            {
                'account': 'Food',
                'ptype': 'Month',
                'sum': -1,
            },
            {
                'account': 'Breakfast',
                'ptype': 'Day',
                'sum': 200,
            },
            {
                'account': 'Souvenirs',
                'ptype': 'Month',
                'sum': -1,
            }
        ],
        'expenses': [
            {
                'account': 'Food',
                'ptype': 'Month',
                'days': DAYS_USED,
                'sum': SPENT_SUM,
            },
            {
                'account': 'Food',
                'ptype': 'Day',
                'days': DAYS_USED,
                'sum': 0,
            },
            {
                'account': 'Breakfast',
                'ptype': 'Day',
                'sum': 1200,
            },
            {
                'account': 'Souvenirs',
                'ptype': 'Month',
                'sum': 256,
            },
        ],
        'orders': [
            {
                'date': DATE_AT_TAVERN.isoformat(),
                'merchant': TAVERN,
                'sum': SPENT_IN_TAVERN + SPENT_SNACK,
                'cashflows': [
                    {
                        'account': 'Food',
                        'sum': SPENT_IN_TAVERN,
                    },
                    {
                        'account': 'Snack',
                        'sum': SPENT_SNACK,
                    },
                ],
            },
            {
                'date': DATE_AT_TAVERN.isoformat(),
                'merchant': TAVERN,
                'sum': SPENT_IN_TAVERN + SPENT_BREAKFAST,
                'cashflows': [
                    {
                        'account': 'Food',
                        'sum': SPENT_IN_TAVERN,
                    },
                    {
                        'account': 'Breakfast',
                        'sum': SPENT_BREAKFAST,
                    },
                ],
            },
            {
                'date': DATE_AT_TAVERN_BREAKFAST.isoformat(),
                'merchant': TAVERN,
                'sum': SPENT_BREAKFAST,
                'cashflows': [
                    {
                        'account': 'Breakfast',
                        'sum': SPENT_BREAKFAST,
                    },
                ],
            },
        ],
        'overrun': OVERRUN,
    }


@pytest.fixture
def badgepay_daily_json(badgepay_json):
    json_data = badgepay_json
    json_data['benefits'][0]['sum'] = Decimal(0)
    json_data['benefits'][1]['sum'] = DAY_SUM
    json_data['expenses'][0]['sum'] = Decimal(0)
    json_data['expenses'][1]['sum'] = SPENT_DAY_SUM
    return json_data


@pytest.fixture
def mock_request(monkeypatch):
    def mocker(status_code, json_answer=None):
        monkeypatch.setattr(tvm2, 'get_tvm_ticket_by_deploy', lambda *a, **kw: '')
        answer = Mock(status_code=status_code, json=lambda: json_answer)
        monkeypatch.setattr(requests, 'post', lambda *a, **kw: answer)

    return mocker


@pytest.fixture
def mock_date_and_holidays(monkeypatch):
    def mocker(cur_date=CUR_DATE):
        monkeypatch.setattr(Month, 'holidays', lambda *a, **kw: HOLIDAYS)
        monkeypatch.setattr(badgepay, 'get_today', lambda *a, **kw: cur_date)

    return mocker


def test_get_report_wrong_code(mock_request):
    mock_request(400)
    with pytest.raises(BadgepayAnswerError):
        get_badgepay_report('asd')


def test_get_report_wrong_json(mock_request):
    mock_request(400, ['a'])
    with pytest.raises(BadgepayAnswerError):
        get_badgepay_report('asd')


def test_get_report_correct_json(mock_request, badgepay_json):
    mock_request(200, badgepay_json)
    report = get_badgepay_report('asd')
    assert report


def test_report_month_sum_exists(badgepay_json):
    report = attr_ext.from_kwargs(BadgepayReport, **badgepay_json)
    assert report.food_month_sum == MONTH_SUM


def test_report_spent_sum_exists(badgepay_json):
    report = attr_ext.from_kwargs(BadgepayReport, **badgepay_json)
    assert report.food_spent_sum_month == SPENT_SUM


def test_report_day_sum_exists(badgepay_daily_json):
    report = attr_ext.from_kwargs(BadgepayReport, **badgepay_daily_json)
    assert report.food_day_sum == DAY_SUM


def test_report_day_spent_sum_exists(badgepay_daily_json):
    report = attr_ext.from_kwargs(BadgepayReport, **badgepay_daily_json)
    assert report.food_spent_sum_day == SPENT_DAY_SUM


def test_report_month_sum_nonexists(badgepay_json):
    report = attr_ext.from_kwargs(BadgepayReport, **badgepay_json)
    report.benefits = []
    assert report.food_month_sum == 0


def test_report_spent_month_sum_nonexists(badgepay_json):
    report = attr_ext.from_kwargs(BadgepayReport, **badgepay_json)
    report.expenses = []
    assert report.food_spent_sum_month == 0


@pytest.mark.django_db
def test_get_short_food_report(mock_date_and_holidays, mock_request, badgepay_json):
    mock_request(200, badgepay_json)
    mock_date_and_holidays()
    expecting = ShortFoodReport(
        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        food_balance_compensation_days=FOOD_BALLANCE_COMPENSATION_DAYS - DAYS_USED,
        food_days_total=FOOD_BALLANCE_COMPENSATION_DAYS,
        recommended_per_day=RECOMMENDED_PER_DAY,
        month_remained_sum=REMAINED_SUM,
        day_remained_sum=0,
        month_limit=MONTH_SUM,
        day_limit=0,
        overspending=OVERRUN,
    )
    result = get_short_food_report('login')

    assert result == expecting


@pytest.mark.django_db
def test_get_short_food_report_daily_mode(mock_date_and_holidays, mock_request, badgepay_daily_json):
    mock_request(200, badgepay_daily_json)
    mock_date_and_holidays()
    expecting = ShortFoodReport(
        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        food_balance_compensation_days=FOOD_BALLANCE_COMPENSATION_DAYS - DAYS_USED,
        food_days_total=FOOD_BALLANCE_COMPENSATION_DAYS,
        recommended_per_day=0,
        month_remained_sum=0,
        day_remained_sum=REMAINED_DAY_SUM,
        month_limit=0,
        day_limit=DAY_SUM,
        overspending=OVERRUN,
    )
    result = get_short_food_report('login')

    assert result == expecting


@pytest.mark.django_db
def test_get_short_food_report_daily_mode_with_penalty(mock_date_and_holidays, mock_request, badgepay_daily_json):
    NEW_SPENT_TODAY = Decimal(100500)
    badgepay_daily_json['expenses'][1]['sum'] = NEW_SPENT_TODAY
    mock_request(200, badgepay_daily_json)
    mock_date_and_holidays()
    expecting = ShortFoodReport(
        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        # Если баланс дня <= 0, то количество дней компенсации ("days" : 3) нужно уменьшать на 1.
        food_balance_compensation_days=FOOD_BALLANCE_COMPENSATION_DAYS - DAYS_USED - 1,
        food_days_total=FOOD_BALLANCE_COMPENSATION_DAYS,
        recommended_per_day=0,
        month_remained_sum=0,
        day_remained_sum=DAY_SUM - NEW_SPENT_TODAY,
        month_limit=0,
        day_limit=DAY_SUM,
        overspending=OVERRUN,
    )
    result = get_short_food_report('login')

    assert result == expecting


@pytest.mark.django_db
def test_get_short_food_report_on_last_month_day(mock_date_and_holidays, mock_request, badgepay_json):
    mock_request(200, badgepay_json)
    mock_date_and_holidays(LAST_DAY)
    expecting = ShortFoodReport(
        work_days_left=1,
        food_balance_compensation_days=FOOD_BALLANCE_COMPENSATION_DAYS - DAYS_USED,
        food_days_total=FOOD_BALLANCE_COMPENSATION_DAYS,
        recommended_per_day=REMAINED_SUM,
        month_remained_sum=REMAINED_SUM,
        day_remained_sum=0,
        month_limit=MONTH_SUM,
        day_limit=0,
        overspending=OVERRUN,
    )

    result = get_short_food_report('login')

    assert result == expecting


@pytest.mark.django_db
def test_get_short_food_report_negative_sum(mock_date_and_holidays, mock_request, badgepay_json):
    expenses = next(
        it for it in badgepay_json['expenses']
        if it['account'] == FOOD_ACCOUNT and it['ptype'] == 'Month'
    )
    new_spent = MONTH_SUM + 1000
    expenses['sum'] = new_spent

    mock_request(200, badgepay_json)
    mock_date_and_holidays()

    expecting = ShortFoodReport(
        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        food_balance_compensation_days=FOOD_BALLANCE_COMPENSATION_DAYS - DAYS_USED,
        food_days_total=FOOD_BALLANCE_COMPENSATION_DAYS,
        recommended_per_day=0,
        month_remained_sum=MONTH_SUM - new_spent,
        day_remained_sum=0,
        month_limit=MONTH_SUM,
        day_limit=0,
        overspending=OVERRUN,
    )

    result = get_short_food_report('login')

    assert result == expecting


@pytest.mark.django_db
def test_get_full_food_report(mock_date_and_holidays, mock_request, badgepay_json):
    mock_request(200, badgepay_json)
    mock_date_and_holidays()
    expecting = FullFoodReport(

        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        work_days_passed=NUM_OF_WORKING_DAYS_PASSED,

        food_balance_compensation_days=FOOD_BALLANCE_COMPENSATION_DAYS - DAYS_USED,
        food_days_total=FOOD_BALLANCE_COMPENSATION_DAYS,
        recommended_per_day=REMAINED_SUM/NUM_OF_WORKING_DAYS_LEFT,
        middle_spent_per_day=SPENT_SUM/NUM_OF_WORKING_DAYS_PASSED,
        remained_sum_month=REMAINED_SUM,
        remained_sum_day=0,  # для случая месячных лимитов

        limit_month=MONTH_SUM,
        limit_day=0,  # для случая месячных лимитов
        overspending=OVERRUN,

        orders=[
            ReportOrder(
                date=DATE_AT_TAVERN,
                merchant=TAVERN,
                sum=SPENT_IN_TAVERN + SPENT_SNACK,
                food=SPENT_IN_TAVERN,
                breakfast=Decimal(0),
                snack=SPENT_SNACK,
                money=Decimal(0),
            ),
            ReportOrder(
                date=DATE_AT_TAVERN,
                merchant=TAVERN,
                sum=SPENT_IN_TAVERN + SPENT_BREAKFAST,
                food=SPENT_IN_TAVERN,
                breakfast=SPENT_BREAKFAST,
                snack=Decimal(0),
                money=Decimal(0),
            ),
            ReportOrder(
                date=DATE_AT_TAVERN_BREAKFAST,
                merchant=TAVERN,
                sum=SPENT_BREAKFAST,
                food=Decimal(0),
                breakfast=SPENT_BREAKFAST,
                snack=Decimal(0),
                money=Decimal(0),
            ),
        ],
    )

    result = get_full_food_report('login')

    assert result == expecting


@pytest.mark.django_db
def test_get_short_food_report_for_daily_mode(mock_date_and_holidays, mock_request, badgepay_daily_json):
    mock_request(200, badgepay_daily_json)
    mock_date_and_holidays()
    expecting = ShortFoodReport(
        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        food_balance_compensation_days=FOOD_BALLANCE_COMPENSATION_DAYS - DAYS_USED,
        food_days_total=FOOD_BALLANCE_COMPENSATION_DAYS,
        recommended_per_day=0,
        month_remained_sum=0,
        day_remained_sum=REMAINED_DAY_SUM,
        month_limit=0,
        day_limit=DAY_SUM,
        overspending=OVERRUN,
    )
    result = get_short_food_report('login')

    assert result == expecting


@pytest.mark.django_db
def test_get_short_food_report_no_days_left(mock_date_and_holidays, mock_request):
    _badgepay_json = {
        'login': 'login',
        'benefits': [
            {
                'account': 'Food',
                'ptype': 'Day',
                'days': 15,
                'sum': Decimal('100.00'),
            },
        ],
        'limits': [
        ],
        'expenses': [
            {
                'account': 'Food',
                'ptype': 'Day',
                'days': 15,
                'sum': Decimal('40.00'),
            },
        ],
        'orders': [
        ],
        'overrun': Decimal('110.00'),
    }

    mock_request(200, _badgepay_json)
    mock_date_and_holidays()

    result = get_short_food_report('login')

    expected = ShortFoodReport(
        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        food_balance_compensation_days=0,
        food_days_total=15,
        recommended_per_day=0,
        month_remained_sum=0,
        day_remained_sum=Decimal('-40.00'),
        month_limit=0,
        day_limit=Decimal('100.00'),
        overspending=Decimal('110.00'),
    )

    assert result == expected


@pytest.mark.django_db
def test_get_full_food_report_no_days_left(mock_date_and_holidays, mock_request):
    _badgepay_json = {
        'login': 'login',
        'benefits': [
            {
                'account': 'Food',
                'ptype': 'Day',
                'days': 15,
                'sum': Decimal('100.00'),
            },
        ],
        'limits': [
        ],
        'expenses': [
            {
                'account': 'Food',
                'ptype': 'Day',
                'days': 15,
                'sum': Decimal('40.00'),
            },
        ],
        'orders': [
        ],
        'overrun': Decimal('110.00'),
    }

    mock_request(200, _badgepay_json)
    mock_date_and_holidays()

    result = get_full_food_report('login')

    expected = FullFoodReport(
        work_days_left=NUM_OF_WORKING_DAYS_LEFT,
        work_days_passed=NUM_OF_WORKING_DAYS_PASSED,

        food_balance_compensation_days=0,
        food_days_total=15,
        recommended_per_day=0,
        middle_spent_per_day=0,
        remained_sum_month=0,
        remained_sum_day=Decimal('-40.00'),

        limit_month=0,
        limit_day=Decimal('100.00'),
        overspending=Decimal('110.00'),

        orders=[
        ],
    )

    assert result == expected
