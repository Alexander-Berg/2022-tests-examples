from yatest import common

import json
import subprocess
import threading
import time


def get_process_info(pid):
    params = ['%cpu', '%mem', 'vsz', 'rss', 'dsiz']
    cmd = ['ps', 'www', '-p', str(pid), '-o', ' '.join(params)]
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    if p.returncode:
        raise Exception('no process info')
    info_values = out.strip().split('\n')[-1].strip().split()
    info = dict(zip(params, info_values))
    return info


def append_process_info(pid, fname):
    info = get_process_info(pid)
    with open(fname, 'a+') as f:
        f.write(json.dumps(info) + "\n")


class PeriodicProf(threading.Thread):
    def __init__(self, pid, out_file):
        super(PeriodicProf, self).__init__()
        self.Pid = pid
        self.OutFile = out_file

    def run(self):
        while True:
            try:
                append_process_info(self.Pid, self.OutFile)
                time.sleep(5)
            except:
                return


def max_metrics(pfile):
    metrics_to_save = ['rss', '%cpu']
    maxs = dict([(nm, -1) for nm in metrics_to_save])
    with open(pfile, 'r') as f:
        for l in f.readlines():
            try:
                d = json.loads(l)
                for metr in metrics_to_save:
                    maxs[metr] = max(maxs[metr], float(d.get(metr, -1)))
            except:
                pass
    result = {}
    if maxs.get('rss', -1) > 0:
        result['rss_gb'] = round(maxs['rss'] / float(1000000), 2)
    if maxs.get('%cpu', -1) > 0:
        result['cpu'] = round(maxs['%cpu'] / float(100), 1)
    return result
