# coding: utf-8


def test_reboot_via_ssh_is_enabled(project):
    assert project.reboot_via_ssh, "Reboot via SSH must be enabled in project {}".format(project.id)
