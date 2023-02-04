import random

from lagom import Container

from staff.budget_position.workflow_service import entities
from staff.budget_position.tests.workflow_tests.utils import StaffServiceMock, TableflowMock, OEBSServiceMock


def make_container(
    staff_service_mock: StaffServiceMock,
    table_flow_mock: TableflowMock,
    oebs_service_mock: OEBSServiceMock,
) -> Container:
    container = Container()
    container[entities.StaffService] = staff_service_mock
    container[entities.TableflowService] = table_flow_mock
    container[entities.OEBSService] = oebs_service_mock
    return container


def test_bonus_scheme_details_no_non_review_bonus() -> None:
    # given
    table_flow_service = TableflowMock()
    oebs_service = OEBSServiceMock()

    staff_service = StaffServiceMock()
    scheme_id = random.randint(10, 100000)
    staff_service.set_bonus_scheme_details(entities.BonusSchemeDetails(
        scheme_id=scheme_id,
        name='name',
        description='description',
        scheme_rows=[
            entities.BonusSchemeRow(random.randint(10, 100000), 'Процент от оклада', 'test', random.random()),
            entities.BonusSchemeRow(random.randint(10, 100000), 'test', 'Значением', random.random()),
        ],
    ))

    container = make_container(staff_service, table_flow_service, oebs_service)
    calculator = container[entities.SchemesCalculator]

    # when
    result = calculator.bonus_scheme_details(scheme_id)

    # then
    assert result.non_review_bonus is None


def test_bonus_scheme_details():
    # given
    table_flow_service = TableflowMock()
    oebs_service = OEBSServiceMock()

    staff_service = StaffServiceMock()
    scheme_id = random.randint(10, 100000)
    bonus = random.random()
    staff_service.set_bonus_scheme_details(entities.BonusSchemeDetails(
        scheme_id=scheme_id,
        name='name',
        description='description',
        scheme_rows=[
            entities.BonusSchemeRow(random.randint(10, 100000), 'Процент от оклада', 'test', random.random()),
            entities.BonusSchemeRow(random.randint(10, 100000), 'test', 'Значением', random.random()),
            entities.BonusSchemeRow(random.randint(10, 100000), 'Процент от оклада', 'Значением', random.random()),
            entities.BonusSchemeRow(random.randint(10, 100000), 'Процент от оклада', 'Значением', random.random()),
        ],
        non_review_bonus=bonus
    ))

    container = make_container(staff_service, table_flow_service, oebs_service)
    calculator = container[entities.SchemesCalculator]

    # when
    result = calculator.bonus_scheme_details(scheme_id)

    # then
    assert result.non_review_bonus == bonus
