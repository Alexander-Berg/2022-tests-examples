# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm
from datetime import datetime, timedelta

from balance import constants as cst, mapper
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.edo import create_edo_offer


@pytest.fixture(name='edo_viewer_role')
def create_edo_viewer_role():
    return create_role((cst.PermissionCode.EDO_VIEWER, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='client_viewer_role')
def create_client_viewer_role():
    return create_role((cst.PermissionCode.VIEW_CLIENTS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='person_viewer_role')
def create_person_viewer_role():
    return create_role((cst.PermissionCode.VIEW_PERSONS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='contract_viewer_role')
def create_contract_viewer_role():
    return create_role((cst.PermissionCode.VIEW_CONTRACTS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.mark.smoke
@pytest.mark.permissions
class TestCaseEdoPersonActualOffers(TestCaseApiAppBase):
    BASE_API = u'/v1/edo/person/actual-offers'

    def test_own_client(self, client_role, person):
        security.set_roles([client_role])
        security.set_passport_client(person.client)
        res = self.test_client.get(self.BASE_API, {'client_id': person.client_id}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.contains(
                hm.has_entry('person_id', person.id),
            ),
        )

    @pytest.mark.parametrize('match_client', [True, False])
    def test_client_constraints(
        self,
        admin_role,
        edo_viewer_role,
        person_viewer_role,
        client,
        edo_offer,
        match_client,
    ):
        client_batch = create_role_client(client=client if match_client else None).client_batch_id
        roles = [
            admin_role,
            edo_viewer_role,
            (person_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch}),
        ]
        security.set_roles(roles)

        person = create_person(client)
        person.inn = edo_offer.person_inn
        person.kpp = edo_offer.person_kpp
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': person.client_id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        if match_client:
            firm = edo_offer.firm
            person_match = hm.contains(
                hm.has_entries({
                    'person_id': person.id,
                    'person_inn': person.inn,
                    'person_kpp': person.kpp,
                    'person_name': person.name,
                    'actual_edo_offers': hm.contains(
                        hm.has_entries({
                            'edo_offer': hm.has_entries({
                                'edo_type':   edo_offer.edo_type.name,
                                'firm':       hm.has_entries({
                                                  'id':   firm.id,
                                                  'name': firm.fixed_title
                                              }),
                                'status':     edo_offer.status
                            }),
                            'total_count': 1,
                        }),
                    ),
                }),
            )
        else:
            person_match = hm.empty()

        tmp = response.get_json().get('data', {})
        hm.assert_that(
            response.get_json().get('data', {}),
            person_match,
        )

    @pytest.mark.parametrize(
        'match_client, ans',
        [
            pytest.param(None, http.FORBIDDEN, id='wo role'),
            pytest.param(True, http.OK, id='w right client'),
            pytest.param(False, http.FORBIDDEN, id='w wrong role'),
        ],
    )
    def test_edo_viewer_role(
        self,
        client,
        admin_role,
        edo_viewer_role,
        person_viewer_role,
        edo_offer,
        match_client,
        ans,
    ):
        roles = [admin_role, person_viewer_role]
        if match_client is not None:
            client_batch = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((edo_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch}))
        security.set_roles(roles)

        person = create_person(client=client)
        person.inn = edo_offer.person_inn
        person.kpp = edo_offer.person_kpp
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': client.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(ans))

    @pytest.mark.parametrize('person_kpp', ['987654321', None])
    def test_complex(
        self,
        client,
        admin_role,
        edo_viewer_role,
        person_viewer_role,
        person_kpp
    ):
        client_batch = create_role_client(client=client).client_batch_id
        roles = [
            admin_role,
            edo_viewer_role,
            (person_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch}),
        ]
        security.set_roles(roles)

        offers = [
            create_edo_offer(active_start_date=datetime(2021, 1, 1),
                             active_end_date=datetime(2021, 1, 2), edo_status='WAITING_TO_BE_SEND', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2),
                             active_end_date=datetime(2021, 1, 3), edo_status='INVITED_BY_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 1),
                             active_end_date=datetime(2021, 1, 2), edo_status='INVITED_BY_ME', default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 3),
                             active_end_date=None, edo_status='FRIENDS', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2),
                             active_end_date=None, edo_status='REJECTS_ME', default_flag=False),
        ]

        person = create_person(client)
        person.inn = '987654321'
        person.kpp = person_kpp

        for offer in offers:
            if offer is not None:
                offer.person_inn = person.inn
                offer.person_kpp = person.kpp

        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': person.client_id},
        )

        edo_offer = offers[3]
        firm = edo_offer.firm
        hm.assert_that(
            response.get_json().get('data', {}),
            hm.contains(
                hm.has_entries({
                    'person_id': person.id,
                    'person_inn': person.inn,
                    'person_kpp': person.kpp,
                    'person_name': person.name,
                    'actual_edo_offers': hm.contains(
                        hm.has_entries({
                            'edo_offer': hm.has_entries({
                                'edo_type':   edo_offer.edo_type.name,
                                'firm':       hm.has_entries({
                                                  'id':   firm.id,
                                                  'name': firm.fixed_title
                                              }),
                                'status':     edo_offer.status
                            }),
                            'total_count': 3,
                        }),
                    ),
                }),
            ),
        )

    @pytest.mark.parametrize('person_kpp', ['987654321', None])
    def test_multiple_firms(
        self,
        client,
        admin_role,
        edo_viewer_role,
        person_viewer_role,
        person_kpp
    ):
        client_batch = create_role_client(client=client).client_batch_id
        roles = [
            admin_role,
            edo_viewer_role,
            (person_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch}),
        ]
        security.set_roles(roles)

        offers = [
            # Yandex
            create_edo_offer(active_start_date=datetime(2021, 1, 1),
                             active_end_date=datetime(2021, 1, 2), edo_status='WAITING_TO_BE_SEND', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2),
                             active_end_date=datetime(2021, 1, 3), edo_status='INVITED_BY_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 1),
                             active_end_date=datetime(2021, 1, 2), edo_status='INVITED_BY_ME', default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 3),
                             active_end_date=None, edo_status='FRIENDS', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2),
                             active_end_date=None, edo_status='REJECTS_ME', default_flag=False),

            # Yandex (ex. Market)
            create_edo_offer(active_start_date=datetime(2021, 1, 2), firm_id=111,
                             active_end_date=datetime(2021, 1, 3), edo_status='REJECTS_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 1), firm_id=111,
                             active_end_date=datetime(2021, 1, 2), edo_status='INVITED_BY_ME',
                             default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 3), firm_id=111,
                             active_end_date=None, edo_status='FRIENDS', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2), firm_id=111,
                             active_end_date=None, edo_status='FRIENDS',
                             default_flag=False),
        ]

        person = create_person(client)
        person.inn = '987654321'
        person.kpp = person_kpp

        for offer in offers:
            if offer is not None:
                offer.person_inn = person.inn
                offer.person_kpp = person.kpp

        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': person.client_id},
        )

        actual_edo_offers = [offers[3], offers[7]]
        firms = [offer.firm for offer in actual_edo_offers]
        total_counts = [3, 2]
        hm.assert_that(
            response.get_json().get('data', {}),
            hm.contains(
                hm.has_entries({
                    'person_id': person.id,
                    'person_inn': person.inn,
                    'person_kpp': person.kpp,
                    'person_name': person.name,
                    'actual_edo_offers': hm.contains_inanyorder(*[
                        hm.has_entries({
                            'edo_offer': hm.has_entries({
                                'edo_type':   actual_edo_offers[i].edo_type.name,
                                'firm':       hm.has_entries({
                                                  'id':   firms[i].id,
                                                  'name': firms[i].fixed_title
                                              }),
                                'status':     actual_edo_offers[i].status
                            }),
                            'total_count': total_counts[i],
                        })
                        for i in range(len(actual_edo_offers))
                    ]),
                }),
            ),
        )

    @pytest.mark.parametrize('person_kpp', ['987654321', None])
    def test_filter_incident_rows(
        self,
        client,
        admin_role,
        edo_viewer_role,
        person_viewer_role,
        person_kpp
    ):
        """
        rows inserted by hand to fix oebs incidents by oebs team have date greater or equal to actual
        """
        client_batch = create_role_client(client=client).client_batch_id
        roles = [
            admin_role,
            edo_viewer_role,
            (person_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch}),
        ]
        security.set_roles(roles)

        offers = [
            # Yandex
            create_edo_offer(active_start_date=datetime(2021, 1, 1), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 2), edo_status='WAITING_TO_BE_SEND', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 3), edo_status='INVITED_BY_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 1), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 2), edo_status='INVITED_BY_ME', default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 3),
                             active_end_date=None, edo_status='FRIENDS', default_flag=True),
            # incident row
            create_edo_offer(active_start_date=datetime(2021, 1, 3), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 5), edo_status='FRIENDS', default_flag=True),
            # incident row
            create_edo_offer(active_start_date=datetime(2021, 1, 5), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 6), edo_status='REJECTED_BY_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2),
                             active_end_date=None, edo_status='REJECTS_ME', default_flag=False),

            # Yandex (ex. Market)
            create_edo_offer(active_start_date=datetime(2021, 1, 2), firm_id=111, enabled_flag=False,
                             active_end_date=datetime(2021, 1, 3), edo_status='REJECTS_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 1), firm_id=111,
                             active_end_date=datetime(2021, 1, 2), edo_status='INVITED_BY_ME',
                             enabled_flag=False, default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 3), firm_id=111,
                             active_end_date=None, edo_status='FRIENDS', default_flag=True, enabled_flag=True),
            # incident row
            create_edo_offer(active_start_date=datetime(2021, 1, 3), firm_id=111, default_flag=True,
                             enabled_flag=False, active_end_date=datetime(2021, 1, 7), edo_status='FRIENDS'),
            # incident row
            create_edo_offer(active_start_date=datetime(2021, 1, 7), firm_id=111, default_flag=True,
                             enabled_flag=False, active_end_date=datetime(2021, 1, 9), edo_status='ERROR'),
            create_edo_offer(active_start_date=datetime(2021, 1, 2), firm_id=111,
                             active_end_date=None, edo_status='FRIENDS', default_flag=False),
        ]

        person = create_person(client)
        person.inn = '987654321'
        person.kpp = person_kpp

        for offer in offers:
            if offer is not None:
                offer.person_inn = person.inn
                offer.person_kpp = person.kpp

        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': person.client_id},
        )

        actual_edo_offers = [offers[3], offers[9]]
        firms = [offer.firm for offer in actual_edo_offers]
        total_counts = [3, 2]
        hm.assert_that(
            response.get_json().get('data', {}),
            hm.contains(
                hm.has_entries({
                    'person_id': person.id,
                    'person_inn': person.inn,
                    'person_kpp': person.kpp,
                    'person_name': person.name,
                    'actual_edo_offers': hm.contains_inanyorder(*[
                        hm.has_entries({
                            'edo_offer': hm.has_entries({
                                'edo_type':   actual_edo_offers[i].edo_type.name,
                                'firm':       hm.has_entries({
                                                  'id':   firms[i].id,
                                                  'name': firms[i].fixed_title
                                              }),
                                'status':     actual_edo_offers[i].status
                            }),
                            'total_count': total_counts[i],
                        })
                        for i in range(len(actual_edo_offers))
                    ]),
                }),
            ),
        )


