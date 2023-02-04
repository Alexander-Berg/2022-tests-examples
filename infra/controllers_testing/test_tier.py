import pytest

import infra.callisto.controllers.sdk.tier as tiers
import infra.callisto.controllers.utils.funcs as funcs


def test_yt_state_and_timestamp():
    for x in range(1548801657, 1548801657 + 10):
        assert funcs.yt_state_to_timestamp(funcs.timestamp_to_yt_state(x)) == x


def test_yt_state_and_timestamp_flaps():
    # fix current behavior to prevent disasters
    assert funcs.timestamp_to_yt_state(1548801657) == '20190130-014057'
    assert funcs.yt_state_to_timestamp('20190130-014057') == 1548801657


def test_parse_by_template():
    params = funcs.parse_by_template('abc-xxx-123-yyy-tail', 'abc-{item1}-123-{item2}-tail', '-')
    assert params == dict(item1='xxx', item2='yyy')


def test_fullname():
    assert tiers.WebFreshTier.make_shard((2, 3), 123).fullname == 'WebFreshTier-2-3-0000000123'
    assert tiers.MsUserData.make_shard((0, 0), 123).fullname == 'rearr-jupiter-msuserdata-000-0000000123'
    assert tiers.ImgTier0.make_shard((0, 0), 123).fullname == 'imgsidx-000-19700101-030203'

    for tier in (tiers.PlatinumTier0, tiers.WebTier1):
        for i in range(tier.groups_count):
            for j in range(tier.shards_in_group):
                shard = tier.make_shard((i, j), 12345678)
                assert shard.fullname == 'primus-{}-{}-{}-0012345678'.format(tier.name, i, j)
                assert tier.parse_shard(shard.fullname).fullname == shard.fullname


def test_tiers():
    for tier in tiers.TIERS.values():
        assert tier.groups_count > 0
        assert tier.shards_in_group > 0


def test_parse_fullname():
    for tier in tiers.TIERS.values():
        shard = tier.make_shard((0, 0), 123)
        parsed = tiers.parse_shard(shard.fullname)
        assert shard.fullname == parsed.fullname
        assert shard == parsed


def test_not_existing_shard():
    with pytest.raises(ValueError):
        tiers.parse_shard('123_xyz_foo__|__bar_baz')
