const React = require('react');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CardInfoRow = require('./CardInfoRow');

const transportTaxMock = {
    name: 'transportTax',
    label: 'Налог',
    tooltip: 'Налог рассчитан для двигателя мощностью 240 л.с. в Санкт-Петербурге по тарифу 2018 года',
    value: '18 000 ₽ / год',
    fullName: 'Транспортный налог',
};

const electricRangeMock = {
    name: 'electricRange',
    label: 'Запас хода',
    tooltip: 'Максимальное расстояние, которое автомобиль может проехать на одном заряде аккумулятора.',
    value: '433 км',
    fullName: 'Запас хода',
};

const ContextProvider = createContextProvider(contextMock);

it('для транспортного налога добавит инфо-попап', () => {
    const tree = shallow(
        <ContextProvider>
            <CardInfoRow item={ transportTaxMock }/>
        </ContextProvider>,
    ).dive();
    expect(tree.find('.CardInfoRow__tooltip').exists()).toBe(true);
});

it('для транспортного налога в мобилке добавит модал', () => {
    const tree = shallow(
        <ContextProvider>
            <CardInfoRow item={ transportTaxMock } isMobile/>
        </ContextProvider>,
    ).dive();
    expect(tree.find('.CardInfoRow__modal_transportTax').exists()).toBe(true);
});

it('для запаса хода добавит инфо-попап', () => {
    const tree = shallow(
        <ContextProvider>
            <CardInfoRow item={ electricRangeMock }/>
        </ContextProvider>,
    ).dive();
    expect(tree.find('.CardInfoRow__tooltip').exists()).toBe(true);
});

it('для запаса хода в мобилке добавит модал', () => {
    const tree = shallow(
        <ContextProvider>
            <CardInfoRow item={ electricRangeMock } isMobile/>
        </ContextProvider>,
    ).dive();
    expect(tree.find('.CardInfoRow__modal_electricRange').exists()).toBe(true);
});

describe('для поля с вином', () => {
    const item = {
        name: 'vin',
        value: 'foo',
        label: 'vin',
    };

    it('поставит просто значение', () => {
        const tree = shallow(
            <ContextProvider>
                <CardInfoRow item={ item }/>
            </ContextProvider>,
        ).dive();
        const valueEl = tree.find('.CardInfoRow__cell').at(1);
        expect(valueEl).toMatchSnapshot();
    });

    it('с квотой поставит линк', () => {
        const vinReport = {
            billing: {
                quota_left: 3,
            },
        };

        const tree = shallow(
            <ContextProvider>
                <CardInfoRow item={ item } vinReport={ vinReport }/>
            </ContextProvider>,
        ).dive();
        const valueEl = tree.find('.CardInfoRow__cell').at(1);
        expect(valueEl).toMatchSnapshot();
    });

    it('без квоты поставит промо кнопку', () => {
        const vinReport = {
            billing: {},
        };

        const tree = shallow(
            <ContextProvider>
                <CardInfoRow item={ item } vinReport={ vinReport }/>
            </ContextProvider>,
        ).dive();
        const valueEl = tree.find('.CardInfoRow__cell').at(1);
        expect(valueEl).toMatchSnapshot();
    });
});
