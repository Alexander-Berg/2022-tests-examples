import subprocess
import json
import logging


class FioJob(object):
    def __init__(self, name='test', fio_bin='fio', cgroups=[], logger=None, **kwargs):
        self.name = name
        self.args = kwargs
        self.result = None
        self.cgroups = cgroups
        self.proc = None
        self.logger = logger or logging.getLogger()
        self.fio_bin = fio_bin

    def __setup(self):
        for cgroup in self.cgroups:
            cgroup.attach()

    def start(self):
        assert self.fio_bin is not None
        cmd = [self.fio_bin, '--output-format=json']
        cmd.append('--name={}'.format(self.name))
        cmd.append('--ioengine={}'.format(self.args.pop('ioengine', 'psync')))
        for k, v in self.args.items():
            cmd.append('--{}={}'.format(k, v))
        self.logger.info("start '%s'", "' '".join(cmd))
        self.proc = subprocess.Popen(cmd, preexec_fn=self.__setup, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    def finish(self):
        output, stderr = self.proc.communicate()
        text = output.decode('utf-8')
        stderr_text = stderr.decode('utf-8')
        self.logger.debug('stderr: %s', stderr_text)
        self.logger.debug('result: %s', text)
        text = text[text.find('{'):]
        self.result = json.loads(text)['jobs'][0]
        self.proc = None
        return self.result

    def done(self):
        return self.proc.poll() is not None

    def wait(self, timeout=None):
        try:
            self.proc.wait(timeout)
            return True
        except subprocess.TimeoutExpired:
            return False

    def stop(self):
        if self.proc is not None:
            self.proc.terminate()
            self.proc.wait()
            self.proc = None

    def kill(self):
        if self.proc is not None:
            self.proc.kill()
            self.proc.wait()
            self.proc = None

    def run(self):
        self.start()
        return self.finish()
