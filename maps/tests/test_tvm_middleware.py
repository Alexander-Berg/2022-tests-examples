import aiohttp
import pytest

from smb.common.aiotvm import CheckTicketFails
from maps_adv.common.lasagna import Lasagna, tvm_auth

pytestmark = [pytest.mark.asyncio]


async def resource(*args, **kwargs):
    return aiohttp.web.Response(text="Hello from resource")


@tvm_auth.only("TVM_SECURE_WHITELIST")
async def secure_resource(*args, **kwargs):
    return aiohttp.web.Response(text="Hello from secure_resource")


@tvm_auth.skip
async def no_tvm_resource(*args, **kwargs):
    return aiohttp.web.Response(text="Hello from no_tvm_resource")


async def ping_resource(*args, **kwargs):
    return aiohttp.web.Response(text="Hello from ping_resource")


class App(Lasagna):
    async def _setup_layers(self, db):
        _api = aiohttp.web.Application()

        _api.add_routes(
            [
                aiohttp.web.get("/example/", resource),
                aiohttp.web.get("/secure-example/", secure_resource),
                aiohttp.web.get("/no-tvm-example/", no_tvm_resource),
                aiohttp.web.get("/ping", ping_resource),
            ]
        )
        return _api


@pytest.mark.parametrize("handle_url", ["/example/", "/secure-example/"])
async def test_401_for_request_without_tvm_headers(run, handle_url):
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100, 500],
            "TVM_SECURE_WHITELIST": [900],
        }
    )
    client = await run(app)

    resp = await client.get(handle_url)

    assert resp.status == 401


@pytest.mark.parametrize("handle_url", ["/example/", "/secure-example/"])
async def test_403_for_request_with_wrong_tvm_header(run, aiotvm, handle_url):
    aiotvm.fetch_service_source_id.side_effect = CheckTicketFails(r"¯\_(ツ)_/¯")
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100, 500],
            "TVM_SECURE_WHITELIST": [900],
        }
    )
    client = await run(app)

    resp = await client.get(handle_url, headers=[("X-Ya-Service-Ticket", "Pustite")])

    assert resp.status == 403


@pytest.mark.parametrize("handle_url", ["/example/", "/secure-example/"])
async def test_403_for_request_with_not_allowed_tvm_id(run, aiotvm, handle_url):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100, 500],
            "TVM_SECURE_WHITELIST": [900],
        }
    )
    client = await run(app)

    resp = await client.get(handle_url, headers=[("X-Ya-Service-Ticket", "Pustite a")])

    assert resp.status == 403


async def test_200_for_request_with_tvm_id_from_default_whitelist(run, aiotvm):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100, 500, 100500],
        }
    )
    client = await run(app)

    resp = await client.get(
        "/example/", headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    assert resp.status == 200
    assert await resp.text() == "Hello from resource"


async def test_200_for_request_with_tvm_id_from_specific_whitelist(run, aiotvm):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_SECURE_WHITELIST": [100, 500, 100500],
        }
    )
    client = await run(app)

    resp = await client.get(
        "/secure-example/", headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    assert resp.status == 200
    assert await resp.text() == "Hello from secure_resource"


@pytest.mark.parametrize("handle_url", ["/example/", "/secure-example/"])
async def test_works_correctly_with_tvm_client(run, aiotvm, handle_url):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100, 500],
            "TVM_SECURE_WHITELIST": [900],
        }
    )
    client = await run(app)

    await client.get(handle_url, headers=[("X-Ya-Service-Ticket", "Pustite please")])

    aiotvm.fetch_service_source_id.assert_called_with(ticket="Pustite please")


@pytest.mark.parametrize(
    "handle_url, expected_response",
    [
        ("/example/", "Hello from resource"),
        ("/secure-example/", "Hello from secure_resource"),
        ("/no-tvm-example/", "Hello from no_tvm_resource"),
    ],
)
# tvm client is not setup
@pytest.mark.parametrize(
    "tvm_config_kw",
    (
        {"TVM_DAEMON_URL": "http://tvm.daemon", "TVM_TOKEN": None},
        {"TVM_DAEMON_URL": None, "TVM_TOKEN": "token"},
        {"TVM_DAEMON_URL": "http://tvm.daemon"},
        {"TVM_TOKEN": "token"},
        {},
    ),
)
# ignores whitelists if tvm is not setup
@pytest.mark.parametrize(
    "whitelist_config_kw",
    (
        {"TVM_WHITELIST": [111], "TVM_SECURE_WHITELIST": [111]},
        {"TVM_WHITELIST": [100500], "TVM_SECURE_WHITELIST": [100500]},
    ),
)
@pytest.mark.parametrize(
    "req_kw", ({"headers": [("X-Ya-Service-Ticket", "Pustite please")]}, {})
)
async def test_ignores_tvm_if_not_configured(
    handle_url,
    expected_response,
    tvm_config_kw,
    whitelist_config_kw,
    req_kw,
    run,
    aiotvm,
):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App({**tvm_config_kw, **whitelist_config_kw})
    client = await run(app)

    resp = await client.get(handle_url, **req_kw)

    assert resp.status == 200
    assert await resp.text() == expected_response
    assert aiotvm.fetch_service_source_id.called is False


