import mock
import pytest
import json
import os
from checks import walle_fstab
from juggler.bundles import Status


def mock_rdev(rdev):
    m = mock.Mock()
    m.st_rdev = rdev
    return m



@pytest.fixture
def patch_minor_major(monkeypatch):
    def minor_patch(i):
        overrides = {
            2308: 4,
            2307: 3,
            2306: 2
        }
        if i not in overrides:
            return 0
        return overrides[i]
    monkeypatch.setattr(os, 'minor', minor_patch)

    def major_patch(i):
        overrides = {
            2308: 9,
            2307: 9,
            2306: 9
        }
        if i not in overrides:
            return 0
        return overrides[i]
    monkeypatch.setattr(os, 'major', major_patch)


@pytest.fixture
def real_files_mock(monkeypatch, patch_minor_major):
    def mock_fn(path, *args):
        files = {
            '/etc/fstab': mock.mock_open(
                read_data=
                '''
proc	/proc		proc	nodev,noexec,nosuid	0	0
# / was on /dev/md2 during installation
UUID=f660fef4-1a9d-4275-8c2c-e4612bf1fd05	/		ext4	rw,relatime	0	1
# /home was on /dev/md3 during installation
UUID=cc2a8f22-bed9-4f56-8fc1-c7f6b8968442		/home	ext4	barrier=1,noatime,lazytime,nosuid,nodev	0 2
# /place was on /dev/md4 during installation
UUID=92894e32-f95b-4600-a660-6d502c443726		/place	ext4	barrier=1,noatime,lazytime,nosuid,nodev	0 2
tmpfs		/place/db/bsconfig/webcache/shm	tmpfs	defaults,noexec,nosuid,huge=always,size=130G	0 0
/place/coredumps		/var/remote-log/coredumps	none	rw,bind,quota,data=ordered,usrquota,lazytime,stripe=128,noatime	0 0
/place/db		/var/remote-log/db	none	rw,bind,quota,data=ordered,usrquota,lazytime,stripe=128,noatime	0 0
/var/log/yandex		/var/remote-log/yandex	none	bind,rw,data=ordered,stripe=128,relatime	0 0
'''),
            '/proc/1/mountinfo': mock.mock_open(
                read_data=
                '''
18 23 0:18 / /sys rw,nosuid,nodev,noexec,relatime shared:9 - sysfs sysfs rw
19 23 0:4 / /proc rw,nosuid,nodev,noexec,relatime shared:25 - proc proc rw
20 23 0:6 / /dev rw,relatime shared:2 - devtmpfs udev rw,size=131932400k,nr_inodes=32983100,mode=755
21 20 0:19 / /dev/pts rw,nosuid,noexec,relatime shared:3 - devpts devpts rw,gid=5,mode=620,ptmxmode=000
22 23 0:20 / /run rw,nosuid,noexec,relatime shared:4 - tmpfs tmpfs rw,size=26388860k,mode=755
23 0 9:2 / / rw,relatime shared:1 - ext4 /dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05 rw,stripe=128,data=ordered
24 18 0:8 / /sys/kernel/debug rw,relatime shared:10 - debugfs none rw
25 18 0:7 / /sys/kernel/security rw,relatime shared:11 - securityfs none rw
26 22 0:21 / /run/lock rw,nosuid,nodev,noexec,relatime shared:5 - tmpfs none rw,size=5120k
27 22 0:22 / /run/shm rw,nosuid,nodev,relatime shared:6 - tmpfs none rw
28 23 9:2 /var/log/yandex /var/remote-log/yandex rw,relatime shared:26 - ext4 /dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05 rw,stripe=128,data=ordered
29 23 9:3 / /home rw,nosuid,nodev,noatime shared:27 - ext4 /dev/md3 rw,lazytime,stripe=128,data=ordered
31 23 9:4 / /place rw,noatime shared:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
32 31 0:24 / /place/db/bsconfig/webcache/shm rw,nosuid,noexec,relatime shared:29 - tmpfs tmpfs rw,size=136314880k,huge=always
33 23 9:4 /coredumps /var/remote-log/coredumps rw,nosuid,nodev,noatime shared:30 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
34 23 9:4 /db /var/remote-log/db rw,nosuid,nodev,noatime shared:31 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
35 18 0:25 / /sys/fs/pstore rw,relatime shared:12 - pstore pstore rw
36 18 0:26 / /sys/fs/cgroup rw,relatime shared:13 - tmpfs cgroup rw
37 36 0:27 / /sys/fs/cgroup/freezer rw,relatime shared:14 - cgroup cgroup rw,freezer
38 36 0:28 / /sys/fs/cgroup/memory rw,relatime shared:15 - cgroup cgroup rw,memory
39 36 0:29 / /sys/fs/cgroup/cpu rw,relatime shared:16 - cgroup cgroup rw,cpu
40 36 0:30 / /sys/fs/cgroup/cpuacct rw,relatime shared:17 - cgroup cgroup rw,cpuacct
41 36 0:31 / /sys/fs/cgroup/cpuset rw,relatime shared:18 - cgroup cgroup rw,cpuset
42 36 0:32 / /sys/fs/cgroup/net_cls rw,relatime shared:19 - cgroup cgroup rw,net_cls
43 36 0:33 / /sys/fs/cgroup/blkio rw,relatime shared:20 - cgroup cgroup rw,blkio
44 36 0:34 / /sys/fs/cgroup/devices rw,relatime shared:21 - cgroup cgroup rw,devices
45 36 0:35 / /sys/fs/cgroup/hugetlb rw,relatime shared:22 - cgroup cgroup rw,hugetlb
46 36 0:36 / /sys/fs/cgroup/pids rw,relatime shared:23 - cgroup cgroup rw,pids
47 36 0:37 / /sys/fs/cgroup/systemd rw,relatime shared:24 - cgroup cgroup rw,name=systemd
48 22 0:38 / /run/porto/kvs rw,nosuid,nodev,noexec,relatime shared:7 - tmpfs tmpfs rw,size=32768k,mode=750,gid=1333
49 22 0:39 / /run/porto/pkvs rw,nosuid,nodev,noexec,relatime shared:8 - tmpfs tmpfs rw,size=32768k,mode=750,gid=1333
50 18 0:11 / /sys/kernel/tracing rw,nosuid,nodev,noexec,relatime shared:32 - tracefs none rw,mode=755
274 31 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/porto_volumes/3/volume rw,nosuid,nodev,noatime shared:271 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
328 31 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/910d00f3ea22 rw,nosuid,nodev,noatime shared:281 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
318 31 9:4 /db/iss3/volumes/561e45a6068e-persist /place/porto_volumes/4/volume rw,nosuid,nodev,noatime shared:291 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
348 31 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/561e45a6068e rw,nosuid,nodev,noatime shared:301 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
381 31 0:69 / /place/vartmp/skynet/cqudp rw,relatime shared:472 - tmpfs tmpfs rw,size=131072k,mode=1755,uid=1044
610 31 0:70 / /place/porto_volumes/7/volume rw,nosuid,nodev,relatime shared:552 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/49,workdir=/proc/self/fd/50
588 31 0:70 / /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG rw,nosuid,nodev,relatime shared:532 master:552 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/49,workdir=/proc/self/fd/50
456 31 9:4 /db/iss3/volumes/1e3442011042-persist /place/porto_volumes/8/volume rw,nosuid,nodev,noatime shared:543 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
625 31 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/1e3442011042 rw,nosuid,nodev,noatime shared:565 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
599 31 9:4 /porto_volumes/9/native /place/porto_volumes/9/volume rw,nosuid,nodev,noatime shared:576 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
647 31 9:4 /porto_volumes/9/native /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH rw,nosuid,nodev,noatime shared:587 master:576 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
658 588 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG/logs rw,nosuid,nodev,noatime shared:598 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
669 588 9:4 /porto_volumes/9/native /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG/place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH rw,nosuid,nodev,noatime shared:609 master:576 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
639 31 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/porto_volumes/10/volume rw,nosuid,nodev,noatime shared:659 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
694 647 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK rw,nosuid,nodev,noatime shared:670 master:659 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
683 31 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/porto_volumes/11/volume rw,nosuid,nodev,noatime shared:683 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
759 647 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO rw,nosuid,nodev,noatime shared:694 master:683 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
710 31 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/porto_volumes/12/volume rw,nosuid,nodev,noatime shared:707 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
783 647 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI rw,nosuid,nodev,noatime shared:718 master:707 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
844 31 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/porto_volumes/16/volume rw,nosuid,nodev,noatime shared:803 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
879 647 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B rw,nosuid,nodev,noatime shared:814 master:803 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
868 31 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/porto_volumes/17/volume rw,nosuid,nodev,noatime shared:827 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
903 647 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO rw,nosuid,nodev,noatime shared:838 master:827 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
892 31 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/porto_volumes/18/volume rw,nosuid,nodev,noatime shared:851 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
927 647 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU rw,nosuid,nodev,noatime shared:862 master:851 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
916 31 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/porto_volumes/19/volume rw,nosuid,nodev,noatime shared:875 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
951 647 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH rw,nosuid,nodev,noatime shared:886 master:875 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
940 31 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/porto_volumes/20/volume rw,nosuid,nodev,noatime shared:899 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
975 647 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH rw,nosuid,nodev,noatime shared:910 master:899 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
964 31 9:4 /db/iss3/resources/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT /place/porto_volumes/21/volume rw,nosuid,nodev,noatime shared:923 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
999 647 9:4 /db/iss3/resources/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT rw,nosuid,nodev,noatime shared:934 master:923 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
100 31 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/porto_volumes/339/volume rw,nosuid,nodev,noatime shared:323 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
400 647 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B rw,nosuid,nodev,noatime shared:356 master:323 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
109 31 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/porto_volumes/350/volume rw,nosuid,nodev,noatime shared:1166 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
552 647 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C rw,nosuid,nodev,noatime shared:1180 master:1166 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
52 31 9:4 /db/iss3/resources/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD /place/porto_volumes/1009/volume rw,nosuid,nodev,noatime shared:324 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
374 647 9:4 /db/iss3/resources/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD rw,nosuid,nodev,noatime shared:345 master:324 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1295 31 0:93 / /place/porto_volumes/1071/volume rw,nosuid,nodev,relatime shared:1325 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/58,workdir=/proc/self/fd/59
1267 31 0:93 / /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD rw,nosuid,nodev,relatime shared:1299 master:1325 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/58,workdir=/proc/self/fd/59
773 31 9:4 /porto_volumes/1072/native /place/porto_volumes/1072/volume rw,nosuid,nodev,noatime shared:1313 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1313 31 9:4 /porto_volumes/1072/native /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT rw,nosuid,nodev,noatime shared:1341 master:1313 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1327 1267 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD/logs rw,nosuid,nodev,noatime shared:1355 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1452 1267 9:4 /porto_volumes/1072/native /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD/place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT rw,nosuid,nodev,noatime shared:1369 master:1313 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1284 31 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/porto_volumes/1073/volume rw,nosuid,nodev,noatime shared:1422 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1483 1313 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK rw,nosuid,nodev,noatime shared:1436 master:1422 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1469 31 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/porto_volumes/1074/volume rw,nosuid,nodev,noatime shared:1452 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1513 1313 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO rw,nosuid,nodev,noatime shared:1466 master:1452 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1499 31 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/porto_volumes/1075/volume rw,nosuid,nodev,noatime shared:1482 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1584 1313 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI rw,nosuid,nodev,noatime shared:1496 master:1482 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1532 31 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/porto_volumes/1076/volume rw,nosuid,nodev,noatime shared:1512 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1614 1313 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C rw,nosuid,nodev,noatime shared:1526 master:1512 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1600 31 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/porto_volumes/1077/volume rw,nosuid,nodev,noatime shared:1542 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1644 1313 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B rw,nosuid,nodev,noatime shared:1556 master:1542 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1660 31 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/porto_volumes/1079/volume rw,nosuid,nodev,noatime shared:1602 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1704 1313 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B rw,nosuid,nodev,noatime shared:1616 master:1602 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1690 31 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/porto_volumes/1080/volume rw,nosuid,nodev,noatime shared:1632 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1734 1313 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO rw,nosuid,nodev,noatime shared:1646 master:1632 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1720 31 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/porto_volumes/1081/volume rw,nosuid,nodev,noatime shared:1662 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1764 1313 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU rw,nosuid,nodev,noatime shared:1676 master:1662 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1750 31 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/porto_volumes/1082/volume rw,nosuid,nodev,noatime shared:1692 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1794 1313 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH rw,nosuid,nodev,noatime shared:1706 master:1692 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1780 31 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/porto_volumes/1083/volume rw,nosuid,nodev,noatime shared:1722 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1824 1313 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH rw,nosuid,nodev,noatime shared:1736 master:1722 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
92 31 9:4 /db/iss3/resources/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK /place/porto_volumes/1281/volume rw,nosuid,nodev,noatime shared:530 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
817 1313 9:4 /db/iss3/resources/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK rw,nosuid,nodev,noatime shared:667 master:530 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1658 31 9:4 /db/iss3/resources/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL /place/porto_volumes/1294/volume rw,nosuid,nodev,noatime shared:1915 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1986 1313 9:4 /db/iss3/resources/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL rw,nosuid,nodev,noatime shared:1929 master:1915 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
544 31 0:43 / /place/porto_volumes/1763/volume rw,nosuid,nodev,relatime shared:466 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/52,workdir=/proc/self/fd/55
878 31 0:43 / /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C rw,nosuid,nodev,relatime shared:748 master:466 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/52,workdir=/proc/self/fd/55
156 31 9:4 /porto_volumes/1764/native /place/porto_volumes/1764/volume rw,nosuid,nodev,noatime shared:817 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1249 31 9:4 /porto_volumes/1764/native /place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_cqXBZdG5GMN rw,nosuid,nodev,noatime shared:964 master:817 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1263 878 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/data rw,nosuid,nodev,noatime shared:1065 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1315 878 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/logs rw,nosuid,nodev,noatime shared:1155 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1361 878 9:4 /porto_volumes/1764/native /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_cqXBZdG5GMN rw,nosuid,nodev,noatime shared:1182 master:817 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
414 31 0:54 / /place/porto_volumes/1765/volume rw,nosuid,nodev,relatime shared:1344 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/55,workdir=/proc/self/fd/58
1167 31 0:54 / /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH rw,nosuid,nodev,relatime shared:1424 master:1344 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/55,workdir=/proc/self/fd/58
94 31 9:4 /porto_volumes/1766/native /place/porto_volumes/1766/volume rw,nosuid,nodev,noatime shared:1483 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1245 31 9:4 /porto_volumes/1766/native /place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_k0cQdW330IO rw,nosuid,nodev,noatime shared:1540 master:1483 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1425 1167 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/data rw,nosuid,nodev,noatime shared:1565 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1440 1167 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/logs rw,nosuid,nodev,noatime shared:1592 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1465 1167 9:4 /porto_volumes/1766/native /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_k0cQdW330IO rw,nosuid,nodev,noatime shared:1629 master:1483 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
'''
            ),
        }
        return files[path](path, *args)

    def realpath_mock(path):
        overrides = {
            '/dev/disk/by-uuid/cc2a8f22-bed9-4f56-8fc1-c7f6b8968442': '/dev/md3',
            '/dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05': '/dev/md2',
            '/dev/disk/by-uuid/92894e32-f95b-4600-a660-6d502c443726': '/dev/md4',
            '/place/coredumps': '/place/coredumps',
            '/coredumps': '/place/coredumps',
            '/db': '/place/db',
            '/place/db': '/place/db',
            '/var/log/yandex': '/var/log/yandex',
            '/non/existing/path': '/non/existing/path'
        }
        return overrides[path]
    monkeypatch.setattr(os.path, 'realpath', realpath_mock)

    def exists_mock(path):
        overrides = {
            '/dev/md3', '/dev/md2', '/dev/md4', '/place/coredumps', '/place/db', '/var/log/yandex'
        }
        return path in overrides
    monkeypatch.setattr(os.path, 'exists', exists_mock)

    def stat_mock(path):
        overrides = {
            '/dev/md2': mock_rdev(2306),
            '/dev/md3': mock_rdev(2307),
            '/dev/md4': mock_rdev(2308),
            '/place/coredumps': mock_rdev(0),
            '/place/db': mock_rdev(0),
            '/var/log/yandex': mock_rdev(0),
        }
        if path not in overrides:
            return None
        return overrides[path]
    monkeypatch.setattr('os.stat', stat_mock)

    return mock_fn


