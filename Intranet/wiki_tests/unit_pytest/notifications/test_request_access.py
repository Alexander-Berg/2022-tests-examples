import pytest
from mock import patch

from django.conf import settings

from wiki.cloudsearch.staff_client import STAFF_API_CLIENT
from wiki.notifications.generators.request_access import AccessRequestGen
from wiki.notifications.models import PageEvent
from wiki.utils import timezone
from wiki.pages.models import AccessRequest
from model_mommy import mommy
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet


def _generate_request_access_event(page, applicant):
    request = mommy.make(AccessRequest, applicant=applicant, page=page)

    event = mommy.make(
        PageEvent,
        event_type=PageEvent.EVENT_TYPES.request_access,
        author=applicant,
        timeout=timezone.now(),
        page=page,
        meta={'access_request_id': request.id, 'reason': 'foobar'},
    )

    emails = AccessRequestGen().generate([event])
    return emails.popitem()


@pytest.mark.django_db
def test_simple_access_request(wiki_users, test_page):
    email_details, message = _generate_request_access_event(page=test_page, applicant=wiki_users.kolomeetz)
    assert wiki_users.thasonic.email in email_details.receiver_email


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_access_request_is_all_authors_dismissed(get_chief, wiki_users, test_page):
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    get_chief.side_effect = lambda login: set()

    email_details, message = _generate_request_access_event(page=test_page, applicant=wiki_users.kolomeetz)
    assert settings.SUPPORT_EMAIL in email_details.receiver_email


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_access_request_is_all_authors_dismissed_and_change_page_author(get_chief, wiki_users, test_page):
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    staff_ans = {'thasonic': {'chapson'}}
    get_chief.side_effect = lambda login: staff_ans.get(login, set())

    email_details, message = _generate_request_access_event(page=test_page, applicant=wiki_users.kolomeetz)
    assert settings.SUPPORT_EMAIL not in email_details.receiver_email
    assert wiki_users.chapson.email in email_details.receiver_email
