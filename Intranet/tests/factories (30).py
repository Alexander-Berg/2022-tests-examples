from datetime import date, datetime

import factory

from staff.departments.models import Geography
from staff.oebs.models import (
    Job,
    Bonus,
    Review,
    Reward,
    HRProduct,
    Occupation,
    BusinessCenter,
    HeadcountPosition,
    Organization,
)


class HeadcountPositionFactory(factory.DjangoModelFactory):
    id = factory.Sequence(lambda n: '{}'.format(n))
    position_code = factory.Sequence(lambda n: n)

    total_headcount = 1
    state_headcount = 1
    state_cross_headcount = 1

    position_product_id = 1
    position_geo = 'Rusc'
    position_bonus_id = 1
    position_reward_id = 1
    position_review_id = 1
    position_headcount = 1
    position_is_crossing = False

    position_category_is_new = True

    relevance_date = date.today()

    class Meta:
        model = HeadcountPosition


class ReviewFactory(factory.DjangoModelFactory):
    scheme_id = factory.Sequence(lambda n: n + 1)
    name = factory.Sequence(lambda n: 'review_%d' % n)
    start_date = date.today()
    st_translation_id = factory.Sequence(lambda n: n)

    class Meta:
        model = Review


class BonusFactory(factory.DjangoModelFactory):
    scheme_id = factory.Sequence(lambda n: n + 1)
    name = factory.Sequence(lambda n: 'bonus_%d' % n)
    start_date = date.today()
    st_translation_id = factory.Sequence(lambda n: n)

    class Meta:
        model = Bonus


class RewardFactory(factory.DjangoModelFactory):
    scheme_id = factory.Sequence(lambda n: n + 1)
    name = factory.Sequence(lambda n: 'reward_%d' % n)
    start_date = date.today()
    st_translation_id = factory.Sequence(lambda n: n)

    class Meta:
        model = Reward


class GeographyFactory(factory.DjangoModelFactory):
    oebs_code = factory.Sequence(lambda n: 'code_%d' % n)
    name = factory.Sequence(lambda n: 'geography_%d' % n)
    name_en = factory.Sequence(lambda n: 'geography_%d' % n)
    st_translation_id = factory.Sequence(lambda n: n)
    created_at = factory.LazyAttribute(lambda x: datetime.now())
    modified_at = factory.LazyAttribute(lambda x: datetime.now())

    class Meta:
        model = Geography


class HRProductFactory(factory.DjangoModelFactory):
    product_id = factory.Sequence(lambda n: n)
    product_name = factory.Sequence(lambda n: 'name_%d' % n)
    st_translation_id = factory.Sequence(lambda n: n)

    class Meta:
        model = HRProduct


class OccupationFactory(factory.DjangoModelFactory):
    scale_name = factory.Sequence(lambda n: 'name_%d' % n)
    scale_description = factory.Sequence(lambda n: 'occupation_%d' % n)

    class Meta:
        model = Occupation


class JobFactory(factory.DjangoModelFactory):
    start_date = date.min
    code = factory.Sequence(lambda n: n + 10000)

    class Meta:
        model = Job


class BusinessCenterFactory(factory.DjangoModelFactory):
    code = factory.Sequence(lambda n: 'code_%d' % n)
    staff_usage = 'ДА'

    class Meta:
        model = BusinessCenter


class OrganizationFactory(factory.DjangoModelFactory):
    org_id = factory.Sequence(lambda n: n)
    staff_usage = 'Y'

    class Meta:
        model = Organization
