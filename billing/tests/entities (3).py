"""
Модуль с дефолтными тестовыми сущностями.
Полезно, когда тебе нужен просто какой-то дефолтный экземпляр сущности.
Мы стараемся не использовать nullable поля, поэтому для создания сущности конструктору часто нужно передать
много разных параметров. Не хочется оверспецифицировать тест, поэтому часто используемые сущности размещаем тут.
Но важно не переборщить.

Правила:
    1. Сущность должна быть максиамльно дефолтной. Специальные случаи лучше в отдельную сущность выносить;
    2. Название фикстуры должно начинаться с `entity_`;
    3. В тестах лучше не определять фикстуры с таким префиксом! И не переопределять существующие `entity_`;
    4. Крайне желательно чтобы никакой логики не было.
    5. Если сущность хранимая, айдишник лучше не зашивать.

Воспринимай эти сущности как прототип. Иногда можно просто создать их в базе,
но иногда надо поменять поля - прямо в тесте и меняй.
"""
from datetime import date
from decimal import Decimal
from uuid import UUID, uuid4

import pytest
from pay.lib.entities.cart import Cart, CartItem, CartTotal, ItemQuantity, ItemReceipt
from pay.lib.entities.enums import DeliveryCategory
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.order import Contact
from pay.lib.entities.receipt import (
    Agent,
    AgentType,
    MarkQuantity,
    PaymentsOperator,
    PaymentSubjectType,
    PaymentType,
    Receipt,
    ReceiptItem,
    ReceiptItemQuantity,
    Supplier,
    TaxType,
    TransferOperator,
)
from pay.lib.entities.shipping import (
    Address,
    CourierOption,
    Location,
    ShippingMethod,
    ShippingMethodType,
    ShippingWarehouse,
    YandexDeliveryOption,
)
from pay.lib.entities.threeds import ThreeDS2AuthenticationRequest, ThreeDSBrowserData

