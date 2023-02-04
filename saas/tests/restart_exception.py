import os


class RtyTestRestartException(Exception):
    LogPath = ''

    def __init__(self, path):
        self.LogPath = path
        super(RtyTestRestartException, self).__init__()


def check_logfile_busy_ports(logfile, checking_path):
    with open(logfile, 'r') as f:
        for line in f.readlines():
            if 'port is occupied' in line or 'port is busy' in line or 'Address already in use' in line:
                raise RtyTestRestartException(checking_path)


def check_log_busy_ports(log_path):
    if os.path.isfile(log_path):
        check_logfile_busy_ports(log_path, log_path)
    elif os.path.isdir(log_path):
        for f in os.listdir(log_path):
            ff = os.path.join(log_path, f)
            if os.path.isfile(ff) and f.endswith(('.stderr.log', 'proxy.log', '.err.txt')):
                check_logfile_busy_ports(ff, log_path)
