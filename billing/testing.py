from datetime import date
from typing import List, Optional

from sendr_interactions.clients.spark import SparkClient as BaseSparkClient
from sendr_interactions.clients.spark import SparkData
from sendr_interactions.clients.spark.entities import (
    AddressData,
    LeaderData,
    MerchantType,
    OrganizationData,
    PhoneData,
    SparkAddressData,
    SparkOrganizationData,
)
from sendr_interactions.clients.spark_suggest import SparkSuggestClient as BaseSparkSuggestClient
from sendr_interactions.clients.spark_suggest import SparkSuggestItem

from billing.yandex_pay_admin.yandex_pay_admin.interactions.base import BaseInteractionClient


class TestingSparkClient(BaseInteractionClient, BaseSparkClient):
    async def get_info(self, inn: str, spark_id: Optional[int] = None) -> SparkData:
        return SparkData(
            spark_id='6440744',
            organization_data=SparkOrganizationData(
                organization=OrganizationData(
                    type=MerchantType.OOO,
                    name='ООО "ГРУЗОВОЕ ТАКСИ "ГАЗЕЛЬКИН"',
                    english_name='OOO GRUZOVOE TAKSI GAZELKIN',
                    full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ГРУЗОВОЕ ТАКСИ "ГАЗЕЛЬКИН"',
                    inn='7839382402',
                    kpp='783901001',
                    ogrn='1089847201938',
                ),
                actual_date=date(2022, 6, 17),
            ),
            okved_list=[],
            registration_date=date(2008, 5, 16),
            leaders=[
                LeaderData(
                    name='Жданов Игорь Александрович',
                    position='генеральный директор',
                    inn='780431553754',
                    actual_date=date(2020, 3, 16),
                )
            ],
            addresses=[
                SparkAddressData(
                    address=AddressData(
                        type='legal',
                        city='Санкт-Петербург',
                        country='RUS',
                        street='набережная Обводного Канала',
                        zip='190020',
                        home='148/2 литера А',
                    ),
                    actual_date=date(2022, 1, 1),
                )
            ],
            phones=[
                PhoneData(code='812', number='4060707', verification_date=None),
                PhoneData(code='812', number='6000606', verification_date=date(2015, 10, 1)),
            ],
            active=True,
        )


class TestingSparkSuggestClient(BaseInteractionClient, BaseSparkSuggestClient):
    DATA = [
        SparkSuggestItem(
            spark_id=10229326,
            name='ЯНДЕКС.ТАКСИ, ООО',
            full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ  ЯНДЕКС.ТАКСИ',
            inn='7704340310',
            ogrn='5157746192731',
            address='123112, г. Москва, проезд 1-Й Красногвардейский, д. 21 стр. 1 пом. 36.9 этаж 36',
            leader_name='Аникин Александр Михайлович',
            region_name='Москва',
        ),
        SparkSuggestItem(
            spark_id=6440744,
            name='ГРУЗОВОЕ ТАКСИ ГАЗЕЛЬКИН, ООО',
            full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ  ГРУЗОВОЕ ТАКСИ  ГАЗЕЛЬКИН',
            inn='7839382402',
            ogrn='1089847201938',
            address='190020, г. Санкт-Петербург, набережная Обводного Канала, д. 148 к. 2 литера А пом. 221',
            leader_name='Жданов Игорь Александрович',
            region_name='Санкт Петербург',
        ),
        SparkSuggestItem(
            spark_id=8400926,
            name='ТАКСИ ВЕЗЕТ, ООО',
            full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ  ТАКСИ ВЕЗЕТ',
            inn='2308189240',
            ogrn='1122308004620',
            address='350051, Краснодарский край, г.о. город Краснодар, г Краснодар, ул Гаражная, д. 81 3, офис 4',
            leader_name='Хайруллин Ренат Фазылович',
            region_name='Краснодарский край',
        ),
    ]

    async def get_hint(self, query: str, regions: Optional[List[int]] = None) -> List[SparkSuggestItem]:
        return list([item for item in self.DATA if query.upper() in item.name or query in item.inn])