@pytest.fixture
def real_files_mock_faulty_mounts(monkeypatch, patch_minor_major):
    def mock_fn(path, *args):
        files = {
            '/etc/fstab': mock.mock_open(
                read_data=
                '''
proc	/proc		proc	nodev,noexec,nosuid	0	0
# / was on /dev/md2 during installation
UUID=f660fef4-1a9d-4275-8c2c-e4612bf1fd05	/		ext4	rw,relatime	0	1
# /home was on /dev/md3 during installation
UUID=cc2a8f22-bed9-4f56-8fc1-c7f6b8968442		/home	ext4	barrier=1,noatime,lazytime,nosuid,nodev	0 2
# /place was on /dev/md4 during installation
UUID=92894e32-f95b-4600-a660-6d502c443726		/place	ext4	barrier=1,noatime,lazytime,nosuid,nodev	0 2
tmpfs		/place/db/bsconfig/webcache/shm	tmpfs	defaults,noexec,nosuid,huge=always,size=130G	0 0
/place/coredumps		/var/remote-log/coredumps	none	rw,bind,quota,data=ordered,usrquota,lazytime,stripe=128,noatime	0 0
/place/db		/var/remote-log/db	none	rw,bind,quota,data=ordered,usrquota,lazytime,stripe=128,noatime	0 0
/var/log/yandex		/var/remote-log/yandex	none	bind,rw,data=ordered,stripe=128,relatime	0 0
'''),
            '/proc/1/mountinfo': mock.mock_open(
                read_data=
                '''
18 23 0:18 / /sys rw,nosuid,nodev,noexec,relatime shared:9 - sysfs sysfs rw
19 23 0:4 / /proc rw,nosuid,nodev,noexec,relatime shared:25 - proc proc rw
20 23 0:6 / /dev rw,relatime shared:2 - devtmpfs udev rw,size=131932400k,nr_inodes=32983100,mode=755
21 20 0:19 / /dev/pts rw,nosuid,noexec,relatime shared:3 - devpts devpts rw,gid=5,mode=620,ptmxmode=000
22 23 0:20 / /run rw,nosuid,noexec,relatime shared:4 - tmpfs tmpfs rw,size=26388860k,mode=755
23 0 9:2 / / rw,relatime shared:1 - ext4 /dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05 rw,stripe=128,data=ordered
24 18 0:8 / /sys/kernel/debug rw,relatime shared:10 - debugfs none rw
25 18 0:7 / /sys/kernel/security rw,relatime shared:11 - securityfs none rw
26 22 0:21 / /run/lock rw,nosuid,nodev,noexec,relatime shared:5 - tmpfs none rw,size=5120k
27 22 0:22 / /run/shm rw,nosuid,nodev,relatime shared:6 - tmpfs none rw
28 23 9:2 /var/log/yandex /var/remote-log/yandex rw,relatime shared:26 - ext4 /dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05 rw,stripe=128,data=ordered
31 23 9:4 / /place ro,noatime shared:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
32 31 0:24 / /place/db/bsconfig/webcache/shm rw,nosuid,noexec,relatime shared:29 - tmpfs tmpfs rw,size=136314880k,huge=always
33 23 9:4 /coredumps /var/remote-log/coredumps rw,nosuid,nodev,noatime shared:30 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
34 23 9:4 /db /var/remote-log/db rw,nosuid,nodev,noatime shared:31 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
35 18 0:25 / /sys/fs/pstore rw,relatime shared:12 - pstore pstore rw
36 18 0:26 / /sys/fs/cgroup rw,relatime shared:13 - tmpfs cgroup rw
37 36 0:27 / /sys/fs/cgroup/freezer rw,relatime shared:14 - cgroup cgroup rw,freezer
38 36 0:28 / /sys/fs/cgroup/memory rw,relatime shared:15 - cgroup cgroup rw,memory
39 36 0:29 / /sys/fs/cgroup/cpu rw,relatime shared:16 - cgroup cgroup rw,cpu
40 36 0:30 / /sys/fs/cgroup/cpuacct rw,relatime shared:17 - cgroup cgroup rw,cpuacct
41 36 0:31 / /sys/fs/cgroup/cpuset rw,relatime shared:18 - cgroup cgroup rw,cpuset
42 36 0:32 / /sys/fs/cgroup/net_cls rw,relatime shared:19 - cgroup cgroup rw,net_cls
43 36 0:33 / /sys/fs/cgroup/blkio rw,relatime shared:20 - cgroup cgroup rw,blkio
44 36 0:34 / /sys/fs/cgroup/devices rw,relatime shared:21 - cgroup cgroup rw,devices
45 36 0:35 / /sys/fs/cgroup/hugetlb rw,relatime shared:22 - cgroup cgroup rw,hugetlb
46 36 0:36 / /sys/fs/cgroup/pids rw,relatime shared:23 - cgroup cgroup rw,pids
47 36 0:37 / /sys/fs/cgroup/systemd rw,relatime shared:24 - cgroup cgroup rw,name=systemd
48 22 0:38 / /run/porto/kvs rw,nosuid,nodev,noexec,relatime shared:7 - tmpfs tmpfs rw,size=32768k,mode=750,gid=1333
49 22 0:39 / /run/porto/pkvs rw,nosuid,nodev,noexec,relatime shared:8 - tmpfs tmpfs rw,size=32768k,mode=750,gid=1333
50 18 0:11 / /sys/kernel/tracing rw,nosuid,nodev,noexec,relatime shared:32 - tracefs none rw,mode=755
274 31 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/porto_volumes/3/volume rw,nosuid,nodev,noatime shared:271 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
328 31 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/910d00f3ea22 rw,nosuid,nodev,noatime shared:281 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
318 31 9:4 /db/iss3/volumes/561e45a6068e-persist /place/porto_volumes/4/volume rw,nosuid,nodev,noatime shared:291 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
348 31 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/561e45a6068e rw,nosuid,nodev,noatime shared:301 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
381 31 0:69 / /place/vartmp/skynet/cqudp rw,relatime shared:472 - tmpfs tmpfs rw,size=131072k,mode=1755,uid=1044
610 31 0:70 / /place/porto_volumes/7/volume rw,nosuid,nodev,relatime shared:552 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/49,workdir=/proc/self/fd/50
588 31 0:70 / /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG rw,nosuid,nodev,relatime shared:532 master:552 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/49,workdir=/proc/self/fd/50
456 31 9:4 /db/iss3/volumes/1e3442011042-persist /place/porto_volumes/8/volume rw,nosuid,nodev,noatime shared:543 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
625 31 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/1e3442011042 rw,nosuid,nodev,noatime shared:565 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
599 31 9:4 /porto_volumes/9/native /place/porto_volumes/9/volume rw,nosuid,nodev,noatime shared:576 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
647 31 9:4 /porto_volumes/9/native /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH rw,nosuid,nodev,noatime shared:587 master:576 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
658 588 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG/logs rw,nosuid,nodev,noatime shared:598 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
669 588 9:4 /porto_volumes/9/native /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG/place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH rw,nosuid,nodev,noatime shared:609 master:576 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
639 31 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/porto_volumes/10/volume rw,nosuid,nodev,noatime shared:659 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
694 647 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK rw,nosuid,nodev,noatime shared:670 master:659 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
683 31 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/porto_volumes/11/volume rw,nosuid,nodev,noatime shared:683 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
759 647 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO rw,nosuid,nodev,noatime shared:694 master:683 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
710 31 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/porto_volumes/12/volume rw,nosuid,nodev,noatime shared:707 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
783 647 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI rw,nosuid,nodev,noatime shared:718 master:707 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
844 31 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/porto_volumes/16/volume rw,nosuid,nodev,noatime shared:803 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
879 647 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B rw,nosuid,nodev,noatime shared:814 master:803 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
868 31 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/porto_volumes/17/volume rw,nosuid,nodev,noatime shared:827 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
903 647 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO rw,nosuid,nodev,noatime shared:838 master:827 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
892 31 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/porto_volumes/18/volume rw,nosuid,nodev,noatime shared:851 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
927 647 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU rw,nosuid,nodev,noatime shared:862 master:851 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
916 31 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/porto_volumes/19/volume rw,nosuid,nodev,noatime shared:875 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
951 647 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH rw,nosuid,nodev,noatime shared:886 master:875 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
940 31 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/porto_volumes/20/volume rw,nosuid,nodev,noatime shared:899 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
975 647 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH rw,nosuid,nodev,noatime shared:910 master:899 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
964 31 9:4 /db/iss3/resources/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT /place/porto_volumes/21/volume rw,nosuid,nodev,noatime shared:923 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
999 647 9:4 /db/iss3/resources/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT rw,nosuid,nodev,noatime shared:934 master:923 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
100 31 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/porto_volumes/339/volume rw,nosuid,nodev,noatime shared:323 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
400 647 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B rw,nosuid,nodev,noatime shared:356 master:323 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
109 31 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/porto_volumes/350/volume rw,nosuid,nodev,noatime shared:1166 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
552 647 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C rw,nosuid,nodev,noatime shared:1180 master:1166 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
52 31 9:4 /db/iss3/resources/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD /place/porto_volumes/1009/volume rw,nosuid,nodev,noatime shared:324 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
374 647 9:4 /db/iss3/resources/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD rw,nosuid,nodev,noatime shared:345 master:324 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1295 31 0:93 / /place/porto_volumes/1071/volume rw,nosuid,nodev,relatime shared:1325 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/58,workdir=/proc/self/fd/59
1267 31 0:93 / /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD rw,nosuid,nodev,relatime shared:1299 master:1325 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/58,workdir=/proc/self/fd/59
773 31 9:4 /porto_volumes/1072/native /place/porto_volumes/1072/volume rw,nosuid,nodev,noatime shared:1313 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1313 31 9:4 /porto_volumes/1072/native /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT rw,nosuid,nodev,noatime shared:1341 master:1313 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1327 1267 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD/logs rw,nosuid,nodev,noatime shared:1355 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1452 1267 9:4 /porto_volumes/1072/native /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD/place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT rw,nosuid,nodev,noatime shared:1369 master:1313 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1284 31 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/porto_volumes/1073/volume rw,nosuid,nodev,noatime shared:1422 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1483 1313 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK rw,nosuid,nodev,noatime shared:1436 master:1422 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1469 31 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/porto_volumes/1074/volume rw,nosuid,nodev,noatime shared:1452 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1513 1313 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO rw,nosuid,nodev,noatime shared:1466 master:1452 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1499 31 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/porto_volumes/1075/volume rw,nosuid,nodev,noatime shared:1482 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1584 1313 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI rw,nosuid,nodev,noatime shared:1496 master:1482 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1532 31 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/porto_volumes/1076/volume rw,nosuid,nodev,noatime shared:1512 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1614 1313 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C rw,nosuid,nodev,noatime shared:1526 master:1512 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1600 31 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/porto_volumes/1077/volume rw,nosuid,nodev,noatime shared:1542 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1644 1313 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B rw,nosuid,nodev,noatime shared:1556 master:1542 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1660 31 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/porto_volumes/1079/volume rw,nosuid,nodev,noatime shared:1602 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1704 1313 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B rw,nosuid,nodev,noatime shared:1616 master:1602 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1690 31 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/porto_volumes/1080/volume rw,nosuid,nodev,noatime shared:1632 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1734 1313 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO rw,nosuid,nodev,noatime shared:1646 master:1632 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1720 31 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/porto_volumes/1081/volume rw,nosuid,nodev,noatime shared:1662 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1764 1313 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU rw,nosuid,nodev,noatime shared:1676 master:1662 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1750 31 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/porto_volumes/1082/volume rw,nosuid,nodev,noatime shared:1692 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1794 1313 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH rw,nosuid,nodev,noatime shared:1706 master:1692 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1780 31 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/porto_volumes/1083/volume rw,nosuid,nodev,noatime shared:1722 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1824 1313 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH rw,nosuid,nodev,noatime shared:1736 master:1722 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
92 31 9:4 /db/iss3/resources/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK /place/porto_volumes/1281/volume rw,nosuid,nodev,noatime shared:530 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
817 1313 9:4 /db/iss3/resources/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK rw,nosuid,nodev,noatime shared:667 master:530 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1658 31 9:4 /db/iss3/resources/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL /place/porto_volumes/1294/volume rw,nosuid,nodev,noatime shared:1915 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1986 1313 9:4 /db/iss3/resources/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL rw,nosuid,nodev,noatime shared:1929 master:1915 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
544 31 0:43 / /place/porto_volumes/1763/volume rw,nosuid,nodev,relatime shared:466 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/52,workdir=/proc/self/fd/55
878 31 0:43 / /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C rw,nosuid,nodev,relatime shared:748 master:466 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/52,workdir=/proc/self/fd/55
156 31 9:4 /porto_volumes/1764/native /place/porto_volumes/1764/volume rw,nosuid,nodev,noatime shared:817 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1249 31 9:4 /porto_volumes/1764/native /place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_cqXBZdG5GMN rw,nosuid,nodev,noatime shared:964 master:817 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1263 878 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/data rw,nosuid,nodev,noatime shared:1065 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1315 878 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/logs rw,nosuid,nodev,noatime shared:1155 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1361 878 9:4 /porto_volumes/1764/native /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_cqXBZdG5GMN rw,nosuid,nodev,noatime shared:1182 master:817 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
414 31 0:54 / /place/porto_volumes/1765/volume rw,nosuid,nodev,relatime shared:1344 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/55,workdir=/proc/self/fd/58
1167 31 0:54 / /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH rw,nosuid,nodev,relatime shared:1424 master:1344 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/55,workdir=/proc/self/fd/58
94 31 9:4 /porto_volumes/1766/native /place/porto_volumes/1766/volume rw,nosuid,nodev,noatime shared:1483 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1245 31 9:4 /porto_volumes/1766/native /place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_k0cQdW330IO rw,nosuid,nodev,noatime shared:1540 master:1483 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1425 1167 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/data rw,nosuid,nodev,noatime shared:1565 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1440 1167 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/logs rw,nosuid,nodev,noatime shared:1592 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1465 1167 9:4 /porto_volumes/1766/native /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_k0cQdW330IO rw,nosuid,nodev,noatime shared:1629 master:1483 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
'''
            ),
        }
        return files[path](path, *args)

    def realpath_mock(path):
        overrides = {
            '/dev/disk/by-uuid/cc2a8f22-bed9-4f56-8fc1-c7f6b8968442': '/dev/md3',
            '/dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05': '/dev/md2',
            '/dev/disk/by-uuid/92894e32-f95b-4600-a660-6d502c443726': '/dev/md4',
            '/place/coredumps': '/place/coredumps',
            '/coredumps': '/place/coredumps',
            '/db': '/place/db',
            '/place/db': '/place/db',
            '/var/log/yandex': '/var/log/yandex',
            '/non/existing/path': '/non/existing/path'
        }
        return overrides[path]

    monkeypatch.setattr(os.path, 'realpath', realpath_mock)

    def exists_mock(path):
        overrides = {
            '/dev/md3', '/dev/md2', '/dev/md4', '/place/coredumps', '/place/db', '/var/log/yandex'
        }
        return path in overrides

    monkeypatch.setattr(os.path, 'exists', exists_mock)

    def stat_mock(path):
        overrides = {
            '/dev/md2': mock_rdev(2306),
            '/dev/md3': mock_rdev(2307),
            '/dev/md4': mock_rdev(2308),
            '/place/coredumps': mock_rdev(0),
            '/place/db': mock_rdev(0),
            '/var/log/yandex': mock_rdev(0),
        }
        if path not in overrides:
            return None
        return overrides[path]

    monkeypatch.setattr('os.stat', stat_mock)

    return mock_fn


