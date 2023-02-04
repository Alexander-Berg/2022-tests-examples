from datetime import date, timedelta

from staff.budget_position.workflow_service.entities import (
    GetSchemeRequest,
    InsuranceCalculator,
    InsuranceDetails,
    RewardSchemeDetails,
)


def scheme_details_template() -> RewardSchemeDetails:
    return RewardSchemeDetails(
        scheme_id=100500,
        description='test',
        schemes_line_id=100500,
        category='Professionals',
        food='',
        dms=[],
        ai=[],
        dms_group='',
        bank_cards=[],
        name='Test',
    )


def test_insurance_calculator_correctly_detects_health_insurance_options():
    # given
    insurance_details = InsuranceDetails(name='РЕСО', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [insurance_details]
    calculator = InsuranceCalculator(reward_scheme_details, GetSchemeRequest())

    # when
    result = calculator._has_health_insurance_option_in_scheme()

    # then
    assert result


def test_insurance_calculator_correctly_detects_absence_of_health_insurance_options():
    # given
    insurance_details = InsuranceDetails(name='Без ДМС', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [insurance_details]
    calculator = InsuranceCalculator(reward_scheme_details, GetSchemeRequest())

    # when
    result = calculator._has_health_insurance_option_in_scheme()

    # then
    assert not result


def test_insurance_calculator_correctly_detects_life_insurance_options():
    # given
    insurance_details = InsuranceDetails(name='РЕСО', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.ai = [insurance_details]
    calculator = InsuranceCalculator(reward_scheme_details, GetSchemeRequest())

    # when
    result = calculator._has_life_insurance_option_in_scheme()

    # then
    assert result


def test_insurance_calculator_correctly_detects_absence_of_life_insurance_options():
    # given
    insurance_details = InsuranceDetails(name='Без НС', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.ai = [insurance_details]
    calculator = InsuranceCalculator(reward_scheme_details, GetSchemeRequest())

    # when
    result = calculator._has_life_insurance_option_in_scheme()

    # then
    assert not result


def test_insurance_calculator_correctly_detects_non_term_contract():
    # given
    request = GetSchemeRequest()
    calculator = InsuranceCalculator(scheme_details_template(), request)

    # when
    result = calculator._will_work_more_than_six_months()

    # then
    assert result


def test_insurance_calculator_correctly_detects_more_than_six_months_term_contract():
    # given
    request = GetSchemeRequest(contract_term=7)
    calculator = InsuranceCalculator(scheme_details_template(), request)

    # when
    result = calculator._will_work_more_than_six_months()

    # then
    assert result


def test_insurance_calculator_correctly_detects_less_than_six_months_term_contract():
    # given
    request = GetSchemeRequest(contract_term=5)
    calculator = InsuranceCalculator(scheme_details_template(), request)

    # when
    result = calculator._will_work_more_than_six_months()

    # then
    assert not result


def test_insurance_calculator_correctly_detects_more_than_half_year_date_term_contract():
    # given
    request = GetSchemeRequest(contract_term_date=date.today() + timedelta(days=186))
    calculator = InsuranceCalculator(scheme_details_template(), request)

    # when
    result = calculator._will_work_more_than_six_months()

    # then
    assert result


def test_insurance_calculator_correctly_detects_less_than_half_year_date_term_contract():
    # given
    request = GetSchemeRequest(contract_term_date=date.today() + timedelta(days=180))
    calculator = InsuranceCalculator(scheme_details_template(), request)

    # when
    result = calculator._will_work_more_than_six_months()

    # then
    assert not result


def test_has_health_insurance_when_main_workplace_and_non_termcontract():
    # given
    insurance_details = InsuranceDetails(name='РЕСО', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [insurance_details]
    request = GetSchemeRequest(is_main_work_place=True)
    calculator = InsuranceCalculator(reward_scheme_details, request)

    # when
    result = calculator.has_health_insurance()

    # then
    assert result


def test_has_no_health_insurance_when_not_main_workplace():
    # given
    insurance_details = InsuranceDetails(name='РЕСО', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [insurance_details]
    request = GetSchemeRequest(is_main_work_place=False)
    calculator = InsuranceCalculator(reward_scheme_details, request)

    # when
    result = calculator.has_health_insurance()

    # then
    assert not result


def test_has_no_health_insurance_when_no_option_scheme():
    # given
    insurance_details = InsuranceDetails(name='Без ДМС', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [insurance_details]
    request = GetSchemeRequest(is_main_work_place=True)
    calculator = InsuranceCalculator(reward_scheme_details, request)

    # when
    result = calculator.has_health_insurance()

    # then
    assert not result


def test_has_life_insurance_when_main_workplace_and_non_termcontract_and_has_health_insurance():
    # given
    insurance_details = InsuranceDetails(name='РЕСО', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [insurance_details]
    reward_scheme_details.ai = [insurance_details]
    request = GetSchemeRequest(is_main_work_place=True)
    calculator = InsuranceCalculator(reward_scheme_details, request)

    # when
    result = calculator.has_life_insurance()

    # then
    assert result


def test_has_life_insurance_when_main_workplace_and_non_termcontract_when_no_health_insurance():
    # given
    insurance_details = InsuranceDetails(name='РЕСО', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [InsuranceDetails(name='Без ДМС', type='test', ya_insurance=True)]
    reward_scheme_details.ai = [insurance_details]
    request = GetSchemeRequest(is_main_work_place=True)
    calculator = InsuranceCalculator(reward_scheme_details, request)

    # when
    result = calculator.has_life_insurance()

    # then
    assert result


def test_has_no_life_insurance_when_not_main_workplace():
    # given
    insurance_details = InsuranceDetails(name='РЕСО', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [InsuranceDetails(name='Без ДМС', type='test', ya_insurance=True)]
    reward_scheme_details.ai = [insurance_details]
    request = GetSchemeRequest(is_main_work_place=False)
    calculator = InsuranceCalculator(reward_scheme_details, request)

    # when
    result = calculator.has_life_insurance()

    # then
    assert not result


def test_has_no_life_insurance_when_no_option_in_scheme():
    # given
    insurance_details = InsuranceDetails(name='Без НС', type='test', ya_insurance=True)
    reward_scheme_details = scheme_details_template()
    reward_scheme_details.dms = [InsuranceDetails(name='Без ДМС', type='test', ya_insurance=True)]
    reward_scheme_details.ai = [insurance_details]
    request = GetSchemeRequest(is_main_work_place=True)
    calculator = InsuranceCalculator(reward_scheme_details, request)

    # when
    result = calculator.has_life_insurance()

    # then
    assert not result
