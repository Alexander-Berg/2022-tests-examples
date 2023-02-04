# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.auth.utils import (
    hide_auth_from_headers,
    obfuscate_ticket,
)
from intranet.yandex_directory.src.yandex_directory.auth.scopes import (
    expand_scopes,
    scope,
)
from testutils import TestCase
from hamcrest import (
    assert_that,
    has_length,
    is_in,
    equal_to,
    same_instance,
    is_not,
)
from werkzeug.datastructures import Headers


class TestExpandScopes(TestCase):
    def test_expand_scopes_expands_star_into_all_scopes(self):
        # Если среди скоупов есть *, то он раскрывается во все скоупы которые
        # возможны.

        results = expand_scopes(['*'])

        # проверим, что скоупов стало больше
        assert_that(
            results,
            has_length(49)
        )

        # И проверим несколько из скоупов, чтобы убедиться
        # в том, что всё раскрывается верно.
        assert_that(
            scope.work_on_behalf_of_any_user,
            is_in(results)
        )


class TestHideTicketsFromHeaders(TestCase):
    def test_obfuscate_headers(self):
        # Проверяем, что у тикета скрываются последние символы после ':'.
        ticket = 'asdfasdf:qwerqwer:zxcvzxcv'
        obfuscated_ticket = obfuscate_ticket(ticket)
        assert_that(
            obfuscated_ticket,
            equal_to('asdfasdf:qwerqwer:********')
        )

        # Проверяем, что скрываются нужные тикеты.
        headers = Headers({
            'Ticket': 'token:hide',
            'Authorization': 'name token:hide',
            'X-Ya-Service-Ticket': 'token:hide',
            'X-Ya-User-Ticket': 'token:hide',
            'Yet-Another-Ticket': 'token:show',
        })
        obfuscated_headers = hide_auth_from_headers(headers)
        assert_that(
            obfuscated_headers,
            equal_to({
                'Ticket': 'token:****',
                'Authorization': 'name token:****',
                'X-Ya-Service-Ticket': 'token:****',
                'X-Ya-User-Ticket': 'token:****',
                'Yet-Another-Ticket': 'token:show',
            })
        )

        # Проверяем некорректные тикеты
        headers = Headers({
            'Ticket': 'tokenhide',
            'authorization': 'name tokenhide',
            'Yet-Another-Ticket': 'tokenshow',
        })
        obfuscated_headers = hide_auth_from_headers(headers)
        assert_that(
            obfuscated_headers,
            equal_to(
                {
                    'Ticket': '*********',
                    'authorization': 'name *********',
                    'Yet-Another-Ticket': 'tokenshow',
                }
            )
        )

    def test_copy_if_there_were_changes(self):
        # Мы должны возвращать копию, если что-то было изменено,
        # чтобы не вызвать странных спеэффектов
        headers = {'ticket': 'value'}
        result = hide_auth_from_headers(headers)

        assert_that(
            result,
            is_not(same_instance(headers)),
        )
