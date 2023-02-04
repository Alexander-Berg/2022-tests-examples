from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.cart import Cart, CartItem, CartTotal, ItemQuantity
from pay.lib.entities.order import Order, PaymentStatus
from sendr_auth.entities import User

from billing.yandex_pay_admin.yandex_pay_admin.storage import Storage
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.agent import Agent
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, Partner, RegistrationData
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.role import Role


@pytest.fixture
def agent_entity(randn) -> Agent:
    return Agent(
        agent_id=uuid4(),
        oauth_client_id=str(randn()),
        name='some agent',
    )


@pytest.fixture
async def agent(storage: Storage, agent_entity):
    return await storage.agent.create(agent_entity)


@pytest.fixture
def partner_entity(agent) -> Partner:
    return Partner(
        partner_id=uuid4(),
        name='some partner name',
        registration_data=RegistrationData(
            contact=Contact(
                email='email@test',
                phone='+1(000)555-0100',
                first_name='John',
                last_name='Doe',
                middle_name='Татьянович',
            ),
            tax_ref_number='0123 АБ',
            ogrn='ogrn',
            kpp='kpp',
            legal_address='Moscow',
            postal_address='Beverly Hills, 90210',
            postal_code='90210',
            full_company_name='Yandex LLC',
            ceo_name='some ceo name',
        ),
        agent_id=agent.agent_id,
        agent_partner_id='agent_partner_id',
    )


@pytest.fixture
async def partner(storage: Storage, partner_entity: Partner):
    return await storage.partner.create(partner_entity)


@pytest.fixture
def merchant_entity(partner_entity) -> Merchant:
    return Merchant(
        merchant_id=uuid4(),
        name='some-merchant-name',
        partner_id=partner_entity.partner_id,
    )


@pytest.fixture
async def merchant(storage: Storage, partner, merchant_entity):
    return await storage.merchant.create(merchant_entity)


@pytest.fixture
async def order_entity() -> Order:
    return Order(
        currency_code='RUB',
        checkout_order_id=uuid4(),
        order_id='merchant-order-id',
        order_amount=Decimal('420.00'),
        payment_status=PaymentStatus.AUTHORIZED,
        cart=Cart(
            items=[
                CartItem(
                    title='Awesome Product',
                    discounted_unit_price=Decimal('42.00'),
                    product_id='product-1',
                    quantity=ItemQuantity(count=Decimal('10')),
                    total=Decimal('420.00'),
                ),
            ],
            total=CartTotal(amount=Decimal('420.00')),
        ),
        metadata='metadata',
        created=datetime(2022, 2, 2, tzinfo=timezone.utc),
        updated=datetime(2022, 2, 3, tzinfo=timezone.utc),
    )


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def user(uid):
    return User(uid=uid)


@pytest.fixture
async def role(storage: Storage, partner: Partner, uid: int) -> Role:
    return await storage.role.create(
        Role(
            partner_id=partner.partner_id,
            uid=uid,
            role=RoleType.OWNER,
        ),
    )
