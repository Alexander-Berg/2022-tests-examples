# -*- coding: utf-8 -*-


import pytest
from django.template.loader import get_template, TemplateDoesNotExist

from idm.core.models import System, Approve, RoleRequest
from idm.tests.utils import create_user, raw_make_role, refresh
from idm.utils.cleansing import hide_secret_params_in_url, SECRET_DATA_PLACEHOLDER
from idm.utils.ldap import normalize_ldap_group

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def pt1_system_has_no_default_template_for_email():
    System.objects.get_or_create(name='JIRA', slug='jira')
    try:
        get_template('emails/role_approved_jira.txt')
        assert False, 'template found'
    except TemplateDoesNotExist:
        pass


def test_approve_repr(simple_system):
    user = create_user('vasya')
    petya = create_user('petya')
    masha = create_user('masha')
    volk = create_user('volk')
    role = raw_make_role(user, simple_system, {'role': 'admin'})
    role_request = RoleRequest.objects.create(role=role, requester=user)

    # эту роль должны подтвердить petya или masha или volk
    approve = Approve.objects.create(role_request=role_request)
    approve.requests.create(approver=petya, decision='approve')
    approve.requests.create(approver=masha, decision='decline')
    approve.requests.create(approver=volk)
    role_request.update_approves()
    approve = Approve.objects.select_related('role_request__requester').get()

    # так как один из аппруверов отклонил запрос на роль, то статус всего запроса должен быть declined
    assert repr(approve) == '<Approve: %d, requester=vasya, status=declined, petya:Y | masha:N | volk:W>' % approve.pk


def test_ad_group_normalizer():
    assert normalize_ldap_group(
        'CN=EBSAccessGroup,OU=OEBS Apps Access,OU=Interfaces ,OU=Projects,OU=Groups, DC=ld,DC=yandex,DC=ru'
    ) == 'CN=EBSAccessGroup,OU=OEBS Apps Access,OU=Interfaces,OU=Projects,OU=Groups,DC=ld,DC=yandex,DC=ru'


def test_hide_secret_params_in_url():
    urls = [
        (
            'https://example.com/?token=some_token',
            'https://example.com/?token={}'.format(SECRET_DATA_PLACEHOLDER),
        ),
        (
            'https://example.com/?token=some_token&token=other_token',
            'https://example.com/?token={0}&token={0}'.format(SECRET_DATA_PLACEHOLDER),
        ),
        (
            'https://example.com/?token=some_token&org_id=123',
            'https://example.com/?token={}&org_id=123'.format(SECRET_DATA_PLACEHOLDER),
        ),
        (
            'https://example.com/?token=some_token&&user=token',
            'https://example.com/?token={0}&&user=token'.format(SECRET_DATA_PLACEHOLDER),
        ),
    ]
    for url, expected in urls:
        assert hide_secret_params_in_url(url) == expected
