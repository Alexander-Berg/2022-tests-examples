const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const SalesItemUnfoldedMenu = require('./SalesItemUnfoldedMenu');

let store;

const config = {
    data: {
        host: 'auto.ru',
    },
};

const offer = {
    seoTitle: 'seoTitle',
    id: '123456',
    hash: 'aaa',
    section: 'used',
};

it('должен правильно отрендерить меню для оффера cars', () => {
    store = mockStore({
        config: config,
    });
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemUnfoldedMenu offer={{
        ...offer,
        category: 'cars',
        counters: {},
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить меню для оффера cars с историей звонков', () => {
    store = mockStore({
        config: config,
    });
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemUnfoldedMenu offer={{
        ...offer,
        category: 'cars',
        counters: { calls_all: 8 },
        seller: { redirect_phones: true },
        seller_type: 'PRIVATE',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить меню для оффера moto', () => {
    store = mockStore({
        config: config,
    });
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemUnfoldedMenu offer={{
        ...offer,
        category: 'moto',
        counters: { calls_all: 8 },
        seller: { redirect_phones: true },
        seller_type: 'PRIVATE',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно обработать клик на снятие с продажи', () => {
    const expectedActions = [ { payload: { category: 'testCategory', offerID: '123456-aaa' }, type: 'OPEN_HIDE_OFFER_MODAL' } ];
    store = mockStore({
        config: config,
    });
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemUnfoldedMenu offer={{
        ...offer,
        category: 'testCategory',
        saleId: '123456-aaa',
        counters: {},
    }}/>, { context: context }).dive();
    wrapper.find('Link[children="Снять с продажи"]').simulate('click');
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен открывать и закрывать поп-ап с историей звонков', () => {
    store = mockStore({
        config: config,
    });
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemUnfoldedMenu offer={{
        ...offer,
        category: 'cars',
        counters: { calls_all: 8 },
        seller: { redirect_phones: true },
        seller_type: 'PRIVATE',
    }}/>, { context: context }).dive();
    expect(wrapper.state().callsHistoryModal).toBe(false);
    wrapper.find('Link').filterWhere(n => n.contains('История звонков')).simulate('click', { preventDefault: () => {} });
    expect(wrapper.state().callsHistoryModal).toBe(true);
    wrapper.find('OfferCallHistoryModal').simulate('requestHide');
    expect(wrapper.state().callsHistoryModal).toBe(false);
});