@pytest.fixture
def real_files_mock_root_1(monkeypatch, patch_minor_major):
    def mock_fn(path, *args):
        files = {
            '/etc/fstab': mock.mock_open(
                read_data=
                '''
proc	/proc		proc	nodev,noexec,nosuid	0	0
# / was on /dev/md2 during installation
UUID=f660fef4-1a9d-4275-8c2c-e4612bf1fd05	/		ext4	rw,relatime	0	1
# /home was on /dev/md3 during installation
UUID=cc2a8f22-bed9-4f56-8fc1-c7f6b8968442		/home	ext4	barrier=1,noatime,lazytime,nosuid,nodev	0 2
# /place was on /dev/md4 during installation
UUID=92894e32-f95b-4600-a660-6d502c443726		/place	ext4	barrier=1,noatime,lazytime,nosuid,nodev	0 2
tmpfs		/place/db/bsconfig/webcache/shm	tmpfs	defaults,noexec,nosuid,huge=always,size=130G	0 0
/place/coredumps		/var/remote-log/coredumps	none	rw,bind,quota,data=ordered,usrquota,lazytime,stripe=128,noatime	0 0
/place/db		/var/remote-log/db	none	rw,bind,quota,data=ordered,usrquota,lazytime,stripe=128,noatime	0 0
/var/log/yandex		/var/remote-log/yandex	none	bind,rw,data=ordered,stripe=128,relatime	0 0
'''),
            '/proc/1/mountinfo': mock.mock_open(
                read_data=
                '''
18 23 0:18 / /sys rw,nosuid,nodev,noexec,relatime shared:9 - sysfs sysfs rw
19 23 0:4 / /proc rw,nosuid,nodev,noexec,relatime shared:25 - proc proc rw
20 23 0:6 / /dev rw,relatime shared:2 - devtmpfs udev rw,size=131932400k,nr_inodes=32983100,mode=755
21 20 0:19 / /dev/pts rw,nosuid,noexec,relatime shared:3 - devpts devpts rw,gid=5,mode=620,ptmxmode=000
22 23 0:20 / /run rw,nosuid,noexec,relatime shared:4 - tmpfs tmpfs rw,size=26388860k,mode=755
23 1 9:2 / / rw,relatime shared:1 - ext4 /dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05 rw,stripe=128,data=ordered
24 18 0:8 / /sys/kernel/debug rw,relatime shared:10 - debugfs none rw
25 18 0:7 / /sys/kernel/security rw,relatime shared:11 - securityfs none rw
26 22 0:21 / /run/lock rw,nosuid,nodev,noexec,relatime shared:5 - tmpfs none rw,size=5120k
27 22 0:22 / /run/shm rw,nosuid,nodev,relatime shared:6 - tmpfs none rw
28 23 9:2 /var/log/yandex /var/remote-log/yandex rw,relatime shared:26 - ext4 /dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05 rw,stripe=128,data=ordered
29 23 9:3 / /home rw,nosuid,nodev,noatime shared:27 - ext4 /dev/md3 rw,lazytime,stripe=128,data=ordered
31 23 9:4 / /place rw,noatime shared:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
32 31 0:24 / /place/db/bsconfig/webcache/shm rw,nosuid,noexec,relatime shared:29 - tmpfs tmpfs rw,size=136314880k,huge=always
33 23 9:4 /coredumps /var/remote-log/coredumps rw,nosuid,nodev,noatime shared:30 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
34 23 9:4 /db /var/remote-log/db rw,nosuid,nodev,noatime shared:31 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
35 18 0:25 / /sys/fs/pstore rw,relatime shared:12 - pstore pstore rw
36 18 0:26 / /sys/fs/cgroup rw,relatime shared:13 - tmpfs cgroup rw
37 36 0:27 / /sys/fs/cgroup/freezer rw,relatime shared:14 - cgroup cgroup rw,freezer
38 36 0:28 / /sys/fs/cgroup/memory rw,relatime shared:15 - cgroup cgroup rw,memory
39 36 0:29 / /sys/fs/cgroup/cpu rw,relatime shared:16 - cgroup cgroup rw,cpu
40 36 0:30 / /sys/fs/cgroup/cpuacct rw,relatime shared:17 - cgroup cgroup rw,cpuacct
41 36 0:31 / /sys/fs/cgroup/cpuset rw,relatime shared:18 - cgroup cgroup rw,cpuset
42 36 0:32 / /sys/fs/cgroup/net_cls rw,relatime shared:19 - cgroup cgroup rw,net_cls
43 36 0:33 / /sys/fs/cgroup/blkio rw,relatime shared:20 - cgroup cgroup rw,blkio
44 36 0:34 / /sys/fs/cgroup/devices rw,relatime shared:21 - cgroup cgroup rw,devices
45 36 0:35 / /sys/fs/cgroup/hugetlb rw,relatime shared:22 - cgroup cgroup rw,hugetlb
46 36 0:36 / /sys/fs/cgroup/pids rw,relatime shared:23 - cgroup cgroup rw,pids
47 36 0:37 / /sys/fs/cgroup/systemd rw,relatime shared:24 - cgroup cgroup rw,name=systemd
48 22 0:38 / /run/porto/kvs rw,nosuid,nodev,noexec,relatime shared:7 - tmpfs tmpfs rw,size=32768k,mode=750,gid=1333
49 22 0:39 / /run/porto/pkvs rw,nosuid,nodev,noexec,relatime shared:8 - tmpfs tmpfs rw,size=32768k,mode=750,gid=1333
50 18 0:11 / /sys/kernel/tracing rw,nosuid,nodev,noexec,relatime shared:32 - tracefs none rw,mode=755
274 31 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/porto_volumes/3/volume rw,nosuid,nodev,noatime shared:271 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
328 31 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/910d00f3ea22 rw,nosuid,nodev,noatime shared:281 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
318 31 9:4 /db/iss3/volumes/561e45a6068e-persist /place/porto_volumes/4/volume rw,nosuid,nodev,noatime shared:291 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
348 31 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/561e45a6068e rw,nosuid,nodev,noatime shared:301 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
381 31 0:69 / /place/vartmp/skynet/cqudp rw,relatime shared:472 - tmpfs tmpfs rw,size=131072k,mode=1755,uid=1044
610 31 0:70 / /place/porto_volumes/7/volume rw,nosuid,nodev,relatime shared:552 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/49,workdir=/proc/self/fd/50
588 31 0:70 / /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG rw,nosuid,nodev,relatime shared:532 master:552 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/49,workdir=/proc/self/fd/50
456 31 9:4 /db/iss3/volumes/1e3442011042-persist /place/porto_volumes/8/volume rw,nosuid,nodev,noatime shared:543 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
625 31 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/1e3442011042 rw,nosuid,nodev,noatime shared:565 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
599 31 9:4 /porto_volumes/9/native /place/porto_volumes/9/volume rw,nosuid,nodev,noatime shared:576 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
647 31 9:4 /porto_volumes/9/native /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH rw,nosuid,nodev,noatime shared:587 master:576 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
658 588 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG/logs rw,nosuid,nodev,noatime shared:598 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
669 588 9:4 /porto_volumes/9/native /place/db/iss3/volumes/ISS-AGENT_wyXDONd1GUG/place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH rw,nosuid,nodev,noatime shared:609 master:576 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
639 31 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/porto_volumes/10/volume rw,nosuid,nodev,noatime shared:659 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
694 647 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK rw,nosuid,nodev,noatime shared:670 master:659 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
683 31 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/porto_volumes/11/volume rw,nosuid,nodev,noatime shared:683 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
759 647 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO rw,nosuid,nodev,noatime shared:694 master:683 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
710 31 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/porto_volumes/12/volume rw,nosuid,nodev,noatime shared:707 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
783 647 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI rw,nosuid,nodev,noatime shared:718 master:707 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
844 31 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/porto_volumes/16/volume rw,nosuid,nodev,noatime shared:803 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
879 647 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B rw,nosuid,nodev,noatime shared:814 master:803 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
868 31 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/porto_volumes/17/volume rw,nosuid,nodev,noatime shared:827 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
903 647 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO rw,nosuid,nodev,noatime shared:838 master:827 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
892 31 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/porto_volumes/18/volume rw,nosuid,nodev,noatime shared:851 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
927 647 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU rw,nosuid,nodev,noatime shared:862 master:851 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
916 31 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/porto_volumes/19/volume rw,nosuid,nodev,noatime shared:875 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
951 647 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH rw,nosuid,nodev,noatime shared:886 master:875 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
940 31 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/porto_volumes/20/volume rw,nosuid,nodev,noatime shared:899 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
975 647 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH rw,nosuid,nodev,noatime shared:910 master:899 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
964 31 9:4 /db/iss3/resources/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT /place/porto_volumes/21/volume rw,nosuid,nodev,noatime shared:923 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
999 647 9:4 /db/iss3/resources/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/51e0f6f7b5cfa74a0f2029759782c7c34d5c606b_X7vAzLHoZmT rw,nosuid,nodev,noatime shared:934 master:923 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
100 31 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/porto_volumes/339/volume rw,nosuid,nodev,noatime shared:323 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
400 647 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B rw,nosuid,nodev,noatime shared:356 master:323 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
109 31 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/porto_volumes/350/volume rw,nosuid,nodev,noatime shared:1166 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
552 647 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C rw,nosuid,nodev,noatime shared:1180 master:1166 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
52 31 9:4 /db/iss3/resources/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD /place/porto_volumes/1009/volume rw,nosuid,nodev,noatime shared:324 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
374 647 9:4 /db/iss3/resources/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD /place/db/iss3/instances/17050_prod_report_int_vla_9fLIC7NlaWH/dynamic/be78f44c854cd4b987212f520f63a552563cfe86_q7uCbiOdGvD rw,nosuid,nodev,noatime shared:345 master:324 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1295 31 0:93 / /place/porto_volumes/1071/volume rw,nosuid,nodev,relatime shared:1325 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/58,workdir=/proc/self/fd/59
1267 31 0:93 / /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD rw,nosuid,nodev,relatime shared:1299 master:1325 - overlay overlay rw,lowerdir=L2:L1:L0,upperdir=/proc/self/fd/58,workdir=/proc/self/fd/59
773 31 9:4 /porto_volumes/1072/native /place/porto_volumes/1072/volume rw,nosuid,nodev,noatime shared:1313 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1313 31 9:4 /porto_volumes/1072/native /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT rw,nosuid,nodev,noatime shared:1341 master:1313 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1327 1267 9:4 /db/iss3/volumes/1e3442011042-persist /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD/logs rw,nosuid,nodev,noatime shared:1355 master:543 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1452 1267 9:4 /porto_volumes/1072/native /place/db/iss3/volumes/ISS-AGENT_OC86tE6JfpD/place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT rw,nosuid,nodev,noatime shared:1369 master:1313 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1284 31 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/porto_volumes/1073/volume rw,nosuid,nodev,noatime shared:1422 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1483 1313 9:4 /db/iss3/resources/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/85b094e5d312c4e360d5d7936aa902c246064689_nX76O6oRxhK rw,nosuid,nodev,noatime shared:1436 master:1422 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1469 31 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/porto_volumes/1074/volume rw,nosuid,nodev,noatime shared:1452 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1513 1313 9:4 /db/iss3/resources/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e45d463ddf704e38f8094ed519b03d8960734e0d_Cy61UuUNRNO rw,nosuid,nodev,noatime shared:1466 master:1452 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1499 31 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/porto_volumes/1075/volume rw,nosuid,nodev,noatime shared:1482 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1584 1313 9:4 /db/iss3/resources/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/ee3a601edd05c3ae6f321f480d2ec93f7dd805b4_HHrVET7jhUI rw,nosuid,nodev,noatime shared:1496 master:1482 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1532 31 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/porto_volumes/1076/volume rw,nosuid,nodev,noatime shared:1512 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1614 1313 9:4 /db/iss3/resources/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/379882a14ea47086b3e9727c29bd16e12ef168b5_vz9prIdIJ6C rw,nosuid,nodev,noatime shared:1526 master:1512 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1600 31 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/porto_volumes/1077/volume rw,nosuid,nodev,noatime shared:1542 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1644 1313 9:4 /db/iss3/resources/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/b6d74fe3067227142e659a4eb6042b1b085711f5_sfZuVy8Ki1B rw,nosuid,nodev,noatime shared:1556 master:1542 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1660 31 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/porto_volumes/1079/volume rw,nosuid,nodev,noatime shared:1602 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1704 1313 9:4 /db/iss3/resources/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/dcef1b89c10cf32a6c5c0a717c0b08c524b66ef7_V3VyzVBcT4B rw,nosuid,nodev,noatime shared:1616 master:1602 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1690 31 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/porto_volumes/1080/volume rw,nosuid,nodev,noatime shared:1632 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1734 1313 9:4 /db/iss3/resources/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/45c667f613b4c7e13f9ee56c64267e95e610843f_M4pHpWDm0hO rw,nosuid,nodev,noatime shared:1646 master:1632 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1720 31 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/porto_volumes/1081/volume rw,nosuid,nodev,noatime shared:1662 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1764 1313 9:4 /db/iss3/resources/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/314d571616066c54a0e65beb1c137eac5d37c86e_1vkb1DncvxU rw,nosuid,nodev,noatime shared:1676 master:1662 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1750 31 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/porto_volumes/1082/volume rw,nosuid,nodev,noatime shared:1692 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1794 1313 9:4 /db/iss3/resources/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/67646567e5e09e57037204646f6a62c8b35bc698_BKinflxEVQH rw,nosuid,nodev,noatime shared:1706 master:1692 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1780 31 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/porto_volumes/1083/volume rw,nosuid,nodev,noatime shared:1722 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1824 1313 9:4 /db/iss3/resources/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e14ada8d1c5d065f99b70a76f9c9063640e292c9_JonJBMfxBoH rw,nosuid,nodev,noatime shared:1736 master:1722 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
92 31 9:4 /db/iss3/resources/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK /place/porto_volumes/1281/volume rw,nosuid,nodev,noatime shared:530 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
817 1313 9:4 /db/iss3/resources/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/0c2ebaf99e3ad60a74dde083420b6c97011cb923_iIQbP85dLwK rw,nosuid,nodev,noatime shared:667 master:530 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1658 31 9:4 /db/iss3/resources/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL /place/porto_volumes/1294/volume rw,nosuid,nodev,noatime shared:1915 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1986 1313 9:4 /db/iss3/resources/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL /place/db/iss3/instances/17050_prod_report_int_vla_Dai7t5QSZaT/dynamic/e2114e8827ccdba5f2597b842017240e39877e66_SybvCabmRpL rw,nosuid,nodev,noatime shared:1929 master:1915 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
544 31 0:43 / /place/porto_volumes/1763/volume rw,nosuid,nodev,relatime shared:466 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/52,workdir=/proc/self/fd/55
878 31 0:43 / /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C rw,nosuid,nodev,relatime shared:748 master:466 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/52,workdir=/proc/self/fd/55
156 31 9:4 /porto_volumes/1764/native /place/porto_volumes/1764/volume rw,nosuid,nodev,noatime shared:817 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1249 31 9:4 /porto_volumes/1764/native /place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_cqXBZdG5GMN rw,nosuid,nodev,noatime shared:964 master:817 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1263 878 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/data rw,nosuid,nodev,noatime shared:1065 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1315 878 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/logs rw,nosuid,nodev,noatime shared:1155 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1361 878 9:4 /porto_volumes/1764/native /place/db/iss3/volumes/ISS-AGENT_cHacI8syM7C/place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_cqXBZdG5GMN rw,nosuid,nodev,noatime shared:1182 master:817 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
414 31 0:54 / /place/porto_volumes/1765/volume rw,nosuid,nodev,relatime shared:1344 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/55,workdir=/proc/self/fd/58
1167 31 0:54 / /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH rw,nosuid,nodev,relatime shared:1424 master:1344 - overlay overlay rw,lowerdir=L0,upperdir=/proc/self/fd/55,workdir=/proc/self/fd/58
94 31 9:4 /porto_volumes/1766/native /place/porto_volumes/1766/volume rw,nosuid,nodev,noatime shared:1483 master:28 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1245 31 9:4 /porto_volumes/1766/native /place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_k0cQdW330IO rw,nosuid,nodev,noatime shared:1540 master:1483 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1425 1167 9:4 /db/iss3/volumes/910d00f3ea22-persist /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/data rw,nosuid,nodev,noatime shared:1565 master:271 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1440 1167 9:4 /db/iss3/volumes/561e45a6068e-persist /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/logs rw,nosuid,nodev,noatime shared:1592 master:291 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
1465 1167 9:4 /porto_volumes/1766/native /place/db/iss3/volumes/ISS-AGENT_JoS83lfTvMH/place/db/iss3/instances/10575_rtc_sla_tentacles_production_vla_k0cQdW330IO rw,nosuid,nodev,noatime shared:1629 master:1483 - ext4 /dev/md4 rw,lazytime,quota,usrquota,stripe=128,data=ordered
'''
            ),
        }
        return files[path](path, *args)

    def realpath_mock(path):
        overrides = {
            '/dev/disk/by-uuid/cc2a8f22-bed9-4f56-8fc1-c7f6b8968442': '/dev/md3',
            '/dev/disk/by-uuid/f660fef4-1a9d-4275-8c2c-e4612bf1fd05': '/dev/md2',
            '/dev/disk/by-uuid/92894e32-f95b-4600-a660-6d502c443726': '/dev/md4',
            '/place/coredumps': '/place/coredumps',
            '/coredumps': '/place/coredumps',
            '/db': '/place/db',
            '/place/db': '/place/db',
            '/var/log/yandex': '/var/log/yandex',
            '/non/existing/path': '/non/existing/path'
        }
        return overrides[path]
    monkeypatch.setattr(os.path, 'realpath', realpath_mock)

    def exists_mock(path):
        overrides = {
            '/dev/md3', '/dev/md2', '/dev/md4', '/place/coredumps', '/place/db', '/var/log/yandex'
        }
        return path in overrides
    monkeypatch.setattr(os.path, 'exists', exists_mock)

    def stat_mock(path):
        overrides = {
            '/dev/md2': mock_rdev(2306),
            '/dev/md3': mock_rdev(2307),
            '/dev/md4': mock_rdev(2308),
            '/place/coredumps': mock_rdev(0),
            '/place/db': mock_rdev(0),
            '/var/log/yandex': mock_rdev(0),
        }
        if path not in overrides:
            return None
        return overrides[path]
    monkeypatch.setattr('os.stat', stat_mock)

    return mock_fn


