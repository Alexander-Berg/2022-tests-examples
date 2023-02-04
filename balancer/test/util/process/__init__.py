# -*- coding: utf-8 -*-
import os
import time
import signal
import subprocess
import sys
from collections import namedtuple
from abc import ABCMeta, abstractmethod, abstractproperty
from balancer.test.util.resource import AbstractResource


_CallResult = namedtuple('_CallResult', ['stdout', 'stderr', 'return_code'])


def forward_process_stderr():
    return os.getenv("Y_FORWARD_PROCESS_STDERR", None)


def _log_cmd(cmd):
    return ' '.join(cmd).replace('[', '\\[').replace(']', '\\]')


def call(cmd, logger, text=None):
    logger.info('call subprocess: {}'.format(_log_cmd(cmd)))
    process = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate(input=text)
    return _CallResult(stdout, stderr, process.returncode)


def get_memory_usage(pids, logger):
    pids = [str(p) for p in pids]
    output = call(["ps", "-o", "rss=", "-p", ','.join(pids)], logger).stdout
    return [1024 * int(line) for line in output.splitlines()]


def is_alive(pid):
    try:
        os.kill(pid, 0)
    except OSError:
        return False
    else:
        return True


def _loop(check, delay, timeout):
    start_time = time.time()
    while not check():
        if time.time() - start_time > timeout:
            return False
        time.sleep(delay)
    return True


def get_children(pid, logger, recursive=True):
    output = call(['pgrep', '-P', str(pid)], logger).stdout
    children = [int(line) for line in output.splitlines()]
    if recursive:
        res = list(children)
        for child in children:
            res.extend(get_children(child, logger, True))
        return res
    else:
        return children


def kill(pid):
    if is_alive(pid):
        os.kill(pid, signal.SIGTERM)

        def killed():
            return not is_alive(pid)

        if not _loop(killed, 0.01, 1):
            os.kill(pid, signal.SIGKILL)


class ProcessException(Exception):
    pass


class BalancerStartError(Exception):
    pass


class AbstractProcessOptions(object):
    def __init__(self, logger, name):
        super(AbstractProcessOptions, self).__init__()
        self.__logger = logger
        self.__name = name

    @property
    def logger(self):
        return self.__logger

    @property
    def name(self):
        return self.__name


class AbstractProcess(AbstractResource):
    __metaclass__ = ABCMeta

    def __init__(self, options):
        super(AbstractProcess, self).__init__()
        self._options = options
        try:
            self.check_process()
        except:
            # self.stop()
            raise

    @property
    def name(self):
        return self._options.name

    @abstractmethod
    def check_process(self):
        raise NotImplementedError()

    @abstractproperty
    def return_code(self):
        raise NotImplementedError()

    @abstractproperty
    def pid(self):
        raise NotImplementedError()

    @abstractmethod
    def is_alive(self):
        raise NotImplementedError()

    def _check_alive(self):
        if not self.is_alive():
            raise ProcessException('Process {} is not alive'.format(self.name))

    def _get_children(self, pid, recursive=True):
        return get_children(pid, self._options.logger, recursive=recursive)

    def get_children(self, recursive=True):
        self._check_alive()
        return self._get_children(self.pid, recursive)

    def get_memory_usage(self, recursive=True):
        self._check_alive()
        return get_memory_usage([self.pid] + self.get_children(recursive), self._options.logger)

    stop = AbstractResource.finish


class _ProcessPipe(AbstractResource):
    def __init__(self, pipe):
        super(_ProcessPipe, self).__init__()
        self.__pipe = pipe

    @property
    def pipe(self):
        return self.__pipe

    def _finish(self):
        pass


class _ProcessFilePipe(_ProcessPipe):
    def __init__(self, file_path):
        self.__file_path = file_path
        pipe = open(self.__file_path, 'wb')
        super(_ProcessFilePipe, self).__init__(pipe)

    @property
    def file_path(self):
        return self.__file_path

    def _finish(self):
        self.pipe.close()


