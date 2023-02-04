from mdh.core.models import Record


def test_basic(init_user, init_resource, client, monkeypatch):

    user = init_user()

    url = '/callback/st/'
    data = {
        'key': 'TESTMDH-1',
        'status': 'approved',
        'queue': 'TESTMDH',
        'tags': 'one, two',
    }

    # Неизвестная запись.
    result = client.post(url, data, content_type='application/json')
    assert result.status_code == 200
    assert result.content == b'No record on review found for TESTMDH-1'

    # Неизвестный статус.
    result = client.post(url, {**data, 'status': 'dummy'}, content_type='application/json')
    assert result.status_code == 200
    assert result.content == b'Skipped unknown status dummy for TESTMDH-1'

    resource = init_resource(user=user)

    # Известная запись. Утверждение.
    record1 = Record(
        creator=user,
        issue='TESTMDH-1',
        resource=resource,
        schema=resource.schema,
        attrs={'integer1': 2},
    )
    record1.mark_on_review()
    record1.save()

    result = client.post(url, data, content_type='application/json')
    assert result.status_code == 200
    assert result.content == b'ok'
    record1.refresh_from_db()
    assert record1.is_approved

    # Необработанное исключение.
    record1.mark_on_review()
    record1.save()

    monkeypatch.setattr(Record, 'set_status', lambda: None)
    result = client.post(url, data, content_type='application/json')
    assert result.status_code == 500
    assert result.content.endswith(b'<lambda>() takes 0 positional arguments but 2 were given')