from sendr_auth.entities import User

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import (
    CheckoutOrder,
    StorageAddress,
    StorageCart,
    StorageContact,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def entity_auth_user(uid):
    return User(uid=uid, login_id='entity_auth_user.login_id')


@pytest.fixture
def entity_cart():
    return Cart(
        items=[
            CartItem(
                title='Awesome Product',
                discounted_unit_price=Decimal('42.00'),
                product_id='product-1',
                quantity=ItemQuantity(count=Decimal('10')),
                total=Decimal('420.00'),
                receipt=ItemReceipt(
                    tax=TaxType.VAT_20,
                )
            ),
            CartItem(
                title='Awesome Product 2',
                discounted_unit_price=Decimal('21.00'),
                product_id='product-2',
                quantity=ItemQuantity(count=Decimal('1')),
                total=Decimal('21.00'),
                receipt=ItemReceipt(
                    tax=TaxType.VAT_20,
                )
            ),
        ],
        total=CartTotal(amount=Decimal('441.00')),
    )


@pytest.fixture
def entity_receipt():
    item = ReceiptItem(
        title='Product',
        discounted_unit_price=Decimal('10'),
        quantity=ReceiptItemQuantity(count=Decimal('1')),
        tax=TaxType.VAT_20,
        product_id='p-1',
    )
    return Receipt(items=[item])


@pytest.fixture
def entity_full_receipt():
    item = ReceiptItem(
        title='Full Product',
        discounted_unit_price=Decimal('100'),
        quantity=ReceiptItemQuantity(count=Decimal('1.5')),
        tax=TaxType.VAT_10,
        product_id='p-1',
        excise=Decimal('9.99'),
        payment_method_type=PaymentType.FULL_PAYMENT,
        payment_subject_type=PaymentSubjectType.PAYMENT,
        product_code=bytes.fromhex('aefc6cdc2ce3223315f4f46585de5217c7c8799529285b9b'),
        mark_quantity=MarkQuantity(numerator=1, denominator=3),
        supplier=Supplier(
            inn='123456789',
            name='Supplier Name',
            phones=['+798700000111'],
        ),
        agent=Agent(
            agent_type=AgentType.OTHER,
            operation='operation',
            phones=['+798700000222'],
            transfer_operator=TransferOperator(
                inn='123456789',
                name='Transfer Operator Name',
                address='Transfer Operator Address',
                phones=['+798700000333'],
            ),
            payments_operator=PaymentsOperator(phones=['+79876543210', '+798700012345']),
        ),
    )
    return Receipt(items=[item])


@pytest.fixture
def entity_merchant():
    return Merchant(
        name='merchant-name',
        is_blocked=False,
        callback_url='https://callback-url.test',
        partner_id=uuid4(),
    )


@pytest.fixture
def entity_contact():
    return Contact(
        id='id',
        first_name='first_name',
        second_name='second_name',
        last_name='last_name',
        email='email',
        phone='phone',
    )


@pytest.fixture
def entity_address():
    return Address(
        id='addr-id',
        country='Russia',
        locality='Moscow',
        street='Tolstogo st.',
        region='Moscow',
        building='16',
        room='3556',
        entrance='4',
        floor='3',
        intercom='21',
        comment='comment',
        zip='400000',
        location=Location(latitude=12.34, longitude=56.78),
        address_line='Russia, Moscow, Tolstogo st., 16',
        district='12',
    )


@pytest.fixture
def entity_courier_option():
    return CourierOption(
        courier_option_id='courier-1',
        provider='CDEK',
        category=DeliveryCategory.STANDARD,
        title='Доставка курьером',
        amount=Decimal('39.00'),
        from_date=date(2022, 3, 1),
    )


@pytest.fixture
def entity_yd_option():
    return YandexDeliveryOption(
        receipt=ItemReceipt(tax=TaxType.VAT_20),
        title='Экспресс-доставка',
        allowed_payment_methods=[],
        yandex_delivery_option_id='yandex-delivery:express',
        amount=Decimal('11.11'),
        category=DeliveryCategory.EXPRESS,
    )


@pytest.fixture
def entity_checkout_order(entity_address, entity_cart, uid):
    return CheckoutOrder(
        merchant_id=UUID('b5004499-6640-489c-befe-f67a95e3baaf'),
        uid=uid,
        currency_code='XTS',
        cart=StorageCart.from_cart(entity_cart),
        shipping_address=StorageAddress.from_address(entity_address),
        shipping_contact=StorageContact(id='cid', first_name='fname', email='email', phone='phone'),
        order_amount=Decimal('123.45'),
        order_id='merchant-order-id',
        metadata='mdata',
        enable_coupons=True,
        enable_comment_field=False,
    )


@pytest.fixture
def entity_threeds_authentication_request():
    return ThreeDS2AuthenticationRequest(
        challenge_notification_url='https://challenge_notification_url.test',
        browser_data=ThreeDSBrowserData(
            java_enabled=True,
            language='ru',
            screen_color_depth=24,
            screen_height=1080,
            screen_width=1920,
            window_height=480,
            window_width=640,
            timezone=-180,
            ip='192.0.2.1',
            user_agent='user_agent',
            accept_header='accept_header',
        )
    )


@pytest.fixture
def entity_unittest_psp():
    return PSP(psp_external_id='unittest', psp_id=uuid4())


@pytest.fixture
def entity_integration(storage, entity_psp, entity_merchant):
    return Integration(
        merchant_id=entity_merchant.merchant_id,
        psp_id=entity_psp.psp_id,
        status=IntegrationStatus.DEPLOYED,
        creds=Integration.encrypt_creds({}),
    )


@pytest.fixture
async def entity_psp(storage, rands):
    return PSP(psp_external_id=rands(), psp_id=uuid4())


@pytest.fixture
async def entity_transaction(
    storage, entity_checkout_order, entity_integration, entity_threeds_authentication_request
):
    return Transaction(
        checkout_order_id=entity_checkout_order.checkout_order_id,
        status=TransactionStatus.NEW,
        data=TransactionData(
            user_ip='192.0.2.1',
            threeds=TransactionThreeDSData(
                authentication_request=entity_threeds_authentication_request,
            ),
        ),
        integration_id=entity_integration.integration_id,
        message_id=str(uuid4())
    )


@pytest.fixture
async def entity_operation(storage, entity_checkout_order, entity_merchant):
    return Operation(
        operation_id=uuid4(),
        merchant_id=entity_merchant.merchant_id,
        checkout_order_id=entity_checkout_order.checkout_order_id,
        order_id=entity_checkout_order.order_id,
        amount=Decimal('123.45'),
        operation_type=OperationType.AUTHORIZE,
        status=OperationStatus.PENDING,
        external_operation_id='external_operation_id',
        reason='reason',
        cart=entity_checkout_order.cart,
        shipping_method=entity_checkout_order.shipping_method,
    )


@pytest.fixture
def entity_shipping_method(entity_courier_option):
    return ShippingMethod(
        method_type=ShippingMethodType.COURIER,
        courier_option=entity_courier_option,
    )


@pytest.fixture
def entity_warehouse(entity_address, entity_contact):
    return ShippingWarehouse(
        address=entity_address,
        contact=entity_contact,
        emergency_contact=entity_contact,
    )
