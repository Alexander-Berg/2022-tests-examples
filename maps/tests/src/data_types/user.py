import rstr
import uuid
import re

import maps.automotive.libs.large_tests.lib.blackbox as blackbox


PHONE_ATTR_E164NUMBER = "102"
PHONE_ATTR_MASKED_NUMBER = "103"
PHONE_ATTR_IS_CONFIRMED = "105"
PHONE_ATTR_IS_BOUND = "106"
PHONE_ATTR_IS_SECURED = "108"


class Phone:
    def __init__(self, id=None, number=None, is_confirmed=True, is_bound=True, is_secured=True):
        self.id = id or rstr.digits(10)
        self.number = number or "+7" + rstr.digits(10)
        self.is_confirmed = is_confirmed
        self.is_bound = is_bound
        self.is_secured = is_secured

        phone_parts = re.match("\\+(\\d{1,3})(\\d{3})\\d{5}(\\d{2})", self.number)
        assert phone_parts, f"wrong number format {self.number}"
        self.masked_phone = f"+{phone_parts[1]} {phone_parts[2]} ***-**-{phone_parts[3]}"

    def dump(self):
        return {
            "id": self.id,
            "attributes": {
                PHONE_ATTR_E164NUMBER: self.number,
                PHONE_ATTR_MASKED_NUMBER: self.masked_phone,
                PHONE_ATTR_IS_CONFIRMED: "1" if self.is_confirmed else "0",
                PHONE_ATTR_IS_BOUND: "1" if self.is_bound else "0",
                PHONE_ATTR_IS_SECURED: "1" if self.is_secured else "0"
            }
        }


class User:
    def __init__(self, login=None, session_id=None, uid=None, oauth=None, phones=None):
        self.login = login or rstr.letters(5, 20)
        self.oauth = oauth or rstr.letters(5, 20)
        self.uid = uid or str(uuid.uuid4())
        self.phones = [Phone()] if phones is None else phones

    def register(self):
        blackbox.add_user(self)
