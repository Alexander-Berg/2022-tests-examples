import pytest
from datetime import datetime
from decimal import Decimal
import pytz
from billing.apikeys.apikeys.tariff_executor import TariffStateExec
from billing.apikeys.apikeys import mapper


class TestNextTrariffStateTransfer:

    @pytest.fixture()
    def tarifficator_state_with_next_tariff_data(self):
        return {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 4, 17, tzinfo=pytz.utc),
            'products': {
                '508206': {
                    'consumed': '1500000',
                    'credited': '1500000'
                }
            },
            'next_tariff_state': {
                'products': {
                    '508206': {
                        'PrepayPeriodicallyDiscountedUnit_yearly': {
                            'prev_consume_value': '1500000',
                            'consume_discount': '750000'
                        }
                    }
                }
            }
        }

    @pytest.fixture()
    def temporary_tarifficator_state_with_discount_data(self, mongomock, link_with_fake_tariff):
        temporary_state = mapper.TarifficatorStateTemporary.get_for_link(link_with_fake_tariff)
        temporary_state.state.update({
            'products': {
                '508206': {
                    'PrepayPeriodicallyDiscountedUnit_yearly': {
                        'prev_consume_value': '1500000',
                        'consume_discount': '750000'
                    }
                }
            }
        })
        temporary_state.save()

    def test_next_tariff_state_pushed_to_temporary_state_correctly(self, mongomock, link_with_fake_tariff, tarifficator_state_with_next_tariff_data):
        state_executor = TariffStateExec(link_with_fake_tariff, tarifficator_state_with_next_tariff_data)
        state_executor._transfer_next_tariff_state()
        temporary_state = mapper.TarifficatorStateTemporary.get_for_link(link_with_fake_tariff)
        discount_data = temporary_state.state['products']['508206']['PrepayPeriodicallyDiscountedUnit_yearly']
        assert Decimal(discount_data['prev_consume_value']) == Decimal('1500000')
        assert Decimal(discount_data['consume_discount']) == Decimal('750000')

    def test_next_tariff_pulls_previous_tariff_state_correctly(self, mongomock, link_with_fake_tariff, temporary_tarifficator_state_with_discount_data):
        mapper.TarifficatorState.drop_for_link(link_with_fake_tariff)
        state = mapper.TarifficatorState.get_for_link(link_with_fake_tariff)
        discount_data = state.state['products']['508206']['PrepayPeriodicallyDiscountedUnit_yearly']
        assert Decimal(discount_data['prev_consume_value']) == Decimal('1500000')
        assert Decimal(discount_data['consume_discount']) == Decimal('750000')
