# coding: utf-8
import datetime
import json
import uuid
import os
from waffle.models import Flag

import arrow
import pytest
from django.utils import timezone

from review.gradient import models as gradient_models
from review.lib import datetimes
from review.lib import std
from review.core import const
from review.core import models as core_models
from review.core.logic import roles
from review.oebs import (
    const as oebs_const,
    models as oebs_models,
)
from review.oebs.sync import fake


@pytest.fixture
def calibration_builder(db, person_builder):
    def builder(**kwargs):
        params = {
            'name': 'Calibration ' + str(uuid.uuid4())[:6],
            'start_date': '2010-01-01',
            'finish_date': '2020-03-01',
            'status': const.CALIBRATION_STATUS.DRAFT,
        }
        if 'author' not in kwargs:
            kwargs['author'] = person_builder()
        params.update(kwargs)
        model = core_models.Calibration.objects.create(**params)
        return model

    return builder


@pytest.fixture
def calibration(calibration_builder):
    return calibration_builder()


@pytest.fixture
def calibration_role_builder(db, person_builder):
    def builder(**kwargs):
        skip_denormalization = kwargs.pop('skip_denormalization', False)
        if 'person' not in kwargs:
            kwargs['person'] = person_builder()
        model = core_models.CalibrationRole.objects.create(**kwargs)
        if not skip_denormalization:
            roles.denormalize_person_review_roles()
        return model

    return builder


@pytest.fixture
def calibration_person_review_builder(db, person_builder, calibration_builder, person_review_builder):
    def builder(**kwargs):
        params = {
            'updated_at': timezone.now(),
            'calibration': kwargs['calibration'] if 'calibration' in kwargs else calibration_builder(),
        }
        if 'person_review' not in kwargs:
            person = kwargs.get('person', person_builder())
            params['person_review'] = person_review_builder(person=person)
        else:
            params['person_review'] = kwargs['person_review']

        model = core_models.CalibrationPersonReview.objects.create(**params)
        roles.denormalize_person_review_roles()
        return model

    return builder


@pytest.fixture
def calibration_person_review(calibration_person_review_builder):
    return calibration_person_review_builder()


@pytest.fixture
def marks_scale_builder(db):
    def builder(**kwargs):
        scale = kwargs.pop('scale', {
            "bad": {
                "value": 1008,
                "text_value": "-",
            },
            "not_enough": {
                "value": 1013,
                "text_value": "+-",
            },
            "good": {
                "value": 1021,
                "text_value": "+",
            },
            "excellent": {
                "value": 1034,
                "text_value": "++",
            },
            "outstanding": {
                "value": 1055,
                "text_value": "+++",
            },
            "extraordinary": {
                "value": 1089,
                "text_value": "++++",
            },
        })
        show_absolute = kwargs.pop('show_absolute', False)

        use_colors = kwargs.pop('use_colors', True)
        version = kwargs.pop('version', 2)
        return core_models.MarksScale.objects.create(
            scale=scale,
            show_absolute=show_absolute,
            use_colors=use_colors,
            version=version,
        )

    return builder


@pytest.fixture
def marks_scale(marks_scale_builder):
    return marks_scale_builder()


@pytest.fixture
def review_builder(db, person_builder, marks_scale_builder):
    def builder(**kwargs):
        today = datetimes.now().date()
        params = {
            'name': 'Review ' + str(uuid.uuid4())[:6],
            'start_date': datetimes.shifted(today, months=-2),
            'finish_date': datetimes.shifted(today, months=+2),
            'salary_date': datetimes.shifted(today, months=+1),
            'status': const.REVIEW_STATUS.IN_PROGRESS,
            'mark_mode': const.REVIEW_MODE.MODE_MANUAL,
            'notify_events_other': True,
            'notify_events_superreviewer': True,
            'feedback_from_date': datetimes.shifted(today, months=-2),
            'feedback_to_date': datetimes.shifted(today, months=-8),
        }
        if 'author' not in kwargs:
            kwargs['author'] = person_builder()
        if 'scale' not in kwargs:
            kwargs['scale'] = marks_scale_builder()
        if 'kpi_loaded' not in kwargs:
            kwargs['kpi_loaded'] = datetimes.now()
        params.update(kwargs)
        model = core_models.Review.objects.create(**params)
        roles.denormalize_person_review_roles()
        return model

    return builder


