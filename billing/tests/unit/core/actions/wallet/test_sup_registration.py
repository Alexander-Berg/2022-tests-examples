import pytest

from sendr_interactions.exceptions import InteractionResponseError

from billing.yandex_pay.yandex_pay.core.actions.wallet.sup_registration import RegisterInstallationAction
from billing.yandex_pay.yandex_pay.core.exceptions import SupInvalidTokenRegistrationError
from billing.yandex_pay.yandex_pay.interactions import SUPClient


@pytest.mark.asyncio
async def test_should_call_registration_with_passed_params(mocker):
    register_mocker = mocker.patch.object(
        SUPClient,
        'register_installation',
        mocker.AsyncMock(
            return_value=None,
        ),
    )

    await RegisterInstallationAction(
        app_id='1',
        app_version='2',
        hardware_id='3',
        push_token='4',
        platform='5',
        device_name='6',
        zone_id='7',
        notify_disabled=True,
        active=True,
        install_id='8',
        device_id='9',
        vendor_device_id='10',
        is_huawei=True,
    ).run()

    register_mocker.assert_awaited_once_with(
        app_id='1',
        app_version='2',
        hardware_id='3',
        push_token='4',
        platform='5',
        device_name='6',
        zone_id='7',
        notify_disabled=True,
        active=True,
        install_id='8',
        device_id='9',
        vendor_device_id='10',
        is_huawei=True,
    )


@pytest.mark.asyncio
async def test_should_rethrow_conflict_sup_exception_as_domain_exception(mocker):
    mocker.patch.object(
        SUPClient,
        'register_installation',
        mocker.AsyncMock(
            side_effect=InteractionResponseError(
                status_code=409,
                method='test',
                service='test',
            ),
        ),
    )

    with pytest.raises(SupInvalidTokenRegistrationError) as e:
        await RegisterInstallationAction(
            app_id='1',
            app_version='2',
            hardware_id='3',
            push_token='4',
            platform='5',
            device_name='6',
            zone_id='7',
            notify_disabled=True,
            active=True,
            install_id='8',
            device_id='9',
            vendor_device_id='10',
            is_huawei=True,
        ).run()

    assert e.value.message == "INVALID_PUSH_TOKEN"
