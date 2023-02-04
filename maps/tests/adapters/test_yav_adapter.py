from datetime import datetime, timezone

import pytest

from smb.common.multiruntime.lib.basics import is_arcadia_python
from maps_adv.common.config_loader import OptionNotFound, YavAdapter

if is_arcadia_python:
    from library.python.vault_client.errors import ClientError
else:
    from vault_client.errors import ClientError


def test_initialization_errored_on_client_errors(mock_yav):
    mock_yav.side_effect = ClientError(lol="kek", maka="rek")

    with pytest.raises(ClientError, match="{'lol': 'kek', 'maka': 'rek'}"):
        YavAdapter("oauth_token", "sec-01e6zwtfg5g4b727yq4w3qvnz3")


def test_raises_if_secret_has_no_requested_key(mock_yav):
    mock_yav.return_value = example_result
    adapter = YavAdapter("oauth_token", "sec-01e6zwtfg5g4b727yq4w3qvnz3")

    with pytest.raises(OptionNotFound, match="NONEXISTENT"):
        adapter.load("NONEXISTENT")


def test_returns_value(mock_yav):
    mock_yav.return_value = example_result
    adapter = YavAdapter("oauth_token", "sec-01e6zwtfg5g4b727yq4w3qvnz3")

    got = adapter.load("EXISTING")

    assert got == "YES"


example_result = {
    "comment": "",
    "created_at": datetime(2020, 4, 28, 8, 0, 31, 246000, tzinfo=timezone.utc),
    "created_by": 1120000000098712,
    "creator_login": "sivakov512",
    "secret_name": "aioyav-example",
    "secret_uuid": "sec-01e6zwtfg5g4b727yq4w3qvnz3",
    "value": {"EXISTING": "YES"},
    "version": "ver-01e6zwtfgen5w925z9qztr9hcv",
}
