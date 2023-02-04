from maps.b2bgeo.libs.py_flask_utils import i18n
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get
from ya_courier_backend.config.common import KNOWN_LOCALES


@skip_if_remote
def test_locales_list(env: Environment):
    path = '/api/v1/available-locales'
    result = local_get(env.client, path)
    locales = [l['name'] for l in result]
    assert 'ru_RU' in locales
    assert 'en_US' in locales


@skip_if_remote
def test_ignored(env: Environment):
    ignored_locales = ['ru_RU', 'en_US']
    i18n.Keysets.init(KNOWN_LOCALES, ignored_locales)
    path = '/api/v1/available-locales'
    result = local_get(env.client, path)
    locales = [l['name'] for l in result]
    assert 'ru_RU' not in locales
    assert 'en_US' not in locales
