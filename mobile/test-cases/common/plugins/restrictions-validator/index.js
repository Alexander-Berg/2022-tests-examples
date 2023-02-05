const restrictions = require('../../.common.restrictions.js')

class RestrictedWordError {

    constructor(word, file) {
        this.word = word
        this.file = file
    }

    toString() {
        return `Запрещённое слово '${ this.word }'. Запрещено использовать слова, определённые в файле .common.restriction.js`
    }

}

class EmptyKeyError {

    constructor(key, file) {
        this.key = key
        this.file = file
    }

    toString() {
        return `Пустое описание ключа '${ this.key }'`
    }

}

// Класс валидатора, который будет проверять тесты или yaml-файлы.
class Validator {
    /**
     * @param {Object} config — конфиг .palmsync.conf.js.
     */
    constructor(config) {
        this._config = config;
    }

    /**
     * Валидировать можно как сырые YAML-файлы, так и уже распаршенные тест-кейсы.
     *
     * @param {Object[]} scenarios — YAML-файлы в формате JSON.
     * @param {Object[]} tests — набор тест-кейсов.
     * @returns {RestrictedWordError[]}
     */
    exec(scenarios, tests) {
        const errors = [];
        
        const restrictedWords = restrictions.words.map(w => w.toLowerCase())

        function checkStringForRestrictedWords(string, file) {
            const lowercased = string.toLowerCase()
            restrictedWords.forEach(word => {
                if (lowercased.includes(word)) {
                    errors.push(new RestrictedWordError(word, file.filepath))
                }
            })
        }

        // проверяем валидность каждого тестового сценария
        for (var i = 0; i < scenarios.length; i++) {
            const file = scenarios[i]
            const testCases = file.specs

            for (const testCaseId in testCases) {
                const testCase = testCases[testCaseId]

                //checkStringForRestrictedWords(testCaseId, file) //пока отключил валидацию, включу после решения этой задачки https://st.yandex-team.ru/MAPSMOBILETEST-3439

                testCase.forEach(step => {
                    const stepKey = Object.keys(step)[0]
                    const stepValue = step[stepKey]

                    if (typeof stepValue === 'string' || stepValue instanceof String) {
                        if (stepValue === "") {
                            errors.push(new EmptyKeyError(stepKey, file.filepath))
                        }
                        //else {
                        //     checkStringForRestrictedWords(stepValue, file)
                        // }
                    }
                })
            }
        }

        return errors;
    }

};

// Тело плагина, аналогичное как и у плагинов для синхронизации.
module.exports = (palmsync, opts = {}) => {
    // Подписываемся на событие в валидаторе palmsync
    // Обработчик принимает на входе 2 аргумента
    // registry — реестр плагинов, сюда надо положить инициализированный валидатор, например инстанс класса Validator.
    // options — опции, содержат конфиг .palmsync.conf.js
    palmsync.on(palmsync.events.INIT_SCENARIOS_VALIDATOR, (registry, options) => {
        registry.add(new Validator(options.config));
    });
};
