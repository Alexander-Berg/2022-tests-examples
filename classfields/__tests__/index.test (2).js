const stylelint = require('stylelint');
const path = require('path');

const plugin = require('../index');
const ruleName = plugin.ruleName;

const getOptions = (code, disableFix = true) => {
    return {
        code,
        configBasedir: __dirname,
        fix: ! disableFix,
        config: {
            plugins: [ '../index' ],
            rules: {
                [ruleName]: [
                    {
                        source: path.join(__dirname, 'vars-mock.css'),
                        props: [
                            'font-size'
                        ]
                    },
                    {
                        disableFix
                    }
                ]
            }
        }
    };
};

describe('realty/use-guide-variables', () => {
    it('Работает только для перечисленных свойств', async() => {
        const code = 'div { font-size: 14px; border-radius: 8px; }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(true);
        expect(JSON.parse(result.output)).toMatchSnapshot();
    });

    it('Находит rgb в hex', async() => {
        const code = 'div { color: rgb(255, 0, 0); }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(true);
        expect(JSON.parse(result.output)).toMatchSnapshot();
    });

    it('Находит hex в rgb', async() => {
        const code = 'div { color: #00ff00 }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(true);
        expect(JSON.parse(result.output)).toMatchSnapshot();
    });

    it('Не различает длинную и короткую hex запись', async() => {
        const code = 'div { color: #0f0; background-color: #FF0000; }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(true);
        expect(JSON.parse(result.output)).toMatchSnapshot();
    });

    it('Не ищет цвета с alpha !== 1 в hex', async() => {
        const code = 'div { color: rgba(0, 0, 0, 0.92) }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(false);
    });

    it('Ищет цвета с alpha === 1 в hex', async() => {
        const code = 'div { color: rgba(0, 0, 0, 1) }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(true);
        expect(JSON.parse(result.output)).toMatchSnapshot();
    });

    it('Находит цвета внутри короткой записи css-правила', async() => {
        const code = 'div { border: 1px solid gray; }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(true);
        expect(JSON.parse(result.output)).toMatchSnapshot();
    });

    it('Игнорирует названия цветов в переменных', async() => {
        const code = 'div { color: var(--white); }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(false);
    });

    it('Игнорирует названия цветов url', async() => {
        const code = 'div { background-color: url(https://yandex.ru/#ff0000); }';
        const result = await stylelint.lint(getOptions(code));

        expect(result.errored).toBe(false);
    });
});

describe('realty/use-guide-variables fix', () => {
    const cases = [
        [
            'div { font-size: 14px; }',
            'div { font-size: var(--font-size-14); }'
        ],
        [
            'div { color: #0f0; }',
            'div { color: var(--green); }'
        ],
        [
            'div { border: 1px solid gray; }',
            'div { border: 1px solid var(--gray); }'
        ],
        [
            /* eslint-disable max-len */
            'div { background: linear-gradient(to right, rgba(255, 255, 255, 1), rgba(255, 255, 255, 1) 32px, rgba(255, 255, 255, 0)); }',
            'div { background: linear-gradient(to right, var(--white), var(--white) 32px, rgba(255, 255, 255, 0)); }'
            /* eslint-enable max-len */
        ],
        [
            'div { background-color: url(https://yandex.ru/#ff0000); }',
            'div { background-color: url(https://yandex.ru/#ff0000); }'
        ],
        [
            'div { color: var(--white); }',
            'div { color: var(--white); }'
        ]
    ];

    it('Правильно работает автофикс', async() => {
        /* eslint-disable no-await-in-loop */
        for (const testCase of cases) {
            const [ input, output ] = testCase;
            const result = await stylelint.lint(getOptions(input, false));

            expect(result.errored).toBe(false);
            expect(result.output).toBe(output);
        }
        /* eslint-disable no-await-in-loop */
    });
});
