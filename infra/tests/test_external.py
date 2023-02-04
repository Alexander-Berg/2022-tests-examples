"""
Ensure no new external checks added.

"""
import os

import yatest.common


def test_checks_in_white_list():
    whitelist = {
        'MANIFEST.json',
        'README.md',
        'abnormal_ip_conf.py',
        'bad_raid_check.py',
        'check_atop.sh',
        'check_cpu.py',
        'check_dstate_count.py',
        'check_iss_agent.py',
        'check_network_irq.py',
        'check_rotmissingdocs_cron.sh',
        'check_skycore_procs.py',
        'check_skynet_infra_procs.py',
        'check_skynet_procs.py',
        'cpu_state.sh',
        'cpu_throt_check.py',
        'dns64_check.sh',
        'fastbone.py',
        'fb_route_check.py',
        'fs_free_space.py',
        'fs_mount_opts.py',
        'grub2_check.py',
        'io_monitor.py',
        'ip_groups.sh',
        'link_utilization.sh',
        'load_monitor.py',
        'net_errors.py',
        'numa_memory.py',
        'nvidia_check.py',
        'packages_state.sh',
        'raid_state.py',
        'runtime_debsums.sh',
        'service_qloud_logoped_check.sh',
        'service_qloud_logoped_statforward_check.sh',
        'skybone_locked_mem.py',
        'skynet_copier.sh',
        'ssd_link.sh',
        'tsolro_check.py',
        'ya.make',
    }

    path = os.path.join(yatest.common.test_source_path(), '..', 'external')
    for filename in os.listdir(path):
        assert filename in whitelist
