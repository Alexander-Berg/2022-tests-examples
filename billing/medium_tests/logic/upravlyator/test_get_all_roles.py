# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm

from balance import mapper
import tests.object_builder as ob
from tests.tutils import has_exact_entries
# noinspection PyUnresolvedReferences
from tests.medium_tests.logic.upravlyator.conftest import (
    EXTERNAL_CLIENT_ROLE,
    GROUP_ID,
    add_role_with_constraints,
    create_role_client,
    create_role_client_group,

    parametrize_constraints,
    get_fields_by_constraints,
)

from balance.mapper import TVMACLApp, TVMACLGroupPermission, TVMACLGroup


def stringify_dict_values(x):
    return {k: str(v) for k, v in x.items()}


@pytest.fixture(autouse=True)
def mock_passports_getter(domain_passport):
    old_func = mapper.Passport.get_domain_passports

    def mock_(session):
        return old_func(session, domain_passport.login)

    mapper.Passport.get_domain_passports = staticmethod(mock_)

    yield

    mapper.Passport.get_domain_passports = staticmethod(old_func)


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_domain_role(session, upravlyator, domain_passport, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.flush()

    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains_inanyorder(
            hm.has_entries({'role': str(role.id)}),
            hm.has_entries({'role': str(EXTERNAL_CLIENT_ROLE)})
        ),
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(
                hm.has_entries(role_check),
            ),
        }),
    )


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_domain_role_with_constraint(constraints, session, upravlyator, domain_passport, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    constraint_fields = get_fields_by_constraints(session, constraints)
    add_role_with_constraints(domain_passport, role, constraint_fields)
    session.flush()

    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains(
            has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)}),
            hm.contains(
                has_exact_entries({'role': str(role.id)}),
                has_exact_entries(stringify_dict_values(constraint_fields))
            )
        )
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(
                hm.has_entries()
            )
        })
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_passport_role(session, upravlyator, domain_passport, yndx_passport, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role))
    session.flush()

    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains(
            has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)}),
            hm.contains(
                has_exact_entries({'role': str(role.id)}),
                has_exact_entries({'passport-login': yndx_passport.login})
            )
        )
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(has_exact_entries(role_check))
        })
    )


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_passport_role_with_constraint(constraints, session, upravlyator, domain_passport, yndx_passport, role,
                                            support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    constraint_fields = get_fields_by_constraints(session, constraints)
    add_role_with_constraints(yndx_passport, role, constraint_fields)
    session.flush()

    res = upravlyator.get_all_roles()
    expected_constraint_fields = dict(stringify_dict_values(constraint_fields),
                                      **{'passport-login': yndx_passport.login})

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains(
            has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)}),
            hm.contains(
                has_exact_entries({'role': str(role.id)}),
                has_exact_entries(expected_constraint_fields)
            )
        )
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(
                has_exact_entries(role_check)
            )
        })
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_passport_external_role(session, upravlyator, domain_passport, yndx_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    yndx_passport.master = domain_passport
    session.flush()

    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains(
            has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)}),
        )
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(
                has_exact_entries(role_check)
            )
        })
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_only_domain_external_role(upravlyator, domain_passport, support_tvms, with_tvms):
    support_tvms(with_tvms)

    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains(
            has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)})
        )
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(
                has_exact_entries(role_check)
            )
        })
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_get_group_role(session, upravlyator, domain_passport, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    mapper.RoleGroup.set_roles(session, GROUP_ID, [role])
    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains(
            has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)})
        )
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(
                has_exact_entries(role_check)
            ),
            'groups': hm.contains(
                has_exact_entries({
                    'group': GROUP_ID,
                    'roles': hm.contains(
                        has_exact_entries({'role': str(role.id)})
                    )
                })
            )
        })
    )


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_get_group_role_w_constraint(constraints, session, upravlyator, domain_passport, role, support_tvms, with_tvms):
    support_tvms(with_tvms)

    constraint_fields = get_fields_by_constraints(session, constraints)
    mapper.RoleGroup.set_roles(session, GROUP_ID, [(role, constraint_fields)])
    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains(has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)}))
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains_inanyorder(has_exact_entries(role_check)),
            'groups': hm.contains(
                has_exact_entries({
                    'group': GROUP_ID,
                    'roles': hm.contains(
                        hm.contains(has_exact_entries({'role': str(role.id)}), has_exact_entries(stringify_dict_values(constraint_fields))),
                    )
                })
            )
        }),
    )


