const React = require('react');
const SaveListingContext = require('./SaveListingContext');

const MockDate = require('mockdate');

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const listingMock = require('autoru-frontend/mockData/state/listing');

jest.mock('auto-core/react/lib/listingContext', () => {
    return {
        save: jest.fn(),
        getByKey: jest.fn(),
    };
});

const { getByKey, save } = require('auto-core/react/lib/listingContext');

const state = {
    listing: listingMock,
};

beforeEach(() => {
    MockDate.set('2018-01-05');
});

afterEach(() => {
    MockDate.reset();
});

it('должен построить и сохранить контекст при клике', () => {
    const wrapper = shallow(
        <SaveListingContext store={ mockStore(state) } listingKey={ 1234567890 }>
            <div offer={{ id: '123' }} index={ 3 }/>
        </SaveListingContext>,
    ).dive();
    wrapper.find('div').simulate('click', {}, { index: 3, offer: { id: 123 } });
    expect(save.mock.calls[0]).toMatchSnapshot();
});

it('должен взять контекст из ls по ключу, если такой уже есть', () => {
    getByKey.mockImplementation(() => ({ data: '123' }));
    const wrapper = shallow(
        <SaveListingContext store={ mockStore(state) } listingKey={ 1234567890 }>
            <div offer={{ id: '123' }} index={ 3 }/>
        </SaveListingContext>,
    ).dive();
    wrapper.find('div').simulate('click', {}, { index: 3, offer: { id: 123 } });
    expect(save.mock.calls[0]).toMatchSnapshot();
});
