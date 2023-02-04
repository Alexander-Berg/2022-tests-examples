from infra.rsc.src.app import rsc


def test_app_create(config):
    a = rsc.Application('fake-instance')
    a.setup_environment()
