import contextlib
from http import HTTPStatus
import json
import jwt
import re
from werkzeug.wrappers import Response, Request
from maps.b2bgeo.test_lib.http_server import mock_http_server
from maps.b2bgeo.test_lib.reference_book_values import (
    ZONE_NUMBERS_LIMIT,
    INCOMPATIBLE_ZONES,
    COMPANY_ZONES,
    PUBLIC_ZONES,
)


def _is_authorized(request):
    authorization = request.headers.get('Authorization')
    if (
        not authorization
        or not authorization.startswith('Bearer ')
        or not authorization[len('Bearer ') :]
    ):
        return False
    token = authorization[len('Bearer ') :]
    try:
        jwt.decode(token, options={"verify_signature": False})
    except:
        return False
    return True


@contextlib.contextmanager
def mock_reference_book():
    def _handler(environ, start_response):
        request = Request(environ)
        if not _is_authorized(request):
            return Response('Request is not authorized', status=401)(
                environ, start_response
            )

        if m := re.match(r'/api/v1/internal/reference-book/companies/([0-9]+)/incompatible-zones', request.path):
            if request.method == 'GET':
                company_id = int(m.group(1))
                if company_id not in INCOMPATIBLE_ZONES:
                    return Response(json.dumps([]))(environ, start_response)

                resp_json = INCOMPATIBLE_ZONES[company_id]
                return Response(json.dumps(resp_json))(environ, start_response)

        if m := re.match(r'/api/v1/internal/reference-book/companies/([0-9]+)/zones', request.path):
            if request.method == 'GET':
                company_id = int(m.group(1))
                zone_numbers = (
                    request.args.get('zone_numbers', '').split(',')
                    if request.args.get('zone_numbers', '')
                    else []
                )
                if not zone_numbers:
                    return Response(
                        'zone_numbers cgi patameter not found',
                        status=HTTPStatus.UNPROCESSABLE_ENTITY,
                    )(environ, start_response)

                if len(zone_numbers) > ZONE_NUMBERS_LIMIT:
                    return Response(
                        f'Too many zone_numbers in request. Maximum: {ZONE_NUMBERS_LIMIT}.',
                        status=HTTPStatus.UNPROCESSABLE_ENTITY,
                    )(environ, start_response)

                zone_numbers = set(zone_numbers)
                zones = []
                if company_id in COMPANY_ZONES:
                    for company_zone in COMPANY_ZONES[company_id]:
                        if company_zone['number'] in zone_numbers:
                            zones.append(company_zone)

                for public_zone in PUBLIC_ZONES:
                    if public_zone['number'] in zone_numbers:
                        zones.append(public_zone)

                return Response(json.dumps(zones))(environ, start_response)

        return Response(f'unknown path {request.path}', status=HTTPStatus.NOT_FOUND)(
            environ, start_response
        )

    with mock_http_server(_handler) as url:
        yield url
