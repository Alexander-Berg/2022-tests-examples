# coding: utf-8

from unittest.mock import patch, ANY
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    contains
)

from testutils import TestCase, assert_not_called

from intranet.yandex_directory.src.yandex_directory.core.mailer.sender import non_prod_envs


@patch('intranet.yandex_directory.src.yandex_directory.core.mailer.sender.non_prod_envs.ENVIRONMENTS_FOR_EXTENDED_HEADERS',
       {'prod'})
class TestMailerNonProdEnv(TestCase):
    @patch('intranet.yandex_directory.src.yandex_directory.core.mailer.sender.non_prod_envs.IN_PRODUCTION', False)
    @patch('intranet.yandex_directory.src.yandex_directory.core.mailer.sender.non_prod_envs.app.config',
           dict(
               EMAIL_FOR_TESTS='never-send-here',
           ))
    @patch('intranet.yandex_directory.src.yandex_directory.core.mailer.sender.non_prod_envs.DEPLOY_HEADERS',
           {
               'X-Deploy-Project': 'poisk',
               'X-Deploy-Stage': 'morda-prod',
               'X-Deploy-Unit': 'super',
               'X-Deploy-Box': 'box',
           })
    def test_overrides(self):
        headers_before = {1: 2}
        email_before = 'chapson@yandex-team.ru'

        headers_after, email_after = non_prod_envs.override(headers=headers_before, original_email=email_before)
        assert_that(
            headers_after,
            equal_to({
                1: 2,
                'X-Dir-Original-To': email_before,
                'X-Deploy-Project': 'poisk',
                'X-Deploy-Stage': 'morda-prod',
                'X-Deploy-Unit': 'super',
                'X-Deploy-Box': 'box',
            })
        )
        assert_that(
            email_after,
            equal_to('never-send-here')
        )

    @patch('intranet.yandex_directory.src.yandex_directory.core.mailer.sender.non_prod_envs.IN_PRODUCTION', True)
    def test_does_not_override_in_prod(self):
        headers_before = {1: 2}
        email_before = 'chapson@yandex-team.ru'
        headers_after, email_after = non_prod_envs.override(headers=headers_before, original_email=email_before)
        assert_that(
            headers_after,
            equal_to(headers_before)
        )
        assert_that(
            email_before,
            equal_to(email_after)
        )
