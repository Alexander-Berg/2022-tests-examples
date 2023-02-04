# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, get_profile_data


def test_food_monthly_balance_success(uid, tg_app):
    with patch('uhura.external.staff.get_food_balance') as m:
        m.return_value = get_profile_data()
        handle_utterance(
            tg_app,
            uid,
            'бейдж',
            'Остаток на твоём бейдже — 7 190.80 руб. До конца месяца 15 рабочих дней. Рекомендованная сумма расходов: 479.38 руб.'
        )


def test_food_daily_balance_success(uid, tg_app):
    with patch('uhura.external.staff.get_food_balance') as m:
        profile = get_profile_data()
        profile['food_limit_month'] = "0"
        profile['food_balance_month'] = "0"
        profile['food_balance_day'] = "123"
        m.return_value = profile
        handle_utterance(
            tg_app,
            uid,
            'бейдж',
            'Остаток на твоём бейдже на сегодня — 123.00 руб. Перерасход за месяц — 544.00 руб. До конца месяца 15 рабочих дней.'
        )


def test_food_balance__last_working_day(uid, tg_app):
    with patch('uhura.external.staff.get_food_balance') as m:
        profile = get_profile_data()
        profile['food_balance_remaining_work_days'] = 0
        m.return_value = profile
        handle_utterance(
            tg_app,
            uid,
            'бейдж',
            'Остаток на твоём бейдже — 7 190.80 руб. Сегодня последний рабочий день.'
        )


def test_get_food_balance_error(uid, tg_app):
    with patch('uhura.external.staff.get_food_balance') as m:
        m.return_value = None
        handle_utterance(
            tg_app,
            uid,
            'бейдж',
            'Не могу связаться со стаффом. Попробуй через минуту'
        )