@pytest.mark.smoke
@pytest.mark.permissions
class TestCaseEdoPersonOffersHistory(TestCaseApiAppBase):
    BASE_API = u'/v1/edo/person/offers-history'

    def test_own_client(self, client_role, person, edo_offer):
        person.inn = edo_offer.person_inn
        person.kpp = edo_offer.person_kpp
        self.test_session.flush()

        security.set_roles([client_role])
        security.set_passport_client(person.client)
        params = {
            'person_id': person.id,
        }
        params['firm_id'] = edo_offer.firm_id
        res = self.test_client.get(
            self.BASE_API,
            params=params,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.contains(
                hm.has_entries({
                    'edo_type': edo_offer.edo_type.name,
                    'from_dt':  edo_offer.active_start_date.isoformat(),
                }),
            ),
        )

    @pytest.mark.parametrize('get_by_firm_id', [True, False])
    @pytest.mark.parametrize(
        'match_client, ans',
        [
            (None, http.FORBIDDEN),
            (True, http.OK),
            (False, http.FORBIDDEN),
        ],
    )
    def test_permission(
        self,
        client,
        admin_role,
        edo_viewer_role,
        edo_offer,
        match_client,
        ans,
        get_by_firm_id,
    ):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((edo_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        person = create_person(client)
        person.inn = edo_offer.person_inn
        person.kpp = edo_offer.person_kpp
        self.test_session.flush()

        params = {
            'person_id': person.id,
        }
        if get_by_firm_id:
            params['firm_id'] = edo_offer.firm_id
        else:
            params['firm_inn'] = edo_offer.firm.inn
            params['firm_kpp'] = edo_offer.firm.kpp
        response = self.test_client.get(
            self.BASE_API,
            params=params,
        )
        hm.assert_that(response.status_code, hm.equal_to(ans))

        if match_client:
            hm.assert_that(
                response.get_json().get('data', {}),
                hm.contains(
                    hm.has_entries({
                        'edo_type': edo_offer.edo_type.name,
                        'from_dt':  edo_offer.active_start_date.isoformat(),
                    }),
                ),
            )

    @pytest.mark.parametrize('get_by_firm_id', [True, False])
    @pytest.mark.parametrize('person_kpp', ['987654321', None])
    def test_complex(self, client, admin_role, edo_viewer_role, get_by_firm_id, person_kpp):
        roles = [
            admin_role,
            (edo_viewer_role, {cst.ConstraintTypes.client_batch_id: create_role_client(client=client).client_batch_id})
        ]
        security.set_roles(roles)

        offers = [
            create_edo_offer(active_start_date=datetime(2021, 1, 1), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 2), edo_status='WAITING_TO_BE_SEND',
                             default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2),
                             active_end_date=datetime(2021, 1, 3), edo_status='INVITED_BY_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 1),
                             active_end_date=datetime(2021, 1, 2), enabled_flag=False,
                             edo_status='INVITED_BY_ME', default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 3), active_end_date=None,
                             edo_status='FRIENDS', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2), active_end_date=None,
                             edo_status='REJECTS_ME', default_flag=False),
        ]

        person = create_person(client)
        person.inn = '987654321'
        person.kpp = person_kpp

        for offer in offers:
            if offer is not None:
                offer.person_inn = person.inn
                offer.person_kpp = person.kpp

        self.test_session.flush()

        params = {
            'person_id': person.id,
        }
        if get_by_firm_id:
            params['firm_id'] = offers[0].firm_id
        else:
            params['firm_inn'] = offers[0].firm.inn
            params['firm_kpp'] = offers[0].firm.kpp
        response = self.test_client.get(
            self.BASE_API,
            params=params,
        )

        hm.assert_that(
            response.get_json().get('data', {}),
            hm.contains(*[
                hm.has_entries({
                    'edo_type': offer.edo_type.name,
                    'from_dt': offer.active_start_date.isoformat(),
                    'to_dt': offer.active_end_date.isoformat() if getattr(offer, "active_end_date", None) is not None else None,
                })
                for offer in offers[::-1] if offer is not None and getattr(offer, "default_flag", True)
            ]),
        )

    @pytest.mark.parametrize('get_by_firm_id', [True,  False])
    @pytest.mark.parametrize('person_kpp', ['987654321', None])
    def test_filter_incident_rows(self, client, admin_role, edo_viewer_role, get_by_firm_id, person_kpp):
        roles = [
            admin_role,
            (edo_viewer_role, {cst.ConstraintTypes.client_batch_id: create_role_client(client=client).client_batch_id})
        ]
        security.set_roles(roles)

        offers = [
            create_edo_offer(active_start_date=datetime(2021, 1, 1),
                             active_end_date=datetime(2021, 1, 2), enabled_flag=False,
                             edo_status='INVITED_BY_ME', default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 1), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 2), edo_status='WAITING_TO_BE_SEND', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 3), edo_status='INVITED_BY_ME', default_flag=True),
            create_edo_offer(active_start_date=datetime(2021, 1, 2), active_end_date=None,
                             edo_status='REJECTS_ME', default_flag=False),
            create_edo_offer(active_start_date=datetime(2021, 1, 3), active_end_date=None,
                             edo_status='FRIENDS', default_flag=True),
            # incident row
            create_edo_offer(active_start_date=datetime(2021, 1, 3), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 5), edo_status='FRIENDS', default_flag=True),
            # incident row
            create_edo_offer(active_start_date=datetime(2021, 1, 5), enabled_flag=False,
                             active_end_date=datetime(2021, 1, 7), edo_status='ERROR', default_flag=True),
        ]

        person = create_person(client)
        person.inn = '987654321'
        person.kpp = person_kpp

        for offer in offers:
            if offer is not None:
                offer.person_inn = person.inn
                offer.person_kpp = person.kpp

        self.test_session.flush()

        params = {
            'person_id': person.id,
        }
        if get_by_firm_id:
            params['firm_id'] = offers[1].firm_id
        else:
            params['firm_inn'] = offers[1].firm.inn
            params['firm_kpp'] = offers[1].firm.kpp
        response = self.test_client.get(
            self.BASE_API,
            params=params,
        )

        hm.assert_that(
            response.get_json().get('data', {}),
            hm.contains(*[
                hm.has_entries({
                    'edo_type': offer.edo_type.name,
                    'from_dt':  offer.active_start_date.isoformat(),
                    'to_dt':    offer.active_end_date.isoformat() if getattr(offer, "active_end_date", None) is not None else None,
                })
                for offer in offers[:-2][::-1] if offer is not None and getattr(offer, "default_flag", True)
            ]),
        )

