const React = require('react');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

jest.mock('auto-core/react/dataDomain/sales/actions/fetchMoreOffers', () => {
    return {
        'default': jest.fn(() => ({ type: 'MOCK_ACTION' })),
    };
});

const fetchMoreOffers = require('auto-core/react/dataDomain/sales/actions/fetchMoreOffers').default;

const SalesPager = require('./SalesPager');

let store;

beforeEach(() => {
    store = mockStore({});
});

afterEach(() => {
    fetchMoreOffers.mockClear();
});

it('должен правильно отрендерить кнопку подгрузки, если осталось больше 10 офферов', () => {
    const offers = {
        pagination: {
            page: 1,
            page_size: 10,
            total_offers_count: 88,
            total_page_count: 8,
        },
    };
    const wrapper = shallow(<SalesPager offers={ offers } store={ store }/>).dive();
    expect(wrapper.find('.SalesPager').text()).toEqual('Показать ещё 10');
});

it('должен правильно отрендерить кнопку подгрузки, если осталось меньше 10 офферов', () => {
    const offers = {
        pagination: {
            page: 1,
            page_size: 10,
            total_offers_count: 18,
            total_page_count: 2,
        },
    };
    const wrapper = shallow(<SalesPager offers={ offers } store={ store }/>).dive();
    expect(wrapper.find('.SalesPager').text()).toEqual('Показать ещё 8');
});

it('должен правильно отрендерить кнопку подгрузки, если осталось 10 офферов', () => {
    const offers = {
        pagination: {
            page: 1,
            page_size: 10,
            total_offers_count: 20,
            total_page_count: 2,
        },
    };
    const wrapper = shallow(<SalesPager offers={ offers } store={ store }/>).dive();
    expect(wrapper.find('.SalesPager').text()).toEqual('Показать ещё 10');
});

it('не должен отрендерить кнопку подгрузки, если нет незагруженных офферов', () => {
    const offers = {
        pagination: {
            page: 2,
            page_size: 10,
            total_offers_count: 20,
            total_page_count: 2,
        },
    };
    const wrapper = shallow(<SalesPager offers={ offers } store={ store }/>).dive();
    expect(wrapper).toBeEmptyRender();
});

it('должен отрендерить лоадер, если офферы загружаются', () => {
    const offers = {
        pagination: {
            page: 1,
            page_size: 10,
            total_offers_count: 20,
            total_page_count: 2,
        },
        isLoading: true,
    };
    const wrapper = shallow(<SalesPager offers={ offers } store={ store }/>).dive();
    expect(wrapper.find('Spinner')).toExist();
});

it('должен вызывать подгрузку офферов по клику', () => {
    const offers = {
        pagination: {
            page: 1,
            page_size: 10,
            total_offers_count: 20,
            total_page_count: 2,
        },
    };
    const wrapper = shallow(<SalesPager offers={ offers } store={ store }/>).dive();
    expect(fetchMoreOffers.mock.calls).toHaveLength(0);
    wrapper.find('.SalesPager').simulate('click');
    expect(fetchMoreOffers.mock.calls).toHaveLength(1);
});
