# coding: utf-8

import os
import uwsgi
import uwsgidecorators

from butils.uwsgi_runner import ProxyWsgiApplication
from butils.application import getApplication

from butils import application
application.APP_NAME = 'balance-test-xmlrpc'

from test_xmlrpc.test_xmlrpc_servant import TestXmlRpcApp, TestXmlRpcPinger, create_dispatcher


class TestXmlRpcProxyApp(ProxyWsgiApplication):
    """
    По времени:
    на самом старте uwsgi процесса открывается сокет и начинает собирать соединения (в пределах секунды)
      здесь наверное важно чтобы очередь соединений не переполнилась (по дефолту 100 штук)
    потом происходит импорт данного модуля и по цепочке большей части кода (6-10 секунд)
      ~половина на импорт мапперов
      ~половина на вызов orm.configure_mappers() в publisher
      https://a.yandex-team.ru/arc/trunk/arcadia/billing/balance/balance/publisher/fetch.py#L30
    потом в postfork воркер-процессов инициализируется объект Application  и коннект с базой (0.5-1) секунд
    """

    def init_app(self):
        app = TestXmlRpcApp(worker_mode=True)
        self.dispatcher = create_dispatcher(app)


application = TestXmlRpcProxyApp()


@uwsgidecorators.postfork
def do_post_fork():
    on_worker = " on worker %s (PID %s)" % (uwsgi.worker_id(), os.getpid())
    application.init_app()
    uwsgi.log("Initialized Balance application" + on_worker)
    getApplication().new_session().connection()
    uwsgi.log("Warmed up db connections pool" + on_worker)
    # igogor: чтобы в первые 5 секунд результаты пинга сразу были. Выполнится на всех воркерах по разу, но не страшно
    TestXmlRpcPinger.cache_ping_results()
    uwsgi.log("Warmed up pinger" + on_worker)
    uwsgi.log("Finished postfork" + on_worker)


# запустится раз в пять секунд на одном произвольном воркере.
# Мула под это выделять как-то расточительно. Лучше вместо мула еще одного воркера добавить
@uwsgidecorators.timer(5, target='worker')
def cache_ping_results(signum):
    TestXmlRpcPinger.cache_ping_results()


if __name__ == '__main__':
    pass
    # run()
