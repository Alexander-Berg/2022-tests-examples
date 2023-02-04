# coding: utf-8
import pytest
import arrow

from review.oebs import models
from review.oebs import logic
from review.oebs import const
from review.oebs.sync import fake


@pytest.fixture
def finance_builder(db, person_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'person' not in params:
            params['person'] = person_builder()

        login = params['person'].login
        generate_fields = params.pop('generate_fields', [])
        for oebs_field in const.OEBS_DATA_TYPES:
            if oebs_field in params:
                continue
            if oebs_field in generate_fields:
                params[oebs_field] = fake.generate_data(
                    data_types=[oebs_field],
                    logins=[login],
                )[login][oebs_field]

        if const.SALARY_HISTORY in params:
            for event in params[const.SALARY_HISTORY]:
                date_from = arrow.get(event['dateFrom']).date()
                date_to = arrow.get(event['dateTo']).date()
                event = logic.encrypt_finance_event(event)
                models.FinanceEvents.objects.create(
                    person=params['person'],
                    date_to=date_to,
                    date_from=date_from,
                    event=event,
                    type=const.FINANCE_EVENT_TYPES[const.SALARY_HISTORY]
                )
        if const.GRADE_HISTORY in params:
            for event in params[const.GRADE_HISTORY]:
                date_from = arrow.get(event['dateFrom']).date()
                date_to = arrow.get(event['dateTo']).date()
                event = logic.encrypt_finance_event(event)
                models.FinanceEvents.objects.create(
                    person=params['person'],
                    date_to=date_to,
                    date_from=date_from,
                    event=event,
                    type=const.FINANCE_EVENT_TYPES[const.GRADE_HISTORY]
                )

        params = logic.stringify_values(params)
        logic.encrypt_finance_data(params, serialize=True)
        cur_finance = models.Finance.objects.filter(person=params['person'])
        if cur_finance.exists():
            cur_finance.update(**params)
            return cur_finance.first()
        else:
            return models.Finance.objects.create(**params)

    return builder


@pytest.fixture
def currency_builder(db):
    def builder(**kwargs):
        params = {
            'key': const.DEFAULT_CURRENCIES.RUB,
        }
        params.update(kwargs)
        models.Currency.objects.create(**params)
    return builder


@pytest.fixture
def profession_builder(db):
    def builder(**kwargs):
        key, (name_ru, name_en) = list(const.DEFAULT_PROFESSIONS.items())[0]
        params = {
            'key': key,
            'name_ru': name_ru,
            'name_en': name_en,
        }
        params.update(kwargs)
        models.Profession.objects.create(**params)
    return builder
