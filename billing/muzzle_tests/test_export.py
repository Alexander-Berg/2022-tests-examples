# -*- coding: utf-8 -*-
import pytest

from balance import exc, constants as cst
from muzzle.api import export as export_api
from tests import object_builder as ob


class TestApiReexport(object):

    def get_invalid_reexport_name(self, _session):
        return 'invalid_name', 1

    def get_hidden_person(self, session):
        person = ob.PersonBuilder(hidden=True).build(session).obj
        return 'person', person.id

    def get_hidden_invoice(self, session):
        inv = ob.InvoiceBuilder(hidden=2).build(session).obj
        return 'invoice', inv.id

    def get_unapproved_repayment_invoice(self, session):
        inv = ob.InvoiceBuilder().build(session).obj
        inv.status_id = cst.InvoiceStatusId.PRELIMINARY
        return 'invoice', inv.id

    def get_hidden_act(self, session):
        qty = 1
        order = ob.OrderBuilder().build(session).obj
        inv = ob.InvoiceBuilder(
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    rows=[ob.BasketItemBuilder(order=order, quantity=qty)])
            )
        ).build(session).obj
        inv.turn_on_rows()
        order.calculate_consumption(
            dt=session.now(),
            shipment_info={order.shipment_type: qty},
        )
        act, = inv.generate_act(force=True)
        act.hide()
        return 'act', act.id
