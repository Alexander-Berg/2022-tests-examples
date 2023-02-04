# -*- coding: utf-8 -*-
import pytest
import mock
import hamcrest as hm

from balance import mapper, exc
from balance.constants import FirmId, ConstraintTypes, RoleName
from tests.balance_tests.passport.passport_common import (create_passport,
                             create_role)
from tests import object_builder as ob
from tests.tutils import has_exact_entries


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


class TestPassportSetRoles(object):
    def test_set_new_roles(self, session):
        """Удаляем 2 старые роли и добавляем 2 новые"""
        role_old_1 = create_role(session, ('PermOld1', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_old_2 = create_role(session, ('PermOld2', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_common = create_role(session, ('PermCommon', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_new_1 = create_role(session, ('PermNew1', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_new_2 = create_role(session, ('PermNew2', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))

        client_batch_old_1 = ob.RoleClientBuilder.construct(session).client_batch_id
        client_batch_old_2 = ob.RoleClientBuilder.construct(session).client_batch_id
        client_batch_common = ob.RoleClientBuilder.construct(session).client_batch_id
        client_batch_new_1 = ob.RoleClientBuilder.construct(session).client_batch_id
        client_batch_new_2 = ob.RoleClientBuilder.construct(session).client_batch_id

        passport = create_passport(
            session,
            (role_old_1, FirmId.YANDEX_OOO, client_batch_old_1),
            (role_old_2, FirmId.CLOUD, client_batch_old_2),
            (role_common, FirmId.YANDEX_OOO, client_batch_common),
            (role_common, FirmId.CLOUD, client_batch_common),
        )

        passport.set_roles([
            (role_common, {ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_common}),
            (role_new_1, {ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_new_1}),
            (role_new_2, {ConstraintTypes.firm_id: FirmId.CLOUD, ConstraintTypes.client_batch_id: client_batch_new_2}),
        ])

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties({'role_id': role_common.id, ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_common}),
                hm.has_properties({'role_id': role_new_1.id, ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_new_1}),
                hm.has_properties({'role_id': role_new_2.id, ConstraintTypes.firm_id: FirmId.CLOUD, ConstraintTypes.client_batch_id: client_batch_new_2}),
            ]),
        )

    @pytest.mark.parametrize('new_firm_id', [FirmId.CLOUD, None])
    def test_add_role_with_new_constraints(self, session, new_firm_id):
        """При добавлении к Паспорту существующей роли с новым ограничением,
        добавляется новая запись в passport_roles"""
        role = create_role(session, ('Perm', {ConstraintTypes.firm_id: None}))
        passport = create_passport(
            session,
            (role, FirmId.YANDEX_OOO),
        )

        passport.set_roles([
            (role, {ConstraintTypes.firm_id: FirmId.YANDEX_OOO}),
            (role, {ConstraintTypes.firm_id: new_firm_id}),
        ])

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties({'role_id': role.id, ConstraintTypes.firm_id: FirmId.YANDEX_OOO}),
                hm.has_properties({'role_id': role.id, ConstraintTypes.firm_id: new_firm_id}),
            ]),
        )

    def test_add_role_with_new_client_constraints(self, session):
        """При добавлении к Паспорту существующей роли с новым ограничением,
        добавляется новая запись в passport_roles"""
        client_old_1 = create_client(session)
        client_new_1 = create_client(session)
        client_new_2 = create_client(session)

        client_batch_old_1 = ob.RoleClientBuilder.construct(session, client=client_old_1).client_batch_id
        client_batch_new_1 = ob.RoleClientBuilder.construct(session, client=client_new_1).client_batch_id
        client_batch_new_2 = ob.RoleClientBuilder.construct(session, client=client_new_2).client_batch_id

        role = create_role(session, ('Perm', {ConstraintTypes.client_batch_id: None}))
        passport = create_passport(
            session,
            (role, None, client_batch_old_1),
        )

        passport.set_roles([
            (role, {ConstraintTypes.client_batch_id: client_batch_new_1}),
            (role, {ConstraintTypes.client_batch_id: client_batch_new_2}),
        ])

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties({'role_id': role.id, ConstraintTypes.client_batch_id: client_batch_new_1, 'client_ids': [client_new_1.id]}),
                hm.has_properties({'role_id': role.id, ConstraintTypes.client_batch_id: client_batch_new_2, 'client_ids': [client_new_2.id]}),
            ]),
        )

    def test_add_role_with_new_client_constraints_v2(self, session):
        """добавляем группу с несколькими клиентами"""
        client_old_1 = create_client(session)
        client_new_1 = create_client(session)
        client_new_2 = create_client(session)

        client_batch_old_1 = ob.RoleClientBuilder.construct(session, client=client_old_1).client_batch_id
        client_batch_new_1 = ob.RoleClientGroupBuilder.construct(
            session,
            clients=[client_new_1, client_new_2],
        ).client_batch_id

        role = create_role(session, ('Perm', {ConstraintTypes.client_batch_id: None}))
        passport = create_passport(
            session,
            (role, None, client_batch_old_1),
        )

        passport.set_roles([
            (role, {ConstraintTypes.client_batch_id: client_batch_new_1}),
        ])

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties({
                    'role_id': role.id,
                    ConstraintTypes.client_batch_id: client_batch_new_1,
                    'clients': [client_new_1, client_new_2],
                    'client_ids': [client_new_1.id, client_new_2.id],
                }),
            ]),
        )

    @mock.patch('butils.passport.passport_admsubscribe')
    def test_add_one_role_w_different_constraints(self, mock_admsubscribe, session):
        client_batch_1 = ob.RoleClientBuilder.construct(session).client_batch_id
        client_batch_2 = ob.RoleClientBuilder.construct(session).client_batch_id
        firm_1 = FirmId.BUS
        firm_2 = FirmId.DRIVE

        role = create_role(session, ('Perm', {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}))
        passport = create_passport(session)

        passport.set_roles([
            role,
            (role, {ConstraintTypes.client_batch_id: client_batch_1}),
            (role, {ConstraintTypes.firm_id: firm_1}),
            (role, {ConstraintTypes.client_batch_id: client_batch_1, ConstraintTypes.firm_id: firm_1}),
            (role, {ConstraintTypes.client_batch_id: client_batch_2, ConstraintTypes.firm_id: firm_2}),
        ])

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties(firm_id=None, client_batch_id=None),
                hm.has_properties(firm_id=firm_1, client_batch_id=None),
                hm.has_properties(firm_id=None, client_batch_id=client_batch_1),
                hm.has_properties(firm_id=firm_1, client_batch_id=client_batch_1),
                hm.has_properties(firm_id=firm_2, client_batch_id=client_batch_2),
            ]),
        )

    @pytest.mark.parametrize(
        'add_role',
        [True, False],
    )
    @mock.patch('butils.passport.passport_admsubscribe')
    def test_set_admsubscribe(self, mock_admsubscribe, session, add_role):
        role = create_role(session, 'Perm')
        existing_roles = [] if add_role else [role]
        passport = create_passport(session, *existing_roles)

        setting_roles = [role] if add_role else []
        passport.set_roles(setting_roles)

        passport_roles = passport.passport_roles

        if add_role:
            mock_admsubscribe.assert_called_once_with(passport, unsubscribe=False)
            hm.assert_that(
                passport_roles,
                hm.contains_inanyorder(*[
                    hm.has_properties({'role_id': role.id}),
                ]),
            )
        else:
            mock_admsubscribe.assert_not_called()
            assert passport_roles == []

    def test_delete_role_w_related_role_clients(self, session):
        client_1 = create_client(session)
        client_2 = create_client(session)

        client_batch_id_1 = ob.RoleClientBuilder.construct(session, client=client_1).client_batch_id
        client_batch_id_2 = ob.RoleClientBuilder.construct(session, client=client_2).client_batch_id

        role = create_role(session, ('Perm', {ConstraintTypes.client_batch_id: None}))
        passport = create_passport(
            session,
            (role, None, client_batch_id_1),
            (role, None, client_batch_id_2),
        )
        passport.set_roles([])

        # не удлаяем role_client сразу, они очищаются в таске abc-client-synchronizing-daily
        assert session.query(mapper.RoleClient).filter(mapper.RoleClient.client_id.in_([client_1.id, client_2.id])).count() == 2
        assert passport.passport_roles == []


