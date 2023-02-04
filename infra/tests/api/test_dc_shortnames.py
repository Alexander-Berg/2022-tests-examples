"""Test dc shortnames API"""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.usefixtures("authorized_admin")
def test_add_with_name_and_path(test):
    test.location_names_map.mock({"name": "a", "path": "A|B|C"})
    test.location_names_map.mock({"name": "b", "path": "A|B|C|D"})
    result = test.api_client.post(
        "/v1/shortnames", data={"shortnames": [{"path": "A|B|C", "name": "a"}, {"path": "A|B|C|D", "name": "b"}]}
    )
    assert result.status_code == http.client.OK
    assert result.json == {"shortnames": [{"name": "a", "path": "A|B|C"}, {"name": "b", "path": "A|B|C|D"}]}
    test.location_names_map.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
def test_delete(test):
    test.location_names_map.mock({"path": "A|B|C", "name": "a"}, add=False)
    result = test.api_client.delete("/v1/shortnames/A|B|C")
    assert result.status_code == http.client.NO_CONTENT
    result = test.api_client.get("/v1/shortnames")
    assert result.json == {"result": []}
    test.location_names_map.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
def test_add_with_path(test):
    test.location_names_map.mock({"name": "a", "path": "A|B|C|D"})
    result = test.api_client.put("/v1/shortnames/A|B|C|D", data={"name": "a"})
    assert result.status_code == http.client.OK
    assert result.json == {"name": "a", "path": "A|B|C|D"}
    test.location_names_map.assert_equal()


@pytest.mark.skip(reason="Need to check for shortname levels in API.")
@pytest.mark.usefixtures("authorized_admin")
def test_add_with_name_and_path_exception(test):
    test.location_names_map.mock({"path": "A|B|C", "name": "a"})
    result = test.api_client.post("/v1/shortnames", data={"shortnames": [{"path": "A|B|C|D", "name": "a"}]})
    assert result.status_code == http.client.BAD_REQUEST
    test.location_names_map.assert_equal()


@pytest.mark.skip(reason="Need to check for shortname levels in API.")
@pytest.mark.usefixtures("authorized_admin")
def test_add_with_path_exception(test):
    test.location_names_map.mock({"path": "A|B|C", "name": "a"})
    result = test.api_client.put("/v1/shortnames/A|B|C|D", data={"name": "a"})
    assert result.status_code == http.client.BAD_REQUEST
    test.location_names_map.assert_equal()


def test_dump_tree(test):
    test.location_names_map.mock({"path": "A|B|C", "name": "a"}, {"path": "A|B|C|D", "name": "b"})
    result = test.api_client.get("/v1/shortnames")
    assert result.status_code == http.client.OK
    assert result.json == {"result": [{"name": "a", "path": "A|B|C"}]}
    test.location_names_map.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
def test_add_with_no_required_name(test):
    result = test.api_client.post("v1/shortnames", data={"shortnames": [{"path": "A|B|C"}]})
    assert result.status_code == http.client.BAD_REQUEST


@pytest.mark.usefixtures("authorized_admin")
def test_add_with_no_required_path(test):
    result = test.api_client.post("v1/shortnames", data={"shortnames": [{"name": "some_name"}]})
    assert result.status_code == http.client.BAD_REQUEST


@pytest.mark.usefixtures("authorized_admin")
def test_add_path_with_no_required_name(test):
    result = test.api_client.put("v1/shortnames/A|B|C", data={})
    assert result.status_code == http.client.BAD_REQUEST


@pytest.mark.usefixtures("authorized_admin")
def test_get_one_shortname(test):
    test.location_names_map.mock({"path": "A|B|C|D", "name": "b"})
    result = test.api_client.get("/v1/shortnames/A|B|C|D")
    assert result.status_code == http.client.OK
    assert result.json == {"result": "b"}
    test.location_names_map.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("name", ["a_a", "a|a", "a!", ".", ",", "asdsadasd|", "|asasaa", ""])
def test_check_validation_fails_when_use_put(test, name):
    result = test.api_client.put("/v1/shortnames/A|B|C|D", data={"name": name})
    assert result.status_code == http.client.BAD_REQUEST
    test.location_names_map.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("name", ["a_a", "a|a", "a!", ".", ",", "asdsadasd|", "|asasaa", ""])
def test_check_validation_fails_when_use_post(test, name):
    result = test.api_client.post("/v1/shortnames", data={"shortnames": [{"path": "A|B|C", "name": name}]})
    assert result.status_code == http.client.BAD_REQUEST
    test.location_names_map.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("name", ["man_", "man-a", "man|a", "manA"])
def test_check_validation_fails_when_try_to_add_dc_shortname_out_of_pattern(test, name):
    result = test.api_client.post("/v1/shortnames", data={"shortnames": [{"path": "FI|MANTSALA|A", "name": name}]})
    assert result.status_code == http.client.BAD_REQUEST
    test.location_names_map.assert_equal()
