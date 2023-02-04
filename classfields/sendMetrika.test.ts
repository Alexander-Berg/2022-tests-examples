import type TContext from 'auto-core/types/TContext';

import sendMetrics from './sendMetrika';

const params = [ 'alpha', 'betta', 'gamma' ];

const sendParams = jest.fn();

const context = {
    pageParams: {},
    metrika: {
        sendParams,
    },
} as unknown as TContext;

it('с категорией прокинет категорию', () => {
    const contextWithCategory = {
        ...context,
        pageParams: {
            category: 'used',
        },
    };
    sendMetrics(contextWithCategory, params);
    expect(sendParams.mock.calls[0][0]).toEqual([ 'sales', 'used', ...params ]);
});

it('без категории подставит all', () => {
    sendMetrics(context, params);
    expect(sendParams.mock.calls[0][0]).toEqual([ 'sales', 'all', ...params ]);
});
