import pytest
from wiki.notifications.generators import DeletionGen
from wiki.notifications.models import PageEvent
from wiki.org import org_ctx
from wiki.subscriptions.logic import create_subscription


@pytest.mark.django_db
def test_delete_notifications(client, wiki_users, api_url, test_page):
    test_page.authors.add(wiki_users.kolomeetz)
    create_subscription(wiki_users.kolomeetz, test_page)
    create_subscription(wiki_users.thasonic, test_page)

    client.login(wiki_users.kolomeetz)
    resp = client.delete(f'{api_url}/{test_page.supertag}')
    assert resp.status_code == 200

    event: PageEvent = PageEvent.objects.filter(page=test_page)[0]

    assert event.event_type == event.EVENT_TYPES.delete
    assert event.notify

    with org_ctx(test_page.org):
        emails = DeletionGen().generate([event])
        email_details, message = emails.popitem()
        assert wiki_users.thasonic.email in email_details.receiver_email
