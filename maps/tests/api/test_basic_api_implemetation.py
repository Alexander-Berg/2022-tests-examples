import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "method, url", [("POST", "/tasks/normalizer/"), ("PUT", "/tasks/normalizer/10/")]
)
@pytest.mark.parametrize(
    "content_type", ("application/x-www-form-urlencoded", "multipart/form-data")
)
async def test_error_for_unexpected_content_type(method, url, content_type, api):
    headers = {"content-type": content_type}

    response = await api.request(method, url, data="{}", headers=headers)
    json = await response.json()

    assert response.status == 415
    assert json == {
        "Content-Type": [
            f"Invalid content type {content_type}, expected application/json."
        ]
    }


@pytest.mark.parametrize(
    "method, url", [("POST", "/tasks/normalizer/"), ("PUT", "/tasks/normalizer/10/")]
)
@pytest.mark.parametrize("kwargs", ({"data": "some_shit"}, {}))
async def test_errored_for_invalid_json(method, url, kwargs, api):
    headers = {"content-type": "application/json"}

    response = await api.request(method, url, headers=headers, **kwargs)
    json = await response.json()

    assert response.status == 400
    assert json == {"json": ["Expecting value: line 1 column 1 (char 0)"]}
