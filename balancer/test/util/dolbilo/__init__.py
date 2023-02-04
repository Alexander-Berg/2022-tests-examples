# -*- coding: utf-8 -*-
import time
from balancer.test.util.process import call


class Dexecutor(object):
    def __init__(self, dexecutor_path, logger, out_file, plan=None):
        super(Dexecutor, self).__init__()
        self.__dexecutor_path = dexecutor_path
        self.__logger = logger
        self.__out_file = out_file
        self.__plan = plan

    def set_plan(self, plan):
        self.__plan = plan

    def shoot(self, loader, port):
        cmd = [
            self.__dexecutor_path,
            '-o', self.__out_file,
            '-m', loader.mode,
            '--replace-port', str(port)
        ]
        if loader.plan is not None:
            cmd.extend(['-p', loader.plan])
        else:
            cmd.extend(['-p', self.__plan, '--circular'])
        if loader.simultaneous is not None:
            cmd.extend(['-s', str(loader.simultaneous)])
        if loader.rps is not None:
            cmd.extend(['--rps-schedule', loader.build_schedule()])
        else:
            time.sleep(loader.at_unix - time.time())
        call(cmd, self.__logger)
