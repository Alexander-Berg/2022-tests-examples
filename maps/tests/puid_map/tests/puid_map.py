import pytest
from cyson import UInt
from nile.api.v1 import Record
from nile.drivers.common.progress import CommandFailedError

from maps.wikimap.stat.libs import nile_ut
from maps.wikimap.stat.tasks_payment.dictionaries.puid_map.lib import puid_tree
from maps.wikimap.stat.tasks_payment.dictionaries.puid_map.lib.puid_map import (
    prepare_puid_map_and_info,
    cast_puid,
    _normalize_users_dump,
    _add_outsource_company,
    _add_partner_acl_role,
    _add_staff_info,
)

PIECEWORK_DEPARTMENT = next(iter(puid_tree.PIECEWORK_DEPARTMENTS)).encode()
OUTSOURCE_MANAGER_DEPARTMENT = next(iter(puid_tree.OUTSOURCE_MANAGER_DEPARTMENTS)).encode()


def test_normalize_users_dump():
    result = nile_ut.yt_run(
        _normalize_users_dump,
        users_dump=nile_ut.Table([
            Record(puid=b'101', um_login=b'petr-ivanov', moderation_status=b'common'),
            Record(puid=b'102', um_login=b'ivan.Petrov', moderation_status=b'cartographer'),
        ]),
    )

    assert result == [
        Record(
            puid=b'101',
            role=b'common',
            yalogin=b'petr-ivanov'
        ),
        Record(
            puid=b'102',
            role=b'cartographer',
            yalogin=b'ivan-petrov'
        ),
    ]


def test_add_outsource_company():
    result = nile_ut.yt_run(
        _add_outsource_company,
        log=nile_ut.Table([
            Record(puid=b'101', yalogin=b'petr-ivanov', role=b'common'),
            Record(puid=b'102', yalogin=b'ivan-petrov', role=b'cartographer'),
        ]),
        outsourcers_dump=nile_ut.Table([
            Record(puid=b'102', company=b'outsource-group.geo'),
        ]),
    )

    assert result == [
        Record(
            puid=b'101',
            role=b'common',
            yalogin=b'petr-ivanov'
        ),
        Record(
            outsource_company=b'geo',
            puid=b'102',
            role=b'cartographer',
            yalogin=b'ivan-petrov'
        ),
    ]


def test_add_partner_acl_role():
    result = nile_ut.yt_run(
        _add_partner_acl_role,
        log=nile_ut.Table([
            Record(puid=b'101', yalogin=b'petr-ivanov', role=b'common'),
            Record(puid=b'102', yalogin=b'ivan-petrov', role=b'cartographer'),
        ]),
        acl_roles_dump=nile_ut.Table([
            Record(puid=b'101', role=b'lavka-pedestrian-onfoot'),
            Record(puid=b'102', role=b'other-acl-role'),
        ]),
    )

    assert result == [
        Record(
            partner_acl_role=b'lavka-pedestrian-onfoot',
            puid=b'101',
            role=b'common',
            yalogin=b'petr-ivanov'
        ),
        Record(
            puid=b'102',
            role=b'cartographer',
            yalogin=b'ivan-petrov'
        ),
    ]


def test_add_staff_info():
    result = nile_ut.yt_run(
        _add_staff_info,
        log=nile_ut.Table([
            Record(puid=b'101', yalogin=b'petr-ivanov', role=b'common'),
            Record(puid=b'102', yalogin=b'ivan-petrov', role=b'cartographer'),
        ]),
        staff_dump=nile_ut.Table([
            Record(
                last_name=b'Petrov',
                first_name=b'Ivan',
                login=b'petrov',
                nmaps_logins=[b'ivan-petrov'],
                primary_department=b'Dep 1',
                departments_urls=[b'dep1', b'dep1_2'],
            ),
        ]),
    )

    assert result == [
        Record(
            puid=b'101',
            role=b'common',
            yalogin=b'petr-ivanov'
        ),
        Record(
            departments_urls=[b'dep1', b'dep1_2'],
            first_name=b'Ivan',
            last_name=b'Petrov',
            login=b'petrov',
            primary_department=b'Dep 1',
            puid=b'102',
            role=b'cartographer',
            yalogin=b'ivan-petrov'
        ),
    ]


