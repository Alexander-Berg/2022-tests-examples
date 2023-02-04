import pytest

from hamcrest import assert_that, has_properties, is_

from pay.lib.tvm.acl_matcher import TvmAclMatcher


class TestTvmAclMatcher:
    @pytest.fixture
    def route_name(self):
        return 'the-route'

    @pytest.fixture
    def route_acls(self, route_name):
        return {route_name: ['acl-1', 'acl-2']}

    @pytest.mark.parametrize(
        'source,expected_tvm_id,expected_acls',
        [
            (100, 100, {'common'}),
            ((100, 'acl'), 100, {'acl'}),
            ((100, ['acl1', 'acl1']), 100, {'acl1'}),
            ((100, ['acl1', 'acl2']), 100, {'acl1', 'acl2'}),
        ],
    )
    def test_init(self, route_acls, source, expected_tvm_id, expected_acls):
        matcher = TvmAclMatcher(route_acls, source)

        assert_that(
            matcher,
            has_properties(tvm_id=expected_tvm_id, acls=expected_acls),
        )

    @pytest.mark.parametrize('source', [100, (100, 'common'), (100, ['common', 'custom'])])
    def test_default_acl_matched(self, route_acls, route_name, source):
        route_acls[route_name].append(TvmAclMatcher.DEFAULT_ACL)

        matcher = TvmAclMatcher(route_acls, source)

        assert_that(matcher.match(100, route_name), is_(True))

    @pytest.mark.parametrize(
        'source',
        [
            (100, 'acl-1'),
            (100, ['acl-1']),
            (100, ['acl-1', 'acl-3']),
        ],
    )
    def test_custom_acl_matched(self, route_acls, route_name, source):
        matcher = TvmAclMatcher(route_acls, source)

        assert_that(matcher.match(100, route_name), is_(True))

    @pytest.mark.parametrize(
        'source',
        [
            100,
            (100, []),
            (100, 'common'),
            (100, ['common', 'custom']),
        ],
    )
    def test_acl_not_matched(self, route_acls, route_name, source):
        matcher = TvmAclMatcher(route_acls, source)

        assert_that(matcher.match(100, route_name), is_(False))
