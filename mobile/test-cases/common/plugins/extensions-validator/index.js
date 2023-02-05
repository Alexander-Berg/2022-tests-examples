// Класс для создания замечаний.
class BadFormatError  {
    constructor(customKey, formatter, testCaseId, filepath) {
        this.customKey = customKey;
        this.formatter = formatter
        this.testCaseId = testCaseId
        this.file = filepath
    }

    // формируем текст ошибки.
    toString() {
        return `Ошибка в тест-кейсе '${this.testCaseId}': значение ключа '${this.customKey}' должно быть ${this.formatter.description}`
    }
};

/**
 * нижний регистр
 * латинские буквы или цифры
 * нижние подчёркивания (не более одного подряд, только в середине слов)
 */
class BadRegexpError {

    constructor(name, file) {
        this.name = name
        this.file = file
    }

    toString() {
        return `Некорректное имя ключа '${ this.name }'. Имя должно содержать только цифры, латинские буквы нижнего регистра и нижние подчеркивания.`
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
     * @returns {BadValueError[]}
     */
    exec(scenarios, tests) {
        const errors = [];
        const systemFields = [
            'hermioneTitle',
            'palmsync_synchronization_errors',
            'browsers',
            'filepath',
            'scenarioType',
            'feature',
        ]
        const regex = new RegExp('^([a-z0-9]+_?)+(((?<=_)[a-z0-9])|[a-z0-9]*)$')

        // парсим заведённое нами поле schemeExtension и складываем все ключи в отдельный массив
        const schemeExtensions = this._config.schemeExtension

        const allowedKeys = []
        if (schemeExtensions !== undefined) {
            schemeExtensions.forEach(customKey => {
                if (customKey.meta == true) {
                    allowedKeys[customKey.name] = customKey.format

                    if (!regex.test(customKey.name) && !systemFields.includes(customKey.name)) {
                        errors.push(new BadRegexpError(customKey.name, '.palmsync.conf.js'))
                    }
        		}
            })
        }

        // проверяем валидность каждого тестового сценария
        for (var i = 0; i < scenarios.length; i++) {
        	const file = scenarios[i]
        	const testCases = file.specs

            for (const testCaseId in testCases) {
                const testCase = testCases[testCaseId]

                testCase.forEach(step => {
                    const stepKey = Object.keys(step)[0]
                    const stepValue = step[stepKey]

                    if (Object.keys(allowedKeys).includes(stepKey)) {
                        const formatter = allowedKeys[stepKey]
                        if (formatter(stepValue) !== true) {
                            errors.push(new BadFormatError(stepKey, formatter, testCaseId, file.filepath))
                        }

                        if (typeof stepValue === 'string' || stepValue instanceof String) {
                            if (!regex.test(stepValue)) {
                                errors.push(new BadRegexpError(stepValue, file.filepath))
                            }
                        } else if (Array.isArray(stepValue)) {
                            stepValue.forEach(value => {
                                if (!regex.test(value)) {
                                    errors.push(new BadRegexpError(value, file.filepath))
                                }
                            })
                        } else {
                            console.log(`unknown type of value for key ${stepKey}`)
                        }

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