def test_create_fstab_entry(real_files_mock):
    test_line = "UUID=92894e32-f95b-4600-a660-6d502c443726		" \
                "/place	ext4	barrier=1,noatime,lazytime,nosuid,nodev	0 2"
    mounts = walle_fstab.MountTree(open_func=real_files_mock)
    result = walle_fstab.create_fstab_entry(test_line, mounts)
    assert result.mnt == "/place"
    assert result.opts == set("barrier=1,noatime,lazytime,nosuid,nodev".split(','))
    assert result.fstype == "ext4"
    assert result.dev == "/dev/md4"
    assert result.dev_exists()


def test_read_fstab(real_files_mock):
    mounts = walle_fstab.MountTree(open_func=real_files_mock)
    result = walle_fstab.read_fstab(mounts, open_func=real_files_mock)
    assert len(result) == 8
    assert result[2].dev == "/dev/md3"


def test_check_and_format_description_ok():
    status, message = walle_fstab.check_and_format_description([], [])
    assert status == walle_fstab.Status.OK
    assert message == "Ok"


def test_check_and_format_description_not_mounted(real_files_mock):
    mounts = walle_fstab.MountTree(open_func=real_files_mock)
    fstab = walle_fstab.create_fstab_entry("UUID=92894e32-f95b-4600-a660-6d502c443726               "
                                           "/place  ext4    barrier=1,noatime,lazytime,nosuid,nodev 0 2", mounts)

    expected_message = "FSTAB:UUID(/dev/md4 /place ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) is not mounted"
    expected_message = ', '.join([expected_message, expected_message])

    status, message = walle_fstab.check_and_format_description([fstab, fstab], [])

    assert status == walle_fstab.Status.CRIT
    assert message == expected_message


