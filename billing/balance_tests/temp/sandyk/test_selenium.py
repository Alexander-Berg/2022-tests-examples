__author__ = 'sandyk'


import pytest
import time
import pytest

from selenium import webdriver

def test_selenium():
    browser = webdriver.Firefox()
    url = 'https://balance.greed-tm1f.yandex.ru/paypreview.xml?person_id=3783705&request_id=68936732&paysys_id=1003&contract_id=&coupon=&mode=ci'
    browser.get(url)
    time.sleep(5)
    browser.close()

if __name__ == "__main__":
    # test_selenium()
    pytest.main("test_selenium.py")

