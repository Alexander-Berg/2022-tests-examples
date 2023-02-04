# -*- coding: utf-8 -*-
# TODO: extract common code with balancer runner
import time
import socket
import errno
from balancer.test.util.process import Process, BaseProcessManager, ProcessException
from balancer.test.util.predef import http


PING_REQUEST = http.request.get('/__is_alive')


class CacheDaemon(Process):
    def __init__(self, options, http_connection_manager, config_manager, config):
        self.__config_manager = config_manager
        self.__config = config
        self.__conn_manager = http_connection_manager
        super(CacheDaemon, self).__init__(options)

    def check_process(self):
        st = time.time()

        while True:
            if not self.is_alive():
                raise ProcessException('Process {} is not alive'.format(self.name))
            try:
                with self.__conn_manager.create(self.config.port) as conn:
                    try:
                        conn.perform_request(PING_REQUEST)
                        self._options.logger.info('{} started in {} s'.format(self.name, (time.time() - st)))
                        break
                    except Exception, ex:
                        raise ProcessException('{} exception: {}'.format(self.name, str(ex)))
            except socket.error as e:
                if e.errno == errno.ECONNREFUSED:
                    pass
                else:
                    raise ProcessException('{} exception: {}'.format(self.name, str(e)))

            if time.time() - st > self._options.timeout:
                raise ProcessException('{} timed out'.format(self.name))
            time.sleep(0.01)

    @property
    def config(self):
        return self.__config

    def _finish(self):
        try:
            super(CacheDaemon, self)._finish()
        finally:
            self.__config.finish()


class CacheDaemonManager(BaseProcessManager):
    TIMEOUT = 20

    def __init__(self, resource_manager, logger, fs_manager, http_connection_manager,
                 config_manager, cachedaemon_path):
        super(CacheDaemonManager, self).__init__(resource_manager, logger, fs_manager)
        self.__fs_manager = fs_manager
        self.__conn_manager = http_connection_manager
        self.__cachedaemon_path = cachedaemon_path
        self.__config_manager = config_manager

    def start(self, config):
        """
        :param CacheDaemonConfig config: cachedaemon config
        :rtype: CacheDaemon
        """
        self.__config_manager.fill(config)

        cmd = self.__build_cmd(config)

        options = self._popen(
            cmd=cmd,
            name='cached',
            timeout=self.TIMEOUT,
        )

        cachedaemon = CacheDaemon(
            options=options,
            http_connection_manager=self.__conn_manager,
            config_manager=self.__config_manager,
            config=config,
        )
        self._resource_manager.register(cachedaemon)
        return cachedaemon

    def __build_cmd(self, config):
        cmd = list()
        cmd.append(self.__cachedaemon_path)
        cmd.append(config.get_path())
        cmd.extend(config.build_opts())
        return cmd
