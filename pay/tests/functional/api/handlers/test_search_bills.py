import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, has_entry, has_item, has_properties


@pytest.mark.usefixtures('mock_app_authentication')
@pytest.mark.asyncio
async def test_schedules_search(app, storage, user):
    now = utcnow()
    user.synced_revision = user.revision
    user = await storage.user.save(user)

    await app.post('/api/v1/search/bills', raise_for_status=True)

    created_tasks = await alist(storage.task.find(filters={'created': lambda field: field > now}))
    assert_that(
        created_tasks,
        has_item(
            has_properties(
                action_name='search_fines',
                params=has_entry(
                    'action_kwargs',
                    has_entry('uid', user.uid),
                ),
            )
        ),
    )