@pytest.fixture
def review(review_builder):
    return review_builder()


@pytest.fixture
def person_review_builder(db, person_builder, review_builder, finance_builder):
    def builder(**kwargs):
        params = {
            'updated_at': timezone.now(),
        }
        params.update(kwargs)
        if 'review' not in params:
            review_params = std.subdict_by_key_prefix(
                dict_=params,
                prefix='review__',
                remove_keys=True,
            )
            params['review'] = review_builder(**review_params)
        if 'person' not in params:
            person_params = std.subdict_by_key_prefix(
                dict_=params,
                prefix='person__',
                remove_keys=True,
            )
            std.bulk_del_keys(params, list(person_params.keys()))
            params['person'] = person_builder(**person_params)
        review_salary_date = params['review'].salary_date

        if not oebs_models.FinanceEvents.objects.filter(
            person=params['person'],
            type=oebs_const.FINANCE_EVENT_TYPES[oebs_const.SALARY_HISTORY],
            date_from__lte=review_salary_date,
            date_to__gte=review_salary_date,
        ).exists():
            finance_date_to = review_salary_date + datetime.timedelta(days=1)
            login = params['person'].login
            fake_data = fake.generate_data(
                data_types=[oebs_const.CURRENT_SALARY],
                logins=[login],
            )
            finance_builder(
                person=params['person'],
                salary_history=[{
                    "salarySum": 100,
                    "currency": 'RUB',
                    "basis": "MONTHLY",
                    "dateFrom": '2000-01-01',
                    "dateTo": finance_date_to.strftime('%Y-%m-%d'),
                }],
                **fake_data[login]
            )
        model = core_models.PersonReview.objects.create(**params)
        roles.denormalize_person_review_roles()
        return model

    return builder


@pytest.fixture
def person_review(person, review, person_review_builder):
    return person_review_builder(
        person=person,
        review=review,
        updated_at=timezone.now(),
    )


@pytest.fixture
def review_goodie_builder(db, review_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'review' not in params:
            params['review'] = review_builder()
        return core_models.Goodie.objects.create(**params)

    return builder


@pytest.fixture
def review_goodie(review, review_goodie_builder):
    return review_goodie_builder(review=review)


@pytest.fixture
def review_role_builder(db, person_builder, review_builder):
    def builder(**kwargs):
        skip_denormalization = kwargs.pop('skip_denormalization', False)

        params = {}
        params.update(kwargs)
        if 'review' not in params:
            params['review'] = review_builder()
        if 'person' not in params:
            params['person'] = person_builder()

        created = core_models.ReviewRole.objects.create(**params)
        if not skip_denormalization:
            roles.denormalize_person_review_roles()
        return created

    return builder


@pytest.fixture
def review_role_admin(review_role_builder):
    return review_role_builder(type=const.ROLE.REVIEW.ADMIN)


@pytest.fixture
def review_role_superreviewer(review_role_builder):
    return review_role_builder(type=const.ROLE.REVIEW.SUPERREVIEWER)


@pytest.fixture
def review_role_accompanying_hr(review_role_builder):
    return review_role_builder(type=const.ROLE.REVIEW.ACCOMPANYING_HR)


@pytest.fixture
def robot(
    global_role_builder,
    person_builder,
):
    robot = person_builder(login='robot-review')
    global_role_builder(
        type=const.ROLE.GLOBAL.ROBOT,
        person=robot,
    )
    return robot


@pytest.fixture
def person_review_role_builder(db, person_builder, person_review_builder):
    def builder(**kwargs):
        skip_denormalization = kwargs.pop('skip_denormalization', False)
        params = {}
        params.update(kwargs)
        if 'person_review' not in params:
            person_review_params = {}
            if 'review' in params:
                person_review_params['review'] = params.pop('review')
            params['person_review'] = person_review_builder(**person_review_params)
        if 'person' not in params:
            params['person'] = person_builder()
        role = core_models.PersonReviewRole.objects.create(**params)
        if not skip_denormalization:
            roles.denormalize_person_review_roles()
        return role

    return builder


@pytest.fixture
def excel_template_builder(db, review_builder):
    def builder(**kwargs):
        params = dict(kwargs)
        if 'review' not in params:
            params['review'] = review_builder()
        role = core_models.ExcelTemplate.objects.create(**params)
        return role

    return builder


@pytest.fixture
def person_review_role_reviewer(person_review_role_builder):
    return person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )


