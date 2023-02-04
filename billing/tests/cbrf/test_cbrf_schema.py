from datetime import datetime

import pytest

from refs.cbrf.models import Bank


@pytest.mark.django_db
def test_basic(client, django_assert_num_queries):

    Bank(
        bic='044525229',
        name_full='aname',
        date_added=datetime(2019, 12, 25),
        restricted=True,
        accounts=[
            {'number': '123', 'type': 'ABC', 'restrictions': [{'code': 'EEE', 'date': '2021-01-02'}]},
        ],
        restrictions=[
            {'code': 'FFF', 'date': '2021-01-05'}
        ],
    ).save()

    Bank(
        bic='111111111',
        name_full='bname',
        date_added=datetime(2021, 4, 21),
        accounts=[{'number': '456', 'type': 'ABC'}],
        restrictions=[],
    ).save()

    def query(bic):
        return client.get(
            '/api/cbrf/?query=query{banks(bic:"%s", state:"any")'
            '{bic nameFull accounts{number type restrictions{code date}} restrictions{code date}}}' % bic
        ).json()

    with django_assert_num_queries(1) as _:

        assert query('044525229') == {'data': {'banks': [
            {'accounts': [{'number': '123', 'restrictions': [{'code': 'EEE', 'date': '2021-01-02'}], 'type': 'ABC'}],
             'bic': '044525229', 'nameFull': 'aname',
             'restrictions': [{'code': 'FFF', 'date': '2021-01-05'}]}]}}

    assert query('111111111') == {'data': {'banks': [
        {'accounts': [{'number': '456', 'restrictions': None, 'type': 'ABC'}], 'bic': '111111111',
         'nameFull': 'bname', 'restrictions': []}
    ]}}

    with django_assert_num_queries(2) as _:

        result = client.get('/api/cbrf/?query=query{listing {bic nameFull}}').json()

        assert result == {'data': {'listing': [
            {'bic': '044525229', 'nameFull': 'aname'},
            {'bic': '111111111', 'nameFull': 'bname'},
        ]}}
