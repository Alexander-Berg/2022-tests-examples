# coding: utf-8

import allure

__author__ = 'kostya-karpus'


def find_and_click_by_text(driver, text):
    with allure.step(u'Find and click element'):
        driver.find_elements('xpath', u".//*[text()='{}']".format(text))[0].click()

    return driver