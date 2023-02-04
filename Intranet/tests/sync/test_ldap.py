# coding: utf-8
"""
Тесты взаимодействия с AD
"""


import ldap
import pytest
from django.conf import settings
from idm.sync.ldap.connector import NewLDAP
from mock import call, Mock, MagicMock

pytestmark = [pytest.mark.django_db]


def test_newldap_connector():
    """
    test for connector as a context_manager
    """
    with NewLDAP() as fake_newldap:
        mocked_ldap = fake_newldap.ldap
        mocked_ldap.initialize.assert_called_with('ldap://fake.ldap.net')

        # for ldaps we must call TLS options
        calls = [
            call(ldap.OPT_X_TLS_CACERTFILE, settings.CA_BUNDLE),
            call(ldap.OPT_X_TLS_DEMAND, True),
            call(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_NEVER),
            call(ldap.OPT_X_TLS_NEWCTX, 0),
        ]

        mocked_ldap.set_option.assert_has_calls(calls)

        assert fake_newldap.ldap.start_tls_s.called
        fake_newldap.ldap.simple_bind_s.assert_called_with('fake_user', 'fake_password')
        assert fake_newldap.ldap
        mocked_ldap_from_inst = fake_newldap.ldap

    assert fake_newldap.ldap is None
    assert mocked_ldap_from_inst.unbind_s.called  # unbind was called


def test_search_user(fake_newldap):
    fake_newldap.search_s = MagicMock()
    fake_newldap.search_s.return_value = None

    client = NewLDAP()
    client.connect()
    user_account = client.search_user('frodo')

    calls = [call(cn, ldap.SCOPE_SUBTREE, '(SAMAccountName=frodo)') for cn in settings.AD_ACTIVE_USERS_OU]
    calls += [call(settings.AD_LDAP_OLD_USERS_OU, ldap.SCOPE_SUBTREE, '(SAMAccountName=frodo)')]
    assert fake_newldap.search_s.call_args_list == calls

    assert user_account is None

    fake_newldap.search_s.return_value = [(
        'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
            'userAccountControl': ['66080']
        }
    )]
    account = client.search_user('frodo')
    assert account.is_active()
    assert account.display_name == 'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst'


@pytest.mark.parametrize('acc_control', ['66080', '546'])
def test_is_user_active(fake_newldap, acc_control):
    """
    bin(66080) == 0b10000001000100000
    bin(546)   == 0b00000001000100010
    Важен вот этот бит             ^
    Означает уволенность, если выставлен в 1
    """
    fake_newldap.search_s = MagicMock()

    with NewLDAP() as client:
        fake_newldap.search_s.return_value = [(
            'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
                'cn': ['Frodo Baggins'],
                'memberOf': [
                    'CN=Office.8021X.Developers_2,OU=Office.WiFi,OU=Projects,OU=Groups,DC=Ruler,DC=tst',
                    'CN=ZOO4_Virtuals,OU=ServerAccess,OU=Projects,OU=Groups,DC=Ruler,DC=tst',
                ],
                'userAccountControl': [acc_control],
            }
        )]
        if acc_control == '66080':
            expected = True
        else:
            expected = False
        assert client.is_user_active('frodo') is expected


def test_add_user_to_groups(arda_users, fake_newldap):
    frodo = arda_users.frodo

    with NewLDAP() as client:
        fake_newldap.search_s.return_value = [(
            'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
                'cn': ['Frodo Baggins'],
                'memberOf': [
                    'CN=Office.8021X.Developers_2,OU=Office.WiFi,OU=Projects,OU=Groups,DC=Ruler,DC=tst',
                    'CN=ZOO4_Virtuals,OU=ServerAccess,OU=Projects,OU=Groups,DC=Ruler,DC=tst',
                ],
                'userAccountControl': ['66080'],
            }
        )]

        # add fails
        fake_newldap.modify_s.side_effect = AssertionError('EXCEPTION')
        with pytest.raises(AssertionError):
            client.add_user_to_ad_group(frodo, 'fake_group')
        assert frodo.actions.count() == 0

        # user already have this group
        fake_newldap.modify_s.side_effect = None
        existing_group = 'CN=Office.8021X.Developers_2,OU=Office.WiFi,OU=Projects,OU=Groups,DC=Ruler,DC=tst'
        assert client.add_user_to_ad_group(frodo, existing_group) is True
        assert frodo.actions.count() == 0

        # adding new group
        assert client.add_user_to_ad_group(frodo, 'new_group', ad_reason_data={'reason': 'some reason'}) is True
        assert frodo.actions.count() == 1
        action = frodo.actions.get()
        assert action.action == 'user_ad_add_to_group'
        assert action.data == {
            'group': 'new_group',
            'reason': 'some reason',
        }


def test_remove_user_from_group(arda_users, fake_newldap):
    frodo = arda_users.frodo

    with NewLDAP() as client:
        # user do not have this group
        fake_newldap.search_s.return_value = [(
            'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
                'cn': ['Frodo Baggins'],
                'memberOf': [
                    'CN=Office.8021X.Developers_2,OU=Office.WiFi,OU=Projects,OU=Groups,DC=Ruler,DC=tst',
                    'CN=ZOO4_Virtuals,OU=ServerAccess,OU=Projects,OU=Groups,DC=Ruler,DC=tst',
                ],
                'userAccountControl': ['66080'],
            }
        )]

        # non-existing group
        assert client.remove_user_from_ad_group(frodo, 'blah-blah') is True
        assert frodo.actions.count() == 0

        # removing group
        existing_group = 'CN=ZOO4_Virtuals,OU=ServerAccess,OU=Projects,OU=Groups,DC=Ruler,DC=tst'
        assert client.remove_user_from_ad_group(frodo, existing_group, ad_reason_data={'reason': 'some reason'}) is True
        assert frodo.actions.count() == 1
        action = frodo.actions.get()
        assert action.action == 'user_ad_remove_from_group'
        assert action.data == {
            'group': existing_group,
            'reason': 'some reason',
        }