class TestGroupSetRoles(object):
    def test_set_new_roles(self, session):
        """Удаляем 2 старые роли и добавляем 2 новые"""
        group_id = 666

        client_old_1 = create_client(session)
        client_old_2 = create_client(session)
        client_common = create_client(session)
        client_new_1 = create_client(session)
        client_new_2 = create_client(session)

        # чисто тестовый случай, т.к. в реальности не может быть у group_id несколько групп клиентов
        client_batch_old = ob.RoleClientGroupBuilder.construct(session, clients=[client_old_1, client_old_2]).client_batch_id
        client_batch_common = ob.RoleClientGroupBuilder.construct(session, clients=[client_common]).client_batch_id
        client_batch_new = ob.RoleClientGroupBuilder.construct(session, clients=[client_new_1, client_new_2]).client_batch_id

        role_old_1 = create_role(session, ('PermOld1', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_old_2 = create_role(session, ('PermOld2', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_common = create_role(session, ('PermCommon', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_new_1 = create_role(session, ('PermNew1', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        role_new_2 = create_role(session, ('PermNew2', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))
        passport_role = create_role(session, ('PassportRole', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}))

        passport = create_passport(
            session,
            (passport_role, FirmId.MARKET),
        )

        session.add(mapper.PassportGroup(passport=passport, group_id=group_id))
        session.add_all([
            mapper.RoleGroup(session, group_id=group_id, role=role_old_1, firm_id=FirmId.YANDEX_OOO, client_batch_id=client_batch_old),
            mapper.RoleGroup(session, group_id=group_id, role=role_old_2, firm_id=FirmId.MARKET, client_batch_id=client_batch_old),
            mapper.RoleGroup(session, group_id=group_id, role=role_common, firm_id=FirmId.YANDEX_OOO, client_batch_id=client_batch_common),
            mapper.RoleGroup(session, group_id=group_id, role=role_common, firm_id=FirmId.MARKET, client_batch_id=client_batch_common),
        ])
        session.flush()

        mapper.RoleGroup.set_roles(
            session,
            object_id=group_id,
            roles_definitions=[
                (role_common, {ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_common}),
                (role_new_1, {ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_new}),
                (role_new_2, {ConstraintTypes.firm_id: FirmId.CLOUD, ConstraintTypes.client_batch_id: client_batch_new}),
            ],
        )

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties({'group_id': group_id, 'role_id': role_common.id, 'clients': [client_common],
                                   ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_common}),
                hm.has_properties({'group_id': group_id, 'role_id': role_new_1.id, 'clients': [client_new_1, client_new_2],
                                   ConstraintTypes.firm_id: FirmId.YANDEX_OOO, ConstraintTypes.client_batch_id: client_batch_new}),
                hm.has_properties({'group_id': group_id, 'role_id': role_new_2.id, 'clients': [client_new_1, client_new_2],
                                   ConstraintTypes.firm_id: FirmId.CLOUD, ConstraintTypes.client_batch_id: client_batch_new}),
                hm.has_properties({'group_id': None, 'role_id': passport_role.id, 'clients': [],
                                   ConstraintTypes.firm_id: FirmId.MARKET, ConstraintTypes.client_batch_id: None}),
                hm.has_properties({'group_id': None, 'role_id': RoleName.CLIENT, 'clients': [],
                                   ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}),
            ]),
        )

    @pytest.mark.parametrize('new_firm_id', [FirmId.CLOUD, None])
    def test_add_role_with_new_constraints(self, session, new_firm_id):
        group_id = ob.get_big_number()

        role = create_role(session, ('Perm', {ConstraintTypes.firm_id: None}))
        passport = create_passport(
            session,
            (role, FirmId.YANDEX_OOO),
        )

        session.add_all([
            mapper.PassportGroup(passport=passport, group_id=group_id),
            mapper.RoleGroup(session, group_id=group_id, role=role, firm_id=FirmId.YANDEX_OOO),
        ])
        session.flush()

        mapper.RoleGroup.set_roles(
            session,
            object_id=group_id,
            roles_definitions=[
                (role, {ConstraintTypes.firm_id: FirmId.YANDEX_OOO}),
                (role, {ConstraintTypes.firm_id: new_firm_id}),
            ],
        )

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties({'group_id': None, 'role_id': RoleName.CLIENT, ConstraintTypes.firm_id: None}),
                hm.has_properties({'group_id': None, 'role_id': role.id, ConstraintTypes.firm_id: FirmId.YANDEX_OOO}),
                hm.has_properties({'group_id': group_id, 'role_id': role.id, ConstraintTypes.firm_id: FirmId.YANDEX_OOO}),
                hm.has_properties({'group_id': group_id, 'role_id': role.id, ConstraintTypes.firm_id: new_firm_id}),
            ]),
        )

    def test_add_role_with_new_client_constraints(self, session):
        group_id = 666

        client_old_1 = create_client(session)
        client_old_2 = create_client(session)
        client_new_1 = create_client(session)
        client_new_2 = create_client(session)
        client_new_3 = create_client(session)

        # чисто тестовый случай, т.к. в реальности не может быть у group_id несколько групп клиентов
        client_batch_old_1 = ob.RoleClientGroupBuilder.construct(session, clients=[client_old_1]).client_batch_id
        client_batch_old_2 = ob.RoleClientGroupBuilder.construct(session, clients=[client_old_1, client_old_2]).client_batch_id
        client_batch_new = ob.RoleClientGroupBuilder.construct(session, clients=[client_new_1, client_new_2, client_new_3]).client_batch_id

        role = create_role(session, ('Perm', {ConstraintTypes.client_batch_id: None}))
        passport = create_passport(session, (role, None, client_batch_old_1))

        session.add_all([
            mapper.PassportGroup(passport=passport, group_id=group_id),
            mapper.RoleGroup(session, group_id=group_id, role=role, client_batch_id=client_batch_old_2),
        ])
        session.flush()

        mapper.RoleGroup.set_roles(
            session,
            object_id=group_id,
            roles_definitions=[
                (role, {ConstraintTypes.client_batch_id: client_batch_new}),
            ],
        )

        passport_roles = passport.passport_roles
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(*[
                hm.has_properties({'group_id': None, 'role_id': RoleName.CLIENT, 'client_ids': hm.empty()}),
                hm.has_properties({'group_id': None, 'role_id': role.id, 'client_ids': hm.contains(client_old_1.id),
                                   ConstraintTypes.client_batch_id: client_batch_old_1}),
                hm.has_properties({'group_id': group_id, 'role_id': role.id, 'client_ids': hm.contains_inanyorder(client_new_1.id, client_new_2.id, client_new_3.id),
                                   ConstraintTypes.client_batch_id: client_batch_new}),
            ]),
        )

    def test_wrong_object(self, session):
        with pytest.raises(exc.EXCEPTION) as exc_info:
            mapper.RolePassport.set_roles(
                session,
                object_id=1234,
                roles_definitions=[],
            )
        assert exc_info.value.msg == 'Invalid parameter for function: Must be clearly defined'

    def test_delete_role_w_related_role_clients(self, session):
        client_1 = create_client(session)
        client_2 = create_client(session)

        client_batch_id = ob.RoleClientGroupBuilder.construct(session, clients=[client_1, client_2]).client_batch_id

        role = create_role(session, ('Perm', {ConstraintTypes.client_batch_id: None}))
        passport = create_passport(
            session,
            (role, None, client_batch_id)
        )
        passport.set_roles([])

        role_clients = (
            session.query(mapper.RoleClient)
            .filter(
                mapper.RoleClient.client_batch_id == client_batch_id,
                mapper.RoleClient.client_id.in_([client_1.id, client_2.id]),
            )
            .all()
        )
        client_group = session.query(mapper.RoleClientGroup).getone(client_batch_id=client_batch_id)
        assert len(role_clients) == 2
        assert passport.passport_roles == []
        assert client_group.clients == [client_1, client_2]
        assert client_group.client_ids == [client_1.id, client_2.id]


