# coding: utf-8
from datetime import timedelta
from unittest import mock

import pytest
from django.conf import settings
from django.core.management import call_command
from django.utils import timezone
from freezegun import freeze_time
from waffle.testutils import override_switch

from core.management.commands.idm_create_issue_of_failed_role_review import (
    COMMENT_TEMPLATE,
    ROLE_LINK_TEMPLATE,
    INVISIBLE_ROLE_LINKS_TEMPLATE,
)
from idm.tests.utils import make_role, add_perms_by_role

pytestmark = [pytest.mark.django_db]


@pytest.fixture(autouse=True)
def use_robot_here(idm_robot):
    """Все тесты в этом модуле используют фикстуру с роботом"""


@pytest.fixture(autouse=True)
def calendar_get_holidays_mock():
    with mock.patch('idm.integration.calendar.get_holidays', return_value=[]) as _mock:
        yield _mock


@pytest.mark.parametrize('has_failed_roles', (True, False))
@pytest.mark.parametrize('has_issue', (True, False))
@mock.patch('idm.core.management.commands.idm_create_issue_of_failed_role_review.MAX_LINK_NUMBER', 1)
def test_idm_create_issue_of_failed_role_review(simple_system, arda_users, has_failed_roles, has_issue):
    legolas = arda_users.legolas
    frodo = arda_users.get('frodo')
    bilbo = arda_users.get('bilbo')
    frodo_manager = make_role(frodo, simple_system, {'role': 'manager'})
    bilbo_manager = make_role(bilbo, simple_system, {'role': 'manager'})

    add_perms_by_role('responsible', legolas, simple_system)
    if has_failed_roles:
        simple_system.metainfo.roles_failed_on_last_review = [frodo_manager.id, bilbo_manager.id]
        simple_system.metainfo.save(update_fields=['roles_failed_on_last_review'])

    with mock.patch('idm.core.management.commands.idm_create_issue_of_failed_role_review.registry') as mock_registry:
        mock_client = mock.Mock()
        mock_registry.get_repository.return_value = mock_client
        mock_issue = mock.Mock()
        mock_client.client.issues.find.return_value = [mock_issue] if has_issue else []
        mock_client.client.issues.create.return_value = mock_issue

        with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
            with override_switch('create_issue_of_failed_role_review', active=True):
                call_command('idm_create_issue_of_failed_role_review')
        if has_failed_roles:
            mock_client.client.issues.find.assert_called_once_with(
                query='Queue: IDMALERTS and Resolution: empty() and Tags: "simple"'
            )
            if has_issue:
                mock_client.client.issues.create.assert_not_called()
            else:
                mock_client.client.issues.create.assert_called_once()
            mock_issue.comments.create.assert_called_once_with(
                text=COMMENT_TEMPLATE.format(
                    links=ROLE_LINK_TEMPLATE.format(
                        i=1,
                        role_id=frodo_manager.pk,
                        system_slug=simple_system.slug
                    ) + INVISIBLE_ROLE_LINKS_TEMPLATE.format(
                        len_invisible_links=1,
                        invisible_links=ROLE_LINK_TEMPLATE.format(
                            i=2,
                            role_id=bilbo_manager.pk,
                            system_slug=simple_system.slug
                        )
                    ),
                    system_name=simple_system.get_name()
                ),
                summonees=[legolas.username],
            )
        else:
            mock_client.client.issues.find.assert_not_called()
            mock_client.client.issues.create.assert_not_called()
            mock_issue.comments.create.assert_not_called()