class ProcessOptions(AbstractProcessOptions):
    def __init__(self, logger, name, cmd, stdin, stdout, stderr,
                 multiprocess, timeout, cwd, env, cmd_prefix, process):
        super(ProcessOptions, self).__init__(logger, name)
        self.__cmd = cmd
        self.__stdin = stdin
        self.__stdout = stdout
        self.__stderr = stderr
        self.__multiprocess = multiprocess
        self.__timeout = timeout
        self.__cwd = cwd
        self.__env = env
        self.__cmd_prefix = cmd_prefix
        self.__process = process

    @property
    def cmd(self):
        return self.__cmd

    @property
    def stdin(self):
        return self.__stdin

    @property
    def stdout(self):
        return self.__stdout

    @property
    def stderr(self):
        return self.__stderr

    @property
    def multiprocess(self):
        return self.__multiprocess

    @property
    def timeout(self):
        return self.__timeout

    @property
    def cwd(self):
        return self.__cwd

    @property
    def env(self):
        return self.__env

    @property
    def cmd_prefix(self):
        return self.__cmd_prefix

    @property
    def process(self):
        return self.__process


class ProcessMockOptions(AbstractProcessOptions):
    def __init__(self, logger, name, pid):
        super(ProcessMockOptions, self).__init__(logger, name)
        self.__pid = pid

    @property
    def pid(self):
        return self.__pid


class Process(AbstractProcess):
    MAX_ERR_LINES = 20

    def __init__(self, options):
        self.__stopped = False
        super(Process, self).__init__(options)

    def _check_alive(self):
        if not self.is_alive():
            stderr = self._options.stderr
            if isinstance(stderr, _ProcessFilePipe):
                stderr.finish()
                with open(stderr.file_path, 'r') as f:
                    text = f.read()
                lines = text.splitlines()
                if len(lines) > self.MAX_ERR_LINES:
                    prefix = self.MAX_ERR_LINES / 2
                    suffix = self.MAX_ERR_LINES / 2
                    text = '\n'.join(
                        lines[:prefix] +
                        ['===== {} lines skipped ====='.format(len(lines) - (prefix + suffix))] +
                        lines[-suffix:]
                    )
                if text:
                    raise ProcessException('Process {} is not alive (exit code: {}). Stderr:\n{}'.format(self.name, self.__process.returncode, text))
                else:
                    raise ProcessException('Process {} is not alive (exit code: {}). Stderr is empty'.format(self.name, self.__process.returncode))
            else:
                raise ProcessException('Process {} is not alive (exit code: {})'.format(self.name, self.__process.returncode))

    def check_process(self):
        time.sleep(self._options.timeout)
        self._check_alive()

    @property
    def return_code(self):
        if self.is_alive():
            raise ProcessException('{} is alive'.format(self.name))
        else:
            return self.__process.returncode

    @property
    def pid(self):
        return self.__process.pid

    @property
    def stdout(self):
        return self._options.stdout

    @property
    def stderr(self):
        return self._options.stderr

    @property
    def stdout_file(self):
        return self.stdout.file_path

    @property
    def stderr_file(self):
        return self.stderr.file_path

    @property
    def exit_code(self):
        return self.__process.returncode

    def is_alive(self):
        return self.__process.poll() is None

    def _finish(self):
        time.sleep(0.1)  # so that the logging thread in the process has enough time to dump the queue

        if not self.__stopped:
            self._options.logger.info('Shutting down {}'.format(self.name))
            if not self._options.multiprocess:
                self.__stop_main()
            else:
                children = self.get_children()
                self.__stop_main()
                self._options.logger.info('Shutting down {} children: {}'.format(self.name, children))
                for child in children:
                    kill(child)

            self._options.stdout.finish()
            self._options.stderr.finish()

            self.__stopped = True
        else:
            raise ProcessException('Process {} already stopped'.format(self.name))

    def __terminated(self):
        return not self.is_alive()

    def check_exit_code(self):
        if self.__process.returncode != 0:
            raise ProcessException('Process {} stopped abnormally, exit code = {}'.format(self.name, self.__process.returncode))

    def __stop_main(self):
        if self.is_alive():
            self.__process.terminate()

            try:
                self.__process.wait(20)
            except subprocess.TimeoutExpired:
                self.__process.kill()
                self.__process.wait()

    @property
    def __process(self):
        return self._options.process


