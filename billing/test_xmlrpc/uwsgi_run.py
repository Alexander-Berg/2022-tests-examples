# coding: utf-8
"""
Модуль предназначен для формирования параметров запуска и запуска uwsgi процесса
Делать import uwsgi можно только внутри uwsgi процесса.
Поэтому модуль запуска uwsgi процесса и модуль работающий внутри uwsgi удобнее разделить.
"""

import os

from butils.uwsgi_runner import UwsgiRunnerBase

# typing imports
from typing import List, Text


PING_CACHE = "pingcache"


class TestXmlRpcRunner(UwsgiRunnerBase):
    def __init__(self, config_path=None):  # type: (Text) -> None
        super(TestXmlRpcRunner, self).__init__(app_name="TestXmlRpc",
                                               pkg_name="balance-test-xmlrpc",
                                               entry_point="test_xmlrpc.start",
                                               config_path=config_path)

    def test_xmlrpc_port(self):  # type: () -> Text
        return self.get_uwsgi_config().findtext("Port")

    def _extra_args(self):  # type: () -> List[str]
        test_xmlrpc_uwsgi_config = self.get_uwsgi_config()
        host = test_xmlrpc_uwsgi_config.findtext("Host")
        port = self.test_xmlrpc_port()
        return [
            "--http-socket", "%s:%s" % (host, port),
            "--cache2", "name=%s,items=10" % (PING_CACHE,),
        ]


def test_xmlrpc_start():  # type: () -> None
    TestXmlRpcRunner().run()