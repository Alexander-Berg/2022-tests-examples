import json
from unittest import mock

from maps.garden.libs_server.config.settings_provider import EnvironmentSettingsProvider


@mock.patch("maps.garden.libs_server.config.config.SETTINGS_PREFIX", ".")
def test_settings_provider():
    expected_stable_settings = {
        "hello_contour": 123
    }

    user_template = {
        "hello_contour": "<<contour_name>>"
    }

    with open("environment_settings.stable.json", "w") as f:
        json.dump(expected_stable_settings, f)

    with open("environment_settings_user_template.json", "w") as f:
        json.dump(user_template, f)

    settings_provider = EnvironmentSettingsProvider()

    stable_settings = settings_provider.get_settings(contour_name="stable")
    assert stable_settings == expected_stable_settings

    user_settings = settings_provider.get_settings(contour_name="my_contour")
    assert user_settings == {
        "hello_contour": "my_contour"
    }