def test_outsourcer():
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([
            Record(puid=b'101', um_login=b'petr-ivanov', moderation_status=b'cartographer'),
            Record(puid=b'102', um_login=b'ivan.Petrov', moderation_status=b'cartographer'),
        ]),
        outsourcers_dump=nile_ut.Table([
            Record(puid=b'101', company=b'outsource-group.geo'),
            Record(puid=b'102', company=b'outsource-group.geo'),
        ]),
        acl_roles_dump=nile_ut.Table([]),
        staff_dump=nile_ut.Table([
            Record(
                last_name=b'Petrov',
                first_name=b'Ivan',
                login=b'petrov',
                nmaps_logins=[b'ivan-petrov'],
                primary_department=b'Dep 1',
                departments_urls=[b'dep1', OUTSOURCE_MANAGER_DEPARTMENT],
            ),
        ]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\toutsource\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\toutsource\tgeo\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\toutsource\tgeo\tpetr-ivanov (101)\t'),

        Record(puid=UInt(102), puid_tree=b'\tall\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\toutsource\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\toutsource\tgeo\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\toutsource\tgeo\t[manager] ivan-petrov (102)\t'),
    ]
    assert puid_info == [
        Record(puid=101, payment='piecework', involvement='outsource', group='geo', person='petr-ivanov'),
        Record(puid=102, payment='piecework', involvement='outsource', group='geo', person='[manager] ivan-petrov'),
    ]


@pytest.mark.parametrize('outsource_group', [b'outsource-group', b''])
def test_outsourcer_with_unknown_outsource_group(outsource_group):
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([
            Record(puid=b'101', um_login=b'petr-ivanov', moderation_status=b'cartographer'),
            Record(puid=b'102', um_login=b'ivan.Petrov', moderation_status=b'cartographer'),
        ]),
        outsourcers_dump=nile_ut.Table([
            Record(puid=b'101', company=outsource_group),
            Record(puid=b'102', company=outsource_group),
        ]),
        acl_roles_dump=nile_ut.Table([]),
        staff_dump=nile_ut.Table([
            Record(
                last_name=b'Petrov',
                first_name=b'Ivan',
                login=b'petrov',
                nmaps_logins=[b'ivan-petrov'],
                primary_department=b'Dep 1',
                departments_urls=[b'dep1', OUTSOURCE_MANAGER_DEPARTMENT],
            ),
        ]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\toutsource\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\toutsource\tunknown\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\toutsource\tunknown\tpetr-ivanov (101)\t'),

        Record(puid=UInt(102), puid_tree=b'\tall\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\toutsource\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\toutsource\tunknown\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\toutsource\tunknown\t[manager] ivan-petrov (102)\t'),
    ]
    assert puid_info == [
        Record(puid=101, payment='piecework', involvement='outsource', group='unknown', person='petr-ivanov'),
        Record(puid=102, payment='piecework', involvement='outsource', group='unknown', person='[manager] ivan-petrov'),
    ]


def test_outsource_manager_without_outsource_company():
    with pytest.raises(CommandFailedError, match=r'ivan-petrov \(101\) from outsource department "Dep 1" has no outsource company'):
        nile_ut.yt_run(
            prepare_puid_map_and_info,
            nile_ut.Job(),
            users_dump=nile_ut.Table([
                Record(puid=b'101', um_login=b'ivan.Petrov', moderation_status=b'cartographer'),
            ]),
            outsourcers_dump=nile_ut.Table([]),
            acl_roles_dump=nile_ut.Table([]),
            staff_dump=nile_ut.Table([
                Record(
                    last_name=b'Petrov',
                    first_name=b'Ivan',
                    login=b'petrov',
                    nmaps_logins=[b'ivan-petrov'],
                    primary_department=b'Dep 1',
                    departments_urls=[b'dep1', OUTSOURCE_MANAGER_DEPARTMENT],
                ),
            ]),
        )


def test_robot():
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([
            Record(puid=b'101', um_login=b'robot-fedor', moderation_status=b'robot'),
            Record(puid=b'102', um_login=b'robot-alice', moderation_status=b'robot')
        ]),
        outsourcers_dump=nile_ut.Table([]),
        acl_roles_dump=nile_ut.Table([]),
        staff_dump=nile_ut.Table([
            Record(
                last_name=b'Alice',
                first_name=b'Alice',
                login=b'robot-alice-staff',
                nmaps_logins=[b'robot-alice'],
                primary_department=b'Dep 1',
                departments_urls=[b'dep1', b'dep2'],
            ),
        ]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\trobot\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\trobot\trobot-fedor (101)\t'),

        Record(puid=UInt(102), puid_tree=b'\tall\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\trobot\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\trobot\trobot-alice (102)\t'),
    ]
    assert puid_info == [
        Record(puid=101, payment='free', involvement='robot', group='robot', person='robot-fedor'),
        Record(puid=102, payment='free', involvement='robot', group='robot', person='robot-alice'),
    ]