class ProcessMock(AbstractProcess):
    @property
    def pid(self):
        return self._options.pid

    @property
    def return_code(self):
        raise NotImplementedError()

    def is_alive(self):
        return is_alive(self.pid)

    def check_process(self):
        self._check_alive()

    def _finish(self):
        pass


class BaseProcessManager(object):
    CHECK_TIMEOUT = 1

    def __init__(self, resource_manager, logger, fs_manager):
        super(BaseProcessManager, self).__init__()
        self._resource_manager = resource_manager
        self.__logger = logger
        self.__fs_manager = fs_manager

    def _popen(self, cmd, name=None, stdin=None, stdout=None, stderr=None,
               multiprocess=True, timeout=None, cwd=None, env=None):
        if name is None:
            name = cmd[0]
        name = name.replace('/', '_')
        stdout_pipe = self.__build_pipe(stdout, '{}_stdout.txt'.format(name))
        stderr_pipe = self.__build_pipe(stderr, '{}_stderr.txt'.format(name))
        if timeout is None:
            timeout = self.CHECK_TIMEOUT
        cmd_prefix = list()
        if env is not None:
            env.update(os.environ)
            cmd_prefix = ['{}={}'.format(k, v) for k, v in env.iteritems()]

        self.__logger.info('Starting process: {}'.format(_log_cmd(cmd_prefix + cmd)))
        if env is not None:
            self.__logger.info('Starting process (filtered): {}'.format(
                _log_cmd([
                    '{}={}'.format(k, v) for k, v in env.iteritems() if k in set([
                        'LD_PRELOAD', 'GETADDRINFO_PY', 'GAI_PYTHONPATH', 'BACKENDS_INFO', 'ADDRS_INFO',
                    ])
                ] + cmd)
            ))
        process = subprocess.Popen(
            cmd,
            stdin=stdin,
            stdout=stdout_pipe.pipe,
            stderr=sys.stderr if forward_process_stderr() else stderr_pipe.pipe,
            cwd=cwd,
            env=env,
        )
        return ProcessOptions(
            logger=self.__logger,
            name=name,
            cmd=cmd,
            stdin=stdin,
            stdout=stdout_pipe,
            stderr=stderr_pipe,
            multiprocess=multiprocess,
            timeout=timeout,
            cwd=cwd,
            env=env,
            cmd_prefix=cmd_prefix,
            process=process,
        )

    def _mock_options(self, pid, name):
        name = '{}_mock_{}'.format(name, pid)
        process_options = ProcessMockOptions(self.__logger, name, pid)
        self.__resource_manager.register(process_options)
        return process_options

    def _call(self, cmd, text=None):
        return call(cmd, self.__logger, text)

    def __build_pipe(self, pipe, pipe_name):
        if pipe is None:
            file_path = self.__fs_manager.create_file(pipe_name)
            result = _ProcessFilePipe(file_path)
        else:
            result = _ProcessPipe(pipe)
        self._resource_manager.register(result)
        return result


class ProcessManager(BaseProcessManager):
    def start(self, cmd, name=None, stdin=None, stdout=None, stderr=None,
              multiprocess=True, timeout=None, cwd=None, env=None):
        options = self._popen(
            cmd, name=name, stdin=stdin, stdout=stdout, stderr=stderr,
            multiprocess=multiprocess, timeout=timeout, cwd=cwd, env=env,
        )
        process = Process(options)
        self._resource_manager.register(process)
        return process

    def call(self, cmd, text=None):
        return self._call(cmd, text)

    def start_mock(self, pid, name):
        options = self._mock_options(pid, name)
        process = ProcessMock(options)
        self._resource_manager.register(process)
        return process