@pytest.mark.parametrize(
    "handle_url, whitelist_name",
    [("/example/", "TVM_WHITELIST"), ("/secure-example/", "TVM_SECURE_WHITELIST")],
)
async def test_403_if_tvm_whitelist_is_empty(run, aiotvm, handle_url, whitelist_name):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            whitelist_name: [],
        }
    )
    client = await run(app)

    resp = await client.get(
        handle_url, headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    assert resp.status == 403


@pytest.mark.parametrize(
    "handle_url, whitelist_name",
    [("/example/", "TVM_WHITELIST"), ("/secure-example/", "TVM_SECURE_WHITELIST")],
)
async def test_logs_warning_if_tvm_whitelist_is_empty(
    run, aiotvm, handle_url, whitelist_name, caplog
):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            whitelist_name: [],
        }
    )
    client = await run(app)

    await client.get(handle_url, headers=[("X-Ya-Service-Ticket", "Pustite please")])

    warnings = [r for r in caplog.records if r.levelname == "WARNING"]
    assert len(warnings) == 1
    assert (
        warnings[0].message == f"Tvm whitelist {whitelist_name} is empty. Auth failed."
    )


@pytest.mark.parametrize(
    ("url", "expected_resp_text"),
    [("/ping", "Hello from ping_resource"), ("/sensors/", '{"sensors": []}')],
)
@pytest.mark.parametrize(
    "req_kw", ({"headers": [("X-Ya-Service-Ticket", "Pustite please")]}, {})
)
async def test_ignores_some_urls_by_default(
    url, expected_resp_text, req_kw, run, aiotvm
):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100, 500],
            "TVM_SECURE_WHITELIST": [900],
        }
    )
    client = await run(app)

    resp = await client.get(url, **req_kw)

    assert resp.status == 200
    assert await resp.text() == expected_resp_text
    assert aiotvm.fetch_service_source_id.called is False


@pytest.mark.parametrize(
    "req_kw", ({"headers": [("X-Ya-Service-Ticket", "Pustite please")]}, {})
)
async def test_ignores_tvm_for_configured_resource(req_kw, run, aiotvm):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100, 500],
            "TVM_SECURE_WHITELIST": [900],
        }
    )
    client = await run(app)

    resp = await client.get("/no-tvm-example/", **req_kw)

    assert resp.status == 200
    assert await resp.text() == "Hello from no_tvm_resource"
    assert aiotvm.fetch_service_source_id.called is False


async def test_ignores_default_whitelist_if_specific_one_is_required(run, aiotvm):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100500],
            "TVM_SECURE_WHITELIST": [100, 500],
        }
    )
    client = await run(app)

    resp = await client.get(
        "/secure-example/", headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    assert resp.status == 403


async def test_403_if_required_specific_tvm_whitelist_is_not_configured(run, aiotvm):
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100500],
        }
    )
    client = await run(app)

    resp = await client.get(
        "/secure-example/", headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    assert resp.status == 403


async def test_logs_warning_if_required_specific_tvm_whitelist_is_not_configured(
    run, aiotvm, caplog
):
    aiotvm.fetch_service_source_id.return_value = 100500
    app = App(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_WHITELIST": [100500],
        }
    )
    client = await run(app)

    await client.get(
        "/secure-example/", headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    warnings = [r for r in caplog.records if r.levelname == "WARNING"]
    assert len(warnings) == 1
    assert (
        warnings[0].message
        == "Missed tvm auth config TVM_SECURE_WHITELIST. Auth failed."
    )


async def test_applied_skip_decorator_if_it_is_first(run, aiotvm):
    @tvm_auth.skip
    @tvm_auth.only("TVM_SECURE_WHITELIST")
    async def extra_resource(*args, **kwargs):
        return aiohttp.web.Response(text="Hello from extra_resource")

    class ExtraApp(Lasagna):
        async def _setup_layers(self, db):
            _api = aiohttp.web.Application()

            _api.add_routes(
                [
                    aiohttp.web.get("/extra-example/", extra_resource),
                ]
            )
            return _api

    aiotvm.fetch_service_source_id.return_value = 100500

    app = ExtraApp(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_SECURE_WHITELIST": [999],
        }
    )
    client = await run(app)

    resp = await client.get(
        "/extra-example/", headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    assert resp.status == 200


async def test_applied_only_decorator_if_it_is_first(run, aiotvm):
    @tvm_auth.only("TVM_SECURE_WHITELIST")
    @tvm_auth.skip
    async def extra_resource(*args, **kwargs):
        return aiohttp.web.Response(text="Hello from extra_resource")

    class ExtraApp(Lasagna):
        async def _setup_layers(self, db):
            _api = aiohttp.web.Application()

            _api.add_routes(
                [
                    aiohttp.web.get("/extra-example/", extra_resource),
                ]
            )
            return _api

    aiotvm.fetch_service_source_id.return_value = 100500

    app = ExtraApp(
        {
            "TVM_DAEMON_URL": "http://tvm.daemon",
            "TVM_TOKEN": "token",
            "TVM_SECURE_WHITELIST": [999],
        }
    )
    client = await run(app)

    resp = await client.get(
        "/extra-example/", headers=[("X-Ya-Service-Ticket", "Pustite please")]
    )

    assert resp.status == 403
