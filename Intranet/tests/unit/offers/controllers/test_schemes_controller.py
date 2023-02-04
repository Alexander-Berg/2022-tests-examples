import pytest

from unittest.mock import patch, Mock

from intranet.femida.src.offers.controllers import OfferSchemesController
from intranet.femida.src.staff.bp_registry import BPRegistryAPI, BPRegistryError

from intranet.femida.tests import factories as f


def test_controller_requires_request_on_absent_schemes_data():
    offer = f.OfferFactory(grade=15, profession=f.ProfessionFactory())
    ctl = OfferSchemesController(offer, {})
    assert ctl.new_schemes_should_be_requested()


def test_controller_requires_request_on_changed_request_data():
    offer = f.OfferFactory(grade=15, profession=f.ProfessionFactory())
    updated_data = {
        'department': offer.department,
        'grade': 16,
        'profession': offer.profession,
    }
    ctl = OfferSchemesController(offer, updated_data)
    assert ctl.new_schemes_should_be_requested()


def test_controller_will_not_ask_for_request_on_request_data_absence():
    offer = f.OfferFactory(grade=15)
    ctl = OfferSchemesController(offer, {})
    assert not ctl.new_schemes_should_be_requested()


@patch.object(BPRegistryAPI, 'get_review_scheme')
def test_schemes_controller_doesnt_intercept_registry_errors(review_mock):
    review_mock.side_effect = Mock(side_effect=BPRegistryError('Test'))
    offer = f.OfferFactory(grade=15, profession=f.ProfessionFactory())
    updated_data = {
        'department': offer.department,
        'grade': 16,
        'profession': offer.profession,
    }
    ctl = OfferSchemesController(offer, updated_data)

    with pytest.raises(BPRegistryError):
        ctl.request_schemes_from_staff()


@patch.object(BPRegistryAPI, 'get_review_scheme', return_value={'scheme_id': 1})
@patch.object(BPRegistryAPI, 'get_bonus_scheme', return_value={'scheme_id': 2})
@patch.object(BPRegistryAPI, 'get_reward_scheme', return_value={'scheme_id': 3})
def test_schemes_controller_returns_results_from_staff(review_mock, bonus_mock, reward_mock):
    offer = f.OfferFactory(grade=15, profession=f.ProfessionFactory())
    ctl = OfferSchemesController(offer, {})

    result = ctl.request_schemes_from_staff()

    assert result['review_scheme_id'] == 1
    assert result['bonus_scheme_id'] == 2
    assert result['reward_scheme_id'] == 3
