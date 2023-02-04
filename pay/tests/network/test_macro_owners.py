def test_macro_owners(deploy_unit, macros_owners, network_macro_valid_abc_roles):
    """At least one of valid_abc_roles must exists in macro owners"""

    name, du = deploy_unit
    macro = du.get('network_defaults').get('network_id')
    owners = [x.get('name') for x in macros_owners.get(macro).get('owners')]

    found = []
    for owner in owners:
        if owner in network_macro_valid_abc_roles:
            found.append(owner)

    assert len(found) > 0
