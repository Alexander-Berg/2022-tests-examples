from mock import patch

from plan.common.utils.ok import OkClient
from common import factories
from utils import vcr_test


def test_create_success():
    client = OkClient()
    oebs_agreement = factories.OEBSAgreementFactory(issue='ABC-10159')

    cassette_name = 'ok/test_create_success.json'

    with vcr_test().use_cassette(cassette_name):
        client.create_request(oebs_agreement, [('smosker', ), ('alimpiev', )])

    oebs_agreement.refresh_from_db()
    assert oebs_agreement.ok_id == '9b8ec907-f604-4302-852d-7105766e4a1e'


def test_post_comment_success():
    client = OkClient()
    oebs_agreement = factories.OEBSAgreementFactory(
        issue='ABC-10159',
        ok_id='9b8ec907-f604-4302-852d-7105766e4a1e'
    )

    with patch('plan.common.utils.ok.create_comment') as create_comment:
        client.add_request_to_ticket(oebs_agreement)
    create_comment.assert_called_once_with(
        'ABC-10159',
        ('{{iframe src="https://ok.yandex-team.ru/approvements/9b8ec907-f604-4302-852d-7105766e4a1e'
         '?_embedded=1" frameborder=0 width=100% height=400px scrolling=no}}')
    )


def test_check_status_success():
    client = OkClient()
    oebs_agreement = factories.OEBSAgreementFactory(
        ok_id='9b8ec907-f604-4302-852d-7105766e4a1e'
    )

    cassette_name = 'ok/test_check_status_success.json'

    with vcr_test().use_cassette(cassette_name):
        status, resolution = client.get_request_state(oebs_agreement)

    assert status == 'in_progress'
    assert not resolution


def test_close_request_success():
    client = OkClient()
    oebs_agreement = factories.OEBSAgreementFactory(
        ok_id='9b8ec907-f604-4302-852d-7105766e4a1e'
    )

    cassette_name = 'ok/test_close_request_success.json'

    with vcr_test().use_cassette(cassette_name):
        client.close_request(oebs_agreement)
