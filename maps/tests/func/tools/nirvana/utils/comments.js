function formatCommentsFromToloka(comments) {
    const parsedComments = comments
        .map(({solutions, tasks}) => {
            if (!solutions || !solutions[0]) {
                return null;
            }

            if (Object.keys(solutions[0].output_values).length === 0) {
                return null;
            }

            const {comment, ...checkboxes} = solutions[0].output_values || {};
            const {name, url, diffUrl} = tasks[0].input_values;

            return '|| ' + [
                name,
                comment,
                formatBoolean(checkboxes.labelBug),
                formatBoolean(checkboxes.displayBug),
                formatBoolean(checkboxes.missingElement),
                formatBoolean(checkboxes.elementOverlap),
                formatBoolean(checkboxes.colorBug),
                formatBoolean(checkboxes.interactivityBug),
                `((${diffUrl} ссылка))`,
                `((${url} ссылка))`
            ].join(' | ') + ' ||';
        })
        .filter(Boolean)
        .join(' ');

    if (!parsedComments) {
        return '✅ Релиз протестирован, все правки корректны!'
    }

    return [
        '<{Комментарии из Толоки',
        `#| || **Тест** | **Комментарий** | **Баг подписи** | **Баг отображения** | **Пропал элемент** | **Наложение элемента** | **Баг цвета** | **Ошибка интерактивности** | **Дифф** | **Интерактив** || ${parsedComments} |#}>`
    ].join('\n');
}

function formatTestsResults(results, reportLink) {
    return [
        '<{Результаты тестов',
        '#|',
        '|| **Режим** | **Всего** | **Упавшие** | **Повторы** ||',
        `|| **((${reportLink}/day/index.html День))** | **${results.day.total}** | **${results.day.failed}** | **${results.day.retries}** ||`,
        `|| **((${reportLink}/night/index.html Ночь))** | **${results.night.total}** | **${results.night.failed}** | **${results.night.retries}** ||`,
        '|#}>'
    ].join('\n')
}

function formatBoolean(bool) {
    return bool ? '⛔️' : '✅';
}

module.exports = {
    formatCommentsFromToloka,
    formatTestsResults
};