@parametrize_constraints
@pytest.mark.parametrize('with_tvms', [True, False])
def test_constraints(constraints, session, upravlyator, domain_passport, yndx_passport, role, roles, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role_domain, role_yndx = roles

    fst_constraint_fields = get_fields_by_constraints(session, constraints)
    sd_constraint_fields = get_fields_by_constraints(session, constraints)

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role_domain))
    pr = [pr_ for pr_ in domain_passport.real_passport_roles if pr_.role == role_domain][0]
    for field, value in fst_constraint_fields.items():
        setattr(pr, field, value)
    session.flush()

    yndx_passport.master = domain_passport
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role_domain))
    pr = [pr_ for pr_ in yndx_passport.real_passport_roles if pr_.role == role_domain][0]
    for field, value in sd_constraint_fields.items():
        setattr(pr, field, value)
    session.flush()

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role_domain))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role_domain))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role_yndx))

    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role_domain, **fst_constraint_fields))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role_domain, **sd_constraint_fields))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role_yndx, **sd_constraint_fields))
    session.flush()

    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': [
            {'role': str(EXTERNAL_CLIENT_ROLE)},
            [{'role': str(role_domain.id)}, stringify_dict_values(fst_constraint_fields)],
            {'role': str(role.id)},
            {'role': str(role_domain.id)},
            [{'role': str(role_domain.id)},
                dict(stringify_dict_values(sd_constraint_fields), **{'passport-login': yndx_passport.login})],
            [{'role': str(role_domain.id)}, {'passport-login': yndx_passport.login}],
            [{'role': str(role_yndx.id)}, {'passport-login': yndx_passport.login}],
        ]
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        has_exact_entries({
            'code': 0,
            'users': hm.contains(role_check),
            'groups': hm.contains(
                has_exact_entries({
                    'group': GROUP_ID,
                    'roles': hm.contains_inanyorder(
                        has_exact_entries({'role': str(role.id)}),
                        hm.contains(has_exact_entries({'role': str(role_domain.id)}), has_exact_entries(stringify_dict_values(fst_constraint_fields))),
                        hm.contains(has_exact_entries({'role': str(role_domain.id)}), has_exact_entries(stringify_dict_values(sd_constraint_fields))),
                        hm.contains(has_exact_entries({'role': str(role_yndx.id)}), has_exact_entries(stringify_dict_values(sd_constraint_fields))),
                    ),
                }),
            ),
        }),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_client_batch_id(session, upravlyator, domain_passport, yndx_passport, roles, support_tvms, with_tvms):
    support_tvms(with_tvms)

    role1, role2 = roles

    firm_id = ob.FirmBuilder().build(session).obj.id
    template_group_id = ob.CorrectionTemplateGroupBuilder().build(session).obj.id

    role_client = create_role_client(session)
    client_id = role_client.client_id
    client_batch_id_1 = role_client.client_batch_id
    client_batch_id_2 = create_role_client_group(session).client_batch_id

    yndx_passport.master = domain_passport
    session.flush()

    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role1, client_batch_id=client_batch_id_1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role2, client_batch_id=client_batch_id_1))
    session.add(mapper.RealRolePassport(passport=domain_passport, role=role2, client_batch_id=client_batch_id_1,
                                        firm_id=firm_id, template_group_id=template_group_id))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role1, client_batch_id=client_batch_id_1))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role2, client_batch_id=client_batch_id_1))
    session.add(mapper.RealRolePassport(passport=yndx_passport, role=role2, client_batch_id=client_batch_id_1,
                                        firm_id=firm_id, template_group_id=template_group_id))

    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role1))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role1, client_batch_id=client_batch_id_2))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role2, client_batch_id=client_batch_id_2))
    session.add(mapper.RoleGroup(session, group_id=GROUP_ID, role=role2, client_batch_id=client_batch_id_2,
                                 firm_id=firm_id, template_group_id=template_group_id))
    session.flush()

    res = upravlyator.get_all_roles()

    role_check = {
        'login': domain_passport.login,
        'roles': hm.contains_inanyorder(
            has_exact_entries({'role': str(EXTERNAL_CLIENT_ROLE)}),
            has_exact_entries({'role': str(role1.id)}),
            hm.contains(
                has_exact_entries({'role': str(role1.id)}),
                has_exact_entries({'client_id': str(client_id)}),
            ),
            hm.contains(
                has_exact_entries({'role': str(role2.id)}),
                has_exact_entries({'client_id': str(client_id)}),
            ),
            hm.contains(
                has_exact_entries({'role': str(role2.id)}),
                has_exact_entries(
                    stringify_dict_values(
                        {'client_id': client_id, 'firm_id': firm_id, 'template_group_id': template_group_id})),
            ),
            hm.contains(
                has_exact_entries({'role': str(role1.id)}),
                has_exact_entries({'passport-login': yndx_passport.login}),
            ),
            hm.contains(
                has_exact_entries({'role': str(role1.id)}),
                has_exact_entries({'passport-login': yndx_passport.login, 'client_id': str(client_id)}),
            ),
            hm.contains(
                has_exact_entries({'role': str(role2.id)}),
                has_exact_entries({'passport-login': yndx_passport.login, 'client_id': str(client_id)}),
            ),
            hm.contains(
                has_exact_entries({'role': str(role2.id)}),
                has_exact_entries(stringify_dict_values(
                    {'passport-login': yndx_passport.login, 'client_id': client_id,
                     'firm_id': firm_id, 'template_group_id': template_group_id})
                ),
            ),
        ),
    }

    if with_tvms:
        role_check['subject_type'] = 'user'

    hm.assert_that(
        res,
        hm.has_entries({
            'code': hm.equal_to(0),
            'users': hm.contains(
                has_exact_entries(role_check),
            ),
            'groups': hm.contains(
                has_exact_entries({
                    'group': GROUP_ID,
                    'roles': hm.contains_inanyorder(
                        has_exact_entries({'role': str(role1.id)}),
                        hm.contains(
                            has_exact_entries({'role': str(role1.id)}),
                            has_exact_entries({'abc_clients': hm.is_(True)}),
                        ),
                        hm.contains(
                            has_exact_entries({'role': str(role2.id)}),
                            has_exact_entries({'abc_clients': hm.is_(True)}),
                        ),
                        hm.contains(
                            has_exact_entries({'role': str(role2.id)}),
                            has_exact_entries({'abc_clients': hm.is_(True), 'firm_id': str(firm_id),
                                               'template_group_id': str(template_group_id)}),
                        ),
                    ),
                }),
            ),
        }),
    )


