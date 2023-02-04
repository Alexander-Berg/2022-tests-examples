import pytest

from staff.lib.testing import StaffFactory
from staff.oebs.models import Job
import datetime
from django.core.urlresolvers import resolve
from mock import Mock
from json import loads

from staff.person.models import LANG

today = datetime.datetime.now().date()
JQUERY_CODE = 'jQuery1234567890123456789_1234567890123'
COUNT_PYTHON_DEV = 20

DUMP_JOBS = [
    {
        'code': 62,
        'start_date': datetime.date(2005, 1, 1),
        'end_date': datetime.date(2018, 6, 30),
        'name': 'ПРоектные контракты',
        'name_en': 'Project contracts'
    },
    {
        'code': 1062,
        'start_date': datetime.date(1950, 1, 1),
        'end_date': datetime.date(2015, 1, 31),
        'name': 'Сотрудник WF',
        'name_en': 'WF Employee'
    },
    {
        'code': 10065,
        'start_date': datetime.date(1950, 1, 1),
        'end_date': None,
        'name': 'Руководитель отдела медиапланирования',
        'name_en': 'Head of Media Planning Division'
    },
    {
        'code': 10066,
        'start_date': datetime.date(1950, 1, 1),
        'end_date': datetime.date(2014, 1, 31),
        'name': 'Руководитель отдела мобильных сервисов',
        'name_en': 'Head of mobile services Department'
    },
    {
        'code': 10067,
        'start_date': datetime.date(1950, 1, 1),
        'end_date': datetime.date(2014, 1, 31),
        'name': 'Руководитель отдела налогового планирования и контроля',
        'name_en': 'Head of tax planning and control Department'
    },
    {
        'code': 10068,
        'start_date': datetime.date(1950, 1, 1),
        'end_date': datetime.date(2014, 1, 31),
        'name': 'Руководитель отдела платежных систем',
        'name_en': 'Head of payment systems Department'
    }
]


@pytest.fixture
def rus_user():
    st = StaffFactory(login='ya_tester', lang_ui=LANG.RU)
    user = Mock()
    user.get_profile = lambda: st
    user.is_authenticated = lambda: True
    return user


@pytest.fixture
def jobs():
    for item in DUMP_JOBS:
        Job(**item).save()


@pytest.fixture
def python_developers():
    for _ in range(COUNT_PYTHON_DEV):
        Job(
            code=_ + 1,
            start_date=today,
            name='Питонист#{}'.format(_ + 1),
        ).save()


def collect_result(response, status=200):
    assert response.status_code == status
    result = response.content.decode('utf-8')
    assert result.startswith(JQUERY_CODE)
    result = loads(result.replace(JQUERY_CODE, '')[1:-1])
    return lambda x: [item[x] for item in result]


@pytest.mark.django_db
def test_that_check_search_by_name(rf, jobs, rus_user):
    url = '/center/api/autocomplete/multi/'

    # поищем по названию
    kwargs = {
        '_': '1234567890123',
        'callback': JQUERY_CODE,
        'types': 'job',
        'q': 'Руководитель отде'
    }

    request = rf.get(url, kwargs)
    request.user = rus_user
    request.LANGUAGE_CODE = 'ru'

    response = resolve(url).func(request)

    find_only = collect_result(response)
    assert find_only('_id') == [10068, 10065, 10066, 10067]
    assert find_only('_text') == [
        # важен порядок :по длине строки:
        'Руководитель отдела платежных систем',
        'Руководитель отдела медиапланирования',
        'Руководитель отдела мобильных сервисов',
        'Руководитель отдела налогового планирования и контроля',
    ]
    assert find_only('_type') == ['job', ] * 4
    assert find_only('code') == [10068, 10065, 10066, 10067]

    # поищем по коду
    kwargs['q'] = '106'

    request = rf.get(url, kwargs)
    request.user = rus_user
    request.LANGUAGE_CODE = 'ru'
    response = resolve(url).func(request)

    find_only = collect_result(response)
    assert find_only('_id') == [1062]
    assert find_only('_text') == ['Сотрудник WF']
    assert find_only('_type') == ['job', ]
    assert find_only('code') == [1062]

    # поищем по английскому названию
    kwargs['q'] = 'HEAD OF PLAN'

    request = rf.get(url, kwargs)
    request.user = rus_user
    request.LANGUAGE_CODE = 'ru'
    response = resolve(url).func(request)

    find_only = collect_result(response)
    assert find_only('_id') == [10065, 10067]
    assert find_only('_text') == [
        'Руководитель отдела медиапланирования',
        'Руководитель отдела налогового планирования и контроля'
    ]
    assert find_only('_type') == ['job', ] * 2
    assert find_only('code') == [10065, 10067]


@pytest.mark.django_db
def test_that_check_that_limit_works(rf, rus_user, python_developers):
    from staff.multic.views import MulticompleteView

    for limit in [1, 5, None, 25]:
        url = '/center/api/autocomplete/multi/'
        kwargs = {
            '_': '1234567890123',
            'callback': JQUERY_CODE,
            'q': 'Питонист',
            'types': 'job',
            'limit': limit,
        }
        if not limit:
            kwargs.pop('limit')
            limit = MulticompleteView.DEFAULT_LIMIT
        limit = min(limit, COUNT_PYTHON_DEV)

        request = rf.get(url, kwargs)
        request.user = rus_user
        request.LANGUAGE_CODE = 'ru'
        response = resolve(url).func(request)

        find_only = collect_result(response)
        assert find_only('code') == list(range(1, limit + 1))
