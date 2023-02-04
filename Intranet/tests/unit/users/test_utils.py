import pytest

from datetime import datetime, date, timedelta

from unittest.mock import patch

from intranet.femida.src.users.utils import get_recruiter_gaps


def _date_to_gap_format(dt):
    return datetime.combine(dt, datetime.min.time()).isoformat()


def create_gap(date_from, date_to=None, **kwargs):
    date_to = date_to or date_from + timedelta(days=1)

    kwargs.setdefault('person_login', 'username')
    kwargs.setdefault('full_day', True)
    kwargs.setdefault('work_in_absence', False)

    return dict(
        date_from=_date_to_gap_format(date_from),
        date_to=_date_to_gap_format(date_to),
        **kwargs
    )


@patch('intranet.femida.src.users.utils.get_gaps')
@pytest.mark.parametrize('data', [
    {  # 0. Игнорим отсутствия на неполный день
        'date_from': date(2018, 1, 1),
        'date_to': date(2018, 1, 4),
        'gaps': [
            create_gap(date(2018, 1, 2), full_day=False),
        ],
        'result': [],
    },
    {  # 1. Игнорим, если работает во время отсутствия
        'date_from': date(2018, 1, 1),
        'date_to': date(2018, 1, 4),
        'gaps': [
            create_gap(date(2018, 1, 2), work_in_absence=True),
        ],
        'result': [],
    },
    {  # 2. Дата отсутствия между датами поиска
        'date_from': date(2018, 1, 1),
        'date_to': date(2018, 1, 4),
        'gaps': [
            create_gap(date(2018, 1, 2)),
        ],
        'result': [
            ('username', date(2018, 1, 2)),
        ],
    },
    {  # 3. Дата отсутствия на границе даты начала поиска (включено)
        'date_from': date(2018, 1, 1),
        'date_to': date(2018, 1, 4),
        'gaps': [
            create_gap(date(2018, 1, 1)),
        ],
        'result': [
            ('username', date(2018, 1, 1)),
        ],
    },
    {  # 4. Дата отсутствия на границе даты начала поиска (не включено)
        'date_from': date(2018, 1, 2),
        'date_to': date(2018, 1, 4),
        'gaps': [
            create_gap(date(2018, 1, 1)),
        ],
        'result': [],
    },
    {  # 5. Дата отсутствия на границе даты окончания поиска (включено)
        'date_from': date(2018, 1, 1),
        'date_to': date(2018, 1, 4),
        'gaps': [
            create_gap(date(2018, 1, 4)),
        ],
        'result': [
            ('username', date(2018, 1, 4)),
        ],
    },
    {  # 6. Дата отсутствия на границе даты окончания поиска (не включено)
        'date_from': date(2018, 1, 1),
        'date_to': date(2018, 1, 4),
        'gaps': [
            create_gap(date(2018, 1, 5)),
        ],
        'result': [],
    },
    {  # 7. Даты отсутствия включены в даты поиска
        'date_from': date(2018, 1, 1),
        'date_to': date(2018, 1, 10),
        'gaps': [
            create_gap(date(2018, 1, 4), date(2018, 1, 7)),
        ],
        'result': [
            ('username', date(2018, 1, 4)),
            ('username', date(2018, 1, 5)),
            ('username', date(2018, 1, 6)),
        ],
    },
    {  # 8. Даты отсутствия пересекаются с датой начала поиска
        'date_from': date(2018, 1, 5),
        'date_to': date(2018, 1, 10),
        'gaps': [
            create_gap(date(2018, 1, 2), date(2018, 1, 7)),
        ],
        'result': [
            ('username', date(2018, 1, 5)),
            ('username', date(2018, 1, 6)),
        ],
    },
    {  # 9. Даты отсутствия пересекаются с датой окончания поиска
        'date_from': date(2018, 1, 5),
        'date_to': date(2018, 1, 10),
        'gaps': [
            create_gap(date(2018, 1, 8), date(2018, 1, 13)),
        ],
        'result': [
            ('username', date(2018, 1, 8)),
            ('username', date(2018, 1, 9)),
            ('username', date(2018, 1, 10)),
        ],
    },
    {  # 10. Даты отсутствия включают даты поиска
        'date_from': date(2018, 1, 5),
        'date_to': date(2018, 1, 8),
        'gaps': [
            create_gap(date(2018, 1, 3), date(2018, 1, 13)),
        ],
        'result': [
            ('username', date(2018, 1, 5)),
            ('username', date(2018, 1, 6)),
            ('username', date(2018, 1, 7)),
            ('username', date(2018, 1, 8)),
        ],
    },
    {  # 11. Несколько разных пользователей
        'date_from': date(2018, 1, 5),
        'date_to': date(2018, 1, 8),
        'gaps': [
            create_gap(person_login='user1', date_from=date(2018, 1, 4)),
            create_gap(person_login='user1', date_from=date(2018, 1, 8)),
            create_gap(person_login='user2', date_from=date(2018, 1, 3), date_to=date(2018, 1, 6)),
            create_gap(person_login='user3', date_from=date(2018, 1, 5), date_to=date(2018, 1, 7)),
            create_gap(person_login='user4', date_from=date(2018, 1, 7)),
            create_gap(person_login='user5', date_from=date(2018, 1, 7), date_to=date(2018, 1, 11)),
            create_gap(person_login='user6', date_from=date(2018, 1, 8), date_to=date(2018, 1, 15)),
            create_gap(person_login='user7', date_from=date(2018, 1, 9), date_to=date(2018, 1, 27)),
        ],
        'result': [
            ('user1', date(2018, 1, 8)),
            ('user2', date(2018, 1, 5)),
            ('user3', date(2018, 1, 5)),
            ('user3', date(2018, 1, 6)),
            ('user4', date(2018, 1, 7)),
            ('user5', date(2018, 1, 7)),
            ('user5', date(2018, 1, 8)),
            ('user6', date(2018, 1, 8)),
        ],
    },
])
def test_get_recruiter_gaps(mocked_get_gaps, data):
    mocked_get_gaps.return_value = data['gaps']
    result = get_recruiter_gaps([], data['date_from'], data['date_to'])
    assert data['result'] == list(result)
