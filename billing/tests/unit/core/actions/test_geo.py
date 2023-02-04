import pytest

from billing.yandex_pay.yandex_pay.core.actions.geo import CheckRegionAction
from billing.yandex_pay.yandex_pay.core.exceptions import CoreForbiddenRegionError
from billing.yandex_pay.yandex_pay.file_storage.geobase import GeobaseStorage
from billing.yandex_pay.yandex_pay.utils.stats import laas_failures

GOOD_REGION_ID = 555
FORBIDDEN_REGION_ID = 123


@pytest.fixture(autouse=True)
def enable_region_check(yandex_pay_settings):
    yandex_pay_settings.API_CHECK_REGION = True


@pytest.fixture(autouse=True)
def set_forbidden_regions(file_storage):
    file_storage.geobase = GeobaseStorage(forbidden_regions=[{'region_id': FORBIDDEN_REGION_ID}])


@pytest.mark.asyncio
async def test_success():
    await CheckRegionAction(region_id=GOOD_REGION_ID).run()


@pytest.mark.asyncio
async def test_region_id_is_forbidden():
    with pytest.raises(CoreForbiddenRegionError):
        await CheckRegionAction(region_id=FORBIDDEN_REGION_ID).run()


@pytest.mark.asyncio
async def test_laas_failure_logs_error(mocker, mocked_logger):
    CheckRegionAction.context.logger = mocked_logger

    await CheckRegionAction(region_id=None).run()

    mocked_logger.error.assert_called_once_with('LaaS failure')


@pytest.mark.asyncio
async def test_laas_failure_increments_failure_metric():
    before = laas_failures.get()

    await CheckRegionAction(region_id=None).run()
    after = laas_failures.get()

    assert after[0][1] - before[0][1] == 1


@pytest.mark.asyncio
async def test_disable_region_check(yandex_pay_settings):
    """
    Should fail despite the fact that region is forbidden
    """
    yandex_pay_settings.API_CHECK_REGION = False
    await CheckRegionAction(region_id=FORBIDDEN_REGION_ID).run()
