import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent

pytestmark = [pytest.mark.asyncio]


async def test_address_components(client, mock_resolve_orgs, make_multi_response):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.address_components for r in result] == [
        {
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
        },
        {
            AddressComponent.STREET: "Проспект",
            AddressComponent.HOUSE: "2",
            AddressComponent.UNKNOWN: "Неизведанное",
            AddressComponent.COUNTRY: "Россия",
            AddressComponent.REGION: "Регион",
            AddressComponent.PROVINCE: "Провинция",
            AddressComponent.AREA: "Область",
            AddressComponent.LOCALITY: "Город",
            AddressComponent.DISTRICT: "Район",
            AddressComponent.ROUTE: "Сюдой",
            AddressComponent.STATION: "Центр",
            AddressComponent.METRO_STATION: "Питерская",
            AddressComponent.RAILWAY_STATION: "Питерская",
            AddressComponent.VEGETATION: "Трава",
            AddressComponent.AIRPORT: "PUL",
            AddressComponent.OTHER: "Другое",
            AddressComponent.ENTRANCE: "Вход там",
        },
    ]
