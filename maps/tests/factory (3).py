from collections import defaultdict
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from typing import List, Optional

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    OrderOperationType,
    PaymentType,
    PlatformType,
)

_empty = object()


class Factory:
    STARTING_ID = 100
    STARTING_DT = datetime(2000, 1, 11, tzinfo=timezone.utc)

    def __init__(self, con, faker):
        self._con = con
        self._faker = faker
        self._id_counter = defaultdict(lambda: self.STARTING_ID)
        self._dt_counter = defaultdict(lambda: int(self.STARTING_DT.timestamp()))

    def _next_id(self, key):
        self._id_counter[key] += 1
        return self._id_counter[key]

    def _next_dt(self, key):
        self._dt_counter[key] += 1
        return datetime.utcfromtimestamp(self._dt_counter[key])

    async def get_all_clients(self):
        clients_data = await self._con.fetch(
            """
                SELECT *
                FROM clients
                WHERE is_agency = FALSE
            """
        )

        return list(map(dict, clients_data))

    async def get_client(self, client_id):
        client = await self._con.fetchrow(
            """
                SELECT *
                FROM clients
                WHERE id = $1
            """,
            client_id,
        )

        return dict(client) if client else None

    async def get_client_contracts(self, client_id):
        contracts = await self._con.fetch(
            """
                SELECT *
                FROM contracts
                WHERE client_id = $1
                ORDER BY id
            """,
            client_id,
        )

        return list(map(dict, contracts))

    async def create_client(self, **kwargs):
        faker = self._faker
        values = {
            "id": self._next_id("clients"),
            "name": faker.text(max_nb_chars=256),
            "email": faker.email(),
            "phone": faker.phone_number(),
            "is_agency": False,
            "account_manager_id": self._next_id("account_manager_id"),
            "domain": "someTestDomain",
            "partner_agency_id": None,
            "has_accepted_offer": False,
            "created_at": None,
            "representatives": [],
        }
        values.update(kwargs)

        return await self._con.fetchrow(
            """
            INSERT INTO clients (
                id,
                name,
                email,
                phone,
                is_agency,
                account_manager_id,
                domain,
                partner_agency_id,
                has_accepted_offer,
                created_at,
                representatives
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
            RETURNING *
            """,
            *values.values(),
        )

    async def update_created_at(self, client_id, dt):
        return await self._con.execute(
            """
            UPDATE clients
            SET created_at = $2
            WHERE id = $1
            """,
            client_id,
            dt,
        )

    async def create_agency(self, **kwargs):
        kwargs["is_agency"] = True
        return await self.create_client(**kwargs)

    async def add_client_to_agency(self, client_id, agency_id):
        return await self._con.execute(
            """
                INSERT INTO agency_clients (agency_id, client_id)
                VALUES ($1, $2)
            """,
            agency_id,
            client_id,
        )

    async def get_agency_clients_ids(self, agency_id):
        if agency_id is not None:
            cond = "agency_id = $1"
            params = (agency_id,)
        else:
            cond = "agency_id IS NULL"
            params = ()

        ids = await self._con.fetch(
            f"""
            SELECT client_id
            FROM agency_clients
            WHERE {cond}
            """,
            *params,
        )

        return list(map(lambda r: r["client_id"], ids))

    async def create_contract(self, **kwargs):
        values = {
            "id": self._next_id("contracts"),
            "external_id": "123/321",
            "currency": CurrencyType.RUB,
            "is_active": True,
            "date_start": datetime.now(tz=timezone.utc) - timedelta(days=1),
            "date_end": datetime.now(tz=timezone.utc) + timedelta(days=1),
            "payment_type": PaymentType.PRE,
            "preferred": False,
        }
        values.update(kwargs)
        if "client_id" not in values:
            client = await self.create_client()
            values["client_id"] = client["id"]

        return await self._con.fetchrow(
            """
                INSERT INTO contracts (
                    id,
                    external_id,
                    client_id,
                    currency,
                    is_active,
                    date_start,
                    date_end,
                    payment_type,
                    preferred
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                RETURNING *
            """,
            values["id"],
            values["external_id"],
            values["client_id"],
            values["currency"].name,
            values["is_active"],
            values["date_start"],
            values["date_end"],
            values["payment_type"].name,
            values["preferred"],
        )

    async def create_product(
        self,
        *,
        oracle_id: Optional[int] = None,
        service_id: int = 110,
        version: int = 1,
        title: str = "Название продукта",
        act_text: str = "Текст акта",
        description: str = "Описание продукта",
        currency: CurrencyType = CurrencyType.RUB,
        billing_type: BillingType = BillingType.CPM,
        billing_data: Optional[dict] = None,
        vat_value: Decimal = Decimal("0.2"),  # noqa: B008
        campaign_type: CampaignType = CampaignType.PIN_ON_ROUTE,
        platforms: List[PlatformType] = None,
        min_budget: Decimal = Decimal(0),  # noqa: B008
        cpm_filters: Optional[List[str]] = None,
        comment: str = "",
        available_for_agencies: bool = True,
        available_for_internal: bool = True,
        active_from: datetime = _empty,
        active_to: datetime = _empty,
        type: str = "REGULAR",
        _without_version_=False,
    ):
        if oracle_id is None:
            oracle_id = self._next_id("product_oracle_id")
        if billing_data is None:
            if billing_type is BillingType.CPM:
                billing_data = {"base_cpm": 50}
            elif billing_type is BillingType.FIX:
                billing_data = {"cost": 50, "time_interval": "DAILY"}
            else:
                billing_data = {}

        platforms = platforms or [PlatformType.NAVI]
        cpm_filters = cpm_filters or []
        if active_from is _empty:
            active_from = datetime.now(tz=timezone.utc) - timedelta(days=1)
        if active_to is _empty:
            active_to = datetime.now(tz=timezone.utc) + timedelta(days=1)

        product = await self._con.fetchrow(
            """
            INSERT INTO products
                (oracle_id, service_id, title, act_text, description, currency,
                billing_type, vat_value, campaign_type, platform, platforms,
                comment, available_for_agencies, available_for_internal, type
                )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
            RETURNING *
            """,
            oracle_id,
            service_id,
            title,
            act_text,
            description,
            currency.name,
            billing_type.name,
            vat_value,
            campaign_type.name,
            platforms[0].name,
            list(platform.name for platform in platforms),
            comment,
            available_for_agencies,
            available_for_internal,
            type,
        )

        if not _without_version_:
            await self.create_product_version(
                product["id"],
                version,
                active_from=active_from,
                active_to=active_to,
                billing_data=billing_data,
                min_budget=min_budget,
                cpm_filters=cpm_filters,
            )

        return product

    async def create_product_version(
        self,
        product_id: int,
        version: int = 1,
        *,
        active_from: Optional[datetime] = None,
        active_to: Optional[datetime] = None,
        billing_data: Optional[dict] = None,
        min_budget: Decimal = Decimal(0),  # noqa: B008
        cpm_filters: Optional[List[str]] = None,
    ):
        billing_data = billing_data or {"base_cpm": 50}
        cpm_filters = cpm_filters or []
        active_from = active_from or datetime(2000, 1, 1, tzinfo=timezone.utc)

        return await self._con.fetchrow(
            """
                INSERT INTO product_rules_versions (
                    product_id,
                    version,
                    active_from,
                    active_to,
                    billing_data,
                    min_budget,
                    cpm_filters
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                RETURNING *
            """,
            product_id,
            version,
            active_from,
            active_to,
            billing_data,
            min_budget,
            cpm_filters,
        )

    async def restrict_product_by_client(self, product, client=None, contract_id=None):
        if client is None:
            client = await self.create_client()

        await self._con.execute(
            """
            INSERT INTO product_client_restrictions (product_id, client_id, contract_id)
            VALUES ($1, $2, $3)
            """,
            product["id"],
            client["id"],
            contract_id,
        )

        return client["id"]

    async def get_all_orders(self):
        orders = await self._con.fetch(
            """
                SELECT *
                FROM orders
                ORDER BY id
            """
        )

        return list(map(dict, orders))

    async def get_order(self, order_id):
        order = await self._con.fetchrow(
            """
                SELECT *
                FROM orders
                WHERE id = $1
            """,
            order_id,
        )

        return dict(order) if order else None

    async def create_order(self, **kwargs):
        faker = self._faker
        values = {
            "id": self._next_id("orders"),
            "service_id": 110,
            "created_at": self._next_dt("orders"),
            "tid": faker.random_int(min=0),
            "title": faker.text(max_nb_chars=256),
            "act_text": faker.text(max_nb_chars=256),
            "text": faker.text(max_nb_chars=1000),
            "comment": faker.text(max_nb_chars=1024),
            "limit": faker.pydecimal(
                left_digits=18,
                right_digits=4,
                min_value=Decimal("1000"),
                max_value=Decimal("2000"),
            ),
            "consumed": faker.pydecimal(
                left_digits=18,
                right_digits=4,
                min_value=Decimal("100"),
                max_value=Decimal("500"),
            ),
            "parent_order_id": None,
            "hidden": False,
        }
        values.update(kwargs)

        if "client_id" not in values:
            values["client_id"] = (await self.create_client())["id"]
        if "agency_id" not in values:
            values["agency_id"] = (await self.create_agency())["id"]
        if "contract_id" not in values:
            contract_client_id = (
                values["agency_id"]
                if values["agency_id"] is not None
                else values["client_id"]
            )
            values["contract_id"] = (
                await self.create_contract(client_id=contract_client_id)
            )["id"]
        if "product_id" not in values:
            values["product_id"] = (await self.create_product())["id"]
        if "external_id" not in values:
            values["external_id"] = (
                values["id"] if values["service_id"] == 110 else faker.random_int(min=0)
            )

        return await self._con.fetchrow(
            """
            INSERT INTO orders (
                id,
                external_id,
                service_id,
                created_at,
                tid,
                title,
                act_text,
                text,
                comment,
                client_id,
                agency_id,
                contract_id,
                product_id,
                parent_order_id,
                "limit",
                consumed,
                hidden
            )
            VALUES (
                $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16,
                $17
            )
            RETURNING *
            """,
            values["id"],
            values["external_id"],
            values["service_id"],
            values["created_at"],
            values["tid"],
            values["title"],
            values["act_text"],
            values["text"],
            values["comment"],
            values["client_id"],
            values["agency_id"],
            values["contract_id"],
            values["product_id"],
            values["parent_order_id"],
            values["limit"],
            values["consumed"],
            values["hidden"],
        )

    async def get_orders_logs(self, order_id: int):
        logs = await self._con.fetch(
            """
                SELECT *
                FROM order_logs
                WHERE order_id = $1
                ORDER BY created_at
            """,
            order_id,
        )

        return list(map(dict, logs))

    async def create_order_log(self, **kwargs):
        faker = self._faker
        values = {
            "op_type": OrderOperationType.CREDIT,
            "amount": faker.pydecimal(
                left_digits=18,
                right_digits=4,
                min_value=Decimal("100"),
                max_value=Decimal("200"),
            ),
            "limit": faker.pydecimal(
                left_digits=18,
                right_digits=4,
                min_value=Decimal("1000"),
                max_value=Decimal("2000"),
            ),
            "consumed": faker.pydecimal(
                left_digits=18,
                right_digits=4,
                min_value=Decimal("100"),
                max_value=Decimal("500"),
            ),
        }
        values.update(kwargs)
        if "billed_due_to" not in values:
            if values["op_type"] is OrderOperationType.DEBIT:
                values["billed_due_to"] = datetime.now(tz=timezone.utc) - timedelta(
                    days=1
                )
            else:
                values["billed_due_to"] = None

        if "order_id" not in kwargs:
            values["order_id"] = (await self.create_order())["id"]

        order_log = await self._con.fetchrow(
            """
                INSERT INTO order_logs (
                    op_type,
                    order_id,
                    amount,
                    "limit",
                    consumed,
                    billed_due_to
                )
                VALUES ($1, $2, $3, $4, $5, $6)
                RETURNING *
            """,
            values["op_type"].name,
            values["order_id"],
            values["amount"],
            values["limit"],
            values["consumed"],
            values["billed_due_to"],
        )
        if "created_at" in kwargs:
            await self._con.execute(
                "UPDATE order_logs SET created_at = $1 WHERE id = $2",
                kwargs["created_at"],
                order_log["id"],
            )

        return dict(order_log)

    async def get_product_first_version(self, product_id):
        version = await self._con.fetchrow(
            """
                SELECT *
                FROM product_rules_versions
                WHERE product_id = $1
                ORDER BY active_from
                LIMIT 1
            """,
            product_id,
        )

        return dict(version) if version else None

    async def get_inexistent_client_id(self):
        return await self._con.fetchval(
            """
                SELECT coalesce(MAX(id), 0) + 1
                FROM clients
            """
        )

    async def get_inexistent_contract_id(self):
        return await self._con.fetchval(
            """
                SELECT coalesce(MAX(id), 0) + 1
                FROM contracts
            """
        )

    async def get_clients_by_product(self, product_id):
        rows = await self._con.fetch(
            """
                SELECT product_id, client_id, contract_id
                FROM product_client_restrictions
                WHERE product_id = $1
                ORDER BY product_id, client_id, contract_id
            """,
            product_id,
        )

        return list(map(dict, rows))

    async def get_products_by_client(self, client_id):
        rows = await self._con.fetch(
            """
                SELECT product_id, client_id, contract_id
                FROM product_client_restrictions
                WHERE client_id = $1
                ORDER BY product_id, client_id, contract_id
            """,
            client_id,
        )

        return list(map(dict, rows))


__all__ = ["Factory"]
