#!/usr/bin/env bash

local_editor="export EDITOR='open -W -n -a TextEdit'"


### Hooks ###

# Прекоммитный хук, проверяющий имя ветки
arc config 'pre-commit-hook.mobile/geo/qa/test-cases/.branch-names' \
           'mobile/geo/qa/test-cases/.archooks/pre-commit' ||
>&2 echo 'Прекоммитный хук mobile/geo/qa/test-cases/.archooks/pre-commit не добавлен.'

# Валидация palmsync
arc config pre-push-hook.palmsync_validate mobile/geo/qa/test-cases/.archooks/palmsync_validate ||
>&2 echo 'Хук mobile/geo/qa/test-cases/.archooks/palmsync_validate не добавлен.'


### Arc SHELL aliases ###

# Добавить все файлы в индекс и закоммитить
commit_all="$local_editor && arc add --all && arc commit"
arc config alias.commit-all \!"$commit_all" ||
>&2 echo 'Алиас commit-all не добавлен.'

# Ребейзнуться на свежайший trunk, запушить изменения 
# и создать публичный PR из текущей ветки
review="$local_editor && arc pull trunk && arc rebase trunk && arc pr c --publish --push"
arc config alias.review \!"$review" ||
>&2 echo 'Алиас review не добавлен'

# Создать и опубликовать PR из upstream ветки
review_no_push="$local_editor && arc pr c --publish"
arc config alias.review_no_push \!"$review_no_push" ||
>&2 echo 'Алиас review_no_push не добавлен'

# Ребейзнуться на свежайший trunk, запушить изменения в существующую ветку
update='arc pull trunk && arc rebase trunk && arc push --force'
arc config alias.update \!"$update" ||
>&2 echo 'Алиас update не добавлен'


# Добавить все файлы в индекс, закоммитить, ребейзнуться на свежайший trunk
# запушить изменения в существующую ветку
update_all="$commit_all; $update"
arc config alias.update-all \!"$update_all" ||
>&2 echo 'Алиас update-all не добавлен'

# Добавить все файлы в индекс, закоммитить, ребейзнуться на свежайший trunk
# и создать публичный PR из текущей ветки
review_all="$commit_all; $review"
arc config alias.review-all \!"$review_all" ||
>&2 echo 'Алиас review-all не добавлен'

# Стянуть trunk и отвести от него новую ветку для работы по тикету
# Имя ветки валидируется: должно соответствовать номеру тикета в наших очередях
new() {
    local branch
    local branch_regex

    branch="$1"
    branch_regex='^(GEOTESTSITE|MAPSMOBILETEST)-[0-9]+[a-zA-Z-]*$'

    if [[ -z "$branch" ]]
    then
        >&2 echo "Не указана ветка для создания."
        >&2 echo "Команда не выполнена."
        >&2 arc status
        return 1
    elif ! [[ $branch =~ $branch_regex ]]
    then
        >&2 echo "Допустимые имена веток: GEOTESTSITE-XXX, MAPSMOBILETEST-XXX."
        >&2 echo "Команда не выполнена."
        >&2 arc status
        return 1
    else
        arc co trunk && arc pull && arc co -b "$branch"
        arc status
    fi
}
_new="$(typeset -f new); new"
arc config alias.new \!"$_new" ||
>&2 echo 'Алиас new не добавлен'


# Удалить ветки, полностью замерженные в trunk
cleanup() {
    arc branch --merged trunk | grep -v trunk | xargs -L 1 arc branch -d
    echo "\nArc branches:"
    arc br
    echo
}
_cleanup="$(typeset -f cleanup); cleanup"
arc config alias.cleanup \!"$_cleanup" ||
>&2 echo 'Алиас cleanup не добавлен'


### Arc aliases ###

# Смонировать аркадию в ~/arcadia
arc config alias.remount "mount -m $HOME/arcadia -S $HOME/store" ||
>&2 echo 'Алиас remount не добавлен'
