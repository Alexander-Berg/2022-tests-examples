from django.test import SimpleTestCase as TestCase

from manual_offers_editor.views.vos_offer import OfferProps
from .forms import Form

sample = {
    "offer_id": "i4m7h1f94fq4h",
    "oem": "someoem",
    "props": {"is_new": True, "contract": False},
    "compatibility": [{}],
    "checked": False,
}


class TestForm(TestCase):
    def test__parse_offer(self):
        provider = object()
        form = Form(category=None, brand=None, brand_model=None, offers_provider=provider, **sample)
        assert form.offer_id == "i4m7h1f94fq4h"
        assert form.props == OfferProps([], **{"is_new": True, "contract": False})
        assert form.compatibility == [{}]
        assert form.oem == "someoem"

    def test_validate(self):
        ...
