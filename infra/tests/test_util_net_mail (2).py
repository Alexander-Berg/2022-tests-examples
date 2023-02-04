from sepelib.util.net.mail import Mail


def test_default_sender():
    email = 'anonymous@yandex-team.ru'
    # sender is None case
    m = Mail.from_config({'default_sender': None})
    assert m.default_sender_email is None
    # sender is string case
    m = Mail.from_config({'default_sender': email})
    assert m.default_sender_email == email
    # sender is tuple case
    m = Mail.from_config({'default_sender': ('Somebody To Love', email)})
    assert m.default_sender_email == email
