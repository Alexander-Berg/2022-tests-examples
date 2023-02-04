from datetime import datetime, timezone
from typing import Optional

from asyncpg import Connection


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def create_request(
        self,
        request_id: Optional[int] = None,
        external_id: str = "abc",
        passport_uid: int = 123,
        processed_at: Optional[datetime] = None,
        created_at: Optional[datetime] = None,
    ) -> int:
        return await self._con.fetchval(
            """
            INSERT INTO delete_requests (
                id,
                external_id,
                passport_uid,
                processed_at,
                created_at
            )
            VALUES (coalesce($1, nextval('delete_requests_id_seq')), $2, $3, $4, $5)
            RETURNING id
        """,
            request_id,
            external_id,
            passport_uid,
            processed_at,
            created_at if created_at else datetime.now(tz=timezone.utc),
        )

    async def create_operation(
        self,
        request_id: int,
        service_name: str = "miracle",
        metadata: Optional[dict] = None,
        is_success: bool = True,
    ) -> int:
        return await self._con.fetchval(
            """
            INSERT INTO delete_operations (
                request_id,
                service_name,
                metadata,
                is_success
            )
            VALUES ($1, $2, $3, $4)
            RETURNING id
        """,
            request_id,
            service_name,
            metadata,
            is_success,
        )
