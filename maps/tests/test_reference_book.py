from http import HTTPStatus
import json
import pytest
import requests
from maps.b2bgeo.test_lib.mock_reference_book import mock_reference_book
from maps.b2bgeo.test_lib.reference_book_values import (
    INCOMPATIBLE_ZONES,
    COMPANY_ZONES,
    PUBLIC_ZONES,
    ZONE_NUMBERS_LIMIT,
)


def _get_auth_headers():
    value = (
        'Bearer eyJhbGciOiJFUzI1NiIsImtpZCI6InRlc3Qta2V5LWlkIiwidHlwIjoiSldUIn0.'
        'eyJhdWQiOlsidGVzdC1zZXJ2aWNlIl0sImV4cCI6MTY1NjA5MDQ4NSwiaXNzIjoiaWRlbnR'
        'pdHkiLCJuYmYiOjE2NTYwODk1ODUsInN1YmplY3QiOnsidHlwZSI6ImNvbXBhbnkiLCJ2YW'
        'x1ZSI6eyJhcGlrZXkiOiI5OTIwNDRhNi1kNDliLTRlNjktYTZhMS1kNWRjMjY2MDBiZjAiLCJpZCI6MX19fQ.'
        'Lx70eFOlbhmZp_HfmzSItbC3_Oo_stSX186zP0LJv8lOPF71mF68eBHE1mF-Qlt1pNUK4Bw7g8RgkvSLKih7Fg'
    )

    return {'Authorization': value}


@pytest.mark.parametrize(
    'company_id,result',
    [
        (1, INCOMPATIBLE_ZONES[1]),
        (2, INCOMPATIBLE_ZONES[2]),
        (3, []),
    ],
)
def test_incompatible_zones(company_id, result):
    with mock_reference_book() as url:
        resp = requests.get(
            url
            + f'/api/v1/internal/reference-book/companies/{company_id}/incompatible-zones',
            headers=_get_auth_headers(),
        )
        assert resp.status_code == HTTPStatus.OK
        respJson = json.loads(resp.content)
        assert respJson == result
        for zones in respJson:
            assert len(zones) == 2
            assert zones[0]
            assert zones[1]


@pytest.mark.parametrize(
    'headers,code',
    [
        (None, HTTPStatus.UNAUTHORIZED),
        ({'Authorization': 'OAuth test'}, HTTPStatus.UNAUTHORIZED),
        ({'Authorization': 'Bearer -incorrect-'}, HTTPStatus.UNAUTHORIZED),
        ({'Authorization': 'Bearer '}, HTTPStatus.UNAUTHORIZED),
        ({'Authorization': ''}, HTTPStatus.UNAUTHORIZED),
    ],
)
def test_incompatible_zones_errors(headers, code):
    with mock_reference_book() as url:
        resp = requests.get(
            url + '/api/v1/internal/reference-book/comapnies/1/incompatible-zones',
            headers=headers,
        )
        assert resp.status_code == code


@pytest.mark.parametrize(
    'company_id,result',
    [
        (1, [COMPANY_ZONES[1][0], COMPANY_ZONES[1][1]]),
        (1, [COMPANY_ZONES[1][0], COMPANY_ZONES[1][1], PUBLIC_ZONES[0]]),
        (1, [COMPANY_ZONES[1][0], COMPANY_ZONES[1][1], COMPANY_ZONES[2][0]]),
        (2, [COMPANY_ZONES[1][0], COMPANY_ZONES[1][1], COMPANY_ZONES[2][0]]),
        (
            2,
            [
                COMPANY_ZONES[1][0],
                COMPANY_ZONES[1][1],
                COMPANY_ZONES[2][0],
                PUBLIC_ZONES[1],
            ],
        ),
        (2, PUBLIC_ZONES),
        (3, [COMPANY_ZONES[1][0], COMPANY_ZONES[1][1], COMPANY_ZONES[2][0]]),
        (
            3,
            [
                COMPANY_ZONES[1][0],
                COMPANY_ZONES[1][1],
                COMPANY_ZONES[2][0],
                PUBLIC_ZONES[0],
            ],
        ),
        (
            1,
            [
                COMPANY_ZONES[1][2],
                COMPANY_ZONES[1][3],
                COMPANY_ZONES[2][3],
                PUBLIC_ZONES[0],
            ],
        ),
        (
            2,
            [
                COMPANY_ZONES[1][2],
                COMPANY_ZONES[1][3],
                COMPANY_ZONES[2][3],
                PUBLIC_ZONES[1],
            ],
        ),
    ],
)
def test_zone_by_numbers(company_id, result):
    with mock_reference_book() as url:
        zone_numbers = [x['number'] for x in result]
        result = list(
            filter(
                lambda x: 'company_id' not in x
                or x['company_id'] is None
                or x['company_id'] == company_id,
                result,
            )
        )
        params = {
            'zone_numbers': ','.join(zone_numbers),
        }
        resp = requests.get(
            url + f'/api/v1/internal/reference-book/companies/{company_id}/zones',
            params=params,
            headers=_get_auth_headers(),
        )
        assert resp.status_code == HTTPStatus.OK
        respJson = json.loads(resp.content)
        respJson.sort(key=lambda x: x['id'])
        result.sort(key=lambda x: x['id'])
        assert respJson == result
        for zone in respJson:
            assert zone['polygon']
            assert zone['number']
            assert zone['id']


@pytest.mark.parametrize(
    'params,headers,code',
    [
        ({}, _get_auth_headers(), HTTPStatus.UNPROCESSABLE_ENTITY),
        ({'zone_numbers': ''}, _get_auth_headers(), HTTPStatus.UNPROCESSABLE_ENTITY),
        (
            {
                'company_id': '1',
                'zone_numbers': ','.join(
                    [f'zone{x}' for x in range(ZONE_NUMBERS_LIMIT + 1)]
                ),
            },
            _get_auth_headers(),
            HTTPStatus.UNPROCESSABLE_ENTITY,
        ),
        ({'zone_numbers': 'zone_1'}, None, HTTPStatus.UNAUTHORIZED),
        (
            {'zone_numbers': 'zone_1'},
            {'Authorization': 'OAuth test'},
            HTTPStatus.UNAUTHORIZED,
        ),
        (
            {'zone_numbers': 'zone_1'},
            {'Authorization': 'Bearer -incorrect-'},
            HTTPStatus.UNAUTHORIZED,
        ),
        (
            {'zone_numbers': 'zone_1'},
            {'Authorization': 'Bearer '},
            HTTPStatus.UNAUTHORIZED,
        ),
        ({'zone_numbers': 'zone_1'}, {'Authorization': ''}, HTTPStatus.UNAUTHORIZED),
    ],
)
def test_zone_errors(params, headers, code):
    with mock_reference_book() as url:
        resp = requests.get(
            url + '/api/v1/internal/reference-book/companies/1/zones',
            params=params,
            headers=headers,
        )
        assert resp.status_code == code


@pytest.mark.parametrize(
    'method,path',
    [
        ('GET', '/not-exists'),
        ('GET', '/parent/not-exists'),
        ('POST', '/api/v1/internal/reference-book/companies/1/zones'),
        ('POST', '/api/v1/internal/reference-book/companies/1/incompatible-zones'),
    ],
)
def test_reference_book_not_found(method, path):
    with mock_reference_book() as url:
        resp = requests.request(method, url + path, headers=_get_auth_headers())
        assert resp.status_code == HTTPStatus.NOT_FOUND
