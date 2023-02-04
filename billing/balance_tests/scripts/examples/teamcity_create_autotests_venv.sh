#!/usr/bin/env bash

# upd: сейчас pyenv c нужными версиями ставится автоматически
# https://github.yandex-team.ru/Billing/teamcity-agents/blob/master/bin/teamcity-agents.sh#install_pyenv
# в PATH он не прописывается поэтому его нужно звать напрямую из /opt/pyenv/bin/pyenv

# на нужной машине под пользователем teamcity
# ssh greed-dev3f
# sudo su -l teamcity

# добавляем строку в bash_profile если там такой еще нет
function add_to_bash_profile {
    FILE=/home/teamcity/.bash_profile
	grep -qF "$1" "$FILE" || echo "$1" >> "$FILE"
	source $FILE
}

# устанавливаем pyenv
curl -L https://raw.githubusercontent.com/pyenv/pyenv-installer/master/bin/pyenv-installer | bash
add_to_bash_profile 'export PATH="/home/teamcity/.pyenv/bin:$PATH"'
add_to_bash_profile 'eval "$(pyenv init -)"'
add_to_bash_profile 'eval "$(pyenv virtualenv-init -)"'

# устанавливаем нужную версию питона
# изменяем временную директорию в ~/src, т.к. под teamcity в других местах у нас нет прав на запись и выполнение
add_to_bash_profile 'export TMPDIR="/home/teamcity/src"'
pyenv install 2.7.10

# создаем директорию для virtualenv и переходим в нее
mkdir -p /home/teamcity/autotesting/venvs/python-tests
cd /home/teamcity/autotesting/venvs/

# скачиваем наш requirements.txt
wget https://github.yandex-team.ru/raw/Billing/balance-tests/master/requirements.txt

# создаем и активируем virtualenv с нужной версией питона
virtualenv -p /home/teamcity/.pyenv/versions/2.7.10/bin/python python-tests
source /home/teamcity/autotesting/venvs/python-tests/bin/activate

# обновляем pip и setuptools
pip install --upgrade pip
pip install setuptools --upgrade

# устанавливаем все зависимости из requirements.txt
pip install -r requirements.txt
