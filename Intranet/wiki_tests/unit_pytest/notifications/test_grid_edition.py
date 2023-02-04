import pytest

from datetime import timedelta

from wiki.grids.utils import dummy_request_for_grids, insert_rows, row_change_action
from wiki.notifications.generators import GridEdition as EditionGen
from wiki.notifications.models import PageEvent
from wiki.notifications.queue import Queue
from wiki.subscriptions.logic import create_subscription
from wiki.utils import timezone

pytestmark = [pytest.mark.django_db]


def test_add_column(client, wiki_users, test_grid, test_org_ctx):
    create_subscription(wiki_users.chapson, test_grid)
    create_subscription(wiki_users.asm, test_grid)

    inserted_indexes = insert_rows(
        test_grid,
        [
            {'src': 'source1', 'dst': 'destination1'},
            {'src': 'source2'},
            {'src': 'source3', 'dst': 'destination2', 'staff': 'chapson'},
        ],
        dummy_request_for_grids(),
    )
    test_grid.save()
    for idx in inserted_indexes:
        # Создаст новое событие (или обновит существующее)
        row_change_action[PageEvent.EVENT_TYPES.create](
            test_grid.access_data[test_grid.access_idx[idx]],
            wiki_users.thasonic,
            test_grid,
        )

    # Нужно обновить таймаут на событиях, чтобы они попали в очередь
    PageEvent.objects.all().update(timeout=timezone.now() - timedelta(hours=1))

    new_events = Queue().new_events(test_grid.id)
    generator = EditionGen()
    result = generator.generate(new_events, {})
    assert len(result) == 2
