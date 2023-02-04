import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import BadRequestError


def test_params_validation(coordinator):
    def check_exists(dataset, version, tag=None):
        coordinator.check_exists(dataset, version, tag, existency=False)

    check_exists('good-dataset-1.0', '1')
    check_exists('underscores_are_0k', '1')
    check_exists('1st-digit-also', '1')
    with pytest.raises(BadRequestError):
        check_exists('Uppercase-is-not', '1')
    with pytest.raises(BadRequestError):
        check_exists('no%00zeroes', '1')
    with pytest.raises(BadRequestError):
        check_exists('-first-dash', '1')

    coordinator.list_versions('good-dataset-1.0')
    coordinator.list_versions('underscores_are_0k')
    coordinator.list_versions('1st-digit-also')
    with pytest.raises(BadRequestError):
        coordinator.list_versions('Uppercase-is-not')
    with pytest.raises(BadRequestError):
        coordinator.list_versions('no%00zeroes')
    with pytest.raises(BadRequestError):
        coordinator.list_versions('-first-dash')

    coordinator.list_versions('a', 'good-branch-1.0')
    coordinator.list_versions('a', 'underscores_are_0k')
    coordinator.list_versions('a', '1st-digit-also')
    with pytest.raises(BadRequestError):
        coordinator.list_versions('a', 'Uppercase-is-not')
    with pytest.raises(BadRequestError):
        coordinator.list_versions('a', 'no%00zeroes')
    with pytest.raises(BadRequestError):
        coordinator.list_versions('a', '-first-dash')

    check_exists('a', '1.0-3~debug')
    check_exists('a', '0.0-UPPER')
    check_exists('a', 'we.accept.first.alpha')

    with pytest.raises(BadRequestError):
        check_exists('a', '~but-not-tilde')
    with pytest.raises(BadRequestError):
        check_exists('a', '1_no_underscores')
    with pytest.raises(BadRequestError):
        check_exists('a', 'no%00zeroes')
    with pytest.raises(BadRequestError):
        check_exists('a', 'no:colons')

    with pytest.raises(BadRequestError):
        check_exists('a', '1', '-123')

    check_exists('a', '1', ':good')
    check_exists('a', '1', ':1.0-5~Can-use-version')

    with pytest.raises(BadRequestError):
        check_exists('a', '1', 'should_start_with_lead')
    with pytest.raises(BadRequestError):
        check_exists('a', '1', ':.but_not_punctuation')
    with pytest.raises(BadRequestError):
        check_exists('a', '1', ':no%00zeroes')
    with pytest.raises(BadRequestError):
        check_exists('a', '1', ':no:colons')

    check_exists('same-as-above', '1')
    check_exists('a-123', '1')
    check_exists('a:good', '1')
    check_exists('a:1.0-5~Can-use-version', '1')
    with pytest.raises(BadRequestError):
        check_exists('a:.but_not_punctuation', '1')
    with pytest.raises(BadRequestError):
        check_exists('a:no%00zeroes', '1')
    with pytest.raises(BadRequestError):
        check_exists('a:no:colons', '1')

    def check_announce(info_hash):
        coordinator.http_get(
            '/tracker/announce?info_hash=' + info_hash + '&peer_id=A&port=0&left=0')

    check_announce('00000000000000000000')
    check_announce('%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00')
    check_announce('actually,any_20chars')
    check_announce('1234567890abcdef1234567890ABCDEF12345678')
    with pytest.raises(BadRequestError):
        check_announce('not_less')
    with pytest.raises(BadRequestError):
        check_announce('and_not_more,unless_its_40_hexes')
    with pytest.raises(BadRequestError):
        check_announce('------40_non-hexes_are_not_accepted-----')

    with pytest.raises(BadRequestError):
        coordinator.upload('pkg-a:1', '0-no_underscores', 'gen-ab1')
