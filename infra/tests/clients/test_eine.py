"""Tests for eine api client methods."""
from unittest.mock import call

import pytest
import http.client

from infra.walle.server.tests.lib.util import monkeypatch_config, patch_attr, mock_response, monkeypatch_request
from walle import boxes
from walle.clients import eine
from walle.util import net
from walle.util.misc import drop_none

MOCK_HOST_INV = 9999
MOCK_HOST_MACS = ["00259088B324", "00259088B325"]

INTERNAL_PROVIDER = "internal"
BOX_PROVIDER = "box"
ALL_PROVIDERS = [INTERNAL_PROVIDER, BOX_PROVIDER]


def get_client(provider):
    if provider == INTERNAL_PROVIDER:
        return eine.get_client(eine.get_yandex_internal_provider())
    if provider == BOX_PROVIDER:
        return eine.get_client(eine.get_yandex_box_provider_if_exists("mock-box"))
    raise RuntimeError("Unknown Eine provider")


@pytest.fixture
def mock_eine_configs(monkeypatch):
    monkeypatch_config(monkeypatch, "eine.access_token", "access-token-mock")
    monkeypatch_config(monkeypatch, boxes.EINE_BOXES_SECTION, {})
    monkeypatch_config(monkeypatch, f"{boxes.EINE_BOXES_SECTION}.mock-box", {})


@pytest.fixture
def mock_host_info(monkeypatch, mock_eine_configs):
    class HostInfoMocker:
        def __init__(self):
            # stub it
            self.mock_response({})

        def with_profile(self):
            resp = self._bare_json_response
            resp["einstellung"] = {"_id": "1"}
            self.mock_response({"result": resp})

        def with_full_profile(
            self, profile_name, status=eine.EineProfileStatus.QUEUED, current_stage=None, message=None, otrs_ticket=None
        ):
            resp = self._bare_json_response
            resp.update(self._bare_json_profile)
            resp["einstellung"]["profile_name"] = profile_name
            resp["einstellung"]["current_stage"] = current_stage
            resp["einstellung"]["message"] = message
            resp["einstellung"]["status"] = eine.EineProfileStatus.ALL.index(status)

            if otrs_ticket is not None:
                resp["props"]["otrs_ticket"] = {
                    "timestamp": 150.0,
                    "value": otrs_ticket,
                }

            self.mock_response({"result": resp})

        def with_location(self, switch=None, rack=None, mac=None):
            resp = self._bare_json_response
            resp["props"] = {}
            if switch is not None:
                resp["props"].update(
                    {
                        "switch": {
                            "value": switch,  # "sas1-s370 ge1/0/3",
                            "timestamp": 50.0,
                        }
                    }
                )
            if mac is not None:
                resp["props"].update(
                    {
                        "last_active_mac": {
                            "value": mac,  # "00259088B324",
                            "timestamp": 60.0,
                        }
                    }
                )
            if rack is not None:
                resp["rack"] = rack

            self.mock_response({"result": resp})

        def minimal_valid_response(self):
            self.mock_response({"result": self._bare_json_response})

        @property
        def _bare_json_response(self):
            return {"inventory": str(MOCK_HOST_INV), "in_use": True, "macs": MOCK_HOST_MACS}

        @property
        def _bare_json_profile(self):
            return {
                "props": {},
                "einstellung": {
                    "_id": "2",
                    "profile_name": "profile-mock",
                    "tags_local": ["eaas"],
                    "current_stage": None,
                    "assigned_at": 100.0,
                    "updated_at": 200.0,
                    "message": None,
                    "status": 0,
                    "stages": [
                        {
                            "status": 1,
                            "stage": "flash",
                        }
                    ],
                },
            }

        def mock_response(self, data, status_code=http.client.OK):
            self.mock_request_result = mock_response(data, status_code=status_code)

        def __iter__(self):
            yield self.mock_request_result

    host_info_mocker = HostInfoMocker()

    request_mock = monkeypatch_request(monkeypatch, side_effect=host_info_mocker)
    host_info_mocker.request_mock = request_mock
    return host_info_mocker


