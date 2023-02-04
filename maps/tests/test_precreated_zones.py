import pytest

from maps.b2bgeo.reference_book.lib.models import Zone, db

pytestmark = [pytest.mark.usefixtures("app")]


@pytest.mark.parametrize("number", ["inside_ttk", "public_inside_ttk"])
def test_inside_ttk_zone(number):
    company_zone = db.session.query(Zone).filter(Zone.number == number).one()

    assert company_zone.color_fill == '#1bad03'
    assert company_zone.color_edge == '#1bad03'


@pytest.mark.parametrize("number", ["inside_mkad", "public_inside_mkad"])
def test_inside_mkad_zone(number):
    company_zone = db.session.query(Zone).filter(Zone.number == number).one()

    assert company_zone.color_fill == '#ed4543'
    assert company_zone.color_edge == '#ed4543'
