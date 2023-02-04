# coding: utf-8

'''
Установка зависимостей на агенты вручную
Текущие агенты: greed-dev3f, greed-dev3g, greed-dev1w.balance.os.yandex.net. Установить надо на все
Заходим по ssh
ssh greed-dev3f.yandex.ru
Переключаемся на пользователя teamcity
sudo su -l teamcity
Спросит пароль - вводим свой рабочий
Активируем наш виртуаленв
source ~/autotesting/venvs/python-tests/bin/activate
Устанавливаем новые пакеты
pip install conditional
Выходим
exit
'''

# Чтобы указать одну из тестовых сред или веток разработчика выбираем значение в выпадающем списке env.balance
# Для веток разработчика вид урлов на dev4f можно увидеть здесь btestlib.environments.BalanceEnvironment#dev_env

# Чтобы запуститься на кастомной ветке на greed-dev2e добавляем в запуск параметр
# evn.balance.branch в который пишем имя ветки - new_paystep
# оно сформирует урлы вид которы можно увидеть здесь btestlib.environments.BalanceEnvironment#branch_env

# Урлы можно задать полностью кастомно через параметр env.balance.custom
# Туда надо передать json. Пример:
'''
{
"name": "new_paystep",
"medium_url": "http://new_paystep.greed-dev2e.yandex.ru:8002/xmlrpc",
"test_balance_url": "http://new_paystep.greed-dev2e.yandex.ru:30702/xmlrpc",
"balance_ci": "https://new_paystep.greed-dev2e.yandex.ru",
"balance_ai": "https://new_paystep.admin.greed-dev2e.yandex.ru",
"coverage_url": "not_set",
"xmlrpc_strict_url": "not_set",
}
'''

# Если заданы все три параметра - возьмутся урлы из env.balance.custom.
# Если заданы env.balance и env.branch - возьмется env.branch
