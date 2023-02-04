from datetime import date, datetime
import factory
import uuid

from staff.budget_position.tests import utils as bp_utils
from staff.lib.testing import StaffFactory, TimeStampedFactory, OfficeFactory

from staff.departments.models import (
    Bonus,
    Vacancy,
    VacancyMember,
    RelevanceDate,
    ProposalMetadata,
    HrDeadline,
    HRProduct,
)


class VacancyFactory(factory.DjangoModelFactory):
    class Meta:
        model = Vacancy

    id = factory.Sequence(lambda x: x + 1)
    name = factory.Sequence(lambda n: 'position_%d' % n)
    ticket = factory.Sequence(lambda n: "JOB-%d" % n)
    offer_id = factory.Sequence(lambda x: x + 1)
    candidate_id = factory.Sequence(lambda x: x + 1)
    is_published = False
    budget_position = factory.SubFactory(bp_utils.BudgetPositionFactory)
    headcount_position_code = factory.SelfAttribute('budget_position.code')


class VacancyMemberFactory(factory.DjangoModelFactory):
    class Meta:
        model = VacancyMember

    vacancy = factory.SubFactory(VacancyFactory)
    person = factory.SubFactory(StaffFactory)


class RelevanceDateFactory(factory.DjangoModelFactory):
    class Meta:
        model = RelevanceDate

    relevance_date = date.today()


class ProposalMetadataFactory(factory.DjangoModelFactory):
    class Meta:
        model = ProposalMetadata

    proposal_id = factory.Sequence(lambda x: uuid.uuid4().hex[:25])


class HrDeadlineFactory(factory.DjangoModelFactory):
    class Meta:
        model = HrDeadline

    month = date(2020, 1, 1)
    date = date(2050, 1, 1)


class HRProductFactory(TimeStampedFactory):
    product_name = factory.Sequence(lambda n: 'name_%d' % n)
    st_translation_id = factory.Sequence(lambda n: n)

    class Meta:
        model = HRProduct


class BonusFactory(TimeStampedFactory):
    scheme_id = factory.Sequence(lambda n: n)
    schemes_line_id = factory.Sequence(lambda n: n)
    name = factory.Sequence(lambda n: f'name_{n}')
    description = factory.Sequence(lambda n: f'description_{n}')

    start_date = factory.LazyAttribute(lambda x: datetime.now())

    class Meta:
        model = Bonus


class StaffOfficeLogFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'map.StaffOfficeLog'

    staff = factory.SubFactory(StaffFactory)
    office = factory.SubFactory(OfficeFactory)
    date = factory.LazyAttribute(lambda x: date.today())
