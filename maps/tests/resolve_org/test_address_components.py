import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent

pytestmark = [pytest.mark.asyncio]


async def test_address_components(
    client, mock_resolve_org, make_response, business_go_meta
):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.address_components == {
        AddressComponent.STREET: "Улица",
        AddressComponent.HOUSE: "1",
        AddressComponent.UNKNOWN: "Неизведанное",
        AddressComponent.COUNTRY: "Россия",
        AddressComponent.REGION: "Регион",
        AddressComponent.PROVINCE: "Провинция",
        AddressComponent.AREA: "Область",
        AddressComponent.LOCALITY: "Город",
        AddressComponent.DISTRICT: "Район",
        AddressComponent.ROUTE: "Тудой",
        AddressComponent.STATION: "Парк Культуры",
        AddressComponent.METRO_STATION: "Московская",
        AddressComponent.RAILWAY_STATION: "Московская",
        AddressComponent.VEGETATION: "Что-то про растения",
        AddressComponent.AIRPORT: "SVO",
        AddressComponent.OTHER: "Другое",
        AddressComponent.ENTRANCE: "Вход здесь",
    }