class TestPassportRoles(object):

    def test_definition(self, session):
        group_id = ob.get_big_number()

        role1 = create_role(session, ('Perm1', {ConstraintTypes.firm_id: None}))
        role2 = create_role(session, ('Perm2', {ConstraintTypes.firm_id: None}))

        passport = create_passport(session, (role1, FirmId.YANDEX_OOO))
        session.add_all([
            mapper.PassportGroup(passport=passport, group_id=group_id),
            mapper.RoleGroup(session, group_id=group_id, role=role2, firm_id=FirmId.CLOUD),
        ])
        session.flush()

        passport_roles = [pr.definition.as_dict() for pr in passport.passport_roles]
        hm.assert_that(
            passport_roles,
            hm.contains_inanyorder(
                hm.has_entries({'group_id': None, 'passport_id': passport.passport_id, 'role_id': RoleName.CLIENT, 'firm_id': None}),
                hm.has_entries({'group_id': None, 'passport_id': passport.passport_id, 'role_id': role1.id, 'firm_id': FirmId.YANDEX_OOO}),
                hm.has_entries({'group_id': group_id, 'passport_id': passport.passport_id, 'role_id': role2.id, 'firm_id': FirmId.CLOUD}),
            ),
        )

    def test_constraint_client_id(self, session, client):
        role1 = create_role(
            session,
            ('Perm1', {ConstraintTypes.client_batch_id: None}),
        )

        client_batch_id = ob.RoleClientBuilder.construct(session, client_id=client.id).client_batch_id
        passport = create_passport(session, (role1, FirmId.YANDEX_OOO, client_batch_id))
        session.flush()

        passport_roles = passport.passport_roles
        hm.assert_that(
            [pr.definition.as_dict() for pr in passport_roles],
            hm.contains_inanyorder(
                hm.has_entries({'group_id': None, 'passport_id': passport.passport_id, 'role_id': RoleName.CLIENT, 'firm_id': None, 'client_batch_id': None}),
                hm.has_entries({'group_id': None, 'passport_id': passport.passport_id, 'role_id': role1.id, 'firm_id': FirmId.YANDEX_OOO, 'client_batch_id': client_batch_id}),
            ),
        )

        filter_ = lambda r: r.role_id == role1.id
        passport_role,  = filter(filter_, passport_roles)
        real_passport_role,  = filter(filter_, passport.real_passport_roles)

        for p_role in [passport_role, real_passport_role]:
            hm.assert_that(
                p_role.constraint_values,
                has_exact_entries({
                    ConstraintTypes.firm_id: hm.contains(FirmId.YANDEX_OOO),
                    ConstraintTypes.client_batch_id: hm.contains(client_batch_id),
                }),
            )
            assert p_role.client_ids == [client.id]

        del passport_role.client_ids[0]
        session.flush()
        assert session.query(mapper.RoleClient).filter_by(client_batch_id=client_batch_id).count() == 1  # связь view_only
        assert passport_role.clients == []
        assert passport_role.client_ids == []

    def test_constraint_client_id_for_group(self, session, client):
        group_id = 666
        firm_id = FirmId.YANDEX_OOO

        role = create_role(
            session,
            ('Perm1', {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}),
        )
        passport = create_passport(session)

        client_batch_id = ob.RoleClientBuilder.construct(session, group_id=group_id, client_id=client.id).client_batch_id
        session.add(mapper.PassportGroup(passport_id=passport.passport_id, group_id=group_id))
        group_role = mapper.RoleGroup(group_id=group_id, firm_id=firm_id, role_id=role.id, client_batch_id=client_batch_id)
        session.add(group_role)
        session.flush()

        passport_roles = passport.passport_roles
        hm.assert_that(
            [pr.definition.as_dict() for pr in passport_roles],
            hm.contains(
                hm.has_entries({'group_id': None, 'passport_id': passport.passport_id, 'role_id': RoleName.CLIENT, 'firm_id': None, 'client_batch_id': None}),
                hm.has_entries({'group_id': group_id, 'passport_id': passport.passport_id, 'role_id': role.id, 'firm_id': firm_id, 'client_batch_id': client_batch_id}),
            ),
        )
        hm.assert_that(
            passport_roles,
            hm.contains(
                hm.has_properties({'group_id': None, 'role_id': RoleName.CLIENT, 'firm_id': None, 'client_batch_id': None, 'client_ids': []}),
                hm.has_properties({'group_id': group_id, 'role_id': role.id, 'firm_id': firm_id, 'client_batch_id': client_batch_id, 'client_ids': [client.id]}),
            ),
        )
