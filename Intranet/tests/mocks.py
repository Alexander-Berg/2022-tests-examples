import asyncio
from aioredis_lock import RedisLock


def async_return(result):
    f = asyncio.Future()
    f.set_result(result)
    return f


class StaffApiGatewayMock:

    def __init__(self, *args, **kwargs):
        pass

    @classmethod
    async def init(cls, *args, **kwargs):
        return cls()

    async def get_logins_by_uids(self, uids):
        return {u: u[::-1] for u in uids}

    async def get_ids_by_uids(self, uids):
        return {u: int(u) for u in uids}


class MockRedisLock(RedisLock):
    async def acquire(self, timeout=30, wait_timeout=30) -> bool:
        return async_return(True)

    async def release(self) -> bool:
        return async_return(True)


class MockedAeroclubClient:

    async def add_service_to_trip(self, *args, **kwargs):
        return {
            'data': [{
                'order_number': 1,
                'service_number': 1,
            }]
        }

    async def get_service(self, *args, **kwargs):
        return {
            'travellers': [],
        }

    async def cancel_service(self, *args, **kwargs):
        return {}

    async def add_person_to_service(self, *args, **kwargs):
        return {}

    async def get_profile(self, *args, **kwargs):
        return {
            'id': 957526,
            'date_of_birth': '1900-01-01T00:00:00',
            'first_name': {'ru': 'Константин', 'en': 'Konstantin'},
            'middle_name': {'ru': None, 'en': None},
            'last_name': {'ru': 'Кудрявцев', 'en': 'Kudryavtsev'},
            'sex': 'Male',
            'title': -1,
            'created_at': '1900-01-01T00:00:00',
            'updated_at': '1900-01-01T00:00:00',
            'is_vip': False,
            'photo_fingerprint': None,
            'contacts': {
                'email_addresses': [],
                'phone_numbers': [
                    {
                        'type': 'mobile',
                        'country_code': '7',
                        'area_code': '925',
                        'phone_number': '0475558',
                        'extension_number': None,
                    },
                ]
            },
            'version': 'none',
            'company_info': {
                'id': 42436,
                'company_name': {'ru': 'ЯНДЕКС', 'en': 'YANDEX'},
                'holding_name': None,
            },
            'work_location_city': None,
            'preference': {
                'id': 0,
                'comments': None,
                'internal_comments': None,
                'preference_informations': None,
            },
            'additional_info': [
                {
                    'name_en': 'LOGIN',
                    'name_ru': 'Логин сотрудника',
                    'value_en': 'poisoner',
                    'value_ru': 'poisoner',
                },
                {
                    'name_en': 'PURPOSE',
                    'name_ru': 'Назначение',
                    'value_en': 'to act',
                    'value_ru': 'рабочая командировка',
                }
            ],
            'bonus_cards': [
                {
                    'id': 105590,
                    'bonus_card_issuer': None,
                    'bonus_card_code': 'PC',
                    'number': '19888891',
                    'kind_type': 'Avia',
                },
                {
                    'id': 105593,
                    'bonus_card_issuer': None,
                    'bonus_card_code': 'KB',
                    'number': '88911988',
                    'kind_type': 'Hotel',
                }
            ],
            'documents': [],
        }
