jest.mock('auto-core/router/cabinet.auto.ru/link', () => jest.fn());

const React = require('react');
const _ = require('lodash');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const { shallow } = require('enzyme');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;

const linkCabinet = require('auto-core/router/cabinet.auto.ru/link');

linkCabinet.mockImplementation(contextMock.linkCabinet);

const ContextHOC = require('./ContextHoc');

describe('Context._contextLinkCabinet должен обогащать ссылку параметром client_id', () => {
    const Context = ContextHOC({
        metrikaContext: contextMock.metrika,
        seoSelector: _.noop,
    });

    it('если он есть в pageParams config.data', () => {

        const store = mockStore({
            config: configStateMock.withPageParams({ client_id: 123 }).value(),
        });

        const contextInstance = shallow(<Context store={ store }><div/></Context>).dive().instance();

        const url = contextInstance._contextLinkCabinet('sales', { foo: 'bar' });

        expect(url).toBe('linkCabinet/sales/?client_id=123&foo=bar');
    });

    it('если он есть в params', () => {
        const store = mockStore({
            config: configStateMock.value(),
        });

        const instance = shallow(
            <Context store={ store } params={{ client_id: 123 }}>
                <div/>
            </Context>,
        ).dive().instance();

        const url = instance._contextLinkCabinet('sales', { foo: 'bar' });

        expect(url).toBe('linkCabinet/sales/?client_id=123&foo=bar');
    });
});

describe('Context.hasBunkerFlag должен находить флаги', () => {
    const flag = 'test';
    const Context = ContextHOC({
        seoSelector: _.noop,
    });
    const state = {
        features: {
            [flag]: true,
        },
    };

    it('если он есть', () => {

        const store = mockStore(state);

        const contextInstance = shallow(<Context store={ store }><div/></Context>).dive().instance();

        const value = contextInstance._bunkerHasFlag(flag);

        expect(value).toBe(true);
    });

    it('если его нет', () => {

        const store = mockStore(state);

        const contextInstance = shallow(<Context store={ store }><div/></Context>).dive().instance();

        const value = contextInstance._bunkerHasFlag(flag + flag);

        expect(value).toBe(false);
    });
});
