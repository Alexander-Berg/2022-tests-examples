from maps.garden.sdk.resources import PythonResource
from maps.garden.libs.mtpredictor_model import common
from maps.garden.modules.masstransit_predictor_data.lib.mms_data import verify
from .common import create_data_mms


def test_verify(environment_settings):
    data_mms = create_data_mms(environment_settings)

    result = PythonResource(common.verified_resource(common.DATASET_NAME))

    task = verify.VerifyDataSize()
    task.load_environment_settings(environment_settings)
    task(
        data_mms=data_mms,
        verified=result
    )
