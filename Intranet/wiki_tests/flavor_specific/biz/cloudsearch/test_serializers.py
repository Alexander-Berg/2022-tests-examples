import pytest
from wiki.api_frontend.serializers.query_view import FilterSerializer, QueryViewSerializer


def assert_valid_filters(filters):
    s = QueryViewSerializer(
        data={'query': 'test', 'page_id': 1, 'page_size': 5, 'order_by': 'relevancy', 'filters': filters}
    )

    assert s.is_valid(), f'Validation errors: {s.errors}'


@pytest.mark.django_db
def test_filter_serializer():
    assert FilterSerializer(data={}).is_valid()
    assert FilterSerializer(data={'type': {'equal': 'file'}}).is_valid()
    assert not FilterSerializer(data={'type': {'equal': 'File'}}).is_valid()

    assert QueryViewSerializer(
        data={'query': '', 'page_id': 1, 'page_size': 5, 'order_by': 'relevancy', 'fuzzy': True}
    ).is_valid()  # пустой запрос это ок

    # ------ валидация фильтров ------
    assert_valid_filters({'show_obsolete': False})
    assert_valid_filters({'show_obsolete': False, 'slug': {'prefix': '/ololo/'}})
    assert_valid_filters({'authors.cloud_uid': {'contains': ['bfbnhb6f3bdgrj7n1hhs']}})

    data = {'query': 'lol', 'page_id': 1, 'page_size': 5, 'order_by': 'relevancy', 'fuzzy': True}

    assert not QueryViewSerializer(data={}).is_valid()

    assert QueryViewSerializer(data=data).is_valid()

    data['filters'] = {'authors.uid': {'contains': [1, 2, 3]}}
    assert QueryViewSerializer(data=data).is_valid()

    data = {
        'query': 'masstermax',
        'page_id': 42,
        'page_size': 1337,
        'filters': {'created_at': {'from': 1, 'to': 12}, 'authors_uid': {'contains': [27]}},
    }
    s = QueryViewSerializer(data=data)
    assert s.is_valid()

    data = {
        'query': 'masstermax',
        'page_id': 42,
        'page_size': 1337,
        'filters': {'modified_at': {'from': 'gl', 'to': 'hf'}},
    }
    assert not QueryViewSerializer(data=data).is_valid()

    data = {
        'query': None,
        'page_id': 42,
        'page_size': 1337,
    }
    assert not QueryViewSerializer(data=data).is_valid()

    data = {
        'query': 'marvel',
        'page_id': 'none',
        'page_size': 1,
    }
    assert not QueryViewSerializer(data=data).is_valid()
