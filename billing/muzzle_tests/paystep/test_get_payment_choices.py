# -*- coding: utf-8 -*-

import datetime

import mock
import pytest
from tests import object_builder as ob
from balance.constants import PersonCategoryCodes
from balance.utils.xml2json import xml2json_auto

from tests.muzzle_tests.paystep.paystep_common import (create_client, create_order, create_request, create_person)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


@pytest.mark.single_account
class TestWithSingleAccount(object):
    # noinspection PyMethodMayBeStatic
    def get_person_choices(self, client, session, muzzle_logic):
        order = create_order(session, client)
        request = create_request(session, client=client, order=order)

        answer = muzzle_logic.get_payment_choices(
            session, state_obj=None, request_obj=None,
            request_id=request.id,
            invoice_id=None,
            phrase_manager=mock.Mock()
        )

        json_answer = xml2json_auto(answer)
        person_choices = set()
        for pcp_info in json_answer['persons']['pcp-info']:
            person = pcp_info['person']
            # id == None означает, что такого плательщика можно создать
            person_choices.add((person['id'], person['type']))

        return person_choices

    def test_creation_of_first_individual_is_allowed(self, session, muzzle_logic):
        client = create_client(session, with_single_account=True)
        person_choices = self.get_person_choices(client, session, muzzle_logic)
        assert (None, PersonCategoryCodes.russia_resident_individual) in person_choices
        assert (None, PersonCategoryCodes.russia_resident_legal_entity) in person_choices

    def test_creation_of_second_individual_is_forbidden(self, session, muzzle_logic):
        client = create_client(session, with_single_account=True)
        existing_individual = create_person(session, client=client)
        person_choices = self.get_person_choices(client, session, muzzle_logic)
        assert (None, existing_individual.type) not in person_choices
        assert (str(existing_individual.id), existing_individual.type) in person_choices
        assert (None, PersonCategoryCodes.russia_resident_legal_entity) in person_choices

    def test_creation_of_second_individual_is_allowed_without_single_account(
        self, session, muzzle_logic
    ):
        client = create_client(session)
        existing_individual = create_person(session, client)
        person_choices = self.get_person_choices(client, session, muzzle_logic)
        assert (None, existing_individual.type) in person_choices
        assert (str(existing_individual.id), existing_individual.type) in person_choices
        assert (None, PersonCategoryCodes.russia_resident_legal_entity) in person_choices


def test_shop_35_invoice(session, muzzle_logic):
    client = ob.ClientBuilder.construct(session)
    order = create_order(session, client, service_id=35)
    request = create_request(session, client=client, order=order)
    request.firm_id = 1
    invoice = ob.InvoiceBuilder.construct(session, request=request)
    session.flush()
    answer = muzzle_logic.get_payment_choices(
        session, state_obj=None, request_obj=None,
        request_id=None,
        invoice_id=invoice.id,
        phrase_manager=mock.Mock()
    )

    json_answer = xml2json_auto(answer)
    pass
