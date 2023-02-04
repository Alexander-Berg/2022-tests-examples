# -*- coding: utf-8 -*-
import os
import time
import socket
import errno
import re
import json
from balancer.test.util import settings
from balancer.test.util import process
from balancer.test.util.predef import http


PING_REQUEST = http.request.get('/admin?action=dbg_workers_ready')
WORKERS_READY_FORMAT = re.compile(r'(\d+)/(\d+)')


class ReloadConfigFailure(Exception):
    def __init__(self, response):
        self.response = response
    pass


class Balancer(process.Process):
    def __init__(self, options, http_connection_manager, config_manager, config, ping_request, check_workers):
        self.__config_manager = config_manager
        self.__config = config
        self.__conn_manager = http_connection_manager
        self.__admin_port_candidate = 0
        super(Balancer, self).__init__(options)

    def get_balancer_output(self):
        with open(self.stdout_file) as f:
            stdout = "".join(f.readlines())

        with open(self.stderr_file) as f:
            stderr = "".join(f.readlines()[-10:])

        res = ""
        if len(stderr) > 0:
            res += "== stderr ==\n" + stderr

        if len(stdout) > 0:
            if len(res) > 0:
                res += "\n\n"
            res += "== stdout [last 10 lines] ==\n" + stdout
        return res

    def check_process(self):
        st = time.time()
        timed_out = False
        while not timed_out:
            timed_out = time.time() - st > self._options.timeout
            if not self.is_alive():
                raise process.BalancerStartError('Process {} is not alive, exit code = {}\n{}'.format(
                    self.name,
                    self.exit_code,
                    self.get_balancer_output()
                ))
            try:
                with self.__conn_manager.create(self.config.admin_port) as conn:
                    response = conn.perform_request(PING_REQUEST)
                    if self.__check_workers(response):
                        break
            except Exception, ex:
                if not (isinstance(ex, socket.error) and ex.errno == errno.ECONNREFUSED):
                    raise process.ProcessException('{} exception: {}'.format(self.name, str(ex)))
            time.sleep(1)
        if timed_out:
            raise process.ProcessException('{} timed out'.format(self.name))
        self._options.logger.info('{} started in {} s'.format(self.name, (time.time() - st)))

    def check_workers(self):
        with self.__conn_manager.create(self.config.admin_port) as conn:
            response = conn.perform_request(PING_REQUEST)
            return self.__check_workers(response)

    def __check_workers(self, response):
        if response.status != 200:
            raise process.ProcessException('dbg_workers_ready request got response with {} status'.format(response.status))
        result = WORKERS_READY_FORMAT.search(response.data.content)
        if result is None:
            self.__workers_ready_parse_error(response)
        groups = result.groups()
        if len(groups) != 2:
            self.__workers_ready_parse_error(response)
        ready_workers, all_workers = int(groups[0]), int(groups[1])
        return ready_workers == all_workers

    @staticmethod
    def __workers_ready_parse_error(response):
        raise process.ProcessException('Cannot parse result of dbg_workers_ready request: {}'.format(response.data.content))

    def get_master_pid(self):
        with self.__conn_manager.create(self.config.admin_port) as conn:
            response = conn.perform_request(http.request.get('/admin/events/call/dbg_master_pid'))
            # Event call may return multiple answers on configurations with workers
            master_pid = int(response.data.content.split('\n')[0])
            return master_pid

    def get_admin_port_candidate(self):
        return self.__admin_port_candidate

    def get_workers(self):
        self._check_alive()
        return self._get_children(self.get_master_pid())

    def get_master_memory_usage(self):
        self._check_alive()
        master_pid = self.get_master_pid()
        return process.get_memory_usage([master_pid], self._options.logger)[0]

    def reload_config(self, new_config, keep_ports=False, save_globals=False, timeout=0):
        if keep_ports:
            prev_config = self.__config
        else:
            prev_config = None
        self.__config_manager.fill(new_config, prev_config)
        cgi_opts = new_config.build_cgi_opts()
        self.__admin_port_candidate = new_config.admin_port
        path = "/admin?action=reload_config&new_config_path={}{}".format(new_config.get_path(), cgi_opts)
        if save_globals:
            path += '&save_globals=1'
        if timeout > 0:
            path += '&timeout=' + str(timeout)

        with self.__conn_manager.create(self.__config.admin_port) as conn:
            res = conn.perform_request(http.request.get(path)).data.content
            if res is not None and res != "":
                raise ReloadConfigFailure(res)
        while len(self._get_children(self.pid, False)) != 1:
            time.sleep(0.01)
        self.__config = new_config

    @property
    def config(self):
        return self.__config

    def _finish(self):
        try:
            super(Balancer, self)._finish()
            try:
                super(Balancer, self).check_exit_code()
            except process.ProcessException as e:
                raise process.ProcessException("{}\n{}".format(e.message, self.get_balancer_output()))

        finally:
            self.__config.finish()


