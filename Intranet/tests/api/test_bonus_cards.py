import pytest

from intranet.trip.src.enums import ServiceType

pytestmark = pytest.mark.asyncio


async def _create_person_with_bonus_cards(f, person_id, card_numbers: list[str] = None):
    providers_fields = [
        {'service_type': ServiceType.avia, 'code': 'S7', 'name': 'Эс сэвэн', 'name_en': 'S seven'},
        {'service_type': ServiceType.rail, 'code': 'RZD', 'name': 'РЖД Бонус', 'name_en': 'RZD'},
        {'service_type': ServiceType.hotel, 'code': 'UZ', 'name': 'PEGASUS', 'name_en': 'PEGASUS'},
    ]
    for provider_fields in providers_fields:
        await f.create_service_provider(**provider_fields)

    await f.create_person(person_id=person_id)

    ids = []
    for i, card_number in enumerate(card_numbers or []):
        index = i % len(providers_fields)
        service_provider = providers_fields[index]
        ids.append(
            await f.create_bonus_card(
                person_id=person_id,
                number=card_number,
                provider_type=service_provider['service_type'],
                provider_code=service_provider['code'],
            )
        )
    return ids


async def test_person_bonus_card_list(f, client):
    person_id = 1
    await _create_person_with_bonus_cards(f, person_id=person_id, card_numbers=['123', '456'])

    response = await client.get(url=f'api/persons/{person_id}/bonus_cards')
    assert response.status_code == 200

    data = response.json()

    expected_list = [
        {
            'number': '123',
            'person_id': 1,
            'service_provider_code': 'S7',
            'service_provider_type': 'avia',
            'name': {
                'ru': 'Эс сэвэн',
                'en': 'S seven',
            },
        },
        {
            'number': '456',
            'person_id': 1,
            'service_provider_code': 'RZD',
            'service_provider_type': 'rail',
            'name': {
                'ru': 'РЖД Бонус',
                'en': 'RZD',
            },
        }
    ]

    for item in data:
        item.pop('bonus_card_id')
        assert item in expected_list


async def test_person_bonus_card_detail(f, client):
    person_id = 1
    [card_id] = await _create_person_with_bonus_cards(f, person_id=person_id, card_numbers=['123'])

    response = await client.get(url=f'api/persons/{person_id}/bonus_cards/{card_id}')
    data = response.json()
    assert response.status_code == 200
    assert data['bonus_card_id'] == card_id
    assert data['number'] == '123'
    assert data['person_id'] == 1
    assert data['service_provider_code'] == 'S7'
    assert data['service_provider_type'] == 'avia'


async def test_person_bonus_card_create(f, client):
    person_id = 1
    await _create_person_with_bonus_cards(
        f, person_id=person_id, card_numbers=['123', '234', '345'],
    )

    response = await client.post(
        url=f'api/persons/{person_id}/bonus_cards',
        json={
            'number': '321',
            'service_provider_type': 'hotel',
            'service_provider_code': 'UZ',
        }
    )
    data = response.json()
    assert response.status_code == 201

    assert 'bonus_card_id' in data


async def test_person_bonus_card_delete(f, client):
    person_id = 1
    ids = await _create_person_with_bonus_cards(
        f, person_id=person_id, card_numbers=['123', '234', '345'],
    )

    response = await client.delete(url=f'api/persons/{person_id}/bonus_cards/{ids[0]}')
    assert response.status_code == 204
