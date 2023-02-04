from __future__ import print_function

from .layers import get_layers

from yt_ssh_swarm.sshd import SshDaemon
from yt_ssh_swarm.cypress_synchronizer import Synchronizer

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
import time  # noqa

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


def run_sshd_daemon_and_wait(username, port, key_blob, hostname_list, synchronizer):
    daemon = SshDaemon(username=username, port=port, key_blob=key_blob, hostname_list=hostname_list)

    logger.info("Use user-id:{} user:{}".format(os.getuid(), getpass.getuser()))
    daemon.prepare()
    daemon.start()

    synchronizer.set_attribute("sshd")
    synchronizer.wait_attribute_set("sshd")

    return daemon


def run_hpl_master(run_path, job_count, process_count_per_node, manual_mode=False):
    configure_logging()

    synchronizer_path = yt.ypath_join(run_path, "synchronizer")
    sshd_user = getpass.getuser()
    base_port = 8080
    sshd_port = base_port + int(sshd_user.split("_")[-1])

    yt_client = yt.YtClient("hahn", token=os.environ.get("YT_SECURE_VAULT_YT_TOKEN"))
    synchronizer = Synchronizer(yt_client, synchronizer_path, sshd_port, job_count)
    ssh_key_blob = os.environ["YT_SECURE_VAULT_SSH_KEY"].encode('ascii')

    synchronizer.register_master(sshd_user)

    mpi_hosts_path = os.path.expanduser("~/mpi_hosts.txt")
    with open(mpi_hosts_path, "w") as fout:
        for host in synchronizer.active_hosts:
            fout.write("{host} slots={slots}\n".format(host=host['hostname'], slots=process_count_per_node))
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

    hpl_dat=os.path.expanduser("~/HPL.dat")
    command=['/opt/hpc-benchmarks-cfg/hpldat-samples/hpl_gen.py', '-o', hpl_dat, '81000', str(job_count)]
    logger.info("Execute command: %s", command)
    subprocess.check_call(command, stdout=open(RESULT_FILE, "a+"))

    command=["./mpirun.sh", "-hostfile", mpi_hosts_path, '-np', str(ranks), './hpl.sh', '--config', 'hwcfg/y4n_a100_80g.sh',
             '--config', './hpl_tune.sh', '--config', './xhpl.sh', '--dat', hpl_dat]
    logger.info("Execute command: %s", command)
    subprocess.check_call(command, stdout=open(RESULT_FILE, "a+"), cwd='/opt/hpc-benchmarks-cfg')

    hplai_dat=os.path.expanduser("~/HPL_ai.dat")
    command=['/opt/hpc-benchmarks-cfg/hpldat-samples/hpl_gen.py', '--nbs', '576', '-o', hplai_dat, '75000', str(job_count)]
    logger.info("Execute command: %s", command)
    subprocess.check_call(command, stdout=open(RESULT_FILE, "a+"))

    command=["./mpirun.sh", "-hostfile", mpi_hosts_path, '-np', str(ranks), './hpl.sh', '--config', 'hwcfg/y4n_a100_80g.sh',
             '--config', './hpl_tune.sh', '--config', './xhpl_ai.sh', '--dat', hplai_dat]
    logger.info("Execute command: %s", command)
    subprocess.check_call(command, stdout=open(RESULT_FILE, "a+"), cwd='/opt/hpc-benchmarks-cfg')

    with open(RESULT_FILE, "rb") as fin:
        yt_client.write_file(yt.ypath_join(run_path, RESULT_FILE), fin.read())

    synchronizer.set_completed()


def run_hpl_slave(run_path, job_count):
    configure_logging()

    synchronizer_path = yt.ypath_join(run_path, "synchronizer")

    # expecting username like "yt_slot_0"
    sshd_user = getpass.getuser()
    base_port = 8080
    sshd_port = base_port + int(sshd_user.split("_")[-1])

    yt_client = yt.YtClient("hahn", token=os.environ.get("YT_SECURE_VAULT_YT_TOKEN"))
    synchronizer = Synchronizer(yt_client, synchronizer_path, sshd_port, job_count)
    ssh_key_blob = os.environ["YT_SECURE_VAULT_SSH_KEY"].encode('ascii')
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
    parser.add_argument("--job-count", type=int, default=0)
    parser.add_argument("--ssh-key", default="")
    parser.add_argument("--scheduling_tag_filter", "--dc", default="")
    args = parser.parse_args()

    settings = yson.load(open(args.settings, "rb"))
    settings["memory_limit"] = parse_memory(settings["memory_limit"])
    if int(args.job_count) > 0:
        settings["job_count"] = int(args.job_count)

    if args.scheduling_tag_filter:
        settings["scheduling_tag_filter"] = args.scheduling_tag_filter
    if args.ssh_key:
        settings["ssh_key"] = args.ssh_key

    yt_client = yt.YtClient("hahn")
    yt_client.config["allow_http_requests_to_yt_from_job"] = True

    layers = get_layers(settings["layers_path"])

    run_base_path = "//tmp/RUNTIMECLOUD-18560/hpl_experiment/run"
    run_path = yt_client.find_free_subpath(run_base_path)

    yt_client.create("map_node", run_path, recursive=True, ignore_existing=True)
    yt_client.create("map_node", yt.ypath_join(run_path, "synchronizer"), recursive=True, ignore_existing=True)

    run_master = lambda: run_hpl_master(  # noqa
        run_path,
        settings["job_count"],
        settings["process_count_per_node"],
        args.manual)

    run_slave = lambda: run_hpl_slave(run_path, settings["job_count"])  # noqa
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
        # "scheduling_tag_filter": settings.get("scheduling_tag_filter", ""),
        # "scheduling_segment_data_centers": [settings.get("scheduling_tag_filter", "")]
    }

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

    yt_client.run_operation(op_spec)

    result_path = yt.ypath_join(run_path, RESULT_FILE)

    for chunk in yt_client.read_file(result_path):
        sys.stdout.buffer.write(chunk)
    print("Result saved in", result_path)


if __name__ == "__main__":
    main()
