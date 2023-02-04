from uuid import uuid4

import pytest

from sendr_interactions.clients.startrek.exceptions import AlreadyExistsStartrekError

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.issue import UpdatePartnerIssueAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import CoreFailError
from billing.yandex_pay_admin.yandex_pay_admin.interactions.startrek import StartrekClient
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.contract_document import ContractDocument
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import (
    ContractDocumentType,
    PartnerType,
    PaymentGatewayType,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, Partner, RegistrationData

pytest.skip(allow_module_level=True)  # TODO: remove when refactoring is complete


@pytest.mark.asyncio
async def test_updated_issue_if_exists(mocker, run_action, mock_startrek_update, partner_with_issue):
    await run_action(partner_with_issue.partner_id)

    mock_startrek_update.assert_called_once()


@pytest.mark.asyncio
async def test_creates_issue_if_not_exists(run_action, mock_startrek_create):
    await run_action()

    mock_startrek_create.assert_called_once()


@pytest.mark.parametrize(
    'partner_type, gateway_type, expected_summary',
    (
        (PartnerType.MERCHANT, None, 'Проверить "Some Name"'),
        (PartnerType.PAYMENT_GATEWAY, PaymentGatewayType.DIRECT_MERCHANT, 'Проверить шлюз "Some Name"'),
        (PartnerType.PAYMENT_GATEWAY, PaymentGatewayType.PSP, 'Проверить агрегатор "Some Name"'),
    ),
)
@pytest.mark.asyncio
async def test_issue_summary(
    run_action, storage, partner, mock_startrek_create, partner_type, gateway_type, expected_summary
):
    partner.type = partner_type
    partner.payment_gateway_type = gateway_type
    partner.name = 'Some Name'
    await storage.partner.save(partner)

    await run_action()

    mock_startrek_create.assert_called_once()
    _, kwargs = mock_startrek_create.call_args_list[0]
    assert kwargs['summary'] == expected_summary


@pytest.mark.asyncio
async def test_issue_description(run_action, storage, partner, mock_startrek_create):
    partner.type = PartnerType.PAYMENT_GATEWAY
    partner.payment_gateway_type = PaymentGatewayType.PSP
    partner.verified = None
    await storage.partner.save(partner)

    await run_action()

    mock_startrek_create.assert_called_once()
    _, kwargs = mock_startrek_create.call_args_list[0]
    description = kwargs['description']
    assert str(partner.partner_id) in description
    assert f'Идентификатор партнёра (partner_id): %%{partner.partner_id}%%' in description
    assert f'Разновидность партнёра: %%{partner.type.value}%%' in description
    assert f'Разновидность шлюза: %%{partner.payment_gateway_type.value}%%' in description
    assert 'email@test' in description
    assert '+1(000)555-0100' in description
    assert 'ФИО: Doe John Татьянович' in description
    assert 'Резолюция модерации' not in description
    assert 'Документы' not in description


@pytest.mark.asyncio
async def test_issue_description_when_no_patronymic(run_action, storage, partner, mock_startrek_create):
    partner.type = PartnerType.PAYMENT_GATEWAY
    partner.payment_gateway_type = PaymentGatewayType.PSP
    partner.verified = None
    partner.registration_data.contact.patronymic = None
    await storage.partner.save(partner)

    await run_action()

    mock_startrek_create.assert_called_once()
    _, kwargs = mock_startrek_create.call_args_list[0]
    description = kwargs['description']
    assert 'email@test' in description
    assert '+1(000)555-0100' in description
    assert 'ФИО: Doe John\n' in description


@pytest.mark.asyncio
async def test_issue_description_of_verified_partner(run_action, storage, partner, mock_startrek_create):
    document = await storage.document.create(
        Document(
            document_id=uuid4(),
            partner_id=partner.partner_id,
            path='pa/th',
            digest='digest',
        )
    )
    await storage.contract_document.create(
        ContractDocument(
            contract_document_id=uuid4(),
            partner_id=partner.partner_id,
            type=ContractDocumentType.PCI_DSS_CERT,
            document_id=document.document_id,
        )
    )
    partner.verified = True
    await storage.partner.save(partner)

    await run_action()

    mock_startrek_create.assert_called_once()
    _, kwargs = mock_startrek_create.call_args_list[0]
    description = kwargs['description']
    assert 'Резолюция модерации: !!(green)Verified!!' in description
    assert 'Документы' in description
    assert 'сертификат PCIDSS: pa/th' in description


@pytest.mark.asyncio
async def test_creates__when_already_exists__raises(mocker, run_action, partner):
    mocker.patch.object(StartrekClient, 'create_issue', mocker.AsyncMock(side_effect=AlreadyExistsStartrekError))

    with pytest.raises(CoreFailError):
        await run_action(partner.partner_id)


@pytest.fixture
async def partner_with_issue(storage):
    return await storage.partner.create(
        Partner(
            name='some other partner name',
            psp_external_id='some other external id',
            type=PartnerType.MERCHANT,
            payment_gateway_type=PaymentGatewayType.DIRECT_MERCHANT,
            pay_merchant_id=uuid4(),
            psp_id=uuid4(),
            paypartsup_issue_id='YANDEXPAYTEST-777',
            verified=False,
            registration_data=RegistrationData(
                contact=Contact(
                    email='email@test',
                    phone='+1(000)555-0100',
                    name='John',
                    surname='Doe',
                    patronymic='Татьянович',
                )
            ),
        )
    )


@pytest.fixture
def run_action(partner):
    async def _run_action(partner_id=partner.partner_id):
        return await UpdatePartnerIssueAction(partner_id=partner_id).run()

    return _run_action


@pytest.fixture(autouse=True)
async def mock_startrek_create(mocker):
    return mocker.patch.object(
        StartrekClient, 'create_issue', mocker.AsyncMock(return_value=mocker.Mock(key='YANDEXPAYTEST-777'))
    )


@pytest.fixture(autouse=True)
async def mock_startrek_update(mocker):
    return mocker.patch.object(
        StartrekClient, 'update_issue', mocker.AsyncMock(return_value=mocker.Mock(key='YANDEXPAYTEST-777'))
    )
