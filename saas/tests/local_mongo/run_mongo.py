from yatest import common
import os
import subprocess
import logging


class MongoServerRunner():
    def __init__(self, pm, test_key):
        self._pm = pm
        self.port = None
        if not os.path.isfile('mongo.tgz'):
            raise Exception('mongo.tgz found')

        env = dict(os.environ)
        self.logs_dir = env.get('LOG_PATH') or common.output_path('logs_' + test_key)
        if not os.path.exists(self.logs_dir):
            os.makedirs(self.logs_dir)

        self.unpack_dir = os.path.join(os.getcwd(), 'mongo_unpacked')
        os.mkdir(self.unpack_dir)
        with open(os.path.join(self.logs_dir, 'mongo.untar.log'), 'w') as untar_log:
            p = subprocess.Popen(['tar', 'xvf', 'mongo.tgz', '-C', self.unpack_dir],
                                 stdout=untar_log, stderr=untar_log)
            p.wait()
        if p.returncode != 0:
            raise Exception('mongo untaring failed')

    def get_port(self):
        return self.port

    def __enter__(self):
        self.port = self._pm.get_port()

        mongo_db_dir = os.path.join(os.getcwd(), 'mongo_db')
        os.mkdir(mongo_db_dir)
        self.mongo_bin_path = os.path.join(self.unpack_dir, 'mongo', 'bin', 'mongod')
        self.pidfile_path = os.path.join(os.getcwd(), 'mongo.pid')

        cmd = [self.mongo_bin_path, '--port', str(self.port), '--logpath', os.path.join(self.logs_dir, 'mongo.log'),
               '--nounixsocket', '--dbpath', mongo_db_dir, '--master', '--smallfiles',
               '--noauth', '--pidfilepath', self.pidfile_path, '--fork', '--ipv6']

        with open(os.path.join(self.logs_dir, 'mongo.run.log'), 'w') as mongo_log:
            p = subprocess.Popen(cmd, stdout=mongo_log, stderr=mongo_log)
            p.wait()
        if p.returncode != 0:
            raise Exception('mongo running failed')
        with open(self.pidfile_path, 'r') as f:
            self.pid = int(f.read())

        logging.info("Mongod started at pid %d", self.pid)
        return self

    def __exit__(self, type, value, traceback):
        os.kill(self.pid, 2)
