import pytest

from ads.watchman.timeline.api.lib.app import make_app
from ads.watchman.timeline.api.lib import config
from ads.watchman.timeline.api.lib.extensions.startrek import startrek_ext


def test_startrek_client_created_in_app_context():
    app = make_app(config.Config)
    with app.test_request_context():
        try:
            startrek_ext.get_startrek_client()
        except:
            pytest.fail("Failed creating startrek client in app context")
