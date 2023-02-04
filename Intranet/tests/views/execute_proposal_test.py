import itertools
import random

from staff.departments.controllers.person_action_validator import PersonActionValidator, PersonActionValidationError
from staff.proposal.views.views import _validate_person_actions


class FakeProposalCtl:
    person_actions = []


def test_validate_person_actions_no_actions():
    proposal_ctl = FakeProposalCtl()
    proposal_ctl.person_actions = []
    person_action_validator = PersonActionValidator()

    result = _validate_person_actions(proposal_ctl, person_action_validator)

    assert result is None


def test_validate_person_actions_no_errors():
    proposal_ctl = FakeProposalCtl()
    proposal_ctl.person_actions = range(random.randint(15, 20))
    person_action_validator = PersonActionValidator()
    person_action_validator.validate = lambda _: None

    result = _validate_person_actions(proposal_ctl, person_action_validator)

    assert result is None


def _assert_validation_error(validation_error: PersonActionValidationError):
    error_action = {"test": True}
    good_items_count = random.randint(15, 20)
    proposal_ctl = FakeProposalCtl()
    proposal_ctl.person_actions = itertools.chain(
        range(good_items_count),
        [error_action],
        range(random.randint(15, 20)),
    )

    person_action_validator = PersonActionValidator()

    def _raise():
        raise validation_error
    person_action_validator.validate = lambda x: _raise()

    result = _validate_person_actions(proposal_ctl, person_action_validator)
    error_field = f'persons[actions][{good_items_count}]{validation_error.field}'

    assert result is not None
    assert "errors" in result
    assert error_field in result["errors"]

    return result["errors"][error_field]


def test_validate_person_actions_with_error():
    validation_error = PersonActionValidationError("code")
    validation_error.field = "field"
    validation_error.params = None

    result = _assert_validation_error(validation_error)

    assert result == [{'code': validation_error.code}]


def test_validate_person_actions_with_error_and_param():
    validation_error = PersonActionValidationError("code")
    validation_error.field = "field"
    validation_error.params = "params"

    result = _assert_validation_error(validation_error)

    assert result == [{'code': validation_error.code, 'params': validation_error.params}]
