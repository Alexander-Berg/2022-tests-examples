import pytest
from datetime import time

from intranet.trip.src.enums import Language
from intranet.trip.src.api.schemas import AviaSearchFilter
from intranet.trip.src.logic.providers.aeroclub.converters import (
    AeroclubAviaSearchFiltersConverterIn,
)
from intranet.trip.src.logic.providers.aviacenter.converters import (
    AviacenterAviaSearchFiltersConverterIn,
)
from intranet.trip.src.lib.aeroclub.enums import Time, AviaTransfer, AviaRules
from intranet.trip.src.lib.aviacenter.models.search_request import (
    AviaSearchFilterSegment,
    TimeInterval,
)

pytestmark = pytest.mark.asyncio

avia_search_filter_params = {
    'has_baggage': False,
    'is_restricted_by_travel_policy': True,
    'is_changeable': False,
    'is_refundable': False,
    'without_transfer': True,
    'maximum_transfers_count': 0,

    'departure_there_timespan': [Time.night, Time.morning],
    'arrival_there_timespan': [Time.afternoon, Time.evening],

    'air_companies': ['SU', 'S7'],
    'cabin_classes': ['Economy', 'Business'],
    'departure_from_there': ['DME', 'SVO'],
    'arrival_to_there': ['ROV'],
    'departure_from_back': ['ROV'],
    'arrival_to_back': ['VKO'],

    'departure_time_from': time(10, 30),
    'departure_time_to': time(12, 30),
}
filter_search: AviaSearchFilter = AviaSearchFilter(**avia_search_filter_params)


async def test_aeroclub_search_filter_convert_in():
    converter_in = AeroclubAviaSearchFiltersConverterIn(lang=Language.ru)
    af = converter_in.convert(filter_search)

    is_travel_policy_compliant = not filter_search.is_restricted_by_travel_policy
    assert af.aviaFilter_baggage == filter_search.has_baggage
    assert af.IsTravelPolicyCompliant == is_travel_policy_compliant
    assert (AviaRules.changeable in af.aviaFilter_rules) == filter_search.is_changeable
    assert (AviaRules.refundable in af.aviaFilter_rules) == filter_search.is_refundable
    assert af.aviaFilter_airCompanies == filter_search.air_companies
    assert af.aviaFilter_classes == filter_search.cabin_classes
    assert af.departureFromThere == filter_search.departure_from_there
    assert af.departureFromBack == filter_search.departure_from_back
    assert af.arrivalToThere == filter_search.arrival_to_there
    assert af.arrivalToBack == filter_search.arrival_to_back
    assert af.departureAtThereTimes == filter_search.departure_there_timespan
    assert af.arrivalAtThereTimes == filter_search.arrival_there_timespan
    assert af.departureAtBackTimes is None
    assert af.arrivalAtBackTimes is None


@pytest.mark.parametrize(
    'without_transfer, maximum_transfers_count, transfer_expected_value', (
        (True, 0, [AviaTransfer.direct]),
        (False, 1, [AviaTransfer.direct, AviaTransfer.one]),
        (False, 2, None),
        (False, None, None),
    )
)
async def test_aeroclub_search_filter_convert_transfer_in(
    without_transfer: bool,
    maximum_transfers_count: int,
    transfer_expected_value: list[AviaTransfer],
):
    converter_in = AeroclubAviaSearchFiltersConverterIn(lang=Language.ru)
    filter_search.without_transfer = without_transfer
    filter_search.maximum_transfers_count = maximum_transfers_count
    af = converter_in.convert(filter_search)

    assert af.aviaFilter_transfer == transfer_expected_value


async def test_aviacenter_search_filter_convert_in():
    converter_in = AviacenterAviaSearchFiltersConverterIn(lang=Language.ru)
    af = converter_in.convert(filter_search)

    assert af.is_refundable == filter_search.is_refundable
    assert af.is_exchangeable == filter_search.is_changeable
    assert af.is_restricted_by_travel_policy == filter_search.is_restricted_by_travel_policy
    assert af.has_baggage == filter_search.has_baggage
    assert af.maximum_transfers_count == filter_search.maximum_transfers_count
    assert af.carriers == filter_search.air_companies

    segments = [AviaSearchFilterSegment(
        departure_time_intervals=[
            TimeInterval(
                from_time=filter_search.departure_time_from,
                to_time=filter_search.departure_time_to,
            )
        ]
    )]
    assert af.segments == segments
