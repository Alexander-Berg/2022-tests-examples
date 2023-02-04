import os
from collections import defaultdict
from functools import partial
from multiprocessing import Pool
from random import shuffle
from subprocess import PIPE, Popen

import yabs.logger as logger

global_host_prefix = "bootstrap-"
global_host_postfix = ".yp-c.yandex.net"


def get_hosts():
    logger.info("Trying to get list of production hosts")
    hosts = defaultdict(list)
    for cont_numb, cont_name in enumerate(["eagle", "eagle1", "eagle2"]):
        proc = Popen(["sky", "list", "SG@" + cont_name], stdout=PIPE, stderr=PIPE)
        proc.wait()
        for host in proc.stdout.readlines():
            host = host.strip()
            if not (host.startswith(global_host_prefix) and host.endswith(global_host_postfix)):
                continue
            host_truname = host.split(global_host_prefix)[1].split(global_host_postfix)[0]
            cluster = host_truname.split(".")[1]
            cluster = cluster.replace("myt", "msk")
            countur = host_truname.split("-")[0]
            assert countur == cont_name
            assert cluster in ["sas", "man", "vla", "msk"]
            logger.debug("%s, %s", (cont_numb, cluster), host)
            hosts[(cont_numb, cluster)].append(host)
    for hostnames in hosts.values():
        shuffle(hostnames)
    logger.info(
        "We have %d groups with total %d hosts",
        len(hosts),
        sum((len(i) for i in hosts.values()), 0),
    )
    return hosts


def download_access_logs(item, logs_dir=None):
    key, hostnames = item
    cont_numb, cluster = key
    src_path = "/samogon/%s/active/user/eagle/srvdata/logs/access.log" % cont_numb
    summary_size = 0
    summary_size_threshold = 50 * 1024 * 1024  # 50 Mb
    for hostname in hostnames:
        remote_filename = "root@{host}:{path}".format(host=hostname, path=src_path)
        local_filename = os.path.join(logs_dir, str(cluster) + str(cont_numb) + "_" + hostname)
        try:
            proc = Popen(["scp", remote_filename, local_filename], stdout=PIPE, stderr=PIPE)
            proc.wait()
            if proc.returncode != 0:
                raise Exception(proc.stderr.read())
            summary_size += os.path.getsize(local_filename)
        except Exception as exp:
            logger.error("Exception in download thread, host %s: %s", hostname, exp)
            logger.debug("trying another host")
            continue
        if summary_size >= summary_size_threshold:
            break
    mb_size = summary_size // (1024 * 1024)
    logger.info("Complete for %s size is %d MB", key, mb_size)
    return summary_size


def download_all_access_logs(logs_dir, hosts):
    proc_pool = Pool(len(hosts))
    logger.info("Started download access logs in %d threads", len(hosts))
    f_target = partial(download_access_logs, logs_dir=logs_dir)
    sizes = proc_pool.map(f_target, hosts.items())
    total_mb_size = sum(sizes, 0) // (1024 * 1024)
    lognames = [
        os.path.join(logs_dir, logname)
        for logname in os.listdir(logs_dir)
        if global_host_prefix in logname and global_host_postfix in logname
    ]
    logger.info("Download complete. Total %d files, %d MB", len(lognames), total_mb_size)
    return lognames


def fill_access_logs_dir(logs_dir):
    hosts = get_hosts()
    lognames = download_all_access_logs(logs_dir, hosts)
    return lognames
