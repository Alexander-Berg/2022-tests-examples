# -*- coding: utf-8 -*-
from django.test import TestCase
from django.core.exceptions import ValidationError

from events.surveyme.fields.base.validators import validate_phone


class Test__validate_phone(TestCase):
    def test_must_be_valid_phone(self):
        valid_phones = [
            '+7 916 1234590',
            '+7 916 123-45-90',
            '+7 916 123 45 90',
            '+79161234590',
            '9161234590',
            '4991234567',
            '4951234567',
            '84951234567',
            '+74951234567',
            '+712345',  # min 5 num
            '004951234567',
        ]
        for phone in valid_phones:
            try:
                validate_phone(phone)
                raised = False
            except ValidationError:
                raised = True

            msg = 'Телефон "%s" должен быть валиден.' % phone
            self.assertFalse(raised, msg=msg)

    def test_must_be_invalid_phone(self):
        invalid_phones = [
            '+7',
            '+',
            '',
            'test',
            '05353484618',
            '3423423',
            '4966577548',
            '+71234',  # min 5 num
            '000',
        ]
        for phone in invalid_phones:
            try:
                validate_phone(phone)
                raised = False
            except ValidationError:
                raised = True

            msg = 'Телефон "%s" должен быть не валиден.' % phone
            self.assertTrue(raised, msg=msg)
