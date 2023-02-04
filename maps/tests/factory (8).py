from decimal import Decimal
from typing import List, Optional, Sequence
from datetime import datetime

import pytest
from asyncpg import Connection

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def list_all_landing_data(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM landing_data
            ORDER BY id
            """
        )

        return list(map(dict, rows))

    async def fetch_landing_data(self, biz_id: int, kind: LandingVersion) -> Optional[dict]:
        row = await self._con.fetchrow(
            """
            SELECT landing_data.*
            FROM landing_data
            JOIN biz_state ON (
                CASE $2
                    WHEN 'STABLE' THEN biz_state.stable_version = landing_data.id
                    WHEN 'UNSTABLE' THEN biz_state.unstable_version = landing_data.id
                    ELSE FALSE
                END
            )
            WHERE biz_state.biz_id = $1
            """,
            biz_id,
            kind.value,
        )

        return dict(row) if row else None

    async def list_all_biz_states(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM biz_state
            ORDER BY id
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_promos(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM promotions
            ORDER BY promotion_id
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_promoted_cta(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM promoted_cta
            ORDER BY cta_id
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_promoted_service_lists(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM promoted_service_lists
            ORDER BY list_id
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_promoted_services(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM promoted_services
            ORDER BY service_id
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_call_tracking(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM call_tracking
            ORDER BY phone_id
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_google_counters(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM google_counters
            ORDER BY permalink
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_avatars(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM avatars
            ORDER BY source_url
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_market_int_services(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM market_int_services
            ORDER BY biz_id
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_tiktok_pixels(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM tiktok_pixels
            ORDER BY permalink
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_goods_data(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM goods_data
            ORDER BY permalink
            """
        )

        return list(map(dict, rows))

    async def retrieve_all_vk_pixels(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM vk_pixels
            ORDER BY permalink
            """
        )

        return list(map(dict, rows))

    async def create_avatars(
        self,
        *,
        source_url: str = "http://source.url/1",
        avatars_group_id: int = 8752,
        avatars_name: str = "ahebbfvh",
    ) -> None:
        await self._con.fetchval(
            """
            INSERT INTO avatars (source_url, avatars_group_id, avatars_name)
            VALUES ($1, $2, $3)
            """,
            source_url,
            avatars_group_id,
            avatars_name,
        )

    async def create_substitution_phone(
        self,
        *,
        phone_id: int = 23254,
        biz_id: int = 15,
        formatted_phone: str = "+7 (800) 200-06-00",
    ) -> None:
        await self._con.fetchval(
            """
            INSERT INTO call_tracking (phone_id, biz_id, formatted_phone)
            VALUES ($1, $2, $3)
            """,
            phone_id,
            biz_id,
            formatted_phone,
        )

    async def create_google_counters(
        self,
        *,
        permalink: int = 54321,
        counter_data: List[dict] = None,
    ) -> None:
        if counter_data is None:
            counter_data = [{"id": "ZZZ", "goals": {"route": "ggg123", "call": "ggg234"}}]
        await self._con.fetchval(
            """
            INSERT INTO google_counters (permalink, counter_data)
            VALUES ($1, $2)
            """,
            permalink,
            counter_data,
        )

    async def create_promoted_service_list(
        self,
        *,
        list_id: int = 87653,
        biz_id: int = 15,
        services: List[int] = (4, 8, 15, 16, 23, 42),
    ) -> None:
        await self._con.fetchval(
            """
            INSERT INTO promoted_service_lists (list_id, biz_id, services)
            VALUES ($1, $2, $3)
            """,
            list_id,
            biz_id,
            services,
        )

    async def create_promoted_service(
        self,
        *,
        service_id: int,
        biz_id: int = 15,
        title: str = "КУСЬный бургер",
        image: Optional[str] = "https://avatars.mds.yandex.net/2a0000016a0c63891/img",
        cost: Optional[str] = "100500.25",
        url: Optional[str] = "https://example.com",
        description: Optional[str] = "Описание",
    ) -> None:
        await self._con.fetchval(
            """
            INSERT INTO promoted_services
            (service_id, biz_id, title, cost, image, url, description)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            """,
            service_id,
            biz_id,
            title,
            Decimal(cost),
            image,
            url,
            description,
        )

    async def create_promotion(
        self,
        *,
        promotion_id: int = 35672,
        biz_id: int = 15,
        announcement: str = "Купи 1 кружку кофе и вторую тоже купи",
        description: str = "Самый лучший кофе в городе",
        date_from: str = "2020-04-12",
        date_to: str = "2020-05-11",
        banner_img: Optional[str] = "https://avatars.mds.yandex.net/2a0000016a0c63891/banner",
        link: Optional[str] = "http://promotion.link",
    ) -> None:
        await self._con.fetchval(
            """
            INSERT INTO promotions (promotion_id, biz_id, announcement, description,
                date_from, date_to, banner_img, link)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            """,
            promotion_id,
            biz_id,
            announcement,
            description,
            date_from,
            date_to,
            banner_img,
            link,
        )

    async def create_promoted_cta(
        self,
        *,
        cta_id: int = 4321,
        biz_id: int = 15,
        title: str = "Перейти на сайт",
        link: str = "http://promoted.cta//link",
    ) -> None:
        await self._con.fetchval(
            """
            INSERT INTO promoted_cta (cta_id, biz_id, title, link)
            VALUES ($1, $2, $3, $4)
            """,
            cta_id,
            biz_id,
            title,
            link,
        )

    async def create_market_int_service(
        self,
        *,
        service_id: int = 123,
        biz_id: int = 54321,
        service_data: dict = None,
    ) -> None:
        if service_data is None:
            service_data = {
                "id": 12345,
                "name": "service",
                "description": "This is service",
                "categories": [],
                "min_cost": "0.1",
                "min_duration": "10",
                "action_type": "action",
                "image": "http://images,com/12345",
            }
        await self._con.fetchval(
            """
            INSERT INTO market_int_services (service_id, biz_id, service_data)
            VALUES ($1, $2, $3)
            """,
            service_id,
            biz_id,
            service_data,
        )

    async def create_tiktok_pixels(
        self,
        permalink: int = 54321,
        pixel_data: List[dict] = None,
    ) -> None:
        if pixel_data is None:
            pixel_data = [{"id": "ZZZ", "goals": {"route": "ggg123", "call": "ggg234"}}]
        await self._con.fetchval(
            """
            INSERT INTO tiktok_pixels (permalink, pixel_data)
            VALUES ($1, $2)
            """,
            permalink,
            pixel_data,
        )

    async def create_goods_data(
        self,
        permalink: int = 54321,
        categories: dict = None,
    ) -> None:
        if categories is None:
            categories = {
                "categories": [{"name": "Категория 1"}],
                "source_name": "Источник",
            }
        await self._con.fetchval(
            """
            INSERT INTO goods_data (permalink, categories)
            VALUES ($1, $2)
            """,
            permalink,
            categories,
        )

    async def create_vk_pixels(
        self,
        permalink: int = 54321,
        pixel_data: List[dict] = None,
    ) -> None:
        if pixel_data is None:
            pixel_data = [{"id": "ZZZ", "goals": {"route": "ggg123", "call": "ggg234"}}]
        await self._con.fetchval(
            """
            INSERT INTO vk_pixels (permalink, pixel_data)
            VALUES ($1, $2)
            """,
            permalink,
            pixel_data,
        )

    async def fetch_biz_state(self, biz_id: int) -> Optional[dict]:
        row = await self._con.fetchrow(
            """
            SELECT *
            FROM biz_state
            WHERE biz_id = $1
            """,
            biz_id,
        )

        return dict(row) if row else None

    async def fetch_aliases(self, biz_id: int) -> Optional[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM slug_aliases
            WHERE biz_id = $1
            """,
            biz_id,
        )

        return rows if rows else None

    async def insert_biz_state(
        self,
        *,
        biz_id: int = 15,
        slug: str = "cafe",
        permalink: str = "54321",
        stable_version: Optional[int] = None,
        unstable_version: Optional[int] = None,
        published: Optional[bool] = False,
        blocked: Optional[bool] = False,
        blocking_data: Optional[dict] = None,
    ) -> int:
        return await self._con.fetchval(
            """
            INSERT INTO biz_state (biz_id, slug, permalink, stable_version,
                unstable_version, published, blocked, blocking_data
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            RETURNING id
            """,
            biz_id,
            slug,
            permalink,
            stable_version,
            unstable_version,
            published,
            blocked,
            blocking_data,
        )

    async def insert_slug_alias(
        self,
        *,
        biz_id: int = 15,
        slug: str = "cafe",
        expiration_date: Optional[datetime] = None,
    ) -> int:
        if expiration_date is None:
            return await self._con.fetchval(
                """
                INSERT INTO slug_aliases (biz_id, slug)
                VALUES ($1, $2)
                RETURNING id
                """,
                biz_id,
                slug
            )
        else:
            return await self._con.fetchval(
                """
                INSERT INTO slug_aliases (biz_id, slug, expiration_date)
                VALUES ($1, $2, $3)
                RETURNING id
                """,
                biz_id,
                slug,
                expiration_date,
            )

    async def insert_landing_data(
        self,
        *,
        name: str = "Кафе здесь",
        categories: Sequence[str] = ("Кафе", "Ресторан"),
        description: Optional[str] = "Описание",
        logo: Optional[str] = "https://images.com/logo",
        cover: Optional[str] = "https://images.com/cover",
        contacts: Optional[dict] = None,
        extras: Optional[dict] = None,
        preferences: Optional[dict] = None,
        blocks_options: Optional[dict] = None,
        schedule: Optional[dict] = None,
        photos: Optional[dict] = None,
        photo_settings: Optional[dict] = None,
        chain_id: Optional[int] = None,
        is_updated_from_geosearch: bool = False,
        landing_type: Optional[str] = "DEFAULT",
    ) -> int:
        if contacts is None:
            contacts = {
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "vkontakte": "http://vk.com/cafe",
            }
        if extras is None:
            extras = {}
        if preferences is None:
            preferences = {
                "personal_metrika_code": "metrika_code",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
                "social_buttons": [{
                    "type": "VK",
                    "url": "https://vk.com",
                    "custom_text": "VK",
                }],
            }
        if blocks_options is None:
            blocks_options = {
                "show_cover": True,
                "show_logo": True,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": True,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            }

        return await self._con.fetchval(
            """
            INSERT INTO landing_data (
                name, categories, description, logo, cover, contacts, extras,
                preferences, blocks_options, schedule, photos, photo_settings,
                chain_id, is_updated_from_geosearch, landing_type
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
            RETURNING id
            """,
            name,
            categories,
            description,
            logo,
            cover,
            contacts,
            extras,
            preferences,
            blocks_options,
            schedule,
            photos,
            photo_settings,
            chain_id,
            is_updated_from_geosearch,
            landing_type,
        )

    async def fetch_cached_landing_config(self):
        return await self._con.fetchval("SELECT data FROM cached_landing_config")

    async def set_cached_landing_config(self, data: dict) -> None:
        await self._con.execute("UPDATE cached_landing_config SET data = $1", data)

    async def set_google_counters_for_permalink(self, permalink: int, data: list[dict]) -> None:
        await self._con.execute(
            "INSERT INTO google_counters (permalink, counter_data) VALUES ($1, $2)",
            permalink,
            data,
        )


@pytest.fixture
def factory(con):
    return Factory(con)
