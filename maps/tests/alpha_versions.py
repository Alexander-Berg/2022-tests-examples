def test_alpha_versions(coordinator):
    dataset = 'pkg-b'
    host = 'gen-ab1'
    versions = ['8', '8a', '9', 'a', 'b']
    for version in versions:
        coordinator.upload(dataset, version, host, tvm_id=1)

    def check_exists(ver, pattern='yes'):
        assert pattern in coordinator.http_get(
            '/exists',
            dataset=dataset, version=ver).text

    for version in versions:
        check_exists(version)

    coordinator.http_post('/debug/PurgeExpiredVersions', now=0)

    for version in ['8', '8a', '9']:
        check_exists(version,  'no')

    for version in ['a', 'b']:
        check_exists(version)
