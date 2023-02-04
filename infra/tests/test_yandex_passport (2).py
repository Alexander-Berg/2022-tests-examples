import pytest

from sepelib.yandex import passport


def test_oauth_check_result_str():
    r = passport.OAuthCheckResult('somebodytolove', '234dsf3434', scope='nanny:all metrika:write', error='Yes')
    assert str(r) == "OAuthCheckResult(login='somebodytolove'," \
                     "client_id='234dsf3434',scope='nanny:all metrika:write',error='Yes')"


def test_bad_init_values():
    for value in [0, -1, 0.5]:
        with pytest.raises(ValueError):
            passport.PassportClient(req_timeout=value)
