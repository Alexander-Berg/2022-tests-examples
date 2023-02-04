from datetime import datetime, timezone
from typing import Collection, List, Optional, Set, Union

import pytest
from asyncpg import Connection
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import (
    CallEvent,
    ClientGender,
    OrderEvent,
    SegmentType,
    Source,
)

default_event_timestamp = dt("2020-01-01 00:00:00")


def make_client_creation_kwargs():
    return dict(
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
    )


def make_empty_client_creation_kwargs():
    return dict(
        biz_id=123,
        phone=None,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
    )


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def create_client(
        self,
        client_id: Optional[int] = None,
        source: Source = Source.CRM_INTERFACE,
        metadata: Optional[dict] = None,
        initiator_id: Optional[int] = 112233,
        segments: Optional[Set[SegmentType]] = None,
        labels: Collection[str] = ("mark-2021",),
        cleared_for_gdpr: bool = False,
        **kwargs,
    ) -> int:
        client_creation_kwargs = make_client_creation_kwargs()
        client_creation_kwargs.update(kwargs)

        created_id = await self._create_client(
            client_id=client_id,
            cleared_for_gdpr=cleared_for_gdpr,
            labels=labels,
            **client_creation_kwargs,
        )

        await self.create_revision(
            client_id=created_id,
            source=source,
            metadata=metadata,
            initiator_id=initiator_id,
            **client_creation_kwargs,
        )

        if segments:
            await self.add_client_to_segments(created_id, segments)

        return created_id

    async def create_empty_client(
        self,
        client_id: Optional[int] = None,
        source: Source = Source.CRM_INTERFACE,
        cleared_for_gdpr: bool = False,
        labels: Collection[str] = tuple(),
        **kwargs,
    ):
        client_creation_kwargs = make_empty_client_creation_kwargs()
        client_creation_kwargs.update(kwargs)
        return await self.create_client(
            client_id=client_id,
            source=source,
            cleared_for_gdpr=cleared_for_gdpr,
            labels=labels,
            **client_creation_kwargs,
        )

    async def retrieve_client(self, client_id: int) -> dict:
        sql = """
            SELECT
                biz_id,
                phone,
                email,
                passport_uid,
                first_name,
                last_name,
                gender,
                comment,
                labels,
                cleared_for_gdpr
            FROM clients
            WHERE id=$1
        """
        row = await self._con.fetchrow(sql, client_id)

        return dict(row) if row else None

    async def retrieve_last_revision(self, client_id: int) -> dict:
        sql = """
            SELECT
                biz_id,
                source,
                metadata,
                phone,
                email,
                passport_uid,
                first_name,
                last_name,
                gender,
                comment,
                initiator_id
            FROM client_revisions
            WHERE client_id=$1
            ORDER BY created_at DESC
            LIMIT 1
        """
        row = await self._con.fetchrow(sql, client_id)

        return dict(row) if row else None

    async def retrieve_client_revisions(self, client_id: int) -> List[dict]:
        sql = """
            SELECT
                biz_id,
                source,
                metadata,
                phone,
                email,
                passport_uid,
                first_name,
                last_name,
                gender,
                comment,
                initiator_id
            FROM client_revisions
            WHERE client_id=$1
            ORDER BY created_at DESC
        """
        rows = await self._con.fetch(sql, client_id)

        return list(map(dict, rows))

    async def list_clients(self, ignore_ids: Collection[int] = tuple()) -> List[dict]:
        sql = """
            SELECT
                biz_id,
                phone,
                email,
                passport_uid,
                first_name,
                last_name,
                gender,
                comment,
                labels
            FROM clients
            WHERE id <> ALL($1)
            ORDER BY id
        """
        rows = await self._con.fetch(sql, ignore_ids)

        return [dict(row) for row in rows]

    async def list_revisions(
        self, ignore_client_ids: Collection[int] = tuple()
    ) -> List[dict]:
        sql = """
            SELECT
                client_id,
                biz_id,
                source,
                metadata,
                phone,
                email,
                passport_uid,
                first_name,
                last_name,
                gender,
                comment,
                initiator_id
            FROM client_revisions
            WHERE client_id <> ALL($1)
            ORDER BY created_at DESC
        """
        rows = await self._con.fetch(sql, ignore_client_ids)

        return list(map(dict, rows))

    async def create_event(
        self,
        client_id: int,
        *,
        event_type: Union[OrderEvent, CallEvent],
        event_timestamp: datetime = default_event_timestamp,
        created_at: Optional[datetime] = None,
        **kwargs,
    ) -> int:
        if event_type in OrderEvent:
            return await self.create_order_event(
                client_id=client_id,
                event_type=event_type,
                event_timestamp=event_timestamp,
                created_at=created_at,
                **kwargs,
            )
        elif event_type in CallEvent:
            return await self.create_call_event(
                client_id=client_id,
                event_type=event_type,
                event_timestamp=event_timestamp,
                created_at=created_at,
                **kwargs,
            )

    async def create_order_event(
        self,
        client_id: int,
        *,
        event_type: OrderEvent = OrderEvent.CREATED,
        event_timestamp: datetime = default_event_timestamp,
        created_at: Optional[datetime] = None,
        order_id: int = 5555,
        source: Source = Source.BOOKING_YANG,
    ) -> int:
        biz_id = await self._fetch_client_biz_id(client_id)
        if created_at is None:
            created_at = datetime.now(tz=timezone.utc)

        sql = """
        INSERT INTO order_events (
            client_id,
            biz_id,
            order_id,
            event_type,
            event_timestamp,
            source,
            created_at
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7)
        RETURNING id
        """
        return await self._con.fetchval(
            sql,
            client_id,
            biz_id,
            order_id,
            event_type,
            event_timestamp,
            source.value,
            created_at,
        )

    async def create_call_event(
        self,
        client_id: int,
        *,
        event_type: CallEvent = CallEvent.INITIATED,
        event_value: Optional[str] = None,
        event_timestamp: datetime = default_event_timestamp,
        source: Source = Source.GEOADV_PHONE_CALL,
        session_id: Optional[int] = 1111,
        record_url: Optional[str] = "http://call-record-url",
        await_duration: Optional[int] = 330,
        talk_duration: Optional[int] = 30,
        geoproduct_id: Optional[int] = 2222,
        created_at: Optional[datetime] = None,
    ) -> int:
        biz_id = await self._fetch_client_biz_id(client_id)
        if created_at is None:
            created_at = datetime.now(tz=timezone.utc)

        sql = """
        INSERT INTO call_events (
            client_id,
            biz_id,
            event_type,
            event_value,
            event_timestamp,
            source,
            session_id,
            record_url,
            await_duration,
            talk_duration,
            geoproduct_id,
            created_at
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
        RETURNING id
        """
        return await self._con.fetchval(
            sql,
            client_id,
            biz_id,
            event_type,
            event_value,
            event_timestamp,
            source.value,
            session_id,
            record_url,
            await_duration,
            talk_duration,
            geoproduct_id,
            created_at,
        )

    async def create_resolved_order_events_pair(
        self,
        client_id: int,
        event_type: OrderEvent,
        *,
        event_timestamp: datetime = default_event_timestamp,
    ):
        assert event_type in (OrderEvent.ACCEPTED, OrderEvent.REJECTED)
        await self.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_timestamp,
        )
        await self.create_order_event(
            client_id=client_id,
            event_type=event_type,
            event_timestamp=event_timestamp,
        )

    async def add_client_to_segments(self, client_id: int, segments: Set[SegmentType]):
        if SegmentType.NO_ORDERS in segments and len(segments) > 1:
            raise Exception("Client must to belong to NO_ORDERS segment exclusively")
        if {SegmentType.ACTIVE, SegmentType.LOST}.issubset(segments):
            raise Exception("Client can't belong to contradictory segments")

        for segment in segments:
            if segment == SegmentType.ACTIVE:
                await self.add_client_to_active_segment(client_id=client_id)
            elif segment == SegmentType.LOST:
                await self.add_client_to_lost_segment(client_id=client_id)
            elif segment == SegmentType.REGULAR:
                await self.add_client_to_regular_segment(client_id=client_id)
            elif segment == SegmentType.UNPROCESSED_ORDERS:
                await self.add_client_to_unprocessed_orders_segment(client_id=client_id)
            elif segment == SegmentType.NO_ORDERS:
                await self.add_client_to_no_order_segment(client_id=client_id)
            elif segment == SegmentType.SHORT_LAST_CALL:
                await self.add_client_to_short_last_call_segment(client_id=client_id)
            elif segment == SegmentType.MISSED_LAST_CALL:
                await self.add_client_to_missed_last_call_segment(client_id=client_id)
            else:
                raise Exception(f"Unknown segment {segment}")

    async def add_client_to_regular_segment(self, client_id: int):
        # side effect: automatically add client to UNPROCESSED_ORDERS segment
        # sometimes it's impossible to avoid such side effects
        for _ in range(3):
            await self.create_order_event(
                client_id=client_id,
                event_type=OrderEvent.CREATED,
                event_timestamp=dt("2019-11-03 00:00:01"),
            )

    async def add_client_to_active_segment(
        self,
        client_id: int,
        event_timestamp: Optional[datetime] = None,
    ):
        event_timestamp = event_timestamp or dt("2019-11-03 00:00:01")
        await self.create_resolved_order_events_pair(
            client_id=client_id,
            event_type=OrderEvent.ACCEPTED,
            event_timestamp=event_timestamp,
        )

    async def add_client_to_lost_segment(self, client_id: int):
        await self.create_resolved_order_events_pair(
            client_id=client_id,
            event_type=OrderEvent.ACCEPTED,
            event_timestamp=dt("2018-01-01 00:00:00"),
        )

    async def add_client_to_unprocessed_orders_segment(self, client_id: int):
        await self.create_order_event(
            client_id=client_id, event_type=OrderEvent.CREATED
        )

    async def add_client_to_no_order_segment(self, client_id: int):
        await self.create_call_event(client_id)

    async def add_client_to_short_last_call_segment(self, client_id: int):
        await self.create_call_event(
            client_id=client_id,
            event_type=CallEvent.FINISHED,
            event_value="Success",
            event_timestamp=dt("2018-01-01 00:00:00"),
            talk_duration=9,
        )

    async def add_client_to_missed_last_call_segment(self, client_id: int):
        await self.create_call_event(
            client_id=client_id,
            event_type=CallEvent.FINISHED,
            event_value="NoAnswer",
            event_timestamp=dt("2018-01-01 00:00:00"),
            talk_duration=0,
        )

    async def retrieve_order_events(
        self, client_id: int, biz_id: int = 123
    ) -> List[dict]:
        sql = """
            SELECT
                client_id,
                biz_id,
                order_id,
                event_type,
                event_timestamp,
                source
            FROM order_events
            WHERE client_id=$1 AND biz_id=$2
            ORDER BY created_at DESC
        """
        rows = await self._con.fetch(sql, client_id, biz_id)

        return [dict(row) for row in rows]

    async def retrieve_call_events(
        self, client_id: int, biz_id: int = 123
    ) -> List[dict]:
        sql = """
            SELECT *
            FROM call_events
            WHERE client_id=$1 AND biz_id=$2
            ORDER BY created_at DESC
        """
        rows = await self._con.fetch(sql, client_id, biz_id)

        return [dict(row) for row in rows]

    async def _create_client(
        self,
        client_id: Optional[int],
        biz_id: int,
        phone: Optional[int],
        email: Optional[str],
        passport_uid: Optional[int],
        first_name: Optional[str],
        last_name: Optional[str],
        gender: Optional[ClientGender],
        comment: Optional[str],
        cleared_for_gdpr: bool,
        labels: Collection[str],
        created_at: Optional[datetime] = None,
    ):
        sql = """
            INSERT INTO clients (
                id,
                biz_id,
                phone,
                email,
                passport_uid,
                first_name,
                last_name,
                gender,
                comment,
                cleared_for_gdpr,
                labels,
                created_at
            )
            VALUES (
                coalesce($1, nextval('clients_id_seq')),
                $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12
            )
            RETURNING id
        """

        return await self._con.fetchval(
            sql,
            client_id,
            biz_id,
            str(phone) if phone else None,
            email,
            passport_uid,
            first_name,
            last_name,
            gender,
            comment,
            cleared_for_gdpr,
            labels,
            created_at or datetime.now(),
        )

    async def create_revision(
        self,
        client_id: int,
        source: Source = Source.CRM_INTERFACE,
        biz_id: Optional[int] = 123,
        metadata: Optional[dict] = None,
        phone: Optional[int] = 1234567890123,
        email: Optional[str] = "email@yandex.ru",
        passport_uid: Optional[int] = 456,
        first_name: Optional[str] = "client_first_name",
        last_name: Optional[str] = "client_last_name",
        gender: Optional[ClientGender] = ClientGender.MALE,
        comment: Optional[str] = "this is comment",
        initiator_id: Optional[int] = 112233,
        created_at: Optional[datetime] = None,
    ):
        sql = """
            INSERT INTO client_revisions (
                client_id,
                source,
                metadata,
                biz_id,
                phone,
                passport_uid,
                first_name,
                last_name,
                email,
                gender,
                comment,
                initiator_id,
                created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
        """

        await self._con.fetchval(
            sql,
            client_id,
            source.value,
            metadata,
            biz_id,
            str(phone) if phone else None,
            passport_uid,
            first_name,
            last_name,
            email,
            gender,
            comment,
            initiator_id,
            created_at or datetime.now(),
        )

    async def create_import_call_events_tmp_table(self):
        sql = """
            CREATE TABLE import_call_events_tmp (
            id bigserial NOT NULL,
            geoproduct_id int8 NOT NULL,
            permalink int8 NOT NULL,
            client_phone varchar(16) NOT NULL,
            event_timestamp timestamptz NOT NULL,
            await_duration smallint,
            talk_duration smallint,
            event_value varchar(512),
            session_id int8,
            record_url text,
            CONSTRAINT pk_import_call_events_tmp PRIMARY KEY (id)
        )
        """

        await self._con.execute(sql)

    async def _fetch_client_biz_id(self, client_id: int) -> Optional[int]:
        sql = """
            SELECT biz_id
            FROM clients
            WHERE id = $1
        """

        return await self._con.fetchval(sql, client_id)


@pytest.fixture
async def factory(con):
    return Factory(con)
