import os
import grp
import pwd
import sys
import random

from infra.skylib import porto as portotools


def make_name():
    return 'ut-%s-%s' % (os.getpid(), random.randint(0, sys.maxsize))


def test_memory_limited(portoconn):
    for parts in range(1, 4):
        for limited in range(parts + 1):
            path_parts = [make_name() for _ in range(parts)]
            for n in range(1, parts + 1):
                c = portoconn.CreateWeakContainer('/'.join(path_parts[:n]))
                c.SetProperty('memory_limit', (1 << 30) if n == limited else 0)

            assert portotools.is_memory_limited(portoconn, portoconn.Find('/'.join(path_parts))) == (0 != limited)


def test_parent_container(portoconn):
    for parts in range(1, 4):
        path_parts = [make_name() for _ in range(parts)]
        for n in range(1, parts + 1):
            c = portoconn.CreateWeakContainer('/'.join(path_parts[:n]))

            assert portotools.get_parent_container(portoconn, c.name).name == (
                '/'.join(path_parts[:n - 1]) if n > 1 else '/'
            )


def test_container_user(portoconn):
    pw = pwd.getpwuid(os.getuid())
    user = pw.pw_name
    group = grp.getgrgid(pw.pw_gid).gr_name

    daemon_group = grp.getgrgid(pwd.getpwnam('daemon').pw_gid).gr_name

    name = make_name()
    c = portoconn.CreateWeakContainer(name)
    assert portotools.get_container_user_and_group(portoconn, c, None) == (user, group)
    assert portotools.get_container_user_and_group(portoconn, c, None) == portotools.get_container_user_and_group(portoconn, c.name, None)
    assert portotools.get_container_user_and_group(portoconn, c, 'daemon') == ('daemon', daemon_group)
    assert portotools.get_container_user_and_group(portoconn, c.name, 'daemon') == ('daemon', daemon_group)

    name = make_name()
    c = portoconn.CreateWeakContainer(name)
    c.SetProperty('root', '/tmp')
    assert portotools.get_container_user_and_group(portoconn, c, None) == ('root', 'root')
    assert portotools.get_container_user_and_group(portoconn, c, None) == portotools.get_container_user_and_group(portoconn, c.name, None)
    assert portotools.get_container_user_and_group(portoconn, c, 'daemon') == ('daemon', daemon_group)
    assert portotools.get_container_user_and_group(portoconn, c.name, 'daemon') == ('daemon', daemon_group)