class BalancerMock(process.ProcessMock):
    def __init__(self, options, config):
        self.__config = config
        super(BalancerMock, self).__init__(options)

    @property
    def config(self):
        return self.__config

    def _finish(self):
        self.__config.finish()


class Nginx(process.Process):
    def __init__(self, options, http_connection_manager, config_manager, config, ping_request, check_workers):
        self.__config_manager = config_manager
        self.__config = config
        self.__conn_manager = http_connection_manager
        self.__ping_request = ping_request
        self.__master_pid = None
        if check_workers:
            self.__dbg_workers_request = ping_request == PING_REQUEST
        else:
            self.__dbg_workers_request = False
        super(Nginx, self).__init__(options)

    @property
    def config(self):
        return self.__config

    def check_process(self):
        st = time.time()
        while True:
            if self.__master_pid is None:
                if os.path.exists(self.config.pid_file):
                    with open(self.config.pid_file) as f:
                        try:
                            self.__master_pid = int(f.read())
                        except ValueError:
                            pass
            else:
                try:
                    with self.__conn_manager.create(self.config.admin_port) as conn:
                        try:
                            conn.perform_request(self.__ping_request)
                            self._options.logger.info('{} started in {} s'.format(self.name, (time.time() - st)))
                            break
                        except Exception, ex:
                            raise process.ProcessException('{} exception: {}'.format(self.name, str(ex)))
                except socket.error as e:
                    if e.errno == errno.ECONNREFUSED:
                        pass
                    else:
                        raise process.ProcessException('{} exception: {}'.format(self.name, str(e)))
            if time.time() - st > self._options.timeout:
                raise process.ProcessException('{} timed out'.format(self.name))
            time.sleep(0.01)

    def _finish(self):
        children = process.get_children(self.__master_pid, self._options.logger)
        process.kill(self.__master_pid)
        for child in children:
            process.kill(child)


class BalancerManager(process.BaseProcessManager):
    TIMEOUT = 30

    def __init__(
            self,
            resource_manager,
            logger,
            fs_manager,
            http_connection_manager,
            config_manager,
            balancer_path,
            instances
    ):
        super(BalancerManager, self).__init__(resource_manager, logger, fs_manager)
        self.__fs_manager = fs_manager
        self.__conn_manager = http_connection_manager
        self.__balancer_path = balancer_path
        self.__config_manager = config_manager
        self.__instances = instances
        self.__dump_path = None
        self.__json_path = None

    def start(
            self,
            config,
            ping_request=None,
            env=None,
            timeout=None,
            check_workers=True,
            debug=False
    ):
        self.__config_manager.fill(config)
        pid = self.__search_instance(config.admin_port)
        if pid is None:
            if ping_request is None:
                ping_request = PING_REQUEST

            if timeout is None:
                timeout = self.TIMEOUT

            cmd = self.__build_cmd(config, debug=debug)

            options = self._popen(
                cmd=cmd,
                name='balancer',
                timeout=timeout,
                env=env,
            )

            if settings.flags.USE_NGINX:
                proc_cls = Nginx
            else:
                proc_cls = Balancer

            balancer = proc_cls(
                options=options,
                http_connection_manager=self.__conn_manager,
                config_manager=self.__config_manager,
                config=config,
                ping_request=ping_request,
                check_workers=check_workers,
            )
        else:
            options = self._mock_options(
                pid=pid,
                name='balancer',
            )
            balancer = BalancerMock(options, config)
        self._resource_manager.register(balancer)
        return balancer

    def check_config(self, config, opts=None):
        self.__config_manager.fill(config)
        cmd = self.__build_cmd(config)
        cmd.append('-K')
        cmd += (opts or [])
        return self._call(cmd)

    def dump_json(self, config):
        path = self.__fs_manager.create_file(os.path.splitext(os.path.basename(config.get_path()))[0] + '.json')
        with open(path, 'w') as f:
            json.dump(config.as_json(), f)
        return path

    def __get_resource(self, name):
        return settings.get_resource(
            py_path=os.path.abspath(os.path.join(os.path.dirname(__file__), name)),
            ya_path=name,
        )

    def __build_cmd(self, config, debug=False):
        cmd = list()
        cmd.append(self.__balancer_path)
        if settings.flags.USE_NGINX:
            cmd.append('-c')
            cmd.append(config.get_path())
            cmd.append('-p')
            cmd.append(self.__fs_manager.root_dir)
        else:
            cmd.append(config.get_path())
            cmd.extend(config.build_opts())
            if debug:
                cmd.append('-I')
        return cmd

    def __search_instance(self, admin_port):
        for pid, port in self.__instances:
            if port == admin_port or port is None:
                return pid
        return None
