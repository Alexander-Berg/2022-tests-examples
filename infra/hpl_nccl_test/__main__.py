from __future__ import print_function

from .layers import get_layers

from yt_ssh_swarm.sshd import SshDaemon
from yt_ssh_swarm.cypress_synchronizer import Synchronizer
from yt_ssh_swarm.hpl_tools import parse_hpl_results

import yt.yson as yson
from library.python.vault_client.instances import Production as VaultClient

from yt.common import update

import yt.wrapper as yt

import argparse
import logging
import os
import getpass
import subprocess
import sys
import socket
import time  # noqa
from datetime import datetime


logger = logging.getLogger(__name__)

environment = {}
RESULT_FILE = "yt_ssh.out"


class ConfigError(Exception):
    def __init__(self, name, value, message):
        self.name = name
        self.val = value
        self.message = message

    def __str__(self):
        return 'option {}={}: {}'.format(self.name, self.value, self.message)


class TestStatusError(Exception):
    def __init__(self, name, value, message):
        self.name = name
        self.value = value
        self.message = message

    def __str__(self):
        return 'test={} value={} {}'.format(self.name, self.value, self.message)


def parse_memory(memory):
    if isinstance(memory, int):
        return memory

    if memory.endswith("KiB"):
        memory = float(memory.rstrip("KiB")) * 1024
    elif memory.endswith("MiB"):
        memory = float(memory.rstrip("MiB")) * 1024**2
    elif memory.endswith("GiB"):
        memory = float(memory.rstrip("GiB")) * 1024**3
    elif memory.endswith("TiB"):
        memory = float(memory.rstrip("TiB")) * 1024**4
    elif memory.endswith("KB"):
        memory = float(memory.rstrip("KB")) * 1000
    elif memory.endswith("MB"):
        memory = float(memory.rstrip("MB")) * 1000**2
    elif memory.endswith("GB"):
        memory = float(memory.rstrip("GB")) * 1000**3
    elif memory.endswith("TB"):
        memory = float(memory.rstrip("TB")) * 1000**4

    return int(memory)


def configure_logging():
    logging.getLogger().setLevel(logging.INFO)
    formatter = logging.Formatter("%(asctime)-15s\t%(levelname)s\t%(message)s")
    handler = logging.StreamHandler()
    handler.setFormatter(formatter)
    logging.getLogger().addHandler(handler)


def run_with_logger(cmd, logfile, mode, **kwargs):
    logger.info("Execute command: {}".format(cmd))
    with open(logfile, mode) as flog:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=1, universal_newlines=True, **kwargs)
        while p.poll() is None:
            l=p.stdout.readline()
            flog.write(l)
            flog.flush()
            sys.stderr.write(l)
    return p.returncode


def run_selfcheck(client, logdir, faildir):
    tracelog = os.path.expanduser("~/selftest.trace")
    runlog = os.path.expanduser("~/selftest.log")

    command=["./bash_unit", "-f", "tap", "-t", tracelog, "./test_common_auto.sh"]
    ret = run_with_logger(command, logfile=runlog, mode="w+", cwd='/opt/hpc-benchmarks-cfg/selftest')

    now = datetime.now().strftime("%Y%m%d-%H%M%S")
    log_name=now + "_" + os.environ.get("YT_OPERATION_ID") + "_" + os.environ.get("YT_JOB_ID") + "_selftest.log"
    hostname = socket.gethostname()
    client.create("map_node", yt.ypath_join(logdir, hostname), recursive=True, ignore_existing=True)
    with open(tracelog, "rb") as fin:
        fpath = yt.ypath_join(logdir, hostname, log_name)
        client.write_file(fpath, fin.read())
        logger.debug("selfcheck trace path : %s", fpath)

    if ret == 0:
        return

    # Error path
    fpath = yt.ypath_join(faildir, hostname, log_name)
    client.create("map_node", yt.ypath_join(faildir, hostname), recursive=True, ignore_existing=True)
    with open(runlog, "rb") as fin:
        client.write_file(fpath, fin.read())

    logger.info("selfcheck error log : %s", fpath)
    raise TestStatusError("selftest", "fail", "return code: {}".format(ret))


