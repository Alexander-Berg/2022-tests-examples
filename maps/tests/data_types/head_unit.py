import rstr


class HeadUnit:
    HEX_DIGITS_UPPER = '0123456789ABCDEF'
    HEX_DIGITS_LOWER = HEX_DIGITS_UPPER.lower()

    def __init__(self, head_id=None, device_id=None, uuid=None):
        self.head_id = head_id or HeadUnit.head_id()
        self.uuid = uuid or HeadUnit.uuid()
        self.device_id = device_id or HeadUnit.device_id()

    @staticmethod
    def device_id():
        return rstr.rstr(HeadUnit.HEX_DIGITS_LOWER, 32)

    @staticmethod
    def head_id():
        return rstr.rstr(HeadUnit.HEX_DIGITS_UPPER, 12)

    @staticmethod
    def uuid():
        return rstr.rstr(HeadUnit.HEX_DIGITS_LOWER, 32)
