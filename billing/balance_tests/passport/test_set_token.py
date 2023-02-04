# -*- coding: utf-8 -*-

from balance import mapper
from tests.base import MediumTest


class TestYaMoneyTokens(MediumTest):
    def test_new_token(self):
        session = self.session
        passport = session.query(mapper.Passport).first()
        service = session.query(mapper.Service).getone(7)
        passport.set_token(service, 'QWERTY')
        self.assertEqual('QWERTY', passport.get_token(service))
        test_token = '43456542452241.C1539DB6A4BAD6D0E931FCADE464D4EF0965C7135B0CA5E27F832A8A8B98E5AC613A832CF851868BD620705B1EE6C8E49089B08FBBB4F740D22CAF84AF70DEEEC2A88FBFA6B68AAE5B6FDDB1CE39DA2AA8D12DCCEFD5EBD548D9DDC661DD973381FBA0226EF1D52EA3851E1CA8061F3C309AF61F4D1B01AA42EA6A8405CF7B2D'
        passport.set_token(service, test_token)
        self.assertEqual(test_token, passport.get_token(service))
