from maps.bizdir.sps.proto.business_internal_pb2 import BusinessInternal
from maps.bizdir.sps.utils.attributes import (
    ZERO_REVISION,
    get_last_revision_id,
)


def test_get_last_revision_id__given_business_without_revision_id__returns_default() -> None:
    business = BusinessInternal()
    business.emails.value.email.add(value="email")
    business.feature.add().value.id = "wifi"
    business.feature.add().value.id = "price"

    assert get_last_revision_id(business) == ZERO_REVISION


def test_get_last_revision_id__given_georef__returns_default() -> None:
    business = BusinessInternal()
    business.georeference.point.lon = 36.5
    business.georeference.point.lat = 55.6

    assert get_last_revision_id(business) == ZERO_REVISION


def test_get_last_revision_id__given_different_attributes_with_revision_ids__returns_max() -> None:
    business = BusinessInternal()
    business.company_state.metadata.revision_id = 5
    business.feature.add().metadata.revision_id = 3
    business.feature.add().metadata.revision_id = 4

    assert get_last_revision_id(business) == 5


def test_get_last_revision_id__for_attribute_with_max_revision_id_in_history__returns_it() -> None:
    business = BusinessInternal()
    business.company_state.metadata.revision_id = 5
    business.company_state.change_history.add().revision_id = 7

    assert get_last_revision_id(business) == 7


def test_get_last_revision_id__for_attribute_with_max_revision_id_in_metadata__returns_it() -> None:
    business = BusinessInternal()
    business.company_state.metadata.revision_id = 5
    business.company_state.change_history.add().revision_id = 2

    assert get_last_revision_id(business) == 5