def test_check_and_format_description_ro_mounted(real_files_mock):
    mounts = walle_fstab.MountTree(open_func=real_files_mock)
    fstab = walle_fstab.create_fstab_entry("UUID=92894e32-f95b-4600-a660-6d502c443726               "
                                           "/place  ext4    barrier=1,noatime,lazytime,nosuid,nodev 0 2", mounts)

    expected_message = "FSTAB:UUID(/dev/md4 /place ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) has inconsistent flags"
    expected_message = ', '.join([expected_message, expected_message])

    status, message = walle_fstab.check_and_format_description([], [fstab, fstab])

    assert status == walle_fstab.Status.CRIT
    assert message == expected_message


def test_check_and_format_description_ro_and_not_mounted(real_files_mock):
    mounts = walle_fstab.MountTree(open_func=real_files_mock)
    fstab = walle_fstab.create_fstab_entry("UUID=92894e32-f95b-4600-a660-6d502c443726               "
                                           "/place  ext4    barrier=1,noatime,lazytime,nosuid,nodev 0 2", mounts)
    expected_message = 'FSTAB:UUID(/dev/md4 /place ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) is not mounted, ' \
                       'FSTAB:UUID(/dev/md4 /place ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) has inconsistent flags'

    status, message = walle_fstab.check_and_format_description([fstab], [fstab])

    assert status == walle_fstab.Status.CRIT
    assert message == expected_message