def test_tvm_disable(session, upravlyator, support_tvms):
    support_tvms(False)

    res = upravlyator.get_all_roles()

    assert next((user for user in res['users'] if user.get('subject_type') is not None), None) is None


def test_tvm_roles(session, upravlyator, support_tvms):
    support_tvms(True)

    tvm = TVMACLApp(tvm_id=1, env='test')
    session.add(tvm)
    # tvm1 = TVMACLApp(tvm_id=2, env='prod')

    tvm_group = TVMACLGroup(name='group_as_role')
    session.add(tvm_group)
    session.flush()

    permission = TVMACLGroupPermission(tvm_id=tvm.tvm_id, group_name=tvm_group.name)
    session.add(permission)
    session.flush()

    res = upravlyator.get_all_roles()

    expected_result = hm.has_entries({
        'code': 0,
        'users': hm.contains_inanyorder(
            hm.has_entries({
                'login': tvm.tvm_id,
                'subject_type': 'tvm_app',
                'roles': hm.contains(
                    hm.contains_inanyorder(
                        hm.has_entries({
                            'role': 'tvms_roles',
                            'tvms_roles': tvm_group.name,
                        }),
                        hm.has_entries({'env': tvm.env})
                    )
                )
            }),
            hm.has_entries({'subject_type': 'user'})
        )
    })

    hm.assert_that(res, expected_result)
