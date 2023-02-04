

def test_unistat_record(client, unistat_factory):

    metrics = {
        'metric_1': 1,
        'metric_2': 2.0,
    }
    unistat_factory(metrics=metrics)

    response = client.get(
        '/api/watcher/common/unistat/'
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert len(data) == 2
    expected_metrics = {f'watcher_{key}_max': val for key, val in metrics.items()}
    assert expected_metrics == {el[0]: el[1] for el in data}
