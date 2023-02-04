import pytest

from staff.departments.models import ValuestreamRoles, DepartmentStaff
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.lib.testing import StaffFactory, ValueStreamFactory, DepartmentFactory
from staff.oebs.constants import PERSON_POSITION_STATUS
from staff.proposal.proposal_builder import ProposalBuilder

from staff.departments.controllers.tickets import ProposalContext
from staff.departments.tests.factories import VacancyFactory


@pytest.mark.django_db
def test_proposal_ctx_gives_hc_codes_for_persons(mocked_mongo, deadlines):
    # given
    person = StaffFactory()
    hc_position = HeadcountPositionFactory(current_person=person, status=PERSON_POSITION_STATUS.OCCUPIED)

    proposal_id = (
        ProposalBuilder()
        .with_person(person.login, lambda person: person.staff_position('питонист'))
        .build(StaffFactory().login)
    )

    ctx = ProposalContext.from_proposal_id(proposal_id)

    # when
    codes = ctx.involved_hc_positions_codes

    # then
    assert codes == {hc_position.code}


@pytest.mark.django_db
def test_proposal_ctx_gives_urls_for_new_vs(mocked_mongo, deadlines):
    # given
    person = StaffFactory()
    vs_head = StaffFactory()
    vs1 = ValueStreamFactory(code='vs_tst1')
    vs2 = ValueStreamFactory(code='vs_tst2')
    vs3 = ValueStreamFactory(code='vs_tst3')
    DepartmentStaff.objects.create(staff=vs_head, department=vs1, role_id=ValuestreamRoles.HEAD.value)
    vacancy = VacancyFactory(department=DepartmentFactory())
    free_hc = HeadcountPositionFactory(status=PERSON_POSITION_STATUS.VACANCY_PLAN, valuestream=vs3)

    proposal_id = (
        ProposalBuilder()
        .with_person(person.login, lambda person_config: person_config.value_stream(vs1.url))
        .with_vacancy(vacancy.id, lambda vacancy_config: vacancy_config.value_stream(vs2.url))
        .with_headcount(free_hc.code, lambda headcount_config: headcount_config.value_stream(vs3.url))
        .build(StaffFactory().login)
    )

    ctx = ProposalContext.from_proposal_id(proposal_id)

    # when
    new_value_streams = set(ctx.new_value_streams.values())

    # then
    assert new_value_streams == {vs1.url, vs2.url, vs3.url}


@pytest.mark.django_db
def test_proposal_ctx_gives_hr_products(mocked_mongo, deadlines):
    # given
    some_dep_for_vac = DepartmentFactory()
    some_other_dep_for_vac = DepartmentFactory()
    person = StaffFactory()
    vs_head = StaffFactory()
    vs = ValueStreamFactory(code='vs_tst')
    DepartmentStaff.objects.create(staff=vs_head, department=vs, role_id=ValuestreamRoles.HEAD.value)
    HeadcountPositionFactory(current_person=person, status=PERSON_POSITION_STATUS.OCCUPIED, valuestream=vs)
    vacancy_hp = HeadcountPositionFactory(
        current_person=None,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        valuestream=vs,
    )
    vacancy = VacancyFactory(headcount_position_code=vacancy_hp.code, department=some_dep_for_vac)

    proposal_id = (
        ProposalBuilder()
        .with_person(person.login, lambda person_config: person_config.staff_position('питонист'))
        .for_existing_department(
            some_other_dep_for_vac.url,
            lambda department: department.use_for_vacancy(
                vacancy.id,
                lambda vacancy_config: vacancy_config.with_ticket('T1')
            )
        )
        .build(StaffFactory().login)
    )

    ctx = ProposalContext.from_proposal_id(proposal_id)

    # when
    products_data = ctx.hr_products

    # then
    assert len(products_data) == 1
    assert products_data[vs.url].hr_product_head == vs_head.login
    assert products_data[vs.url].hr_product_slug == vs.code
