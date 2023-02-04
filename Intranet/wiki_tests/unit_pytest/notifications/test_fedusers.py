import pytest

from wiki.notifications.generators import CreationGen
from wiki.notifications.logic import create_page_event_create
from wiki.subscriptions.logic import create_subscription
from wiki.sync.connect.org_ctx import org_ctx
from wiki.users.logic import set_uses_apiv2


@pytest.mark.django_db
def test_fed_user(client, wiki_users, api_url, page_cluster):
    test_page = page_cluster['root/a']

    set_uses_apiv2(wiki_users.thasonic)
    staff = wiki_users.thasonic.staff

    # сделаем федеративным пользователем у которого нет имени и фамилии в выгрузке из облака
    staff.first_name = staff.first_name_en = 'thasonic@yandex-team.ru'
    staff.last_name = staff.last_name_en = 'thasonic@yandex-team.ru'
    staff.login = 'thasonic@yandex-team.ru'
    staff.save()

    create_subscription(wiki_users.thasonic, page_cluster['root'], is_cluster=True)

    event = create_page_event_create(test_page, wiki_users.asm, test_page.revision_set.get(), notify=True)

    with org_ctx(test_page.org):
        emails = CreationGen().generate([event])
        email_details, message = emails.popitem()
        assert email_details.receiver_name == 'thasonic'
