# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm
import sqlalchemy as sa

from balance import constants as cst, mapper
from tests.object_builder import CorrectionTemplateGroupBuilder
from tests.tutils import has_exact_entries

from tests.medium_tests.logic.upravlyator.conftest import (
    GROUP_ID,
    create_role,
    create_passport,
    create_role_client,
    create_role_client_group,
)

from balance.mapper import TVMACLApp, TVMACLGroupPermission, TVMACLGroup


@pytest.fixture(scope='module', autouse=True)
def del_all_roles(modular_session):
    modular_session.query(mapper.RealRolePassport).delete()
    modular_session.query(mapper.RoleGroup).delete()
    modular_session.query(mapper.RoleClientGroup).delete()
    modular_session.query(mapper.RoleClient).delete()
    modular_session.flush()


@pytest.fixture(scope='module')
def data(modular_session):
    session = modular_session

    domain_passport = create_passport(session, login='login', is_internal=True)
    yndx_passport = create_passport(session, 'yndx-%s' % domain_passport.login)
    yndx_passport.master = domain_passport
    role, role1, role2 = tuple(create_role(session) for _i in xrange(3))
    session.flush()

    firm_id_1 = cst.FirmId.YANDEX_OOO
    firm_id_2 = cst.FirmId.MARKET
    template_group_id = CorrectionTemplateGroupBuilder().build(session).obj.id

    role_client = create_role_client(session)
    client_batch_2 = create_role_client_group(session).client_batch_id

    f_ids = [session.execute(sa.Sequence('s_role_user_id').next_value()).scalar() for _i in range(23)]
    data = [
        (mapper.RealRolePassport(id=f_ids[1], passport=domain_passport, role=role),
         {'path': '/role/%s/' % str(role.id), 'login': domain_passport.login}),
        (mapper.RealRolePassport(id=f_ids[6], passport=domain_passport, role=role1),
         {'path': '/role/%s/' % str(role1.id), 'login': domain_passport.login}),
        (mapper.RealRolePassport(id=f_ids[5], passport=domain_passport, role=role1, firm_id=str(firm_id_1), template_group_id=template_group_id),
         {'path': '/role/%s/' % str(role1.id), 'login': domain_passport.login, 'fields': has_exact_entries({'firm_id': str(firm_id_1), 'template_group_id': str(template_group_id)})}),
        (mapper.RealRolePassport(id=f_ids[8], passport=domain_passport, role=role1, firm_id=str(firm_id_2)),
         {'path': '/role/%s/' % str(role1.id), 'login': domain_passport.login, 'fields': has_exact_entries({'firm_id': str(firm_id_2)})}),
        (mapper.RealRolePassport(id=f_ids[17], passport=domain_passport, role=role2, firm_id=str(firm_id_2)),
         {'path': '/role/%s/' % str(role2.id), 'login': domain_passport.login, 'fields': has_exact_entries({'firm_id': str(firm_id_2)})}),
        (mapper.RealRolePassport(id=f_ids[7], passport=yndx_passport, role=role),
         {'path': '/role/%s/' % str(role.id), 'login': domain_passport.login, 'fields': has_exact_entries({'passport-login': yndx_passport.login})}),
        (mapper.RealRolePassport(id=f_ids[2], passport=yndx_passport, role=role1),
         {'path': '/role/%s/' % str(role1.id), 'login': domain_passport.login, 'fields': has_exact_entries({'passport-login': yndx_passport.login})}),
        (mapper.RealRolePassport(id=f_ids[11], passport=yndx_passport, role=role1, firm_id=str(firm_id_1)),
         {'path': '/role/%s/' % str(role1.id), 'login': domain_passport.login, 'fields': has_exact_entries({'firm_id': str(firm_id_1), 'passport-login': yndx_passport.login})}),
        (mapper.RealRolePassport(id=f_ids[14], passport=yndx_passport, role=role1, firm_id=str(firm_id_2)),
         {'path': '/role/%s/' % str(role1.id), 'login': domain_passport.login, 'fields': has_exact_entries({'firm_id': str(firm_id_2), 'passport-login': yndx_passport.login})}),
        (mapper.RealRolePassport(id=f_ids[16], passport=yndx_passport, role=role2, firm_id=str(firm_id_2)),
         {'path': '/role/%s/' % str(role2.id), 'login': domain_passport.login, 'fields': has_exact_entries({'firm_id': str(firm_id_2), 'passport-login': yndx_passport.login})}),
        (mapper.RoleGroup(session, id=f_ids[15], group_id=GROUP_ID, role=role),
         {'path': '/role/%s/' % str(role.id), 'group': GROUP_ID}),
        (mapper.RoleGroup(session, id=f_ids[12], group_id=GROUP_ID, role=role1),
         {'path': '/role/%s/' % str(role1.id), 'group': GROUP_ID}),
        (mapper.RoleGroup(session, id=f_ids[19], group_id=GROUP_ID, role=role1, firm_id=str(firm_id_1), template_group_id=template_group_id),
         {'path': '/role/%s/' % str(role1.id), 'group': GROUP_ID, 'fields': has_exact_entries({'firm_id': str(firm_id_1), 'template_group_id': str(template_group_id)})}),
        (mapper.RoleGroup(session, id=f_ids[3], group_id=GROUP_ID, role=role1, firm_id=str(firm_id_2)),
         {'path': '/role/%s/' % str(role1.id), 'group': GROUP_ID, 'fields': has_exact_entries({'firm_id': str(firm_id_2)})}),
        (mapper.RoleGroup(session, id=f_ids[10], group_id=GROUP_ID, role=role2, firm_id=str(firm_id_2)),
         {'path': '/role/%s/' % str(role2.id), 'group': GROUP_ID, 'fields': has_exact_entries({'firm_id': str(firm_id_2)})}),
        (mapper.RoleGroup(session, id=f_ids[22], group_id=GROUP_ID, role=role, firm_id=str(firm_id_2)),
         {'path': '/role/%s/' % str(role.id), 'group': GROUP_ID, 'fields': has_exact_entries({'firm_id': str(firm_id_2)})}),

        (mapper.RealRolePassport(session, id=f_ids[13], passport=domain_passport, role=role, client_batch_id=role_client.client_batch_id),
         {'path': '/role/%s/' % str(role.id), 'login': domain_passport.login, 'fields': has_exact_entries({'client_id': str(role_client.client_id)})}),
        (mapper.RealRolePassport(session, id=f_ids[18], passport=yndx_passport, role=role, client_batch_id=role_client.client_batch_id),
         {'path': '/role/%s/' % str(role.id), 'login': domain_passport.login, 'fields': has_exact_entries({'client_id': str(role_client.client_id), 'passport-login': yndx_passport.login})}),
        (mapper.RealRolePassport(session, id=f_ids[4], passport=yndx_passport, role=role, firm_id=firm_id_1, client_batch_id=role_client.client_batch_id),
         {'path': '/role/%s/' % str(role.id), 'login': domain_passport.login, 'fields': has_exact_entries({'firm_id': str(firm_id_1), 'client_id': str(role_client.client_id), 'passport-login': yndx_passport.login})}),
        (mapper.RoleGroup(session, id=f_ids[20], group_id=GROUP_ID, role=role, client_batch_id=client_batch_2),
         {'path': '/role/%s/' % str(role.id), 'group': GROUP_ID, 'fields': has_exact_entries({'abc_clients': True})}),
        (mapper.RoleGroup(session, id=f_ids[21], group_id=GROUP_ID, role=role1, client_batch_id=client_batch_2),
         {'path': '/role/%s/' % str(role1.id), 'group': GROUP_ID, 'fields': has_exact_entries({'abc_clients': True})}),
        (mapper.RoleGroup(session, id=f_ids[9], group_id=GROUP_ID, role=role1, client_batch_id=client_batch_2, firm_id=firm_id_1),
         {'path': '/role/%s/' % str(role1.id), 'group': GROUP_ID, 'fields': has_exact_entries({'abc_clients': True, 'firm_id': str(firm_id_1)})}),
    ]

    session.add_all([obj for obj, _res in data[2:]])
    session.flush()

    data = sorted(data, key=lambda x: x[0].id)
    return data


