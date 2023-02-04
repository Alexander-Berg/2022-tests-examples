import pytest

from django.conf import settings
from django.test import override_settings

from intranet.femida.src.hire_orders.controllers import HireOrderController
from intranet.femida.src.offers.choices import WORK_PLACES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('work_place, city_name', (
    (WORK_PLACES.office, 'Los Office'),
    (WORK_PLACES.home, 'Saint Home'),
))
@override_settings(CITY_HOMEWORKER_ID=100500)
def test_vacancy_cleaned_data_cities(simple_hire_order, work_place, city_name):
    f.CityFactory(id=settings.CITY_HOMEWORKER_ID, name_en='Saint Home')
    office = f.OfficeFactory(city__name_en='Los Office')

    offer_data = simple_hire_order.raw_data['offer']
    offer_data['work_place'] = work_place
    offer_data['office'] = office.id
    simple_hire_order.save()

    ctl = HireOrderController(simple_hire_order)
    cities = ctl.cleaned_vacancy_data['cities']
    assert len(cities) == 1
    assert cities[0].name_en == city_name
