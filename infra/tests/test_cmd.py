"""
Trivial, mostly import aka smoke tests.
"""
import argparse

from infra.ya_salt import cmd


def test_all_commands_present():
    assert cmd.disable
    assert cmd.enable
    assert cmd.grains
    assert cmd.run
    assert cmd.status
    assert cmd.override
    assert cmd.render


def test_add_command():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(help='List command for ya-salt')
    for mod in cmd.all_commands:
        mod.add_command(sub)