def test_run_check_ok(real_files_mock):
    status, result = walle_fstab.run_check(real_files_mock)
    assert status == walle_fstab.Status.OK
    assert result == "Ok"


def test_run_check_ok_root_1(real_files_mock_root_1):
    status, result = walle_fstab.run_check(real_files_mock_root_1)
    assert status == walle_fstab.Status.OK
    assert result == "Ok"


def test_run_check_crit(real_files_mock_faulty_mounts):
    status, result = walle_fstab.run_check(real_files_mock_faulty_mounts)
    assert status == walle_fstab.Status.CRIT
    assert result == "FSTAB:UUID(/dev/md3 /home ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) is not mounted, " \
                     "FSTAB:UUID(/dev/md4 /place ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) has inconsistent flags"


def test_check_ok(real_files_mock):
    result = walle_fstab.juggler_check(real_files_mock)
    expected_metadata = {
        "timestamp": walle_fstab.timestamp(),
        "reason": "Ok"
    }
    assert result.status == Status.OK
    assert result.description == json.dumps(expected_metadata)


def test_check_crit(real_files_mock_faulty_mounts):
    result = walle_fstab.juggler_check(real_files_mock_faulty_mounts)
    expected_metadata = {
        "timestamp": walle_fstab.timestamp(),
        "reason": "FSTAB:UUID(/dev/md3 /home ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) is not mounted, "
                  "FSTAB:UUID(/dev/md4 /place ext4 lazytime,barrier=1,nosuid,noatime,nodev 0 2) has inconsistent flags"
    }
    assert result.status == Status.CRIT
    assert result.description == json.dumps(expected_metadata)