@pytest.fixture
def mock_host_pager(monkeypatch, mock_eine_configs):
    class HostPagerMocker:
        def __init__(self):
            self.mock_request_result_chain = []

        def __iter__(self):
            return iter(self.mock_request_result_chain)

        def minimal_valid_response(self):
            self.mock_response({"result": [self._bare_json_response]}, page=1)

        def mock_response(self, data, status_code=http.client.OK, page=None, total_pages=None):
            r = self._mock_response(data, status_code, page, total_pages)
            self.mock_request_result_chain.append(r)

        @staticmethod
        def _mock_response(data, status_code=http.client.OK, page=None, total_pages=None):
            if page is not None:
                data["paginate"] = {
                    "count": total_pages or page,
                    "has_next": page < (total_pages or page),
                    "has_previous": page == 1,
                    "pages": total_pages or page,
                    "per_page": 1,
                }

            return mock_response(data, status_code=status_code)

        @property
        def _bare_json_response(self):
            return {"inventory": str(MOCK_HOST_INV), "in_use": True, "macs": [], "rack": None}

    host_page_mocker = HostPagerMocker()
    request_mock = monkeypatch_request(monkeypatch, side_effect=host_page_mocker)

    host_page_mocker.request_mock = request_mock
    return host_page_mocker


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_status__invalid_json(mock_host_info, provider):
    del mock_host_info  # make linter happy
    client = get_client(provider)
    with pytest.raises(eine.EineInternalError) as exc:
        client.get_host_status(MOCK_HOST_INV)

    assert str(exc.value) == (
        "Error in communication with Einstellung: "
        "The server returned an invalid JSON response: "
        "result['result'] is missing."
    )


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_status__not_found(mock_host_info, provider):
    mock_host_info.mock_response({"description": "Host not found."}, status_code=http.client.NOT_FOUND)
    client = get_client(provider)
    with pytest.raises(eine.EineHostDoesNotExistError) as exc:
        client.get_host_status(MOCK_HOST_INV)

    assert str(exc.value) == (
        "Einstellung returned an error: Can't find host #{}: Host not found.".format(MOCK_HOST_INV)
    )


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info__have_profile(mock_host_info, provider):
    mock_host_info.with_profile()
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV)
    assert info.has_profile()
    assert info.profile_id() == "1"


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info__full_profile(mock_host_info, provider):
    mock_host_info.with_full_profile("profile-mock")
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, profile=True)
    assert info.has_profile()
    assert info.profile() == "profile-mock"
    assert info.profile_id() == "2"
    assert info.profile_assigned_timestamp() == 100.0
    assert info.profile_updated_timestamp() == 200.0
    assert info.local_tags() is not None


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info__no_profile(mock_host_info, provider):
    mock_host_info.minimal_valid_response()
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, profile=True)
    assert not info.has_profile()


@pytest.mark.parametrize(
    ["current_stage", "description"],
    [
        [None, "pending"],
        [0, "flash:planned"],
    ],
)
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info__current_stage(mock_host_info, current_stage, description, provider):
    mock_host_info.with_full_profile("profile-mock", current_stage=current_stage)
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, profile=True)

    assert info.get_stage_description() == description


@pytest.mark.parametrize("profile_status", eine.EineProfileStatus.ALL)
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info_profile_status_with_profile_status(mock_host_info, profile_status, provider):
    mock_host_info.with_full_profile("profile-mock", status=profile_status)
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, profile=True)

    assert info.profile_status() == profile_status


@pytest.mark.parametrize("profile_message", [None, "message-mock"])
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info_profile_status_with_profile_message(mock_host_info, profile_message, provider):
    mock_host_info.with_full_profile("profile-mock", message=profile_message)
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, profile=True)

    if profile_message is None:
        assert info.profile_message() == "<no message>."
    else:
        assert info.profile_message() == profile_message


@pytest.mark.parametrize("ticket_id", [None, "ticket-mock"])
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info_profile_status_with_ticket_id(mock_host_info, ticket_id, provider):
    mock_host_info.with_full_profile("profile-mock", otrs_ticket=ticket_id)
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, profile=True)
    assert info.ticket_id() == ticket_id


