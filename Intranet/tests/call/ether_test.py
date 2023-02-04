import pytest
from unittest.mock import patch

from django.conf import settings

from intranet.vconf.src.call.ether import get_ether_id
from intranet.vconf.tests.call.factories import ConferenceCallFactory, CallTemplateFactory

from intranet.vconf.src.call.ether import HURAL_ETHER_ID, EtherId

pytestmark = pytest.mark.django_db


def test_get_ether_id_hural():
    template = CallTemplateFactory(id=settings.HURAL_TEMPLATE_ID)
    call = ConferenceCallFactory(template=template)
    assert get_ether_id(call) == HURAL_ETHER_ID


def test_get_ether_id():
    ids = [
        EtherId('a', 'b'),
        EtherId('c', 'd'),
        EtherId('e', 'f'),
    ]
    with patch('intranet.vconf.src.call.ether.ETHER_IDS', ids):
        for ether_id in ids[1:]:
            ConferenceCallFactory(
                ether_back_id=ether_id.back_id,
                ether_front_id=ether_id.front_id,
            )
        call = ConferenceCallFactory()

        assert get_ether_id(call) == ids[0]
