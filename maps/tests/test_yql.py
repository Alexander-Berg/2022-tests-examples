import pytest

from yql.api.v1.client import YqlClient

YT_SERVER = "plato"


@pytest.mark.use_local_yt_yql
def test_environment_settings(environment_settings):
    assert YT_SERVER in environment_settings["yt_servers"]
    assert environment_settings["yql"]["server"] == "localhost"

    yql_settings = environment_settings["yql"]

    client = YqlClient(db=YT_SERVER, **yql_settings)

    request = client.query("SELECT 1", syntax_version=1)
    request.run()
    request.get_results(wait=True)
