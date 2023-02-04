from plan.notify.recipients.objects import Recipient
from plan.staff.constants import GENDER, LANG


def check_attrs(instance, email=None, lang_ui=None, gender=None):
    if email:
        assert instance.email == email

    if lang_ui:
        assert instance.lang_ui == lang_ui

    if gender:
        assert instance.gender == gender

    assert instance.is_dismissed is False


def test_check_defaults():
    email = 'a@a.ru'
    recipient = Recipient(email=email)
    check_attrs(
        instance=recipient,
        email=email,
        lang_ui=LANG.RU,
        gender=GENDER.MALE,
    )


def test_set_email():
    email = 'flash@yandex-team.ru'
    recipient = Recipient(email=email)
    check_attrs(
        instance=recipient,
        email=email,
    )


def test_set_email_partial():
    email = 'flash@'
    recipient = Recipient(email=email)
    check_attrs(
        instance=recipient,
        email=email + 'yandex-team.ru',
    )


def test_set_lang_ui():
    email = 'a@a.ru'
    recipient = Recipient(email=email, lang_ui=LANG.EN)
    check_attrs(
        instance=recipient,
        lang_ui=LANG.EN,
    )


def test_set_gender():
    email = 'a@a.ru'
    recipient = Recipient(email=email, lang_ui=LANG.EN)
    check_attrs(
        instance=recipient,
        email=email,
        lang_ui=LANG.EN,
    )


def test_inheritance():
    class BatmanRecipient(Recipient):
        _email = 'batman@ya.ru'

    batman = BatmanRecipient()
    check_attrs(
        instance=batman,
        email='batman@ya.ru',
        lang_ui=LANG.RU,
        gender=GENDER.MALE,
    )
