const React = require('react');
const ClientsItemContent = require('./ClientsItemContent');
const { shallow } = require('enzyme');

const MockDate = require('mockdate');

afterEach(() => {
    MockDate.reset();
});

it('onObserverChange: должен вызвать getChartData с корретными параметрами', async() => {
    MockDate.set('2020-03-18');
    const client = {
        id: 'clientId',
    };
    const getChartData = jest.fn(() => Promise.resolve({
        total_stats: {
            total: {
                sum: 200,
            },
        },
        daily_stats: [ 1, 2, 3 ],
    }));
    const clientsItemContent = shallow(
        <ClientsItemContent
            client={ client }
            getChartData={ getChartData }
        />,
    );

    clientsItemContent.find('InView').simulate('change', true);

    await clientsItemContent.update();

    expect(getChartData).toHaveBeenCalledWith({
        dealer_id: 'clientId',
        from: '2020-02-18',
        to: '2020-03-18',
    });

    expect(clientsItemContent.state().costs).toBe(200);
    expect(clientsItemContent.state().dailyStats).toEqual([ 1, 2, 3 ]);

});

it('должен вернуть ClientsItemContent', () => {
    MockDate.set('2020-03-18');
    const client = {
        id: 'clientId',
    };
    const clientsItemContent = shallow(
        <ClientsItemContent
            client={ client }
        />,
    );

    expect(clientsItemContent.find('.ClientsItemContent')).toExist();
});