@pytest.mark.smoke
@pytest.mark.permissions
class TestCaseEdoPersonContracts(TestCaseApiAppBase):
    BASE_API = u'/v1/edo/person/contracts'

    @pytest.mark.parametrize(
        'match_client, ans',
        [
            pytest.param(None, http.FORBIDDEN, id='wo role'),
            pytest.param(True, http.OK, id='w right client'),
            pytest.param(False, http.FORBIDDEN, id='w wrong role'),
        ],
    )
    def test_edo_role(
        self,
        client,
        admin_role,
        edo_viewer_role,
        contract_viewer_role,
        edo_offer,
        match_client,
        ans,
    ):
        roles = [admin_role, contract_viewer_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((edo_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        person = create_person(client)
        person.inn = edo_offer.person_inn
        person.kpp = edo_offer.person_kpp
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': client.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(ans))

    def test_own_client(self, client_role, person, edo_offer):
        person.inn = edo_offer.person_inn
        person.kpp = edo_offer.person_kpp
        self.test_session.flush()

        contract = ob.ContractBuilder.construct(
            self.test_session,
            external_id='snout test %s' % ob.get_big_number(),
            client=person.client,
            person=person,
            edo_type=edo_offer.edo_type.ed_operator_code,
        )

        security.set_roles([client_role])
        security.set_passport_client(person.client)
        res = self.test_client.get(
            self.BASE_API,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.contains(
                hm.has_entry('contract_external_id', contract.external_id),
            ),
        )

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    def test_permission(
        self,
        client,
        admin_role,
        edo_viewer_role,
        contract_viewer_role,
        edo_offer,
        match_client,
    ):
        client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
        roles = [
            admin_role,
            edo_viewer_role,
            (contract_viewer_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        person = create_person(client)
        person.inn = edo_offer.person_inn
        person.kpp = edo_offer.person_kpp
        contracts = [
            ob.ContractBuilder.construct(
                self.test_session,
                external_id='snout test %s' % ob.get_big_number(),
                client=client,
                person=person,
                edo_type=edo_offer.edo_type.ed_operator_code,
            )
            for _i in range(2)
        ]
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': client.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        if match_client:
            edo_type = (
                self.test_session
                .query(mapper.common.EdoType)
                .getone(ed_operator_code=edo_offer.edo_type.ed_operator_code)
                .name
            )
            contracts_match = hm.contains_inanyorder(*[
                hm.has_entries({
                    'contract_external_id': c.external_id,
                    'edo_type_name': edo_type,
                    'firm_title': c.firm.fixed_title,
                    'from_dt': c.col0.dt.isoformat(),
                    'person_name': person.name,
                })
                for c in contracts
            ])
        else:
            contracts_match = hm.empty()
        hm.assert_that(
            response.get_json().get('data', {}),
            contracts_match,
        )
