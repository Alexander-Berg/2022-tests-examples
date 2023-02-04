from multiprocessing import Process, Pipe
import subprocess
import os
import sys
import logging


class Task(Process):
    def __init__(self, name=None, logger=None, **params):
        """
            task.call_name(args...)       -> remote call task.name(args...)
            task.call_func(func, args...) -> remote call func(args...)
            task.call(args...)            -> remote call subprocess.call(args...)
            task.check_call(args...)      -> remote call subprocess.check_call(args...)
            task.check_output(args...)    -> remote call subprocess.check_output(args...)
        """
        if name is None:
            name = self.__class__.__name__
        Process.__init__(self, name=name)
        self.conn, self.conn2 = Pipe()
        self.logger = logger or logging.getLogger('Task {}'.format(self.name))
        self.params = params
        self.logger.debug('init {}'.format(params))
        self.init(**params)

    def init(self, **params):
        pass

    def __getattr__(self, name):
        if name.startswith('call_'):
            getattr(self, name[5:])

            def _call(*args, **kwargs):
                return self.call_func(name[5:], *args, **kwargs)

            return _call

        raise AttributeError(name)

    def run(self):
        self.conn.close()
        self.conn = None
        conn = self.conn2
        while True:
            func, args, kwargs = conn.recv()
            try:
                if isinstance(func, str):
                    func = getattr(self, func)
                ret = func(*args, **kwargs)
                conn.send((ret, None))
            except Exception as e:
                conn.send((None, e))

    def start(self):
        assert self.pid is None
        super().start()
        self.conn2.close()
        self.conn2 = None

    def stop(self):
        assert self.pid is not None
        try:
            self.conn.send((sys.exit, (0,), {}))
        except BrokenPipeError:
            # child task may be already dead due to crash, kill, etc.
            pass
        self.join()

    def call_func(self, func, *args, recv=True, **kwargs):
        assert self.pid is not None
        self.logger.debug('call_func {} {} {}'.format(func, args, kwargs))
        self.conn.send((func, args, kwargs))
        if recv:
            return self.recv()

    def recv(self):
        ret, e = self.conn.recv()
        if e is not None:
            raise e
        self.logger.debug('result {}'.format(ret))
        return ret

    def call(self, *args, **kwargs):
        return self.call_func(subprocess.call, *args, **kwargs)

    def check_call(self, *args, **kwargs):
        return self.call_func(subprocess.check_call, *args, **kwargs)

    def check_output(self, *args, **kwargs):
        return self.call_func(subprocess.check_output, *args, **kwargs)


class PerCpuTask(object):
    def __init__(self, cpu_task_cls, name=None, **task_params):
        if name is None:
            name = self.__class__.__name__
        self.cpu_task = {}
        for cpu in os.sched_getaffinity(0):
            task = cpu_task_cls(name='{}[{}]'.format(name, cpu), **task_params)
            self.cpu_task[cpu] = task

    def start(self):
        for cpu, task in self.cpu_task.items():
            task.start()
            task.call_func(os.sched_setaffinity, 0, [cpu])

    def stop(self):
        for cpu, task in self.cpu_task.items():
            task.stop()

    def call_func(self, func, *args, **kwargs):
        for cpu, task in self.cpu_task.items():
            task.call_func(func, recv=False, *args, **kwargs)
        res = {}
        for cpu, task in self.cpu_task.items():
            res[cpu] = task.recv()
        return res
