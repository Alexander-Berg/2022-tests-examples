from hamcrest import assert_that, contains_inanyorder, is_not, has_length, equal_to

from yb_darkspirit.core.registration.documents import make_registration_batch
from yb_darkspirit.interactions import TrackerClient
from time import sleep
from io import StringIO

FILE1_TEXT = u'file1 content'
FILE2_TEXT = u'file2 content'


def test_last_attachment(application):
    tracker_client = TrackerClient.from_app(application)
    issue_key = _create_issue(tracker_client)

    _create_comment_with_attachment(tracker_client, issue_key, FILE1_TEXT)
    _last_attachment_contains(tracker_client, issue_key, FILE1_TEXT)

    _create_comment_with_attachment(tracker_client, issue_key, FILE2_TEXT)
    _last_attachment_contains(tracker_client, issue_key, FILE2_TEXT)


def _last_attachment_contains(tracker_client, issue_key, text):
    sleep(1)  # To eliminate Tracker replication latency
    last_attachment, _ = tracker_client.get_last_attachment(issue_key)
    assert_that(last_attachment, equal_to(text))


def _create_issue(tracker_client):
    base_client = tracker_client.client
    return (
        base_client
        .issues
        .create(queue='SPIRITSUP', summary='test summary', description='test description')
        .key
    )


def _create_comment_with_attachment(tracker_client, issue_key, file_content):
    base_client = tracker_client.client
    return (
        base_client
        .issues[issue_key]
        .comments
        .create(text=file_content, attachments=[StringIO(file_content)])
    )


def test_reregistration_tags(reregister_entry):
    assert_that(
        TrackerClient.reregistration_tags(reregister_entry),
        contains_inanyorder(
            'ds_rereg_application',
            'cr_sn_' + reregister_entry.cash_register.serial_number,
            'fn_sn_' + reregister_entry.fiscal_storage.serial_number,
            'firm_inn_' + reregister_entry.firm_inn,
            'load_group_' + reregister_entry.cash_register.current_groups,
            TrackerClient.FN_CHANGE_TAG
        )
    )


def test_create_rereg_application_upload_issue(application, reregister_entry):
    batch = next(make_registration_batch([reregister_entry]))
    client = TrackerClient.from_app(application)
    issue = client.create_rereg_application_upload_issue(reregister_entry, batch)
    assert_that(issue, is_not(None))

    attaches = list(issue.attachments)
    assert_that(attaches, has_length(1))

    attach = list(issue.attachments)[0]
    assert_that(attach.name, equal_to('{}.zip'.format(issue.key)))

    attach_data = ''.join(attach.read())
    assert_that(attach_data, equal_to(batch.data))


def test_find_last_rereg_application_upload_issue(application, reregister_entry):
    batch = next(make_registration_batch([reregister_entry]))
    client = TrackerClient.from_app(application)

    issue = client.create_rereg_application_upload_issue(reregister_entry, batch)
    sleep(10)
    last_issue = client.find_last_rereg_application_upload_issue(reregister_entry)

    assert_that(issue.key, equal_to(last_issue.key))


def test_find_rereg_issue_serial_numbers():
    tags = ['ds_rereg_application', 'cr_sn_3820010612180', 'fn_sn_9960440300351924']
    serial_numbers = TrackerClient._find_rereg_issue_serial_numbers(tags)
    assert serial_numbers is not None

    cr_sn, fn_sn = serial_numbers
    assert cr_sn == '3820010612180'
    assert fn_sn == '9960440300351924'
