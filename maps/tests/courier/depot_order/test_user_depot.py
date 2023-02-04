from http import HTTPStatus
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_patch


@skip_if_remote
def test_user_depot_patch_repeatable_ids(env):
    path = f"/api/v1/companies/{env.default_company.id}/user_depot/{env.default_user.id}"

    local_patch(env.client, path, headers=env.user_auth_headers, data=[env.default_depot.id])

    local_patch(env.client, path, headers=env.user_auth_headers,
                data=[env.default_depot.id, env.default_depot.id], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
