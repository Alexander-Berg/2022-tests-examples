import subprocess
import logging
from .misc import run, run_output


class PrjQuota(object):
    def __init__(self, dir_path, prj_quota_bin='project_quota', create_prj_id=True, logger=None, **kwargs):
        self.dir_path = dir_path
        self.prj_quota_bin = prj_quota_bin
        self.logger = logger or logging.getLogger()

        if create_prj_id:
            ret = self.__init()
            if ret.returncode != 0 and "File exists" not in ret.stderr:
                # subprocess.CalledProcessError() doesn't print stderr
                # raise subprocess.CalledProcessError(returncode=ret.returncode, cmd=ret.args, output=ret.stdout, stderr=ret.stderr)
                raise Exception("{} returned {}: {}".format(ret.args, ret.returncode, ret.stderr))

            ret = self.__on()
            if ret.returncode != 0 and "Device or resource busy" not in ret.stderr:
                raise Exception("{} returned {}: {}".format(ret.args, ret.returncode, ret.stderr))

            self.create(**kwargs)

        self.get_prj_id()

    def __init(self):
        return run([self.prj_quota_bin, 'init', self.dir_path], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    def __on(self):
        return run([self.prj_quota_bin, 'on', self.dir_path], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    def __off(self):
        return run([self.prj_quota_bin, 'off', self.dir_path], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    def create(self, limit=None, ilimit=None):
        if limit is None or ilimit is None:
            limit = ''
            ilimit = ''

        ret = run([self.prj_quota_bin, 'create', self.dir_path, str(limit), str(ilimit)], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if ret.returncode != 0:
            raise Exception("{} returned {}: {}".format(ret.args, ret.returncode, ret.stderr))

        return ret

    def destroy(self):
        ret = run([self.prj_quota_bin, 'destroy', self.dir_path], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if ret.returncode != 0:
            raise Exception("{} returned {}: {}".format(ret.args, ret.returncode, ret.stderr))
        return ret

    def get_prj_id(self):
        return int(run_output([self.prj_quota_bin, 'project', self.dir_path]))

    def set_prj_id(self, prj_id):
        ret = run([self.prj_quota_bin, 'project', self.dir_path, str(prj_id)], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if ret.returncode != 0:
            raise Exception("{} returned {}: {}".format(ret.args, ret.returncode, ret.stderr))
        return ret

    def get_limit(self):
        return int(run_output([self.prj_quota_bin, 'limit', self.dir_path]))

    def set_limit(self, limit):
        ret = run([self.prj_quota_bin, 'limit', self.dir_path, str(limit)], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if ret.returncode != 0:
            raise Exception("{} returned {}: {}".format(ret.args, ret.returncode, ret.stderr))
        return ret

    def get_ilimit(self):
        return int(run_output([self.prj_quota_bin, 'ilimit', self.dir_path]))

    def set_ilimit(self, ilimit):
        ret = run([self.prj_quota_bin, 'ilimit', self.dir_path, str(ilimit)], check=False, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if ret.returncode != 0:
            raise Exception("{} returned {}: {}".format(ret.args, ret.returncode, ret.stderr))
        return ret

    def get_info(self):
        return run_output([self.prj_quota_bin, 'info', self.dir_path])

    def get_space_usage(self):
        info = self.get_info()
        for line in info.splitlines():
            if "usage" in line:
                return int(line.split()[1])

    def get_inodes_usage(self):
        info = self.get_info()
        for line in info.splitlines():
            if "inodes" in line:
                return int(line.split()[1])

    def check(self):
        return run_output([self.prj_quota_bin, 'check', self.dir_path], stderr=subprocess.STDOUT)
