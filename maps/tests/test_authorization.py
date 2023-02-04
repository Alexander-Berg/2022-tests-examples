import logging
import time
import rstr

from .basic_test import BasicTest
import lib.fakeenv as fakeenv
import lib.remote_access_server as server

from data_types.user import User

logger = logging.getLogger("TestAuthorization")


NEED_CONFIRM_TIME = int(time.time() - 11 * 60)  # 11 minutes  ago
UNCOFIRMED_TOKEN_ERROR = {
    "code": "tokenUnconfirmed",
    "description": "Токен должен быть подтвержден"
}
NO_BOUND_PHONE_ERROR = {
    "code": "noBoundPhone",
    "description": "Нет привязанного телефона"
}
PHONE_CHANGED_ERROR = {
    "code": "phoneChanged",
    "description": "Необходимо подтвердить отвязку телефона"
}
BAD_TOKEN_ERROR = {
    "code": "badToken",
    "description": "Не удалось авторизоваться с переданным токеном"
}


def get_wrong_code(code):
    while True:
        wrong_code = rstr.digits(4)
        if wrong_code != code:
            return wrong_code


class TestAuthorization(BasicTest):
    def setup(self):
        self.user = User()
        fakeenv.add_user(self.user)

    def check_has_access(self, error_json=None, token=None):
        status, response = server.get_cars(self.user.oauth if token is None else token)
        assert status == (200 if error_json is None else 401)
        if error_json is not None:
            assert response == error_json

    def test_get_phone_from_blackbox(self):
        response = server.get_phone(self.user.oauth) >> 200
        assert response["phone"] == self.user.get_masked_phone()

        fakeenv.delete_user_phone(self.user)
        response = server.get_phone(self.user.oauth) >> 404
        assert response == NO_BOUND_PHONE_ERROR

    def test_phone_bind_no_sms(self):
        self.check_has_access(error_json=UNCOFIRMED_TOKEN_ERROR)

        server.bind_phone(self.user.oauth) >> 200

        self.check_has_access(error_json=None)

    def test_phone_unbind(self):
        server.bind_phone(self.user.oauth) >> 200

        self.check_has_access(error_json=None)

        sms_list = fakeenv.read_sms(self.user.phone)
        assert len(sms_list) == 0, str(sms_list)

        server.unbind_phone(self.user.oauth) >> 200

        code = self.get_sms_code(self.user.phone)
        server.confirm_phone(self.user.oauth, code) >> 200

        self.check_has_access(error_json=UNCOFIRMED_TOKEN_ERROR)

    def test_phone_bind_with_sms(self):
        self.check_has_access(error_json=UNCOFIRMED_TOKEN_ERROR)

        fakeenv.add_user(self.user, confirmTime=NEED_CONFIRM_TIME)

        server.bind_phone(self.user.oauth) >> 200

        code = self.get_sms_code(self.user.phone)
        server.confirm_phone(self.user.oauth, code) >> 200

        self.check_has_access(error_json=None)

    def test_sms_limit(self):
        self.check_has_access(error_json=UNCOFIRMED_TOKEN_ERROR)

        fakeenv.add_user(self.user, confirmTime=NEED_CONFIRM_TIME)

        fakeenv.set_limit_exceeded(self.user.phone, True)

        server.bind_phone(self.user.oauth) >> 423

        sms_list = fakeenv.read_sms(self.user.phone)
        assert(len(sms_list) == 0)

        self.check_has_access(error_json=UNCOFIRMED_TOKEN_ERROR)

    def test_phone_change(self):
        server.bind_phone(self.user.oauth) >> 200

        self.check_has_access(error_json=None)

        fakeenv.set_user_phone(self.user, "+79150000000")
        self.check_has_access(error_json=PHONE_CHANGED_ERROR)

    def test_phone_delete(self):
        server.bind_phone(self.user.oauth) >> 200

        self.check_has_access(error_json=None)

        fakeenv.delete_user_phone(self.user)
        self.check_has_access(error_json=NO_BOUND_PHONE_ERROR)

    def test_phone_security(self):
        server.bind_phone(self.user.oauth) >> 200

        self.check_has_access(error_json=None)

        fakeenv.set_user_phone(self.user, self.user.phone, secured=False)
        self.check_has_access(error_json=NO_BOUND_PHONE_ERROR)

    def test_wrong_token(self):
        server.bind_phone(self.user.oauth) >> 200

        self.check_has_access(token='wrongtoken', error_json=BAD_TOKEN_ERROR)
        self.check_has_access(token='', error_json=BAD_TOKEN_ERROR)

    def test_phone_unbind_exceed_try_limit(self):
        self.user.registrate()

        server.unbind_phone(self.user.oauth) >> 200

        code = self.get_sms_code(self.user.phone)
        wrong_code = get_wrong_code(code)

        MAX_TRY_LIMIT = 3
        for i in range(MAX_TRY_LIMIT):
            response = server.confirm_phone(self.user.oauth, wrong_code) >> 422
            assert response['code'] == 'wrongSmsCode', response

        response = server.confirm_phone(self.user.oauth, wrong_code) >> 408
        assert response['code'] == 'noRequests', response

        response = server.unbind_phone(self.user.oauth) >> 429
        assert response['code'] == 'otherActiveRequest', response

        fakeenv.add_user(self.user, confirmTime=NEED_CONFIRM_TIME)
        response = server.bind_phone(self.user.oauth) >> 429
        assert response['code'] == 'otherActiveRequest', response
