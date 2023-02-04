# coding=utf-8
__author__ = 'torvald'
import time

import pytest



pytestmark = [pytest.mark.docpath('https://wiki.yandex-team.ru/users/torvald/Billing.Exp'),
              # pytest.mark.docs('Чек-лист')
              ]

@pytest.mark.docs(u'Большой и Важной Функциональности', u'совсем другой')
def test_eq ():
    '''bla bla bla blabla'''
    time.sleep(1)
    assert 1==1

@pytest.mark.docpath('https://wiki.yandex-team.ru/users/aikawa/tests')
@pytest.mark.docs(u'* Проверяем неравенство чисел')
def test_noteq ():
    time.sleep(1)
    assert 2>1

@pytest.mark.docs(u'Большой и Важной Функциональности')
@pytest.mark.xfail(reason='BALANCE-20000')
@pytest.mark.parametrize('x', [1,2,3], ids=lambda x: str(x))
def test_noteq2 (x):
    time.sleep(x)
    assert 1<3

@pytest.mark.docs(u'Такого текста нет')
def test_no_text ():
    time.sleep(1)
    assert 1<100

@pytest.mark.docs(u'Старый текст')
# @pytest.mark.doc(u'Новый текст')
def test_change_text ():
    time.sleep(1)
    assert 1<100

if __name__  == "__main__":
    pytest.main('-v --docs "1" --collect-only')