@pytest.mark.parametrize('role', puid_tree.ROLES['all'])
def test_staff_employees(role):
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([
            Record(puid=b'101', um_login=b'yndx-ivan', moderation_status=role.encode()),
            Record(puid=b'102', um_login=b'yndx-petr', moderation_status=role.encode()),
            Record(puid=b'103', um_login=b'yndx-petr-world', moderation_status=role.encode()),
            Record(puid=b'104', um_login=b'yndx.Igor', moderation_status=role.encode()),
        ]),
        outsourcers_dump=nile_ut.Table([]),
        acl_roles_dump=nile_ut.Table([]),
        staff_dump=nile_ut.Table([
            Record(
                last_name=b'Petrov',
                first_name=b'Ivan',
                login=b'petrov',
                nmaps_logins=[b'yndx-ivan'],
                primary_department=b'Piecework Department 1',
                departments_urls=[PIECEWORK_DEPARTMENT, b'dep1'],
            ),
            Record(
                last_name=b'Ivanov',
                first_name=b'Petr',
                login=b'ivanov',
                nmaps_logins=[b'yndx-petr', b'yndx-petr-world'],
                primary_department=b'Dep 2',
                departments_urls=[b'dep2', PIECEWORK_DEPARTMENT],
            ),
            Record(
                last_name=b'Igorev',
                first_name=b'Igor',
                login=b'igorev',
                nmaps_logins=[b'yndx-igor'],
                primary_department=b'Dep 3',
                departments_urls=[b'dep3', b'dep1'],
            ),
        ]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tstaff\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tstaff\tPiecework Department 1\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tstaff\tPiecework Department 1\tPetrov Ivan (petrov)\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tstaff\tPiecework Department 1\tPetrov Ivan (petrov)\tyndx-ivan (101)\t'),

        Record(puid=UInt(102), puid_tree=b'\tall\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\tstaff\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\tstaff\tDep 2\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\tstaff\tDep 2\tIvanov Petr (ivanov)\t'),
        Record(puid=UInt(102), puid_tree=b'\tall\tpiecework\tstaff\tDep 2\tIvanov Petr (ivanov)\tyndx-petr (102)\t'),

        Record(puid=UInt(103), puid_tree=b'\tall\t'),
        Record(puid=UInt(103), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(103), puid_tree=b'\tall\tpiecework\tstaff\t'),
        Record(puid=UInt(103), puid_tree=b'\tall\tpiecework\tstaff\tDep 2\t'),
        Record(puid=UInt(103), puid_tree=b'\tall\tpiecework\tstaff\tDep 2\tIvanov Petr (ivanov)\t'),
        Record(puid=UInt(103), puid_tree=b'\tall\tpiecework\tstaff\tDep 2\tIvanov Petr (ivanov)\tyndx-petr-world (103)\t'),

        Record(puid=UInt(104), puid_tree=b'\tall\t'),
        Record(puid=UInt(104), puid_tree=b'\tall\tsalary\t'),
        Record(puid=UInt(104), puid_tree=b'\tall\tsalary\tDep 3\t'),
        Record(puid=UInt(104), puid_tree=b'\tall\tsalary\tDep 3\tIgorev Igor (igorev)\t'),
        Record(puid=UInt(104), puid_tree=b'\tall\tsalary\tDep 3\tIgorev Igor (igorev)\tyndx-igor (104)\t'),
    ]
    assert puid_info == [
        Record(puid=101, payment='piecework', involvement='staff', group='Piecework Department 1', person='Petrov Ivan (petrov)'),
        Record(puid=102, payment='piecework', involvement='staff', group='Dep 2',                  person='Ivanov Petr (ivanov)'),
        Record(puid=103, payment='piecework', involvement='staff', group='Dep 2',                  person='Ivanov Petr (ivanov)'),
        Record(puid=104, payment='salary',    involvement='staff', group='Dep 3',                  person='Igorev Igor (igorev)'),
    ]