def create_mock_mounts(line, loop_backing_file=None):
    mock_fstab_mount_info = walle_fstab.MountInfo(line)
    mock_fstab_mount_info.get_loop_backing_file = lambda: loop_backing_file
    mock_mounts = mock.Mock()
    mock_mounts.find_mount_info_for_path.return_value = mock_fstab_mount_info
    return mock_mounts


class TestFSTabEntry(object):
    def test_dev_exists_is_true(self, real_files_mock):
        test_line = "UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype opts dump pass"
        result = walle_fstab.create_fstab_entry(test_line, mounts=None)
        assert result.dev_exists()

    def test_dev_exists_is_false(self, real_files_mock):
        test_line = "/non/existing/path mnt fstype opts dump pass"
        result = walle_fstab.create_fstab_entry(test_line, mounts=None)
        assert not result.dev_exists()

    def test_get_min_maj(self, real_files_mock):
        test_line = "UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype opts dump pass"
        result = walle_fstab.create_fstab_entry(test_line, mounts=None)
        assert result.get_maj_min() == (9, 4)

    def test_is_mounted_dev_exists_non_zero_maj_min_is_true(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 9:4 /non/existing/path mnt opt"))
        assert mock_fstab.is_mounted()

    def test_is_mounted_dev_exists_non_zero_maj_min_is_false_by_mountpoint(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 9:4 /non/existing/path another_mnt opt"))
        assert not mock_fstab.is_mounted()

    def test_is_mounted_dev_exists_non_zero_maj_min_is_false_by_min_maj(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 1:1 /non/existing/path mnt opt"))
        assert not mock_fstab.is_mounted()

    def test_is_mounted_dev_exists_zero_maj_min_is_false(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="/place/coredumps mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 maj_min /non/existing/path mnt opt"))
        assert not mock_fstab.is_mounted()

    def test_is_mounted_dev_exists_zero_maj_min_is_false_by_mnt(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="/place/coredumps mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 maj_min /non/existing/path another_mnt opt"))
        assert not mock_fstab.is_mounted()

    def test_is_mounted_dev_exists_zero_maj_min_is_true_by_backing_file(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="/place/coredumps mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 maj_min /non/existing/path mnt opt",
                                                                      loop_backing_file="/place/coredumps"))
        assert mock_fstab.is_mounted()

    def test_is_mounted_dev_exists_zero_maj_min_is_true_by_path(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="/place/coredumps mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 maj_min /place/coredumps mnt opt"))
        assert mock_fstab.is_mounted()

    def test_is_mounted_not_dev_exists_is_true(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="/non/existing/path mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 maj_min /non/existing/path mnt opt"))
        assert mock_fstab.is_mounted()

    def test_is_mounted_not_dev_exists_is_false(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="/non/existing/path mnt fstype opts dump pass",
                                            mounts=create_mock_mounts("20 10 maj_min /non/existing/path another_mnt opt"))
        assert not mock_fstab.is_mounted()

    def test_is_equal_rw_flag_rw_is_true(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype rw dump pass",
                                            mounts=create_mock_mounts("20 10 min_maj /non/existing/path mnt rw"))
        assert mock_fstab.is_equal_rw_flag()

    def test_is_equal_rw_flag_ro_is_true(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype ro dump pass",
                                            mounts=create_mock_mounts("20 10 min_maj /non/existing/path mnt ro"))
        assert mock_fstab.is_equal_rw_flag()

    def test_is_equal_rw_flag_is_false(self, real_files_mock):
        mock_fstab = walle_fstab.FSTabEntry(line="UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype ro dump pass",
                                            mounts=create_mock_mounts("20 10 min_maj /non/existing/path mnt rw"))
        assert not mock_fstab.is_equal_rw_flag()

    def test_should_be_mounted_to_true(self, real_files_mock):
        test_line = "UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype opts dump pass"
        result = walle_fstab.create_fstab_entry(test_line, mounts=None)
        assert result.shoud_be_mounted()

    def test_should_be_mounted_is_false(self, real_files_mock):
        test_line = "UUID=92894e32-f95b-4600-a660-6d502c443726 mnt fstype noauto dump pass"
        result = walle_fstab.create_fstab_entry(test_line, mounts=None)
        assert not result.shoud_be_mounted()


