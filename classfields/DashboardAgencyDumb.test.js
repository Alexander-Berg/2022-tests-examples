const React = require('react');
const DashboardAgencyDumb = require('./DashboardAgencyDumb');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const baseProps = {
    isLoading: false,
    listing: [ { clientData: { id: 'clientId' } } ],
    clientsPresets: [
        {
            name: 'Все',
            facet: 'all',
            count: 155,
        },
    ],
    pagination: {
        page_num: 1,
        total_page_count: 10,
        page_size: 10,
    },
};

it('должен вернуть набор компонетов: графики, фильтры, сортировки, листинг, пагинацию и модал автопродления', () => {
    expect(shallowToJson(shallow(
        <DashboardAgencyDumb
            { ...baseProps }
        />,
    ))).toMatchSnapshot();
});
