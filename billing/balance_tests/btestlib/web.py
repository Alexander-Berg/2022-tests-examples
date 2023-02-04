# coding=utf-8

import time
import random

from selenium.webdriver.common.by import By


class AlfaBankPayForm(object):
    PASSWORD_INPUT_LOCATOR = (By.NAME, 'password')
    SUBMIT_BUTTON_LOCATOR = (By.NAME, 'form1')

    @classmethod
    def fill_pay_password(cls, driver, password):
        # Пароль почему-то вводится нестабильно.
        # Без sleep тоже не работает.
        # В худшем случае спим 10 секунд.
        password_input = driver.find_element(*cls.PASSWORD_INPUT_LOCATOR)
        for _ in range(10):
            password_input.send_keys(password)
            if password_input.get_attribute('value') == password:
                break
            time.sleep(random.random())
        assert password_input.get_attribute('value') == password, \
            'Failed to fill in pay password'
