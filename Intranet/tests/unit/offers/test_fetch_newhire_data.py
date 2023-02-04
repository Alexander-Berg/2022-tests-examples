import pytest

from intranet.femida.src.offers.newhire.serializers import NewhireOfferBaseSerializer
from intranet.femida.src.vacancies.choices import VACANCY_TYPES

from intranet.femida.tests import factories as f


@pytest.mark.parametrize('vacancy_type, result', (
    (VACANCY_TYPES.new, False),
    (VACANCY_TYPES.replacement, False),
    (VACANCY_TYPES.pool, False),
    (VACANCY_TYPES.internship, True),
))
def test_correct_internship_end_date_for_newhire(vacancy_type, result):
    vacancy = f.VacancyFactory(type=vacancy_type)
    offer = f.OfferFactory(vacancy=vacancy)
    data = NewhireOfferBaseSerializer.serialize(offer)
    assert ('date_completion_internship' in data) == result


def test_correct_abc_services_for_newhire():
    offer = f.OfferFactory()
    active_service = f.ServiceFactory()
    deleted_service = f.ServiceFactory(is_deleted=True)
    offer.abc_services.set([active_service, deleted_service])
    data = NewhireOfferBaseSerializer.serialize(offer)
    assert data['abc_services'] == [active_service.group_id]