@pytest.mark.parametrize(
    'start, batch_size, next_id',
    [
        pytest.param(0, 22, None, id='all'),
        pytest.param(0, 23, None, id='all + 1'),
        pytest.param(0, 10, 10, id='first 5 rows only'),
        pytest.param(5, 10, 15, id='middle'),
        pytest.param(17, 5, None, id='last 5 rows only'),
        pytest.param(16, 5, 21, id='less on 1 row'),
    ],
)
@pytest.mark.parametrize('with_tvms', [True, False])
def test_parts(modular_session, modular_upravlyator, data, start, batch_size, next_id,
               modular_support_tvms, with_tvms):
    modular_support_tvms(with_tvms)

    modular_session.config.__dict__['IDM_BATCH_SIZE'] = batch_size

    kw = {}
    if start:
        kw['next-id'] = data[start][0].id
    res = modular_upravlyator.get_roles(**kw)

    required_res = {'code': 0}
    if next_id is not None:
        required_res['next-url'] = '/idm/get-roles?next-id=%s' % data[next_id][0].id

    contains = []
    for obj, matcher in data[start:start + batch_size]:
        if matcher.get('login') is not None and with_tvms:
            matcher = matcher.copy()
            matcher['subject_type'] = 'user'

        contains.append(has_exact_entries(matcher))

    required_res['roles'] = hm.contains(*contains)

    hm.assert_that(res, has_exact_entries(required_res))


@pytest.mark.parametrize('with_tvms', [True, False])
def test_failed_row(modular_session, modular_upravlyator, modular_support_tvms, with_tvms):
    modular_support_tvms(with_tvms)

    modular_session.config.__dict__['IDM_BATCH_SIZE'] = 1

    domain_passport = create_passport(modular_session, login='login', is_internal=True)
    role = create_role(modular_session)

    user_role_id = modular_session.execute(sa.Sequence('s_role_user_id').next_value()).scalar()
    user_role = mapper.RealRolePassport(id=user_role_id, passport=domain_passport, role=role, client_batch_id=-666)
    modular_session.add(user_role)
    modular_session.flush()

    res = modular_upravlyator.get_roles(**{'next-id': user_role_id})

    required_res = {'code': 0, 'roles': hm.empty()}
    hm.assert_that(res, has_exact_entries(required_res))


def test_tvm_disable(upravlyator, support_tvms):
    support_tvms(False)

    res = upravlyator.get_roles()

    assert next((role for role in res['roles'] if role.get('subject_type')), None) is None


def test_return_tvm_roles(session, upravlyator):
    tvm = TVMACLApp(tvm_id=1, env='test')
    session.add(tvm)

    tvm_group = TVMACLGroup(name='best_group')
    session.add(tvm_group)
    session.flush()

    role = TVMACLGroupPermission(tvm_id=tvm.tvm_id, group_name=tvm_group.name)
    session.add(role)
    session.flush()

    res = upravlyator.get_roles()

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'roles': hm.has_item(
                has_exact_entries({
                    'path': '/role/tvms_roles/%s/' % tvm_group.name,
                    'login': tvm.tvm_id,
                    'subject_type': 'tvm_app',
                    'fields': has_exact_entries({'env': tvm.env})
                })
            )
        }),
    )
