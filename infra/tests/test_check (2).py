def test_output(manifest):
    data = manifest.execute('bundle_info')

    package, revision = data['events'][0].pop('description').split('@', 1)

    expected = {
        'events': [
            {
                'service': 'bundle_info',
                'status': 'OK',
                'tags': ['HOSTMAN-516'],
            }
        ]
    }

    assert expected == data
    assert 'infra/rtc/juggler/bundle' == package
    assert int(revision) >= -1
