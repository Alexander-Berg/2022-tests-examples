# -*- coding: utf-8 -*-

import pytest
import hamcrest as hm
from hamcrest import has_entries, contains_inanyorder, has_length, assert_that, all_of
import sqlalchemy as sa

from balance import constants as cst, mapper
from tests.object_builder import CorrectionTemplateGroupBuilder

from balance.mapper import TVMACLGroup


@pytest.mark.parametrize('with_tvms', [True, False])
def test_info(session, upravlyator, roles, support_tvms, with_tvms):
    support_tvms(with_tvms)

    # создадим тестовую группу, что бы выборка была не пустой
    group = CorrectionTemplateGroupBuilder().build(session).obj

    role1, role2 = roles
    role2.fields.extend([
        'abc_clients', 'client_id',
        cst.ConstraintTypes.firm_id, cst.ConstraintTypes.template_group_id
    ])
    session.flush()

    roles_count = session.execute(
        sa.select([sa.func.count()]).select_from(mapper.Role)).scalar()

    firms_count = session.execute(
        sa.select([sa.func.count()]).select_from(mapper.Firm)).scalar()
    template_groups_count = session.execute(sa.select([sa.func.count()])
                                            .select_from(mapper.CorrectionTemplateGroup)).scalar()
    res = upravlyator.info()

    hm.assert_that(
        res,
        hm.has_entries({
            'code': 0,
            'fields': hm.contains_inanyorder(  # выгружает описание полей
                hm.has_entries({'slug': 'passport-login'}),
                hm.has_entries({
                    'slug': cst.ConstraintTypes.firm_id,
                    'options': hm.has_entries({
                        'widget': 'select',
                        'choices': hm.has_length(firms_count),
                    }),
                }),
                hm.has_entries({
                    'slug': cst.ConstraintTypes.template_group_id,
                    'options': hm.has_entries({
                        'widget': 'select',
                        'choices': hm.all_of(
                            hm.has_length(template_groups_count),
                            hm.has_item(
                                hm.equal_to({'value': str(group.id), 'name': group.title}))
                        )
                    }),
                }),
                hm.has_entries({'slug': 'abc_clients'}),
                hm.has_entries({'slug': 'client_id'}),
            ),
            'roles': hm.has_entries({
                'name': u'роль',
                'slug': 'role',
                'values': hm.has_length(roles_count + 1 if with_tvms else roles_count),
            }),
        }),
    )
    hm.assert_that(
        res['roles']['values'],
        hm.has_entry(
            role1.id,
            hm.has_entries({  # у одной из ролей отключено поле firm_id
                'name': role1.name,
                'fields': hm.contains_inanyorder(
                    hm.has_entries(
                        {'slug': cst.ConstraintTypes.firm_id, 'type': 'undo'}),
                    hm.has_entries(
                        {'slug': cst.ConstraintTypes.template_group_id, 'type': 'undo'}),
                    hm.has_entries({'slug': 'abc_clients', 'type': 'undo'}),
                    hm.has_entries({'slug': 'client_id', 'type': 'undo'}),
                ),
            }),
        ),
    )
    hm.assert_that(
        res['roles']['values'],
        hm.has_entry(  # проверяем, что у других ролей поля не отключены
            role2.id,
            {'name': role2.name},
        ),
    )
    hm.assert_that(
        res['fields'][1]['options']['choices'][-1],
        hm.equal_to(
            {'value': str(cst.FirmId.UNDEFINED), 'name': u'Все фирмы'}),
    )


@pytest.mark.parametrize('with_tvms', [True, False])
def test_tvm_roles(session, upravlyator, support_tvms, with_tvms):
    support_tvms(with_tvms)

    tvm_group = TVMACLGroup(name='test_name')
    session.add(tvm_group)
    session.flush()

    res = upravlyator.info()

    expected_schema = has_entries({
        'roles': has_entries({
            'values': has_entries({
                'tvms_roles': has_entries({
                    'roles': has_entries({
                        'values': has_entries({
                            tvm_group.name: has_entries({
                                'name': tvm_group.name,
                                'fields': contains_inanyorder(
                                    has_entries({
                                        'name': u'Окружение',
                                        'slug': 'env',
                                        'type': 'choicefield',
                                        'required': True,
                                        'options': has_entries({
                                            'widget': 'select',
                                            'custom': False,
                                            'choices': all_of(
                                                has_length(2),
                                                contains_inanyorder(
                                                    has_entries(
                                                        {'name': 'test', 'value': 'test'}),
                                                    has_entries(
                                                        {'name': 'prod', 'value': 'prod'})
                                                )
                                            )
                                        })
                                    })
                                )
                            })
                        })
                    })
                })
            })
        })
    })

    if with_tvms:
        assert_that(res, expected_schema)
    else:
        roles = res['roles']['values'].keys()
        assert 'tvms_roles' not in roles
