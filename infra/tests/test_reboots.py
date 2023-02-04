import mock

from infra.ya_salt.lib import reboots


def test_group_constraint():
    assert reboots.GroupConstraint(['NANNY']).can_reboot() is None
    assert reboots.GroupConstraint([]).can_reboot() is None
    assert reboots.GroupConstraint(['MAN_ISS3_PRODUCTION', 'NANNY']).can_reboot()


def test_static_constraint():
    assert reboots.StaticConstraint(None).can_reboot() is None
    assert reboots.StaticConstraint('Some error').can_reboot() == 'Some error'


def test_walle_constraint():
    hostname = 'm01-sas.pythia.yt.yandex.net'
    walle_project = 'rtc-yt-pythia-masters'
    wc = reboots.WalleConstraint
    # Test error on get_host
    w = mock.Mock()
    w.get_host.side_effect = Exception("No route to me!")
    assert wc(w, hostname, walle_project).can_reboot()
    w.get_host.assert_called_once_with(hostname, fields=('restrictions',))
    # Test has restriction on host
    w = mock.Mock()
    w.get_host.return_value = {'restrictions': ['some']}
    assert wc(w, hostname, walle_project).can_reboot() == wc.HOST_DISABLED_MSG
    # Test error on get_project
    w = mock.Mock()
    w.get_host.return_value = {}
    w.get_project.side_effect = Exception("No route to me too!")
    assert wc(w, hostname, walle_project).can_reboot()
    w.get_project.assert_called_once_with(walle_project, fields=('healing_automation',))
    # Test automation healing disabled
    w = mock.Mock()
    w.get_host.return_value = {}
    w.get_project.return_value = {'healing_automation': {'enabled': False}}
    assert wc(w, hostname, walle_project).can_reboot() == wc.PROJECT_DISABLED_MSG
    # Test testing projects
    assert wc(None, hostname, wc.TESTING_MANAGED_PROJECTS[0]).can_reboot() is None
    # Test all ok
    w = mock.Mock()
    w.get_host.return_value = {}
    w.get_project.return_value = {'healing_automation': {'enabled': True}}
    assert wc(w, hostname, walle_project).can_reboot() is None


def test_project_constraint():
    pc = reboots.ProjectConstraint
    assert pc('rtc', []).can_reboot() is None
    assert pc('yp-iss-sas-dev', []).can_reboot() == pc.PRJ_ERR
    assert pc('rtc-yt-over-kikimr', ['special_reboot']).can_reboot() == pc.TAG_ERR


def test_make_orly_rule():
    test_table = [
        # (prefix, env_type), output
        (('hostman-kernel', ''), 'hostman-kernel-production'),
        (('hostman-kernel', 'production'), 'hostman-kernel-production'),
        (('hostman-initial', 'prestable'), 'hostman-initial-prestable'),
    ]
    for args, output in test_table:
        assert reboots.make_orly_rule(*args) == output


def test_orly_constraint():
    oc = reboots.OrlyConstraint
    # No orly
    assert oc(None, 'hostman-kernel-production').can_reboot() is None
    # Orly error
    m = mock.Mock()
    m.start_operation.return_value = 'No reboots for you'
    assert oc(m, 'hostman-kernel-prestable').can_reboot() is not None
    m.start_operation.assert_called_once_with('hostman-kernel-prestable')
    # All ok
    m.reset_mock()
    m.start_operation.return_value = None
    assert oc(m, 'hostman-initial-prestable').can_reboot() is None


def test_chained_constraint():
    cc = reboots.ChainedConstraint
    assert cc([]).can_reboot() is None
    assert cc([
        reboots.StaticConstraint(None),
        reboots.StaticConstraint(None)]).can_reboot() is None
    assert cc([
        reboots.StaticConstraint(None),
        reboots.StaticConstraint('YES')]).can_reboot() == 'YES'
