# -- coding: utf-8 --

import os
import asyncio
import yaml
import io
import logging
import asyncpg
import json

from library.python import resource
from aiohttp import web, ClientSession

async def proxy_request(request, url):
    async with ClientSession() as session:
        async with session.request(
            request.method,
            '{}{}'.format(url, request.url.relative()),
            data = await request.read()
        ) as resp:
            body = await resp.read()
            return web.Response(
                body=body,
                content_type=resp.content_type,
                status=resp.status
            )

async def get_url(app, instance):
    async with app['pool'].acquire() as connection:
        async with connection.transaction():
            result = await connection.fetchval(
                'select redirect_url from t_nirvana_route where instance_id = $1', instance
            )
            return result

async def set_url(app, instance, url):
    async with app['pool'].acquire() as connection:
        async with connection.transaction():
            await connection.execute(
                '''insert into t_nirvana_route(instance_id, redirect_url)
                values ($1, $2)
                on conflict(instance_id)
                do update set redirect_url = excluded.redirect_url''', instance, url
            )

async def operations(request):
    url = request.app['conf']['router']['default']
    return await proxy_request(request, url)

async def ping(request):
    return web.Response(status=200)

async def process_instance(request):
    instance = request.match_info.get('instance')
    logging.debug('instance: %s', instance)
    url = await get_url(request.app, instance)
    if not url:
        url = request.app['conf']['router']['default']
    logging.debug('redirect to: %s', url)
    return await proxy_request(request, url)

async def put_instance(request):
    instance = request.match_info.get('instance')
    logging.debug('instance: %s', instance)
    body = await request.read()
    environment = json.loads(body)['data']['options'].get('environment')
    router = request.app['conf']['router']
    if environment in router:
        url = router[environment]
    elif not environment:
        url = router['default']
    else:
        url = router['branch'].format(branch=environment)

    logging.debug('redirect to: %s', url)
    await set_url(request.app, instance, url)
    return await proxy_request(request, url)

def resolve_conf(cfg):
    for k, v in cfg.items():
        if isinstance(v, str):
            if v.startswith('$'):
                cfg[k] = os.environ[v[1:]]
        if isinstance(v, dict):
            resolve_conf(v)

async def close_asyncpg(app):
    await app['pool'].close()

async def init(loop):
    app = web.Application(loop=loop)
    conf = yaml.load(io.BytesIO(resource.resfs_read('config.yml')), Loader=yaml.FullLoader)
    resolve_conf(conf)
    app['conf'] = conf

    pg_conf = conf['postgres']
    app['pool'] = await asyncpg.create_pool(
        **pg_conf,
        ssl = True,
        max_cacheable_statement_size = 1,
        loop = loop
    )
    app.on_cleanup.append(close_asyncpg)

    host, port = conf['host'], conf['port']

    #app.router.add_route('GET', '/{power:\d+}', handle)
    app.router.add_route('*', '/v2/call', operations)
    app.router.add_route('GET', '/v2/call/{operation}/{instance}', process_instance)
    app.router.add_route('DELETE', '/v2/call/{operation}/{instance}', process_instance)
    app.router.add_route('PUT', '/v2/call/{operation}/{instance}', put_instance)
    app.router.add_route('GET', '/ping', ping)
    app.router.add_route('GET', '/', ping)
    return app, host, port

def run():
    logging.basicConfig(level=logging.DEBUG)
    loop = asyncio.get_event_loop()

    app, host, port = loop.run_until_complete(init(loop))
    web.run_app(app, host=host, port=port)

if __name__ == '__main__':
    run()
