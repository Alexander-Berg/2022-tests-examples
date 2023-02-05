from maps.poi.streetview_poi.sign_hypotheses.lib.filters import (
    _get_banned_oids_by_orgs_count_from_rows
    )


bld_orgs_rows = [
    {
        'organizations': [
            {'oid': 11}, {'oid': 12}
        ]
    },
    {
        'organizations': [
            {'oid': 21}, {'oid': 22}, {'oid': 23}
        ]
    }
]


def test_ban_oids_by_orgs_count_in_bld():
    malls_oids = _get_banned_oids_by_orgs_count_from_rows(
        bld_orgs_rows, max_orgs_in_bld=2
        )
    assert len(malls_oids) == 3
    assert 21 in malls_oids and 22 in malls_oids and 23 in malls_oids
    assert 12 not in malls_oids and 11 not in malls_oids
