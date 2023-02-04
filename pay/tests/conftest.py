import pytest
import logging
import logging.config
from uuid import UUID

import aiohttp.pytest_plugin

from sendr_qlog import LoggerContext
from sendr_pytest import *  # noqa

from pay.integration.interactions import InteractionClients
from pay.integration.interactions.base import BaseInteractionClient, create_connector
from pay.integration.catalog import Card, Catalog, Databases, Merchants, Merchant, User, Users
from pay.integration.utils.log import configure_logging


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture(scope='session')
def setup_logging():
    configure_logging()


@pytest.fixture(autouse=True)
def loop(event_loop):
    return event_loop


@pytest.fixture
def request_id(rands):
    return rands()


@pytest.fixture
def logger(setup_logging):
    logger = logging.getLogger('integration')
    return LoggerContext(logger)


@pytest.fixture
def setup_connector():
    BaseInteractionClient.CONNECTOR = create_connector()


@pytest.fixture
async def clients(loop, request_id, logger, setup_connector):
    clients = InteractionClients(request_id=request_id, logger=logger)
    yield clients
    await clients.close()


@pytest.fixture
async def catalog(testing_catalog, clients):
    await testing_catalog.init(clients=clients)
    yield testing_catalog
    await testing_catalog.destroy()


@pytest.fixture(scope='session')
def testing_catalog():
    return Catalog(
        db=Databases(),
        users=Users(
            default=User(
                uid=4092658624,
                card=Card(card_id='card-x6737e3988e449f82c46d56c3', pan='4111111111100031', comment='payture non3ds'),
            ),
            rbs_non3ds=User(
                uid=4098619561,
                card=Card(card_id='card-xb103d016bb161d3132a024e2', pan='2200000000000004', comment='rbs-non3ds'),
            ),
            rbs_3dsv1=User(
                uid=4098619561,
                card=Card(
                    card_id='card-x445b8ece4256f1c226ad935a', pan='2200000000000046', comment='rbs-3ds1:veres=y,pares=a'
                ),
            ),
            rbs_3dsv2_frictionless=User(
                uid=4098619561,
                card=Card(
                    card_id='card-xf416ae3da991f3ddf813b6ad',
                    pan='5100000000000180',
                    comment='rbs-3dsv2:ares=y,rreq=y,method=1,autoacs=0',
                ),
            ),
            rbs_3dsv2_challenge=User(
                uid=4098619561,
                card=Card(
                    card_id='card-x06b0dddd98f046ee21b99994',
                    pan='2201382000000021',
                    comment='rbs-3dsv2:ares=c,rreq=y,method=1,autoacs=0',
                ),
            ),
            mts_non3ds=User(
                uid=4098619561,
                card=Card(card_id='card-x0e7544cc092fb371bcdd9b15', pan='5555555555555599', comment='rbs-non3ds'),
            ),
        ),
        merchants=Merchants(
            payture_non3ds=Merchant(
                id=UUID('f990febe-5ff1-4906-bf3f-2751338170c1'),
                callback_url='https://test.pay.yandex.ru/web/api/playground',
                api_key='f990febe-5ff1-4906-bf3f-2751338170c1.qatest',
            ),
            alfabank=Merchant(
                id=UUID('48d6f883-e7f8-4115-86dd-7ee8fadf63c0'),
                callback_url='https://test.pay.yandex.ru/web/api/playground',
                api_key='48d6f883e7f8411586dd7ee8fadf63c0.9Fs1jDUlell4YAKLuJv_78gzWe9tpEeb',
            ),
            rbs_mts=Merchant(
                id=UUID('c9ef1a71-f0ef-47c9-8d7c-d299652a7605'),
                callback_url='https://test.pay.yandex.ru/web/api/playground',
                api_key='c9ef1a71f0ef47c98d7cd299652a7605.MPGJDENOSe9PgDSZCOBFeMVaZYawL4fo',
            ),
            uniteller=Merchant(
                id=UUID('a4d400c5-74db-4b24-a883-4a14bf97fc76'),
                callback_url='https://test.pay.yandex.ru/web/api/playground',
                api_key='a4d400c574db4b24a8834a14bf97fc76.JkCWVyHyimE5M6NHb9sJI6hFh72IuEWm',
            ),
        ),
    )