def create_parent_and_child():
    parent = walle_fstab.MountNode("parent_node")
    child = walle_fstab.MountNode("child_node")
    parent.add_child(child)
    return parent, child


class TestMountNode(object):
    def test_add_child(self):
        parent, child = create_parent_and_child()
        assert parent.children == {"child_node": child}
        assert child.parent == parent

    def test_get_child(self):
        parent, child = create_parent_and_child()
        assert parent.get_child("child_node") == child

    def test_has_child_is_true(self):
        parent, child = create_parent_and_child()
        assert parent.has_child("child_node")

    def test_has_child_is_false(self):
        node = walle_fstab.MountNode("node")
        assert not node.has_child("non_child_node")


class TestMountTree(object):
    def test_find_mount_info_for_path(self, real_files_mock):
        tree = walle_fstab.MountTree(open_func=real_files_mock)
        result = tree.find_mount_info_for_path("/sys/kernel/debug")
        assert str(result) == "MountInfo: 24 18 0:8 / /sys/kernel/debug rw,relatime"

    def test_find_mount_info_for_path_endless_loop_fixed(self):
        """
        Check that we won't get to endless loop
        """
        path = '/faulty_mock/faulty_child'
        root = walle_fstab.MountNode('', info=mock.Mock(return_value='mocked_root'))
        faulty_node = walle_fstab.MountNode('faulty_mock')
        root.add_child(faulty_node)
        faulty_node.parent = None
        faulty_child = walle_fstab.MountNode('faulty_child')
        faulty_node.add_child(faulty_child)

        class MockedTree(walle_fstab.MountTree):
            def __init__(self):
                self.root = root

        mocked_tree = MockedTree()
        info = mocked_tree.find_mount_info_for_path(path)
        assert info() == 'mocked_root'
