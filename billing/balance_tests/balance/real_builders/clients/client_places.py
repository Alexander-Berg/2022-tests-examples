# -*- coding: utf-8 -*-

from balance import balance_steps
from btestlib.constants import PlaceType

def test_place():
    client_id = balance_steps.ClientSteps.create()
    balance_steps.PartnerSteps.create_partner_place(client_id, place_type=PlaceType.RSYA,
                                            url='test-hermione-1.com', internal_type=0)
    return client_id