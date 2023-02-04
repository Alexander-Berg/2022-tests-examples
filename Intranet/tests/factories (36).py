import datetime
import uuid
from typing import Any

from intranet.trip.src import enums
from sqlalchemy.dialects.postgresql import insert
from intranet.trip.src.db.tables import (
    person_table,
    person_relationship_table,
    ext_person_table,
)


date_from = datetime.date.today() + datetime.timedelta(days=60)
date_from_str = date_from.strftime('%Y-%m-%d')
date_middle = datetime.date.today() + datetime.timedelta(days=65)
date_middle_str = date_middle.strftime('%Y-%m-%d')
date_to = datetime.date.today() + datetime.timedelta(days=70)
date_to_str = date_to.strftime('%Y-%m-%d')
date_to_modified = datetime.date.today() + datetime.timedelta(days=75)
date_to_modified_str = date_to_modified.strftime('%Y-%m-%d')


class Factory:

    def __init__(self, conn):
        self.conn = conn

    async def _create_entry(self, table, fields: dict[str, Any]):
        query = insert(table).values(**fields).on_conflict_do_nothing(constraint=table.primary_key)
        return await self.conn.execute(query)

    async def _create_entry_without_pk(self, table, fields: dict[str, Any]):
        query = insert(table).values(**fields)
        return await self.conn.execute(query)

    async def _create_entry_returning_id(self, table, fields: dict[str, Any], id_field):
        query = (
            insert(table)
            .values(**fields)
            .returning(id_field)
            .on_conflict_do_nothing(constraint=table.primary_key)
        )
        return await self.conn.scalar(query)

    async def create_holding(self, holding_id=1, **fields):
        from intranet.trip.src.db.tables import holding_table

        fields = {
            'holding_id': holding_id,
            'name': 'Холдинг',
            **fields,
        }
        return await self._create_entry(holding_table, fields)

    async def create_company(self, company_id=1, holding_id=None, **fields):
        """
        Создание компании.
        Если holding_id == None, то будет создан новый холдинг
        """
        from intranet.trip.src.db.tables import company_table

        if holding_id is None:
            holding_id = 1
            await self.create_holding(holding_id=holding_id)

        fields = {
            'company_id': company_id,
            'name': 'Компания',
            'aeroclub_company_id': 1,
            'aeroclub_company_uid': 'company',
            'provider': enums.Provider.aeroclub,
            'holding_id': holding_id,
            **fields,
        }
        return await self._create_entry(company_table, fields)

    async def add_company_domain(self, company_id: int, domain: str):
        """
        Добавление доменов к компании
        """
        from intranet.trip.src.db.tables import company_domain_table

        fields = {
            'company_id': company_id,
            'domain': domain,
        }
        return await self._create_entry(company_domain_table, fields)

    async def create_person(self, person_id=1, company_id=None, **fields):
        if company_id is None:
            company_id = 1
            await self.create_company(company_id)

        fields = {
            'person_id': person_id,
            'uid': str(person_id),
            'login': 'user',
            'first_name': 'Test',
            'last_name': 'Test',
            'first_name_ac': 'Test',
            'last_name_ac': 'Test',
            'first_name_ac_en': 'Test',
            'last_name_ac_en': 'Test',
            'provider_profile_id': 1234,
            'is_dismissed': False,
            'gender': 'male',
            'phone_number': '+79991111111',
            'email': 'a@a.ru',
            'date_of_birth': '1990-01-01',
            'company_id': company_id,
            **fields,
        }
        return await self._create_entry(person_table, fields)

    async def create_person_without_company(self, person_id=1, **fields):
        fields = {
            'person_id': person_id,
            'uid': str(person_id),
            'login': 'user',
            'first_name': 'Test',
            'last_name': 'Test',
            'is_dismissed': False,
            **fields,
        }
        return await self._create_entry(person_table, fields)

    async def create_ext_person(self, person_id=1, ext_person_id=1, **fields):
        fields = {
            'person_id': person_id,
            'ext_person_id': ext_person_id,
            'name': 'Alias',
            'secret': 'secret',
            'status': enums.ExtPersonStatus.pending,
            'email': 'email@email.com',
            'external_uid': '123',
            'date_of_birth': '1990-01-01',
        }
        return await self._create_entry(ext_person_table, fields)

    async def create_chief_relation(self, chief_id: int, person_id: int, is_direct: bool = False):
        fields = {
            'owner_id': chief_id,
            'dependant_id': person_id,
            'role': enums.PersonRole.chief,
            'is_direct': is_direct,
        }

        return await self._create_entry(person_relationship_table, fields)

    async def create_approver_relation(self, approver_id: int, person_id: int):
        fields = {
            'owner_id': approver_id,
            'dependant_id': person_id,
            'role': enums.PersonRole.approver,
        }
        return await self._create_entry(person_relationship_table, fields)

    async def create_person_document(self, person_id=1, document_id=1, **fields):
        from intranet.trip.src.db.tables import person_document_table

        fields = {
            'person_id': person_id,
            'document_id': document_id,
            'document_type': enums.DocumentType.other,
            'first_name': 'First',
            'last_name': 'Last',
            **fields,
        }
        return await self._create_entry(person_document_table, fields)

    async def create_purpose(self, purpose_id=1, **fields):
        from intranet.trip.src.db.tables import purpose_table

        fields = {
            'purpose_id': purpose_id,
            'name': 'purpose',
            'name_en': 'purpose_en',
            'kind': enums.PurposeKind.any,
            'aeroclub_grade': 16,
            **fields,
        }
        return await self._create_entry(purpose_table, fields)

    async def create_person_trip(
            self,
            trip_id=1,
            person_id=1,
            document_id: int = None,
            route: list[dict] = None,
            **fields,
    ):
        from intranet.trip.src.db.tables import person_trip_table, person_trip_document_table

        fields = {
            'trip_id': trip_id,
            'person_id': person_id,
            'provider': enums.Provider.aeroclub,
            'city_from': 'Moscow',
            'description': 'description',
            'is_hidden': False,
            'with_days_off': True,
            'status': enums.PTStatus.new,
            'is_approved': True,
            'is_authorized': True,
            'aeroclub_journey_id': 1,
            'aeroclub_trip_id': 1,
            **fields,
        }
        await self._create_entry(person_trip_table, fields)
        if document_id:
            await self._create_entry(
                person_trip_document_table,
                fields={
                    'trip_id': trip_id,
                    'person_id': person_id,
                    'document_id': document_id,
                }
            )
        if route is not None:
            for i, point in enumerate(route):
                await self.create_person_trip_route_point(
                    point_id=i + 1,
                    trip_id=trip_id,
                    person_id=person_id,
                    **point,
                )

    async def create_person_trip_document(self, trip_id, person_id, document_id):
        from intranet.trip.src.db.tables import person_trip_document_table

        fields = {
            'trip_id': trip_id,
            'person_id': person_id,
            'document_id': document_id,
        }
        await self._create_entry(person_trip_document_table, fields)

    async def create_trip_purpose(self, purpose_id, trip_id, **fields):
        from intranet.trip.src.db.tables import trip_purpose_table

        fields = {
            'purpose_id': purpose_id,
            'trip_id': trip_id,
            **fields
        }
        await self._create_entry(trip_purpose_table, fields)

    async def create_person_trip_purpose(self, purpose_id, trip_id, person_id, **fields):
        from intranet.trip.src.db.tables import person_trip_purpose_table

        fields = {
            'purpose_id': purpose_id,
            'trip_id': trip_id,
            'person_id': person_id,
            **fields
        }
        await self._create_entry(person_trip_purpose_table, fields)

    async def create_trip(
            self,
            trip_id=1,
            author_id=1,
            purpose_ids: list[int] = None,
            person_ids: list[int] = None,
            manager_ids: list[int] = None,
            route: list[dict] = None,
            **fields,
    ):
        from intranet.trip.src.db.tables import trip_table

        if purpose_ids is None:
            p_id = 9876
            await self.create_purpose(purpose_id=p_id)
            purpose_ids = [9876]

        person_ids = person_ids or []
        manager_ids = manager_ids or [None] * len(person_ids)

        fields = {
            'trip_id': trip_id,
            'city_from': 'Moscow',
            'city_to': 'Ivanovo',
            'date_from': date_from.strftime('%Y-%m-%d'),
            'date_to': date_to.strftime('%Y-%m-%d'),
            'author_id': author_id,
            'issue_travel': 'TRAVEL-123456',
            **fields
        }
        await self._create_entry(trip_table, fields)
        assert 1 <= len(purpose_ids) <= 2
        for purpose_id in purpose_ids:
            await self.create_trip_purpose(purpose_id=purpose_id, trip_id=trip_id)
        for i in range(len(person_ids)):
            person_id = person_ids[i]
            manager_id = manager_ids[i]
            await self.create_person_trip(
                trip_id=trip_id,
                person_id=person_id,
                manager_id=manager_id,
            )
            for purpose_id in purpose_ids:
                await self.create_person_trip_purpose(purpose_id, trip_id, person_id)
        if route is not None:
            for i, point in enumerate(route):
                await self.create_trip_route_point(
                    point_id=i + 1,
                    trip_id=trip_id,
                    **point,
                )
                for person_id in person_ids:
                    await self.create_person_trip_route_point(
                        point_id=i + 1,
                        trip_id=trip_id,
                        person_id=person_id,
                        **point,
                    )

    async def create_trip_route_point(self, point_id=1, **fields):
        from intranet.trip.src.db.tables import trip_route_point_table
        fields = {
            'point_id': point_id,
            **fields,
        }
        await self._create_entry(trip_route_point_table, fields)

    async def create_person_trip_route_point(self, point_id=1, **fields):
        from intranet.trip.src.db.tables import person_trip_route_point_table
        fields = {
            'point_id': point_id,
            **fields,
        }
        await self._create_entry(person_trip_route_point_table, fields)

    async def create_city(self, city_id=1, **fields):
        from intranet.trip.src.db.tables import city_table

        fields = {
            'city_id': city_id,
            'code': 'MASHALLAH',

            **fields,
        }
        await self._create_entry(city_table, fields)

    async def create_conf_details(self, trip_id=1, **fields):
        from intranet.trip.src.db.tables import conf_details_table

        fields = {
            'trip_id': trip_id,
            'conference_name': 'Конференция по новым компьютерным технологиям '
                               'и защите компьютерных программ',
            'tracker_issue': 'INTERCONF-999',
            'conf_date_from': date_from,
            'conf_date_to': date_to,
            'conference_url': 'https://yandex.ru',
            'program_url': 'https://yandex.ru/program',
            'participation_terms': 'participation terms',
            'cancellation_terms': 'бесплатно',
            'price': '0',
            'promo_code': 'qwerty',
            'ticket_type': 'test',

            **fields,
        }
        await self._create_entry(conf_details_table, fields)

    async def create_travel_details(self, trip_id=1, person_id=1, city_id=1, **fields):
        from intranet.trip.src.db.tables import travel_details_table

        fields = {
            'trip_id': trip_id,
            'person_id': person_id,
            'city_id': city_id,
            'tracker_issue': '',
            'is_created_on_provider': False,
            'need_visa_assistance': False,

            'taxi_date_from': None,
            'taxi_date_to': None,
            'is_taxi_activated': False,
            'is_drive_activated': False,

            'taxi_access_phone': '+79256789876',
            'comment': 'Камент.',

            **fields,
        }
        await self._create_entry(travel_details_table, fields)

    async def create_person_conf_details(self, trip_id=1, person_id=1, **fields):
        from intranet.trip.src.db.tables import person_conf_details_table

        fields = {
            'trip_id': trip_id,
            'person_id': person_id,
            'role': enums.ConferenceParticiationType.speaker.name,
            'tracker_issue': 'INTERCONF-888',
            'comment': 'Камент.',
            'price': '0',
            'promo_code': 'qwerty',
            'badge_name': 'badge name',
            'badge_position': 'badge position',
            **fields,
        }
        await self._create_entry(person_conf_details_table, fields)

    async def create_service(self, service_id=1, trip_id=1, person_id=1, **fields):
        from intranet.trip.src.db.tables import service_table

        fields = {
            'service_id': service_id,
            'trip_id': trip_id,
            'person_id': person_id,
            'type': enums.ServiceType.avia,
            'status': enums.ServiceStatus.draft,
            'boarding_pass': '123',
            'provider_service_id': 1,
            'provider_order_id': 1,
            'provider_document_id': 1,
            **fields,
        }
        await self._create_entry(service_table, fields)

    async def create_service_provider(
        self,
        service_type: enums.ServiceType = enums.ServiceType.avia,
        code: str = 'SU',
        name: str = 'Name',
        name_en: str = 'Name',
    ):
        from intranet.trip.src.db.tables import service_provider_table
        fields = {
            'service_type': service_type,
            'code': code,
            'name': name,
            'name_en': name_en,
        }
        await self._create_entry(service_provider_table, fields)

    async def create_bonus_card(
        self,
        person_id: int = None,
        ext_person_id: int = None,
        number: str = '123',
        provider_type: enums.ServiceType = enums.ServiceType.avia,
        provider_code: str = 'SU',
    ) -> int:
        from intranet.trip.src.db.tables import person_bonus_card_table

        assert person_id or ext_person_id

        fields = {
            'person_id': person_id,
            'ext_person_id': ext_person_id,
            'number': number,
            'service_provider_type': provider_type or enums.ServiceType.avia,
            'service_provider_code': provider_code or 'S7',
        }
        id_field = person_bonus_card_table.c.bonus_card_id

        return await self._create_entry_returning_id(person_bonus_card_table, fields, id_field)

    async def create_transaction(
        self,
        transaction_id: uuid.UUID,
        company_id: int,
        person_id: int,
        trip_id: int,
        service_type=enums.ServiceType.avia,
        status=enums.TransactionStatus.paid,
        type=enums.TransactionType.purchase,
        price=100,
        provider_fee=150,
        yandex_fee=250,
        is_obsolete=False,
        **fields
    ):
        from intranet.trip.src.db.tables import billing_transactions_table
        fields = {
            'transaction_id': transaction_id,
            'company_id': company_id,
            'service_type': service_type,
            'trip_id': trip_id,
            'person_id': person_id,
            'status': status,
            'type': type,
            'price': price,
            'provider_fee': provider_fee,
            'yandex_fee': yandex_fee,
            'is_obsolete': is_obsolete,
            **fields,
        }

        await self._create_entry_without_pk(billing_transactions_table, fields)

    async def create_deposit(
        self,
        company_id: int,
        charge_date: datetime.date,
        amount: int,
        author_id: int,
        **fields,
    ):
        from intranet.trip.src.db.tables import billing_deposit_table
        fields = {
            'company_id': company_id,
            'charge_date': charge_date,
            'amount': amount,
            'author_id': author_id,
            **fields,
        }
        await self._create_entry_without_pk(billing_deposit_table, fields)
