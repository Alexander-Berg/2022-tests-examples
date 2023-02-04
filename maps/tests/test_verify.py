from maps.garden.sdk.resources import PythonResource
from maps.garden.libs.mtpredictor_model import common
from maps.garden.modules.masstransit_predictor_data.lib import verify
from .common import create_masstransit_predictor_data_fb


def test_verify(environment_settings):
    masstransit_predictor_data_fb = create_masstransit_predictor_data_fb(environment_settings)

    result = PythonResource(common.verified_resource(common.NEW_DATASET_NAME))

    task = verify.VerifyDataSize()
    task.load_environment_settings(environment_settings)
    task(
        masstransit_predictor_data_fb=masstransit_predictor_data_fb,
        verified=result
    )