def run_sshd_daemon_and_wait(username, port, key_blob, hostname_list, synchronizer):
    daemon = SshDaemon(username=username, port=port, key_blob=key_blob, hostname_list=hostname_list)

    logger.info("Use user-id:{} user:{}".format(os.getuid(), getpass.getuser()))
    daemon.prepare()
    daemon.start()

    synchronizer.set_attribute("sshd")
    synchronizer.wait_attribute_set("sshd")

    return daemon


def run_hpl_master(rundir, logdir, faildir, job_count, process_count_per_node, hpl_hw_config, hpl_memory, hpl_nbs, hpl_thresh, manual_mode=False, need_selfcheck=True):
    configure_logging()

    run_path = yt.ypath_join(rundir, os.environ["YT_OPERATION_ID"])
    synchronizer_path = yt.ypath_join(run_path, "synchronizer")
    sshd_user = getpass.getuser()
    base_port = 8080
    sshd_port = base_port + int(sshd_user.split("_")[-1])

    yt_client = yt.YtClient("hahn", token=os.environ.get("YT_SECURE_VAULT_YT_TOKEN"))
    synchronizer = Synchronizer(yt_client, synchronizer_path, sshd_port, job_count)
    ssh_key_blob = os.environ["YT_SECURE_VAULT_SSH_KEY"].encode('ascii')

    if need_selfcheck:
        run_selfcheck(yt_client, logdir, faildir)

    synchronizer.register_master(sshd_user)
    mpi_hosts_path = os.path.expanduser("~/mpi_hosts.txt")
    logger.info("BEGIN: ~/mpi_hosts.txt")
    with open(mpi_hosts_path, "w") as fout:
        for host in synchronizer.active_hosts:
            logger.info("{host} slots={slots}\n".format(host=host['hostname'], slots=process_count_per_node))
            fout.write("{host} slots={slots}\n".format(host=host['hostname'], slots=process_count_per_node))
    logger.info("END: ~/mpi_hosts.txt")

    daemon = run_sshd_daemon_and_wait(  # noqa
        username=sshd_user,
        port=sshd_port,
        key_blob=ssh_key_blob,
        hostname_list=synchronizer.active_hosts,
        synchronizer=synchronizer)

    if manual_mode:
        logger.info("Enter manual mode for 3600sec")
        time.sleep(3600)

    ranks=job_count * process_count_per_node

    first_error=None
    env={'NCCL_TESTS_MIN_BW': '8'}
    command=["./mpirun.sh", "-hostfile", mpi_hosts_path, '-np', str(ranks), '/opt/nccl-tests/build/sendrecv_perf', '-r', '-1']
    ret = run_with_logger(command, logfile=RESULT_FILE, mode="a+", cwd='/opt/hpc-benchmarks-cfg', env=env)
    if ret and first_error is None:
        first_error = TestStatusError('nccl', 'fail', 'Bad performance for sendrecv_perf, see logs')

    env={'NCCL_TESTS_MIN_BW': '70'}
    command=["./mpirun.sh", "-hostfile", mpi_hosts_path, '-np', str(ranks), '/opt/nccl-tests/build/all_reduce_perf', '-b', '128M', '-e', '1g', '-f', '2']
    ret = run_with_logger(command, logfile=RESULT_FILE, mode="a+", cwd='/opt/hpc-benchmarks-cfg', env=env)
    if ret and first_error is None:
        first_error = TestStatusError('nccl', 'fail', 'Bad performance all_reduce_perf, see logs')

    hpl_dat=os.path.expanduser("~/HPL.dat")
    command=['/opt/hpc-benchmarks-cfg/hpldat-samples/hpl_gen.py', '--nbs', str(hpl_nbs), '--gpu-per-node',
             str(process_count_per_node), '-o', hpl_dat, hpl_memory, str(job_count)]
    subprocess.check_call(command)

    command=["./mpirun.sh", "-hostfile", mpi_hosts_path, '-np', str(ranks), './hpl.sh', '--config', hpl_hw_config,
             '--config', './hpl_tune.sh', '--config', './xhpl_custom.sh', '--dat', hpl_dat]
    ret = run_with_logger(command, logfile=RESULT_FILE, mode="a+", cwd='/opt/hpc-benchmarks-cfg', env=env)
    if ret and first_error is None:
        first_error = TestStatusError('hpl', 'fail', 'main mpirun fail with status: {}'.format(ret))

    with open(RESULT_FILE, "rb") as fin:
        yt_client.write_file(yt.ypath_join(run_path, RESULT_FILE), fin.read())
        logger.info("result file saved ad : %s", yt.ypath_join(run_path, RESULT_FILE))

    # Check HPL performance
    with open(RESULT_FILE, "rb") as fin:
        result = parse_hpl_results(fin)
        logger.info("hpl results: {}".format(result))
        if result['status'] != 'passed':
            raise TestStatusError('hpl', 'status', "Bad status result:{}".format(result))
        if result['gflops_per'] < hpl_thresh:
            raise TestStatusError('hpl', 'bad_perf', "Performance is too low: got={} < thresh={}".format(result['gflops_per'], hpl_thresh))

    synchronizer.set_completed()
    if first_error is not None:
        raise first_error