@pytest.fixture
def person_review_role_top_reviewer(person_review_role_builder):
    return person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    )


@pytest.fixture
def person_review_role_reader(person_review_role_builder):
    return person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.READER,
    )


@pytest.fixture
def person_review_role_superreader(person_review_role_builder):
    return person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.SUPERREADER,
    )


@pytest.fixture
def review_bonus_file():
    bonus_file_path = os.path.abspath(os.path.dirname(__file__))
    bonus_file_name = bonus_file_path + '/bonus_file.csv'
    bonus_file = open(bonus_file_name)
    yield bonus_file
    bonus_file.close()


@pytest.fixture
def bad_file():
    bonus_file_path = os.path.abspath(os.path.dirname(__file__))
    bonus_file_name = bonus_file_path + '/bad_file.xls'
    bonus_file = open(bonus_file_name, 'rb')
    yield bonus_file
    bonus_file.close()


@pytest.fixture
def for_templated_export():
    file_path = os.path.abspath(os.path.dirname(__file__))
    file_name = file_path + '/for_templated_export.xlsx'
    templated_file = open(file_name, 'rb')
    yield templated_file
    templated_file.close()


@pytest.fixture
def global_role_builder(db, person_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'person' not in params:
            params['person'] = person_builder()
        if 'type' not in params:
            params['type'] = const.ROLE.GLOBAL.REVIEW_CREATOR
        return core_models.GlobalRole.objects.create(
            person=params['person'],
            type=params['type'],
        )

    return builder


@pytest.fixture
def main_product_builder(db):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'name' not in params:
            params['name'] = 'somename'
        if 'issue_key' not in params:
            last_id = (
                gradient_models.MainProduct.objects
                .values_list('id', flat=True)
                .order_by('-id')
                .first()
            )
            params['issue_key'] = 'GOALZ-{}'.format(last_id or 0)
        return gradient_models.MainProduct.objects.create(**params)

    return builder


@pytest.fixture
def kpi_builder(db, person_review_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'name' not in params:
            params['name'] = 'somename'
        if 'quarter' not in params:
            params['quarter'] = 5
        if 'year' not in params:
            params['year'] = 2020
        if 'percent' not in params:
            params['percent'] = 0
        if 'person_review' not in params:
            params['person_review'] = person_review_builder()
        if 'weight' not in params:
            params['weight'] = 666
        return core_models.Kpi.objects.create(
            name=params['name'],
            percent=params['percent'],
            person_review=params['person_review']
        )

    return builder


@pytest.fixture
def main_product_review_builder(db, main_product_builder, review_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'main_product' not in params:
            params['main_product'] = main_product_builder()
        if 'review' not in params:
            params['review'] = review_builder()
        return gradient_models.MainProductReview.objects.create(
            review=params['review'],
            main_product=params['main_product'],
        )

    return builder


@pytest.fixture
def umbrella_builder(db, main_product_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'name' not in params:
            params['name'] = 'somename'
        if 'issue_key' not in params:
            last_id = (
                gradient_models.Umbrella.objects
                .values_list('id', flat=True)
                .order_by('-id')
                .first()
            )
            params['issue_key'] = 'GOALZ-{}'.format(last_id or 0)
        return gradient_models.Umbrella.objects.create(**params)

    return builder


@pytest.fixture
def umbrella_review_builder(db, umbrella_builder, review_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'umbrella' not in params:
            params['umbrella'] = umbrella_builder()
        if 'review' not in params:
            params['review'] = review_builder()
        return gradient_models.UmbrellaReview.objects.create(
            umbrella=params['umbrella'],
            review=params['review'],
        )

    return builder


@pytest.fixture
def umbrella_person_review_builder(db, umbrella_builder, person_review_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'umbrella' not in params:
            params['umbrella'] = umbrella_builder()
        if 'person_review' not in params:
            params['person_review'] = person_review_builder()
        return gradient_models.UmbrellaPersonReview.objects.create(
            umbrella=params['umbrella'],
            person_review=params['person_review'],
        )

    return builder


@pytest.fixture
def umbrella_person_builder(db, umbrella_builder, person_builder):

    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'umbrella' not in params:
            params['umbrella'] = umbrella_builder()
        if 'person' not in params:
            params['person'] = person_builder()
        params.setdefault('engagement', 100)
        return gradient_models.UmbrellaPerson.objects.create(**params)

    return builder


@pytest.fixture
def main_product_person_builder(db, main_product_builder, person_builder):

    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'main_product' not in params:
            params['main_product'] = main_product_builder()
        if 'person' not in params:
            params['person'] = person_builder()
        return gradient_models.MainProductPerson.objects.create(**params)

    return builder


@pytest.fixture
def calibration_editor_role_builder(
    calibration_builder,
    calibration_role_builder,
    global_role_builder,
    person_builder,
):
    def builder(type=const.ROLE.CALIBRATION.ADMIN, calibration=None, person=None):

        if type in const.ROLE.CALIBRATION.ALL:
            return calibration_role_builder(
                calibration=calibration or calibration_builder(),
                person=person or person_builder(),
                type=type,
            )
        elif type == const.ROLE.GLOBAL.CALIBRATION_CREATOR:
            return global_role_builder(
                person=person or person_builder(),
                type=type,
            )
        else:
            raise ValueError(
                'Accepted only ROLE.CALIBRATION.ALL and ROLE.GLOBAL.CALIBRATION_CREATOR'
            )
    return builder


@pytest.fixture
def person_review_change_builder(db, person_review_builder, person_builder):
    def builder(**kwargs):
        params = {
            'diff': {
                'mark': {
                    'old': 'A',
                    'new': 'B',
                },
            },
            'created_at': timezone.now(),
        }
        params.update(kwargs)
        if 'person_review' not in params:
            person_review_builder_params = {}
            if 'review' in params:
                person_review_builder_params['review'] = params.pop('review')
            params['person_review'] = person_review_builder(**person_review_builder_params)
        if 'created_at_ago' in params:
            now = arrow.now()
            params['created_at'] = now.replace(**params.pop('created_at_ago')).datetime
        if 'subject' not in params:
            params['subject'] = person_builder()
        role = core_models.PersonReviewChange.objects.create(**params)
        roles.denormalize_person_review_roles()
        return role

    return builder


@pytest.fixture
def person_review_change(person_review_change_builder):
    return person_review_change_builder()


@pytest.fixture
def person_review_comment_builder(db, person_review_builder, person_builder):
    def builder(**kwargs):
        now = timezone.now()
        params = {
            'text_wiki': 'WAT',
            'created_at': now,
            'updated_at': now,
        }
        params.update(kwargs)
        if 'person_review' not in params:
            person_review_builder_params = {}
            if 'review' in params:
                person_review_builder_params['review'] = params.pop('review')
            params['person_review'] = person_review_builder(**person_review_builder_params)
        if 'created_at_ago' in params:
            now = arrow.now()
            params['created_at'] = now.replace(**params.pop('created_at_ago')).datetime
        if 'subject' not in params:
            params['subject'] = person_builder()
        role = core_models.PersonReviewComment.objects.create(**params)
        roles.denormalize_person_review_roles()
        return role

    return builder


@pytest.fixture
def person_review_comment(person_review_comment_builder):
    return person_review_comment_builder()


@pytest.fixture
def test_person_as_review_creator(test_person, global_role_builder):
    global_role_builder(
        person=test_person,
        type=const.ROLE.GLOBAL.REVIEW_CREATOR,
    )


@pytest.fixture
def waffle_flag_builder(db):
    def builder(flag_name, users=None, **kwargs):
        kwargs.setdefault('superusers', False)
        f = Flag.objects.create(name=flag_name, **kwargs)
        for u in users or []:
            f.users.add(u)
        return f

    return builder
