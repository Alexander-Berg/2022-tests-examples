import time

from gunicorn.glogging import Logger


class GunicornLogger(Logger):
    def atoms(self, resp, req, environ, request_time):
        atoms = super(GunicornLogger, self).atoms(resp, req, environ, request_time)
        atoms['unixtime_msec'] = '%.3f' % time.time()
        return atoms
