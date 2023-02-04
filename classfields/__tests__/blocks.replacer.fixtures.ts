export const fixtures = {
    'BlocksReplacer Метод replaceManyBlocks Корректно возвращает блоки неизменными При отсутствии текстовых блоков': {
        blocks: [
            {
                type: 'code',
                code: '<code></code>',
            },
            {
                type: 'table',
                table: {
                    title: `Заголовок таблицы`,
                    data: [['Строка 1'], ['Строка 2']],
                },
            },
        ],
    },
    'BlocksReplacer Метод replaceManyBlocks Корректно возвращает блоки неизменными При нулевом лимите': {
        blocks: [
            {
                type: 'text',
                text: 'Текст текстового блока',
            },
        ],
    },
    'BlocksReplacer Метод replaceManyBlocks Корректно возвращает блоки неизменными При отсутствии параметров': {
        blocks: [
            {
                type: 'text',
                text: 'Текст текстового блока',
            },
        ],
    },

    'BlocksReplacer Метод replaceOneBlock Корректно заменяет ссылки В блоке с типом text': {
        block: {
            type: 'text',
            text: 'Текст в блоке text',
        },
    },
    'BlocksReplacer Метод replaceOneBlock Корректно заменяет ссылки В блоке с типом card': {
        block: {
            type: 'card',
            card: { text: 'Текст в блоке card' },
        },
    },
    'BlocksReplacer Метод replaceOneBlock Корректно заменяет ссылки В блоке с типом bubble': {
        block: {
            type: 'bubble',
            bubble: { content: 'Текст в блоке bubble' },
        },
    },
    'BlocksReplacer Метод replaceOneBlock Корректно возвращает ошибку При передаче недопустимого типа блока': {
        block: {
            type: 'UNSOPPERTED',
            text: '',
        },
    },
};
