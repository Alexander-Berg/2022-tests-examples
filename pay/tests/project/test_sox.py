def test_sox(stage, production):
    """Sox service flag must be enabled in production environment"""

    if production:
        assert 'sox_service' in stage.get('spec')
        assert stage.get('spec').get('sox_service') is True
