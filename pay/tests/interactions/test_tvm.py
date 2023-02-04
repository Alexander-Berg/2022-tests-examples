import pytest

from hamcrest import assert_that, raises, calling, equal_to
from mock import patch

from yb_darkspirit.interactions.tvm import TvmManager, UnexpectedTvmIdException, TvmConfig
from tvmauth import exceptions, TvmClient, TvmToolClientSettings


# The tickets below have been generated with tvmknife.
# See: https://wiki.yandex-team.ru/passport/tvm2/debug/#nuzhentiketdljaunit-testov
VALID_TICKET_100503_2010006 = '3:serv:CBAQ__________9_IggIl5EGEJbXeg:UEXz0YLYG0i6VwK06_mo7TVYb1ettf8V5KPJTksV2B-a7e-TmBQRcyvMrDNq3r0aDWRru8NXI_B2xPraEpTKxXTOEtOYwgPikDXP6vVQ5qP3nf_-39JdFkBNe72Hb51JEIbS6_xwxX6ygZcmoqCiLxFzZ27HwiLKvPhTSaAHLHk'
WRONG_DESTINATION_TICKET_2010006_100503 = '3:serv:CBAQ__________9_IggIltd6EJeRBg:A9S79DrKfYe64z0ERfHcByYvvA9Q9XzMPe5XLhcDtFn5UbI9-Ay1-chxwXs_k7HRVfHTPbL1HaygS9XAuv-6o_PxgnnOIqo-e2E698Vz3YeMLP_mZbbUQfAyGHP7vp-P1gsBA79eS7eKKvGkzF5AeFFNPyHkvTCwDBw3sNXHcEM'
UNEXPECTED_CLIENT_TICKET_100501_2010006 = '3:serv:CBAQ__________9_IggIlZEGEJbXeg:U1zFMm2as3_5ylDExp1tMrTZwERaWvu3O3VQecU3OW5IR7hM0HlvGHFD8eN5fmXcg4mCok49f5o2q9f1Z2jxLdljRNz-SK8_qs_2kRpPeh2IauNFsIW7WPNZ1VXpW4Sxq5qyd7A9G2QxgWf0nuBnivaa6OxcAb54RBKj-wSQGT0'
INVALID_TICKET_100503_201006 = '3:serv:CBAQ__________9_IggIl5EGEJbXeg:UEXz0YLYG'

LOCAL_TVM_AUTH_TOKEN = '600e1f045193d1b9cd21a232a37b4e59'
ANOTHER_SERVICE_ALIAS = 'serviceone'
ANOTHER_SERVICE_TVM_ID = 100503
SELF_TVM_ID = 2010006


def test_valid_tvm_ticket(tvm_manager):
    tvm_manager.check_ticket(VALID_TICKET_100503_2010006)


@pytest.mark.parametrize('ticket,exception_type', [
    (WRONG_DESTINATION_TICKET_2010006_100503, exceptions.TicketParsingException),
    (UNEXPECTED_CLIENT_TICKET_100501_2010006, UnexpectedTvmIdException),
    (INVALID_TICKET_100503_201006, exceptions.TicketParsingException),
])
def test_invalid_ticket(tvm_manager, ticket, exception_type):
    assert_that(
        calling(tvm_manager.check_ticket).with_args(ticket),
        raises(exception_type)
    )


def test_get_ticket(tvm_manager, another_tvm_client):
    ticket = tvm_manager.get_ticket(ANOTHER_SERVICE_ALIAS)
    assert_that(
        another_tvm_client.check_service_ticket(ticket).src,
        equal_to(SELF_TVM_ID)
    )


@pytest.fixture
def tvm_manager(application):
    with patch.object(TvmManager, '_get_local_auth_token') as get_local_auth_mock:
        get_local_auth_mock.return_value = LOCAL_TVM_AUTH_TOKEN
        yield TvmManager.from_app(application)


@pytest.fixture
def another_tvm_client(application):
    conf = TvmConfig(application)
    yield TvmClient(TvmToolClientSettings(
        self_alias=ANOTHER_SERVICE_ALIAS,
        auth_token=LOCAL_TVM_AUTH_TOKEN,
        port=conf.tvmtool_port,
        hostname=conf.tvmtool_host
    ))
