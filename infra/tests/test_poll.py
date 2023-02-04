import asyncio
import json

import pytest

from aiohttp.test_utils import TestClient, TestServer

from infra.yp_drcp.server import make_app, PodResources, update_pod_info


# mark all tests as async
pytestmark = pytest.mark.asyncio


@pytest.fixture
async def client():
    app = await make_app()
    app['pods']['test_pod1'] = PodResources(resources=[], revision='old')

    client = TestClient(TestServer(app))
    await client.start_server()

    try:
        yield client
    finally:
        await client.close()


async def test_immediate_answer(client):
    conn = client.get("/test_pod1")
    resp = await asyncio.wait_for(conn, timeout=2.0)
    assert resp.status == 200
    res = json.loads(await resp.text())
    assert res['revision'] == 'old'
    assert res['resources'] == []


async def test_missing_pod(client):
    conn = client.get("/missing_pod")
    with pytest.raises(asyncio.TimeoutError):
        await asyncio.wait_for(conn, timeout=2.0)


async def test_expired_revision(client):
    conn = client.get("/test_pod1/old")
    with pytest.raises(asyncio.TimeoutError):
        await asyncio.wait_for(conn, timeout=2.0)


async def test_appearing_revision(client):
    conn = asyncio.ensure_future(client.get("/test_pod1/old"))
    with pytest.raises(asyncio.TimeoutError):
        await asyncio.wait_for(asyncio.shield(conn), timeout=2.0)

    await update_pod_info(client.app, 'test_pod1', PodResources(['x'], 'new'))

    resp = await asyncio.wait_for(asyncio.shield(conn), timeout=2.0)
    assert resp.status == 200
    res = json.loads(await resp.text())
    assert res['revision'] == 'new'
    assert res['resources'] == ['x']


async def test_appearing_pod(client):
    async def call(pod_id, revision=None):
        conn = asyncio.ensure_future(
            client.get(f'/{pod_id}/{revision}' if revision else f'/{pod_id}')
        )
        with pytest.raises(asyncio.TimeoutError):
            resp = await asyncio.wait_for(asyncio.shield(conn), timeout=2.0)
            assert False, str(await resp.text()) + str(revision)

        await update_pod_info(client.app, pod_id, PodResources(['x'], revision + '_new' if revision else 'new'))

        resp = await asyncio.wait_for(asyncio.shield(conn), timeout=2.0)
        assert resp.status == 200
        res = json.loads(await resp.text())
        assert res['revision'] == (revision + '_new' if revision else 'new')
        assert res['resources'] == ['x']

    await call('missing_pod1')
    await call('missing_pod2', 'new')