def run_hpl_slave(rundir, logdir, faildir, job_count, need_selfcheck=True):
    configure_logging()

    run_path = yt.ypath_join(rundir, os.environ["YT_OPERATION_ID"])
    synchronizer_path = yt.ypath_join(run_path, "synchronizer")

    # expecting username like "yt_slot_0"
    sshd_user = getpass.getuser()
    base_port = 8080
    sshd_port = base_port + int(sshd_user.split("_")[-1])

    yt_client = yt.YtClient("hahn", token=os.environ.get("YT_SECURE_VAULT_YT_TOKEN"))
    synchronizer = Synchronizer(yt_client, synchronizer_path, sshd_port, job_count)
    ssh_key_blob = os.environ["YT_SECURE_VAULT_SSH_KEY"].encode('ascii')

    if need_selfcheck:
        run_selfcheck(yt_client, logdir, faildir)

    synchronizer.register_slave(
        sshd_user,
        lambda active_hosts:
        run_sshd_daemon_and_wait(
            username=sshd_user,
            port=sshd_port,
            key_blob=ssh_key_blob,
            hostname_list=active_hosts,
            synchronizer=synchronizer))


def get_token(token_path, opt_name):
    if token_path.startswith('yav:'):
        ts = token_path.split(':')
        if len(ts) != 3:
            raise ConfigError(opt_name, token_path, 'unexpected yav path scheme')
        secret = ts[1]
        key = ts[2]
        if not secret.startswith('sec-') and secret.startswith('ver-') :
            raise ConfigError(opt_name, secret, 'bad yavault secret prefix, expect "{sec-,ver-}"')

        val = VaultClient(decode_files=True).get_version(secret)['value']
        if key not in val:
            print("bad key key:{}, val:{}".format(key, val.keys()))
            raise ConfigError(opt_name, token_path, 'secret {} has no key {}'.format(secret, key))
        return val[key]
    elif token_path.startswith("//"):
        raise ConfigError(opt_name, token_path, 'FIXME: cypress path scheme is not yet supported')
    elif os.path.exists(token_path):
        return open(token_path).read().strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--settings")
    parser.add_argument("--manual", action='store_true', default=False)
    parser.add_argument("--no-selfcheck", dest='need_selfcheck', action='store_false', default=True)
    parser.add_argument("--job-count", type=int, default=0)
    parser.add_argument("--ssh-key", default="")
    parser.add_argument("--rundir", default="//tmp/RUNTIMECLOUD-18560/hpl_nccl_test")
    parser.add_argument("--scheduling_tag_filter", "--dc", default="")
    parser.add_argument("--scheduling_segment_modules", default=[], nargs='+')
    args = parser.parse_args()

    settings = yson.load(open(args.settings, "rb"))
    settings["memory_limit"] = parse_memory(settings["memory_limit"])
    if int(args.job_count) > 0:
        settings["job_count"] = int(args.job_count)

    if args.scheduling_tag_filter:
        settings["scheduling_tag_filter"] = args.scheduling_tag_filter
    if args.ssh_key:
        settings["ssh_key"] = args.ssh_key
    if args.scheduling_segment_modules:
        settings["scheduling_segment_modules"] = args.scheduling_segment_modules

    yt_client = yt.YtClient("hahn")
    yt_client.config["allow_http_requests_to_yt_from_job"] = True

    layers = get_layers(settings["layers_path"])

    rundir=yt.ypath_join(args.rundir, "jobs")
    logdir=yt.ypath_join(args.rundir, "log")
    faildir=yt.ypath_join(args.rundir, "errors")
    yt_client.create("map_node", rundir, recursive=True, ignore_existing=True)
    yt_client.create("map_node", logdir, recursive=True, ignore_existing=True)
    yt_client.create("map_node", faildir,  recursive=True, ignore_existing=True)

    run_master = lambda: run_hpl_master(  # noqa
        rundir,
        logdir,
        faildir,
        settings["job_count"],
        settings["process_count_per_node"],
        settings["hpl_hw_config"],
        settings["hpl_memory"],
        settings.get("hpl_nbs", "576"),
        int(settings["hpl_thresh"]),
        manual_mode=args.manual,
        need_selfcheck=args.need_selfcheck)

    run_slave = lambda: run_hpl_slave(rundir, logdir, faildir, settings["job_count"], need_selfcheck=args.need_selfcheck)  # noqa
    task_spec = {
        "system_layer_path": "//home/ignat/hpc_layers/ytserver_layer_ignat.tar.gz",
        "make_rootfs_writable": True,
    }
    spec = {
        "max_failed_job_count": 1,
        "secure_vault": {
            "YT_TOKEN": get_token(os.path.expanduser("~/.yt/token"), "yt_token"),
            "SSH_KEY": get_token(settings['ssh_key'], 'ssh_key')
        },
    }
    if "scheduling_tag_filter" in settings:
        spec["scheduling_tag_filter"] = settings["scheduling_tag_filter"]
    if "scheduling_segment_modules" in settings:
        spec["scheduling_segment_modules"] = settings["scheduling_segment_modules"]

    op_spec = yt.spec_builders.VanillaSpecBuilder()\
        .begin_task("hpl_master")\
            .command(run_master)\
            .layer_paths(layers)\
            .environment(update(environment, settings["environment"]))\
            .gpu_limit(settings["gpu_limit"])\
            .cpu_limit(settings["cpu_limit"])\
            .memory_limit(settings["memory_limit"])\
            .job_count(1)\
            .spec(task_spec)\
        .end_task()  # noqa

    if settings["job_count"] > 1:
        op_spec = \
            op_spec.begin_task("hpl_slave")\
                .command(run_slave)\
                .layer_paths(layers)\
                .environment(update(environment, settings["environment"]))\
                .gpu_limit(settings["gpu_limit"])\
                .cpu_limit(settings["cpu_limit"])\
                .memory_limit(settings["memory_limit"])\
                .job_count(settings["job_count"] - 1)\
                .spec(task_spec)\
            .end_task()  # noqa

    op_spec = op_spec\
        .pool_trees([settings["pool_tree"]])\
        .pool(settings["pool"])\
        .spec(spec)

    op = yt_client.run_operation(op_spec, sync=False)
    print("Start operation: {}".format(op.id))
    # TODO fetch logs from logbrokere here
    op.wait()

    result_path = yt.ypath_join(rundir, op.id, RESULT_FILE)
    for chunk in yt_client.read_file(result_path):
        sys.stdout.buffer.write(chunk)
    print("Result saved in", result_path)


if __name__ == "__main__":
    main()
