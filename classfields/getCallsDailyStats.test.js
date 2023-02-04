const getCallsDailyStats = require('./getCallsDailyStats');

const FIELD_NAMES = require('www-cabinet/data/calls/filter-call-field-names.json');

it('должен проставлять суммы для элементов статистики и обогащать информацией о продукте (название / цвет / лейбл)', () => {
    const state = {
        calls: {
            callsTotalStats: {
                calls_by_day: [
                    {
                        day: '2020-01-19T15:00:20.021Z',
                        succeed_calls_amount: 14,
                        failed_calls_amount: 4,
                    },
                    {
                        day: '2020-01-20T15:00:20.021Z',
                        succeed_calls_amount: 11,
                        failed_calls_amount: 1,
                    },
                    {
                        day: '2020-01-21T15:00:20.021Z',
                        succeed_calls_amount: 9,
                        failed_calls_amount: 4,
                    },
                ],
            },
            filters: {
                [FIELD_NAMES.DATE_TO]: '2020-01-22',
                [FIELD_NAMES.DATE_FROM]: '2020-01-19',
            },
        },
    };

    const result = getCallsDailyStats(state);

    expect(result).toMatchSnapshot();
});
