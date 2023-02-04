# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm

from balance import mapper
from tests.tutils import has_exact_entries

from tests.medium_tests.logic.upravlyator.conftest import (
    create_passport,
)

GROUP_ID_1 = 111
GROUP_ID_2 = 222
GROUP_ID_3 = 333


@pytest.fixture(scope='module', autouse=True)
def del_all_groups(modular_session):
    modular_session.query(mapper.PassportGroup).delete()
    modular_session.flush()


@pytest.fixture(scope='module')
def data(modular_session):
    session = modular_session

    domain_passports = [create_passport(session, login='login_%s' % idx, is_internal=True) for idx in range(3)]
    yndx_passports = []
    for domain_passport in domain_passports:
        yndx_passport = create_passport(session, 'yndx-%s' % domain_passport.login)
        yndx_passport.master = domain_passport
        yndx_passports.append(yndx_passport)
    domain_passports += [create_passport(session, login='login_3', is_internal=True)]
    session.flush()

    data = [
        (mapper.PassportGroup(passport=domain_passports[0], group_id=GROUP_ID_1),
         {'login': domain_passports[0].login, 'group': GROUP_ID_1, 'passport_login': ''}),
        (mapper.PassportGroup(passport=yndx_passports[0], group_id=GROUP_ID_2),
         {'login': domain_passports[0].login, 'group': GROUP_ID_2, 'passport_login': yndx_passports[0].login}),
        (mapper.PassportGroup(passport=yndx_passports[0], group_id=GROUP_ID_3),
         {'login': domain_passports[0].login, 'group': GROUP_ID_3, 'passport_login': yndx_passports[0].login}),
        (mapper.PassportGroup(passport=yndx_passports[1], group_id=GROUP_ID_2),
         {'login': domain_passports[1].login, 'group': GROUP_ID_2, 'passport_login': yndx_passports[1].login}),
        (mapper.PassportGroup(passport=yndx_passports[2], group_id=GROUP_ID_2),
         {'login': domain_passports[2].login, 'group': GROUP_ID_2, 'passport_login': yndx_passports[2].login}),
        (mapper.PassportGroup(passport=yndx_passports[2], group_id=GROUP_ID_3),
         {'login': domain_passports[2].login, 'group': GROUP_ID_3, 'passport_login': yndx_passports[2].login}),
        (mapper.PassportGroup(passport=domain_passports[3], group_id=GROUP_ID_1),
         {'login': domain_passports[3].login, 'group': GROUP_ID_1, 'passport_login': ''}),
        (mapper.PassportGroup(passport=domain_passports[3], group_id=GROUP_ID_2),
         {'login': domain_passports[3].login, 'group': GROUP_ID_2, 'passport_login': ''}),
        (mapper.PassportGroup(passport=domain_passports[3], group_id=GROUP_ID_3),
         {'login': domain_passports[3].login, 'group': GROUP_ID_3, 'passport_login': ''}),
    ]

    session.add_all([obj for obj, _res in data])
    session.flush()

    data = sorted(data, key=lambda x: (x[0].group_id, x[0].passport_id))
    return data


@pytest.mark.parametrize(
    'start, batch_size, next_id',
    [
        pytest.param(0, 9, None, id='all'),
        pytest.param(0, 10, None, id='all + 1'),
        pytest.param(0, 5, 5, id='first 5 rows only'),
        pytest.param(2, 3, 5, id='middle'),
        pytest.param(4, 5, None, id='last 5 rows only'),
        pytest.param(3, 5, 8, id='less on 1 row'),
    ],
)
def test_parts(modular_session, modular_upravlyator, data, start, batch_size, next_id):
    modular_session.config.__dict__['IDM_BATCH_SIZE'] = batch_size

    kw = {}
    if start:
        kw['next-group-id'] = data[start][0].group_id
        kw['next-passport-id'] = data[start][0].passport_id
    res = modular_upravlyator.get_memberships(**kw)

    required_res = {'code': 0}
    if next_id is not None:
        next_item = data[next_id][0]
        next_url = '/idm/get-memberships?next-group-id=%s&next-passport-id=%s' % (next_item.group_id, next_item.passport_id)
        required_res['next-url'] = next_url
    required_res['memberships'] = hm.contains(*[
        has_exact_entries(matcher)
        for obj, matcher in data[start:start + batch_size]
    ])

    hm.assert_that(res, hm.has_entries(required_res))
