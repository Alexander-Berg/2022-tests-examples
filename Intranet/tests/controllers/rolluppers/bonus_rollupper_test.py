import random

from staff.oebs.controllers.rolluppers.bonus_rollupper import get_non_review_bonus


def test_get_non_review_bonus_no_non_review_bonus():
    data = f'{{"value_type": "Процент от оклада", "value_source": "test", "value": {random.random()}}}'
    data += f',{{"value_type": "test", "value_source": "Значением", "value": {random.random()}}}'

    assert get_non_review_bonus(f'[{data}]') is None


def test_get_non_review_bonus():
    total_bonuses = 0
    data = f'{{"value_type": "Процент от оклада", "value_source": "test", "value": {random.random()}}}'
    data += f',{{"value_type": "test", "value_source": "Значением", "value": {random.random()}}}'

    for _ in range(random.randint(3, 7)):
        bonus = random.random()
        total_bonuses += bonus
        data += f',{{"value_type": "Процент от оклада", "value_source": "Значением", "value": {bonus}}}'

    assert get_non_review_bonus(f'[{data}]') == round(total_bonuses * 12, 13)


def test_get_non_review_bonus_truncating():
    bonus = 16.67
    data = f'{{"value_type": "Процент от оклада", "value_source": "Значением", "value": {bonus}}}'

    assert get_non_review_bonus(f'[{data}]') == 200.04
