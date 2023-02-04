import btestlib.config as balance_config
from btestlib import environments
from btestlib import utils as butils
from simpleapi.common import logger

__author__ = 'fellow'

log = logger.get_logger()

server_ora = butils.XmlRpc.ReportingServerProxy(environments.simpleapi_env_ora().balance_test_xmlrpc_url,
                                                namespace='TestBalance')
server_pg = butils.XmlRpc.ReportingServerProxy(environments.simpleapi_env_pg().balance_test_xmlrpc_url,
                                               namespace='TestBalance')


def server():
    if balance_config.SIMPLE_API_VERSION == balance_config.SIMPLE_PG:
        return server_pg
    return server_ora


def find_in_log(path, timestamp, regexp):
    return server().FindInLogByRegexp(path, timestamp, regexp)


def find_config(path):
    return server().FindConfig(path)


def get_test_sequence_name(service_id):
    return server().GetTestSequenceNameForService(service_id)