@pytest.mark.parametrize('user_role', puid_tree.ROLES['all'] - puid_tree.ROLES['user_anonymized'])
def test_personalized_users(user_role):
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([Record(puid=b'101', um_login=b'yndx.Ivan', moderation_status=user_role.encode())]),
        outsourcers_dump=nile_ut.Table([]),
        acl_roles_dump=nile_ut.Table([]),
        staff_dump=nile_ut.Table([]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tuser\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tuser\t' + user_role.encode() + b'\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tuser\t' + user_role.encode() + b'\tyndx-ivan (101)\t'),
    ]
    assert puid_info == [
        Record(puid=101, payment='free', involvement='user', group=user_role, person='yndx-ivan'),
    ]


@pytest.mark.parametrize('user_role', puid_tree.ROLES['user_anonymized'])
def test_anonymized_users(user_role):
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([Record(puid=b'101', um_login=b'yndx-ivan', moderation_status=user_role.encode())]),
        outsourcers_dump=nile_ut.Table([]),
        acl_roles_dump=nile_ut.Table([]),
        staff_dump=nile_ut.Table([]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tuser\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tuser\t' + user_role.encode() + b'\t'),
    ]
    if user_role == 'common':
        assert not puid_info
    else:
        assert puid_info == [
            Record(puid=101, payment='free', involvement='user', group=user_role, person=user_role)
        ]


@pytest.mark.parametrize('partner_acl_role', puid_tree.PARTNER_ACL_ROLES - puid_tree.PARTNER_ACL_ROLES_WITH_COMPANY_NAME)
def test_partner_users_without_company_name(partner_acl_role):
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([Record(puid=b'101', um_login=b'partner-ivan', moderation_status=b'common')]),
        outsourcers_dump=nile_ut.Table([]),
        acl_roles_dump=nile_ut.Table([Record(puid=b'101', role=partner_acl_role.encode())]),
        staff_dump=nile_ut.Table([]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t' + partner_acl_role.encode() + b'\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t' + partner_acl_role.encode() + b'\tno company\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t' + partner_acl_role.encode() + b'\tno company\tpartner-ivan\t'),
    ]

    company = 'sprav' if partner_acl_role.startswith('sprav') else 'no company'
    assert puid_info == [
        Record(puid=101, payment='piecework', involvement='partner', group=company, person='partner-ivan'),
    ]


@pytest.mark.parametrize('partner_acl_role', puid_tree.PARTNER_ACL_ROLES_WITH_COMPANY_NAME)
def test_partner_users_with_company_name(partner_acl_role):
    role_base_name, company = partner_acl_role.encode().split(b'_')
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([Record(puid=b'101', um_login=b'partner-ivan', moderation_status=b'common')]),
        outsourcers_dump=nile_ut.Table([]),
        acl_roles_dump=nile_ut.Table([Record(puid=b'101', role=partner_acl_role.encode())]),
        staff_dump=nile_ut.Table([]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t' + role_base_name + b'\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t' + role_base_name + b'\t' + company + b'\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tpiecework\tpartner\t' + role_base_name + b'\t' + company + b'\tpartner-ivan\t'),
    ]
    assert puid_info == [
        Record(puid=101, payment='piecework', involvement='partner', group=company.decode(), person='partner-ivan'),
    ]


def test_users_with_not_a_partner_acl_role():
    puid_map, puid_info = nile_ut.yt_run(
        prepare_puid_map_and_info,
        nile_ut.Job(),
        users_dump=nile_ut.Table([Record(puid=b'101', um_login=b'user-ivan', moderation_status=b'common')]),
        outsourcers_dump=nile_ut.Table([]),
        acl_roles_dump=nile_ut.Table([Record(puid=b'101', role=b'other_acl_role')]),
        staff_dump=nile_ut.Table([]),
    )
    assert puid_map == [
        Record(puid=UInt(101), puid_tree=b'\tall\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tuser\t'),
        Record(puid=UInt(101), puid_tree=b'\tall\tuser\tcommon\t'),
    ]
    assert not puid_info


def test_cast_puid():
    result = nile_ut.yt_run(
        cast_puid,
        nile_ut.Table([Record(puid=101, role=b'other_acl_role')]),
    )

    assert result == [Record(puid=b'101', role=b'other_acl_role')]