@pytest.mark.parametrize("switch_port", [None, "sas1-s370 ge1/0/3"])
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info__switch(mock_host_info, monkeypatch, switch_port, provider):
    mock_host_info.with_location(switch=switch_port)
    logger = patch_attr(monkeypatch, eine.log, "error")
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, location=True)

    if switch_port is None:
        assert info.switch() is None
    else:
        assert info.switch() == eine.EineSwitchInfo(*switch_port.split(), timestamp=50.0)

    assert logger.mock_calls == []


@pytest.mark.parametrize("rack", [None, "3"] + eine._MAINTENANCE_RACKS)
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info__switch_error(mock_host_info, monkeypatch, rack, provider):
    mock_host_info.with_location(switch="--", rack=rack)
    logger = patch_attr(monkeypatch, eine.log, "error")
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, location=True)

    assert info.switch() is None
    if rack in eine._MAINTENANCE_RACKS:
        assert logger.mock_calls == []
    else:
        assert logger.mock_calls == [
            call("Got an invalid switch/port for host #%s(%s) from Einstellung: %r.", MOCK_HOST_INV, None, "--")
        ]


@pytest.mark.parametrize("active_mac", [None, "00259088B324"])
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_host_info__active_mac(mock_host_info, active_mac, provider):
    mock_host_info.with_location(mac=active_mac)
    client = get_client(provider)
    info = client.get_host_status(MOCK_HOST_INV, location=True)

    if active_mac is None:
        assert info.active_mac() is None
    else:
        assert info.active_mac() == eine.EineMacsInfo(net.format_mac(active_mac), timestamp=60.0)


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_hosts_iterator__invalid_json(mock_host_pager, provider):
    mock_host_pager.mock_response({})
    client = get_client(provider)
    with pytest.raises(eine.EineInternalError) as exc:
        list(client._eine_host_info_iterator())

    assert str(exc.value) == (
        "Error in communication with Einstellung: "
        "The server returned an invalid JSON response: "
        "result['paginate'] is missing."
    )


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_hosts_iterator__iterates_pages(mock_host_pager, provider):
    total_pages = 3
    expected_host_info = []
    for i in range(total_pages):  # 3 pages, one host per page
        parsed_host_info = dict(drop_none(mock_host_pager._bare_json_response), inventory=MOCK_HOST_INV + i)
        expected_host_info.append(eine.EineHostStatus(parsed_host_info))

        host_info = dict(mock_host_pager._bare_json_response, inventory=str(MOCK_HOST_INV + i))
        mock_host_pager.mock_response({"result": [host_info]}, page=i + 1, total_pages=total_pages)

    client = get_client(provider)
    host_list = list(client._eine_host_info_iterator())
    assert host_list == expected_host_info


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_hosts_iterator__iterates_hosts(mock_host_pager, provider):
    total_pages = 3
    per_page = 3
    expected_host_info = []

    for i in range(total_pages):  # 3 pages
        page_data = []
        for j in range(per_page):  # 3 hosts per page
            inv = MOCK_HOST_INV + i * per_page + j
            parsed_host_info = dict(drop_none(mock_host_pager._bare_json_response), inventory=inv)
            expected_host_info.append(eine.EineHostStatus(parsed_host_info))

            page_data.append(dict(mock_host_pager._bare_json_response, inventory=str(inv)))
        mock_host_pager.mock_response({"result": page_data}, page=i + 1, total_pages=total_pages)

    client = get_client(provider)
    host_list = list(client._eine_host_info_iterator())
    assert host_list == expected_host_info


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_hosts_iterator__filters(mock_host_pager, provider):
    """Check that requested filters actually applied."""

    def filters_checker(method, url, params=None, *args, **kwargs):
        del method, url, args, kwargs  # make linter happy
        assert params["einstellung.profile_name"] == "profile-mock"
        return mock_host_pager._mock_response({"result": []}, page=1)

    mock_host_pager.request_mock.side_effect = filters_checker
    client = get_client(provider)
    list(client._eine_host_info_iterator(filters={"einstellung.profile_name": "profile-mock"}))


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_set_host_location(monkeypatch, mock_eine_configs, provider):
    response = mock_response({"result": "Created"}, status_code=http.client.CREATED)
    request_mock = monkeypatch_request(monkeypatch, return_value=response)

    client = get_client(provider)
    client.set_host_location(1, "some_switch", "some_port")
    assert request_mock.called
