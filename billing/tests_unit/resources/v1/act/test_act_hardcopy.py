# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import cPickle as pickle
import datetime as dt
import http.client as http
import mock
import pytest
import hamcrest as hm

from balance import mapper, constants as cst
import butils.dbhelper.helper
from brest.core.tests import security
from yb_snout_api.resources.v1.act.enums import ActPaymentType
from yb_snout_api.utils.ma_fields import DT_FMT
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role
import tests.object_builder as ob


@pytest.mark.smoke
class TestCaseActExportEmail(TestCaseApiAppBase):
    BASE_API = u'/v1/act/send/hardcopy'

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perms, who_is, res',
        [
            ([], 'owner', http.OK),
            ([cst.PermissionCode.SEND_ACTS], 'admin', http.OK),
            ([], 'admin', http.FORBIDDEN),
            ([], 'client', http.FORBIDDEN),
        ],
        ids=lambda x: str(x),
    )
    def test_access(self, perms, who_is, res):
        start_dt = dt.datetime.now()
        roles = [ob.create_role(self.test_session, *perms)]
        if who_is == 'admin':
            roles.append(create_admin_role())
        security.set_roles(roles)
        client = create_client()
        security.set_passport_client(client if who_is == 'owner' else None)
        act = create_act(client=client, extra_params={"person": {"delivery_type": 3}})
        response = self.test_client.secure_post(
            self.BASE_API,
            data={'act_ids': [act.id]},
            is_admin=(who_is == 'admin'),
        )
        hm.assert_that(response.status_code, hm.equal_to(res))
        if res == http.OK:
            msg = self.test_session.query(mapper.EmailMessage).filter(
                mapper.EmailMessage.opcode == cst.GENERIC_MAKO_CREATOR_MESSAGE_OPCODE,
                mapper.EmailMessage.object_id == 0,
                mapper.EmailMessage.passport_id == self.test_session.passport.passport_id,
                mapper.EmailMessage.dt > start_dt,
            ).one_or_none()
            hm.assert_that(msg, hm.not_none())
            template_params = pickle.loads(msg.data)[-1]
            hm.assert_that(template_params['person'].primary_key[0], hm.equal_to(act.invoice.person_id))
            hm.assert_that(template_params['client'].primary_key[0], hm.equal_to(act.invoice.person.client_id))
            hm.assert_that(template_params['acts'], hm.has_length(1))
            hm.assert_that(template_params['acts'][0][0].primary_key[0], hm.equal_to(act.id))

    @pytest.mark.parametrize("hardcopy_type", [
        'manual',
        'oebs'
    ])
    def test_send_hardcopy(self, hardcopy_type):
        start_dt = dt.datetime.now()
        client = create_client(passport=self.test_session.passport)
        acts = [
            create_act(
                client=client,
                extra_params={"person": {"delivery_type": (i if hardcopy_type == 'manual' else 0)}}
            )
            for i in range(2, 5)
        ]
        acts.append(
            create_act(
                client=client,
                person=acts[0].invoice.person
            )
        )
        persons = set(act.invoice.person for act in acts)
        oebs_session = mock.MagicMock()
        with mock.patch('butils.dbhelper.helper.DbHelper.create_raw_connection', oebs_session):
            response = self.test_client.secure_post(
                self.BASE_API,
                data={'act_ids': [act.id for act in acts]},
                is_admin=False
            )

        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        if hardcopy_type == 'manual':
            hm.assert_that(oebs_session.call_count, hm.equal_to(0))
            msg = self.test_session.query(mapper.EmailMessage).filter(
                mapper.EmailMessage.opcode == cst.GENERIC_MAKO_CREATOR_MESSAGE_OPCODE,
                mapper.EmailMessage.object_id == 0,
                mapper.EmailMessage.passport_id == self.test_session.passport.passport_id,
                mapper.EmailMessage.dt > start_dt,
            ).all()
            hm.assert_that(msg, hm.has_length(len(persons)))
        else:
            hm.assert_that(oebs_session.call_count, hm.equal_to(len(persons)))
            hm.assert_that(oebs_session.call_args.kwargs, hm.equal_to({'backend_id': 'oebs'}))
            hm.assert_that(
                oebs_session.mock_calls,
                hm.has_items(
                    hm.has_property(
                        'args',
                        hm.has_item(
                            hm.contains_string('XXCMN_INTAPI_BILLS_PKG.APPS_INITIALIZE')
                        ),
                    ),
                    hm.has_property(
                        'args',
                        hm.has_item(
                            hm.contains_string('XXCMN_INTAPI_BILLS_PKG.load_akts')
                        ),
                    ),
                )
            )
            oebs_act_request = self.test_session.query(mapper.OebsActRequest).filter(
                mapper.OebsActRequest.act_id == act.id
            ).one_or_none()
            hm.assert_that(oebs_act_request, hm.not_none())
