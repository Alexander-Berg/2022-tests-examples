import inject
import mock

from infra.swatlib.auth import staff
from infra.qyp.vmproxy.src import security_policy


def test_security_policy(config):
    def configure(binder):
        binder.bind(staff.IStaffClient, mock.Mock())

    config.set_value('vmproxy.root_users', [])

    inject.clear_and_configure(configure)
    staff_client = inject.instance(staff.IStaffClient)

    def side_effect(spec, fields):
        author = spec['person.login']
        if author == 'johndoe':
            return {
                'result': [
                    {
                        'group': {
                            'id': '10',
                            'type': 'service'
                        }
                    },
                    {
                        'group': {
                            'id': '11',
                            'type': 'service'
                        }
                    }
                ]
            }
        elif author == 'janedoe':
            return {
                'result': [
                    {
                        'group': {
                            'id': '20',
                            'type': 'service'
                        }
                    },
                    {
                        'group': {
                            'id': '21',
                            'type': 'service'
                        }
                    }
                ]
            }
        return {
            'result': []
        }

    staff_client.list_groupmembership.side_effect = side_effect

    sp = security_policy.SecurityPolicy(
        is_enabled=True,
        root_users=['admin']
    )
    logins = ['volozh', 'imperator']
    groups = ['10', '11']

    for login in ('volozh', 'imperator', 'johndoe', 'admin'):
        assert sp.is_allowed(login, logins, groups)
    for login in ('tapot', 'janedoe', 'timmy!111'):
        assert not sp.is_allowed(login, logins, groups)

    sp = security_policy.SecurityPolicy.from_sepelib_config()
    assert not sp.is_enabled
    assert not sp._root_users
