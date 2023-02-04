# coding: utf-8
from __future__ import unicode_literals

import pytest
from django.core.files.uploadedfile import SimpleUploadedFile
from mock import patch

from review.core import const
from review.gradient.models import UmbrellaPersonReview
from review.lib import serializers
from review.lib.file_properties import XlsFileProperties

from tests import helpers


VIEW_PATH = '/frontend/import_umbrellas/'
UPLOAD_PATH = VIEW_PATH + 'upload/'


def get_uploaded_file(contents, name='data_file.xlsx'):
    return SimpleUploadedFile(
        name=name,
        content=serializers.XLSSerializer.serialize(contents),
        content_type=XlsFileProperties.content_type,
    )


@patch('review.frontend.views.import_umbrellas.LOGINS_WHITELIST', ['allowed_login'])
@pytest.mark.parametrize(
    'yenv_type, login, http_status', (
        ('production', 'allowed_login', 200),
        ('production', 'denied_login', 400),
        ('testing', 'allowed_login', 200),
        ('testing', 'denied_login', 200)
    )
)
def test_import_umbrellas_view(client, person_builder, yenv_type, login, http_status):
    person = person_builder(login=login)

    with patch('yenv.type', yenv_type):
        helpers.get(
            client=client,
            path=VIEW_PATH,
            login=person.login,
            status_code=http_status,
        )


@patch('review.frontend.views.import_umbrellas.LOGINS_WHITELIST', ['test_login'])
@pytest.mark.parametrize(
    'yenv_type, http_status, errors', (
        ('production', 400, ['ACCESS DENIED']),
        ('testing', 200, None),
    )
)
def test_import_umbrellas_post_auth(client, person_builder, yenv_type, http_status, errors):
    invalid_login = person_builder(login='invalid_login').login

    with patch('yenv.type', yenv_type):
        result = helpers.post_multipart_data(
            client=client,
            path=UPLOAD_PATH,
            request={'data_file': get_uploaded_file({})},
            login=invalid_login,
            expect_status=http_status
        )

        assert result.get('errors', None) == errors


@patch('review.frontend.views.import_umbrellas.LOGINS_WHITELIST', ['superreviewer_login'])
def test_import_umbrellas_basic_import(
    client,
    person_review_builder,
    umbrella_builder,
    umbrella_person_review_builder,
    main_product_builder,
    main_product_review_builder,
    umbrella_review_builder,
    review_builder,
    person_builder,
):
    user = person_builder(
        login='superreviewer_login',
    )

    review = review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    person_reviews = [
        person_review_builder(review=review),
        person_review_builder(review=review),
        person_review_builder(review=review),
    ]

    main_product = main_product_builder(
        name='main_product',
    )

    umbrellas = [
        umbrella_builder(
            name='umbrella_0',
            main_product=main_product,
        ),

        umbrella_builder(
            name='umbrella_1',
            main_product=main_product,
        )
    ]

    umbrella_person_review_builder(
        umbrella=umbrellas[0],
        person_review=person_reviews[0],
    )

    umbrella_person_review_builder(
        umbrella=umbrellas[1],
        person_review=person_reviews[0],
    )

    upload_contents = [
        {
            'Логин': person_reviews[0].person.login,
            'VS': 'Новый продукт',
            'Зонтик': 'Новый зонт',
        },
        {
            'Логин': person_reviews[2].person.login,
            'VS': 'Другой новый продукт',
            'Зонтик': 'Новый зонт',
        }
    ]

    response = helpers.post_multipart_data(
        client=client,
        path=UPLOAD_PATH,
        request={'data_file': get_uploaded_file(upload_contents)},
        login=user.login,
    )

    assert 'message' in response
    assert 'errors' not in response

    assert UmbrellaPersonReview.objects.filter(
        person_review=person_reviews[1],
    ).count() == 0

    assert list(
        UmbrellaPersonReview.objects.filter(
            person_review=person_reviews[0],
        ).values_list('umbrella__name', 'umbrella__main_product__name')
    ) == [(upload_contents[0]['Зонтик'], upload_contents[0]['VS'])]

    assert list(
        UmbrellaPersonReview.objects.filter(
            person_review=person_reviews[2],
        ).values_list('umbrella__name', 'umbrella__main_product__name')
    ) == [(upload_contents[1]['Зонтик'], upload_contents[1]['VS'])]


@patch('review.frontend.views.import_umbrellas.LOGINS_WHITELIST', ['superreviewer_login'])
def test_import_umbrellas_with_review(
        client,
        person_review_builder,
        umbrella_builder,
        umbrella_person_review_builder,
        main_product_builder,
        main_product_review_builder,
        umbrella_review_builder,
        review_builder,
        person_builder,
):
    user = person_builder(
        login='superreviewer_login',
    )

    reviews = [
        review_builder(status=const.REVIEW_STATUS.IN_PROGRESS),
        review_builder(status=const.REVIEW_STATUS.IN_PROGRESS),
    ]

    persons = [
        person_builder(),
        person_builder(),
        person_builder(),
    ]

    person_reviews = [
        person_review_builder(person=persons[0], review=reviews[0]),
        person_review_builder(person=persons[0], review=reviews[1]),
        person_review_builder(person=persons[1], review=reviews[0]),
    ]

    upload_contents_to_fail = [
        {
            'Логин': persons[0].login,  # не должно, т.к. 2 активных ревью
            'VS': 'Новый продукт',
            'Зонтик': 'Новый зонт',
        },
        {
            'Логин': persons[1].login,  # не должно, т.к. откатится из-за других ошибок
            'VS': 'Новый продукт',
            'Зонтик': 'Новый зонт',
        },
        {
            'Логин': persons[2].login,  # не должно, т.к. нет активных ревью
            'VS': 'Другой новый продукт',
            'Зонтик': 'Новый зонт',
        }
    ]

    result = helpers.post_multipart_data(
        client=client,
        path=UPLOAD_PATH,
        request={'data_file': get_uploaded_file(upload_contents_to_fail)},
        login=user.login,
        expect_status=400,
    )

    assert len(result['errors']) == 2

    assert persons[0].login in result['errors'][0]
    assert persons[2].login in result['errors'][1]

    assert str(reviews[0].id) in result['errors'][0]
    assert str(reviews[1].id) in result['errors'][0]

    assert UmbrellaPersonReview.objects.all().count() == 0

    upload_contents = [
        {
            'Логин': persons[0].login,
            'VS': 'Новый продукт',
            'Зонтик': 'Новый зонт',
            'Ревью': str(reviews[1].id),
        },
        {
            'Логин': persons[1].login,
            'VS': 'Новый продукт',
            'Зонтик': 'Новый зонт',
            'Ревью': '',
        },
    ]

    result = helpers.post_multipart_data(
        client=client,
        path=UPLOAD_PATH,
        request={'data_file': get_uploaded_file(upload_contents)},
        login=user.login,
    )

    assert 'message' in result
    assert 'errors' not in result

    assert UmbrellaPersonReview.objects.all().count() == 2

    assert UmbrellaPersonReview.objects.filter(
        person_review__review=reviews[1],
        person_review__person=persons[0],
    ).count() == 1

    assert UmbrellaPersonReview.objects.filter(
        person_review__review=reviews[0],
        person_review__person=persons[1],
    ).count() == 1
