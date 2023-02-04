from mdh.core.changes import Create


def test_change_basics(init_user, init_resource, spawn_batch_change, init_batch):

    user = init_user()
    res = init_resource(user=user)

    change_1 = spawn_batch_change(type=Create, resource=res)

    batch = init_batch(creator=user, changes=[change_1])

    assert f'{change_1}'
    assert change_1.type_params == {'count': 1}
    assert change_1.is_draft

    batch.mark_published()
    change_1.refresh_from_db()
    assert change_1.is_published
