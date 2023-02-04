from datetime import timedelta

import pytest
from django.utils import timezone

from tests import helpers


UMBRELLA_SUGGEST_URL = '/frontend/umbrellas-suggest/'
MAIN_PRODUCT_SUGGEST_URL = '/frontend/main-product-suggest/'
GET_UMBRELLAS_BY_IDS_URL = '/frontend/umbrellas-by-ids/'
GET_MAIN_PRODUCT_BY_IDS_URL = '/frontend/main-product-by-ids/'
GET_PERSON_UMBRELLAS = '/frontend/persons/{login}/umbrellas/'


@pytest.mark.parametrize(
    'umbrella_name,umbrella_issue_key,main_product_name,search_query,found',
    [
        ('develop', 'GOALZ-123', 'Alice', 'dev', True),
        ('develop', 'GOALZ-123', 'Alice', 'vel', True),
        ('develop', 'GOALZ-123', 'Alice', 'lop', True),
        ('develop', 'GOALZ-123', 'Alice', 'develop', True),
        ('develop', 'GOALZ-123', 'Alice', 'Alice', True),
        ('develop', 'GOALZ-123', 'Alice', 'Ali', True),
        ('develop', 'GOALZ-123', 'Alice', 'lice', True),
        ('develop', 'GOALZ-123', 'Alice', 'goal', True),
        ('develop', 'GOALZ-123', 'Alice', 'goalz-123', True),
        ('develop', 'GOALZ-123', 'Alice', '123', True),
        ('develop', 'GOALZ-123', 'Alice', 'what', False),
        ('develop', 'GOALZ-123', 'Alice', 'Aloce', False),
        ('develop', 'GOALZ-123', 'Alice', 'vevelop', False),
        ('develop', 'GOALZ-123', 'Alice', '456', False),
        ('develop', 'GOALZ-123', 'Alice', 'goals', False),
        ('develop', 'GOALZ-123', None, 'develop', True),
        ('develop', 'GOALZ-123', None, 'None', False),
    ]
)
def test_umbrellas_suggest(
    umbrella_name,
    umbrella_issue_key,
    main_product_name,
    search_query,
    found,
    client,
    umbrella_builder,
    main_product_builder,
):
    if main_product_name is None:
        mp = None
    else:
        mp = main_product_builder(name=main_product_name)
    umbrella = umbrella_builder(main_product=mp, name=umbrella_name, issue_key=umbrella_issue_key)

    response = helpers.get_json(
        client,
        path=UMBRELLA_SUGGEST_URL,
        request={'text': search_query},
    )

    if found:
        assert len(response['umbrellas']) == 1
        assert response['umbrellas'][0]['id'] == umbrella.id
    else:
        assert len(response['umbrellas']) == 0


@pytest.mark.parametrize(
    'main_product_name,search_query,found',
    [
        ('Alice', 'Alice', True),
        ('Alice', 'Ali', True),
        ('Alice', 'lice', True),
        ('Alice', 'what', False),
        ('Alice', 'Aloce', False),
    ]
)
def test_main_product_suggest(
    main_product_name,
    search_query,
    found,
    client,
    main_product_builder,
):
    mp = main_product_builder(name=main_product_name)

    response = helpers.get_json(
        client,
        path=MAIN_PRODUCT_SUGGEST_URL,
        request={'text': search_query},
    )

    if found:
        assert len(response['main_products']) == 1
        assert response['main_products'][0]['name'] == mp.name
    else:
        assert len(response['main_products']) == 0


def test_get_umbrellas_by_ids(client, umbrella_builder, main_product_builder,):
    mp = main_product_builder(name='Alise')
    umbrella_ids = [umbrella_builder(main_product=mp, name='develop').id for _ in range(3)]

    response = helpers.get_json(
        client,
        GET_UMBRELLAS_BY_IDS_URL,
        request={
            'ids': umbrella_ids[:-1]
        }
    )

    assert len(response['umbrellas']) == len(umbrella_ids) - 1


@pytest.mark.parametrize(
    'engaged_from,engaged_to,get_params',
    [
        (None, None, {}),
        (timezone.now() - timedelta(days=1), None, {}),
        (None, timezone.now() + timedelta(days=1), {}),
        (timezone.now() - timedelta(days=1), timezone.now() + timedelta(days=1), {}),
        (None, None, {'engaged_datetime': '2021-01-01'}),
        ('2020-12-01', None, {'engaged_datetime': '2021-01-01'}),
        (None, '2021-01-31', {'engaged_datetime': '2021-01-01'}),
        ('2020-12-15', '2021-01-15', {'engaged_datetime': '2021-01-01'}),
    ],
)
def test_get_person_umbrellas_return_active(
    engaged_from,
    engaged_to,
    get_params,
    client,
    person_builder,
    umbrella_person_builder,
):
    person = person_builder()
    person_umbrella = umbrella_person_builder(person=person, engaged_from=engaged_from, engaged_to=engaged_to)

    response = helpers.get_json(
        client,
        GET_PERSON_UMBRELLAS.format(login=person.login),
        request=get_params,
    )

    assert len(response['person_umbrellas']) == 1
    assert response['person_umbrellas'][0]['umbrella']['id'] == person_umbrella.umbrella.id


def test_get_main_product_by_ids(client, main_product_builder):
    main_product_ids = [main_product_builder(name='Alice').id for _ in range(3)]

    response = helpers.get_json(
        client,
        GET_MAIN_PRODUCT_BY_IDS_URL,
        request={
            'ids': main_product_ids[:-1]
        }
    )

    assert len(response['main_products']) == len(main_product_ids) - 1


@pytest.mark.parametrize(
    'engaged_from,engaged_to,get_params',
    [
        (timezone.now() + timedelta(days=1), None, {}),
        (None, timezone.now() - timedelta(days=1), {}),
        (timezone.now() + timedelta(days=1), timezone.now() + timedelta(days=2), {}),
        (timezone.now() - timedelta(days=2), timezone.now() - timedelta(days=1), {}),
        ('2021-01-02', None, {'engaged_datetime': '2021-01-01'}),
        (None, '2020-12-31', {'engaged_datetime': '2021-01-01'}),
        ('2021-01-02', '2021-01-03', {'engaged_datetime': '2021-01-01'}),
        ('2020-12-02', '2020-12-31', {'engaged_datetime': '2021-01-01'}),
    ],
)
def test_get_person_umbrella_not_return_inactive(
    engaged_from,
    engaged_to,
    get_params,
    client,
    person_builder,
    umbrella_person_builder
):
    person = person_builder()
    umbrella_person_builder(person=person, engaged_from=engaged_from, engaged_to=engaged_to)

    response = helpers.get_json(
        client,
        GET_PERSON_UMBRELLAS.format(login=person.login),
        request=get_params,
    )
    assert len(response['person_umbrellas']) == 0
