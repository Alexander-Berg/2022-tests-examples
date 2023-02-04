import rstr
import uuid

import lib.fakeenv as fakeenv
import lib.remote_access_server as server


class User:
    def __init__(self, oauth=None, name=None, phone=None):
        self.name = name or rstr.letters(4, 12)
        self.phone = phone or "+7"+rstr.digits(10)
        self.oauth = oauth or str(uuid.uuid4())
        self.device_id = rstr.digits(10)

    def get_masked_phone(self):
        return self.phone[:5] + ('*' * 5) + self.phone[-2:]

    def registrate(self):
        fakeenv.add_user(self)
        status, response = server.bind_phone(self.oauth, device_id=self.device_id)
        assert status == 200

    def confirm_phone(self, phone=None):
        phone = phone or self.phone
        sms_list = fakeenv.read_sms(phone)
        assert len(sms_list) == 1, str(sms_list)

        server.confirm_phone(self.oauth, sms_list[0][-4:]) >> 200

    def unbind_phone(self):
        response = server.unbind_phone(self.oauth) >> 200
        if response["isConfirmationRequired"]:
            self.confirm_phone()

    def bind_phone(self, phone=None):
        response = server.bind_phone(self.oauth) >> 200
        if response["isConfirmationRequired"]:
            self.confirm_phone(phone)
        response = server.get_phone(self.oauth) >> 200
        assert response["phone"] == phone[:5] + "*" * 5 + phone[-2:], response

        self.phone = phone

    def change_phone(self, phone=None, confirmTime=None, secured=True):
        new_phone = phone or "+7" + rstr.digits(10)
        fakeenv.set_user_phone(self, new_phone, confirmTime=confirmTime, secured=secured)

        self.unbind_phone()

        self.bind_phone(new_phone)
