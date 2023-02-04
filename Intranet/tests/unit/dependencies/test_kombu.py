import kombu


def test_kombu_version():
    error = (
        'Multiple hosts for mongodb are not properly supported in kombu > 4.6.3. '
        'In addition Femida uses patched mongodb transport (FemidaMongoTransport)'
    )
    assert kombu.__version__ == '4.6.3', error
