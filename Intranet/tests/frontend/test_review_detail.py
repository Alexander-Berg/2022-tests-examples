# coding: utf-8
from review.core import const
from review.shortcuts import models
from django.utils.dateparse import parse_datetime
import arrow

from tests import helpers


REVIEW_URL = '/frontend/reviews/{id}/'
MOCKED_DATE = parse_datetime('2021-08-23T21:23:58.970460')


def test_404_if_review_doesnt_exist(client, person):
    non_existing_review_id = 100500
    assert not models.Review.objects.filter(id=non_existing_review_id).exists()

    helpers.get_json(
        client=client,
        path=REVIEW_URL.format(id=non_existing_review_id),
        login=person.login,
        expect_status=404,
    )


def test_403_if_review_doesnt_available(client, person, review):
    assert not models.ReviewRole.objects.filter(person=person).exists()
    assert not models.PersonReviewRole.objects.filter(person=person).exists()
    assert not models.DepartmentRole.objects.filter(person=person).exists()

    helpers.get_json(
        client=client,
        path=REVIEW_URL.format(id=review.id),
        login=person.login,
        expect_status=403,
    )


def test_get_goodies(
    client,
    review_goodie_builder,
    review_role_builder,
):
    superreviewer_role = review_role_builder(type=const.ROLE.REVIEW.ADMIN)
    review = superreviewer_role.review
    goodie = review_goodie_builder(review=review)
    response = helpers.get_json(
        client,
        path=REVIEW_URL.format(id=review.id),
        login=superreviewer_role.person.login
    )
    assert len(response['goodies']) == 1
    received_goodie = response['goodies'][0]
    checking_fields = [
        'id',
        'bonus',
        'goldstar',
        'level',
        'level_change',
        'mark',
        'options_rsu',
        'salary_change',
    ]
    non_equal = []
    for f in checking_fields:
        received = received_goodie[f]
        expecting = getattr(goodie, f)
        if f in ('bonus', 'salary_change'):
            received = float(received)
        elif f == 'goldstar':
            expecting = const.GOLDSTAR.VERBOSE[expecting]
        if expecting != received:
            non_equal.append((f, expecting, received))
    assert not non_equal


def test_get_product_schema_loaded(
    client,
    review_role_builder,
    review_builder,
):
    review = review_builder(product_schema_loaded=MOCKED_DATE)
    superreviewer_role = review_role_builder(type=const.ROLE.REVIEW.ADMIN, review=review)
    response = helpers.get_json(
        client,
        path=REVIEW_URL.format(id=review.id),
        login=superreviewer_role.person.login
    )

    received_date = parse_datetime(response['product_schema_loaded']).replace(tzinfo=None, microsecond=0)
    expected = MOCKED_DATE.replace(tzinfo=None, microsecond=0)
    assert received_date == expected


def test_get_kpi_loaded(
    client,
    review_role_builder,
):
    superreviewer_role = review_role_builder(type=const.ROLE.REVIEW.ADMIN)
    review = superreviewer_role.review

    helpers.update_model(review, kpi_loaded=MOCKED_DATE)

    response = helpers.get_json(
        client,
        path=REVIEW_URL.format(id=review.id),
        login=superreviewer_role.person.login
    )

    received_date = parse_datetime(response['kpi_loaded']).replace(tzinfo=None, microsecond=0)
    assert received_date == MOCKED_DATE.replace(microsecond=0, tzinfo=None)
