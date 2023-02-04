# coding: utf-8

"""
Генерация строки с логинами для использования в паке генерации логинов
https://aqua.yandex-team.ru/#/pack/5785012ee4b09ee5928eb415
"""

if __name__ == '__main__':
    login_prefix = 'yb-atst-user-'
    postfix_start = 26
    qty = 20
    print ', '.join(
        [login_prefix + str(postfix_number) for postfix_number in range(postfix_start, postfix_start + qty)])
