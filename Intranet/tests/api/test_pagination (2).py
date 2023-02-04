import pytest
from urllib import parse

from watcher.db import Schedule


@pytest.mark.parametrize('order', ['id', '-id'])
def test_paginate_list_schedule(client, schedule_factory, scope_session, order):
    """
    Проверяем работу пагинации - проходим по ссылкам next до конца,
    а потом по prev в начало и сравниваем результаты с ожидаемыми
    """
    scope_session.query(Schedule).delete()
    scope_session.commit()

    expected = [schedule_factory().id for _ in range(10)]
    if order == '-id':
        expected.reverse()
    next = True
    iteration = 0
    query_params = {'page_size': 2, 'order_by': order}

    while next:
        response = client.get(
            '/api/watcher/v1/schedule/',
            params=query_params,
        )
        assert response.status_code == 200, response.text
        data = response.json()
        item_ids = [i['id'] for i in data['result']]
        next = data['next']
        prev = data['prev']
        if next:
            assert next[:8] == 'https://'
        if prev:
            assert prev[:8] == 'https://'

        assert len(item_ids) == 2
        assert item_ids == expected[iteration:iteration + 2]
        iteration += 2
        url_parsed = parse.urlparse(next)
        query_params = dict(parse.parse_qsl(url_parsed.query))

    assert prev
    assert not next

    iteration -= 2
    while prev:
        url_parsed = parse.urlparse(prev)
        query_params = dict(parse.parse_qsl(url_parsed.query))

        response = client.get(
            '/api/watcher/v1/schedule/',
            params=query_params,
        )
        assert response.status_code == 200, response.text
        data = response.json()
        item_ids = [i['id'] for i in data['result']]
        next = data['next']
        prev = data['prev']
        assert len(item_ids) == 2
        assert item_ids == expected[iteration - 2:iteration]
        if next:
            assert next[:8] == 'https://'
        if prev:
            assert prev[:8] == 'https://'

        iteration -= 2

    assert not prev
    assert next
