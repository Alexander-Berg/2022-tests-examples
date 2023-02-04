import os
import sys


def test_cgroup_leakage():
    # open("cgroups_checks.disable", 'a').close()
    # open("cgroups_excess_check.disable", 'a').close()
    open("cgroups_shortage_check.disable", 'a').close()

    with open("allowed_cgroups_excess.conf", 'w') as f:
        f.write(str(150))

    # introduce leakage in 'devices' cgroup controller
    for i in range(200):
        os.mkdir("/sys/fs/cgroup/devices/test_{}".format(i))

    # comment out 'rmdir()' to fail recipe
    for i in range(200):
        os.rmdir("/sys/fs/cgroup/devices/test_{}".format(i))

    # os.mkdir("/sys/fs/cgroup/blkio/test_a")
    # os.rmdir("/sys/fs/cgroup/blkio/test_a")
