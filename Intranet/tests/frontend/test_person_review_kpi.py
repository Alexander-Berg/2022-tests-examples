from tests import helpers
from review.shortcuts import const


def test_get_kpi(client, person_builder, person_review_builder, kpi_builder):
    person_review = person_review_builder(status=const.PERSON_REVIEW_STATUS.ANNOUNCED)
    kpi = kpi_builder(person_review=person_review)

    result = helpers.get_json(
        client=client,
        login=person_review.person.login,
        path='/frontend/person-reviews/{}/kpi/'.format(person_review.id),
    )

    assert 'kpi' in result
    assert result['kpi'] == [{
        'name': kpi.name,
        'year': str(kpi.year),
        'q': 'Q{}'.format(kpi.quarter),
        'percent': kpi.percent,
        'weight': kpi.weight,
    }]


def test_get_kpi_404(client, person_builder, person_review_builder, kpi_builder):
    person_review = person_review_builder()
    kpi_builder(person_review=person_review)

    helpers.get(
        client=client,
        login=person_review.person.login,
        path='/frontend/person-reviews/{}/kpi/'.format(person_review.id),
        status_code=404,
    )
