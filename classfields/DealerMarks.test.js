const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const DealerMarks = require('./DealerMarks');

const store = mockStore({
    dealersListing: {
        dealers: [
            { dealerId: 1, markId: 'bmw' },
            { dealerId: 2, markId: 'audi' },
            { dealerId: 3, markId: 'ferrari' },
            { dealerId: 4, markId: 'cadillac' },
            { dealerId: 5, markId: 'bmw' },
        ],
    },
    breadcrumbs: {
        marks: [
            { val: 'AC', count: 1 },
            { val: 'BMW', count: 2 },
            { val: 'AUDI', count: 3 },
            { val: 'FERRARI', count: 4 },
            { val: 'CADILLAC', count: 5 },
        ],
    },
});

it('правильно заполняет список марок', () => {
    const wrapper = shallow(
        <DealerMarks/>,
        { context: { ...contextMock, store } },
    ).dive();

    // проверяем, что лишние марки из breadcrumbs не попали и отображаются все, что есть у дилеров
    expect(wrapper.find('DealerMarksItem').map(item => item.props().id))
        .toEqual([ 'CADILLAC', 'FERRARI', 'AUDI', 'BMW' ]);
});
