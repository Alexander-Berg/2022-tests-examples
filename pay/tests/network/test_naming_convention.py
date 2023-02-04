import warnings


def test_naming_convention(deploy_units):
    """Network id should match naming convention"""

    wrong_network_units = []

    for k, v in deploy_units.items():

        wrong_network_units.append((k, v))

    assert wrong_network_units == []


def test_network_environment():
    pass


def test_network_acl():
    pass

