const preparer = require('./promocoder');

const CURRENT_YEAR = new Date().getFullYear();
const MOCKS = [
    {
        data: [ {} ],
        result: [],
    },
    {
        data: [
            {
                id: 'id',
                tag: 'boost',
                jsonPayload: {
                    discount: {
                        discountType: 'money',
                        value: 99,
                    },
                },
                count: 10,
            },
        ],
        result: [
            {
                id: 'id',
                tag: 'boost',
                label: '99\u00a0₽, осталось 10',
                name: 'Поднятие в поиске',
                count: '10 шт.',
                count_original: 10,
                deadline: undefined,
                value: '99 ₽',
            },
        ],
    },
    {
        data: [
            {
                id: 'id',
                tag: 'offers-history-reports-1',
                jsonPayload: {
                    discount: {
                        value: 50,
                        discountType: 'percent',
                    },
                },
                count: 1,
            },
        ],
        result: [
            {
                id: 'id',
                tag: 'offers-history-reports-1',
                label: 'скидка 50%, осталось 1',
                name: 'История по VIN',
                count: '1 шт.',
                count_original: 1,
                deadline: undefined,
                value: 'Скидка 50%',
            },
        ],
    },
    {
        data: [
            {
                id: 'id',
                tag: 'offers-history-reports-10',
                jsonPayload: {
                    discount: {
                        value: 50,
                        discountType: 'percent',
                    },
                },
                count: 1,
            },
        ],
        result: [
            {
                id: 'id',
                tag: 'offers-history-reports-10',
                label: 'скидка 50%, осталось 1',
                name: 'Пакет из 10 историй по VIN',
                count: '1 шт.',
                count_original: 1,
                deadline: undefined,
                value: 'Скидка 50%',
            },
        ],
    },
    {
        data: [
            {
                id: 'id',
                tag: 'cashback',
                jsonPayload: {
                    unit: 'money',
                },
                count: 15000,
            },
        ],
        result: [
            {
                id: 'id',
                tag: 'cashback',
                label: '150\u00a0₽',
                name: 'CASHBACK',
                count: undefined,
                count_original: 15000,
                deadline: undefined,
                value: '150\u00a0₽',
            },
        ],
    },
    {
        data: [
            {
                id: 'id',
                tag: 'cashback',
                jsonPayload: {
                },
                count: 15000,
            },
        ],
        result: [],
    },
    {
        data: { error: 'error' },
        result: [],
    },
    {
        data: [
            {
                id: 'id',
                tag: 'vip-package',
                jsonPayload: {
                    discount: {
                        value: 100,
                        discountType: 'percent',
                    },
                },
                count: 1,
            },
        ],
        result: [
            {
                id: 'id',
                tag: 'vip-package',
                label: 'скидка 100%, осталось 1',
                name: 'VIP',
                count: '1 шт.',
                count_original: 1,
                deadline: undefined,
                value: 'Скидка 100%',
            },
        ],
    },
];

for (const mock of MOCKS) {
    it(JSON.stringify(mock.data), () => expect(preparer(mock.data)).toEqual(mock.result));
}

describe('Форматирование промо фич', () => {
    describe('Правильно форматирует фирчу "Выделение цветом"', () => {
        const data = [
            {
                id: 'id',
                tag: 'highlighting',
                jsonPayload: {
                    discount: {
                        value: 33,
                        discountType: 'percent',
                    },
                },
                count: 4,
            },
        ];

        const result = [
            {
                id: 'id',
                tag: 'highlighting',
                label: 'скидка 33%, осталось 4',
                name: 'Выделение цветом',
                value: 'Скидка 33%',
                count: '4 шт.',
                count_original: 4,
                deadline: undefined,
            },
        ];

        it('без даты', () => {
            expect(preparer(data)).toEqual(result);
        });

        it('с регулярной датой', () => {
            const dataModified = [ { ...data[0], deadline: `1990-05-19T18:13:01.822+03:00` } ];
            const resultModified = [ { ...result[0], deadline: 'Активно до 19 мая 1990 г.', deadline_origin: '1990-05-19T18:13:01.822+03:00' } ];
            expect(preparer(dataModified)).toEqual(resultModified);
        });

        it('отбрасывает год, если год равен текущему', () => {
            const dataModified = [ { ...data[0], deadline: `${ CURRENT_YEAR }-05-19T18:13:01.822+03:00` } ];
            const resultModified = [ { ...result[0], deadline: 'Активно до 19 мая', deadline_origin: `${ CURRENT_YEAR }-05-19T18:13:01.822+03:00` } ];
            expect(preparer(dataModified)).toEqual(resultModified);
        });
    });
});
