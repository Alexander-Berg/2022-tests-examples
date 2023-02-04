import pytest

from django.core.management import call_command

from intranet.crt.constants import CERT_TYPE, CERT_STATUS, ACTION_TYPE
from __tests__.utils.common import create_certificate

pytestmark = [pytest.mark.django_db]


def test_fill_revoker(users, crt_robot, certificate_types, django_assert_num_queries):
    type_rc_server = certificate_types[CERT_TYPE.RC_SERVER]
    revoked_cert_without_action = create_certificate(crt_robot, type_rc_server, status=CERT_STATUS.REVOKED)

    issued_cert_with_revoke_action = create_certificate(crt_robot, type_rc_server, status=CERT_STATUS.ISSUED)
    issued_cert_with_revoke_action.actions.create(type=ACTION_TYPE.CERT_REVOKE, user=crt_robot)

    revoked_cert_with_two_revoke_actions = create_certificate(crt_robot, type_rc_server, status=CERT_STATUS.REVOKED)
    revoked_cert_with_two_revoke_actions.actions.create(type=ACTION_TYPE.CERT_REVOKE, user=users['noc_user'])
    revoked_cert_with_two_revoke_actions.actions.create(type=ACTION_TYPE.CERT_REQUEST_APPROVED, user=users['normal_user'])
    revoked_cert_with_two_revoke_actions.actions.create(type=ACTION_TYPE.CERT_REVOKE, user=users['zomb-user'])

    call_command('crt_oneoff_fill_revoker')

    for x in (revoked_cert_without_action, issued_cert_with_revoke_action, revoked_cert_with_two_revoke_actions):
        x.refresh_from_db()

    assert revoked_cert_without_action.revoked_by_id is None
    assert issued_cert_with_revoke_action.revoked_by_id == crt_robot.id
    assert revoked_cert_with_two_revoke_actions.revoked_by_id == users['zomb-user'].id
