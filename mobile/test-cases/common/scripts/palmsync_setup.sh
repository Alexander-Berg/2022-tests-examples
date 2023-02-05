#!/usr/bin/env bash

### Установщик правильной версии ноды для Palmsync
### Устанавливать сорсингом скрипта: `. npm_install.sh`
### Либо предварительно экспортировав переменную NVM_DIR и функцию nvm
### По умолчанию они не экспортируются

__NODE_VERSION='14.15.5'

if ! command -v brew >/dev/null
then
    >&2 echo 'Homebrew не установлен.'
    exit 1
fi

# Установка nvm – менеджера окружений Node.js
brew update 
if ! brew ls --versions nvm >/dev/null
then
    brew install nvm 
fi

# Создать папку для nvm
mkdir -p ~/.nvm

# Добавить инициализацию nvm в профили bash/zsh
__NVM_PREFIX="$(brew --prefix nvm)"
__SET_NVM_DIR='export NVM_DIR="$HOME/.nvm"'
__SOURCE_NVM_SH="[ -s \"$__NVM_PREFIX/nvm.sh\" ] && . \"$__NVM_PREFIX/nvm.sh\""

__init_command=''
if [[ -z $NVM_DIR ]]
then
    __init_command="${__init_command}\n${__SET_NVM_DIR}"
fi
if ! command -v nvm >/dev/null
then
    __init_command="${__init_command}\n${__SOURCE_NVM_SH}"
fi
if [[ -n $__init_command ]]
then
    __init_command="${__init_command}\n"
fi

echo -ne "$__init_command" | tee -a ~/.bash_profile ~/.zshrc
eval "$(echo -ne "$__init_command")"

# Установка Node.js "$__NODE_VERSION" через nvm
nvm install "$__NODE_VERSION"

# Сказать nvm, что нода по умолчанию – "$__NODE_VERSION"
nvm alias default "$__NODE_VERSION"

echo "Текущая версия Node:"
node --version
echo "Текущая версия npm:"
npm --version

cd "$HOME/arcadia/mobile/geo/qa/test-cases/kartograph/tools/palmsync" && npm ci
cd "$HOME/arcadia/mobile/geo/qa/test-cases/mapsmobile/tools/palmsync" && npm ci
cd "$HOME/arcadia/mobile/geo/qa/test-cases/navi/tools/palmsync" && npm ci

unset __NODE_VERSION \
      __NVM_PREFIX \
      __SET_NVM_DIR \
      __init_command
