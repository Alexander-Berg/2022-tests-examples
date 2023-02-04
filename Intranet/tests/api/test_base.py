import pytest


@pytest.mark.parametrize('model', ('schedule', 'shift'))
@pytest.mark.parametrize('attribute', ('id', 'service.id', 'smth'))
def test_filter(scope_session, client, model, attribute):
    params_filter = f'{attribute}=1'
    response = client.get(
        f'/api/watcher/frontend/{model}/',
        params={'filter': params_filter},
    )
    data = response.json()
    if attribute == 'id':
        assert response.status_code == 200, response.text
    elif attribute == 'service.id':
        if model == 'schedule':
            assert response.status_code == 200, response.text
        else:
            assert response.status_code == 400, response.text
            assert data['detail'][0]['msg']['ru'] == "У объекта 'Shift' нет атрибута 'service'"
            assert data['detail'][0]['msg']['en'] == "Object 'Shift' has no attribute 'service'"
    else:
        assert response.status_code == 400, response.text
        assert data['detail'][0]['msg']['ru'] == f"У объекта '{model.capitalize()}' нет атрибута '{attribute}'"
        assert data['detail'][0]['msg']['en'] == f"Object '{model.capitalize()}' has no attribute '{attribute}'"
