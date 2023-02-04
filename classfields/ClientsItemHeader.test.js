const React = require('react');
const ClientsItemHeader = require('./ClientsItemHeader');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

it('должен вернуть ClientsItemHeader', () => {
    const client = {
        origin: 'origin',
        name: 'name',
        average: 0,
        balance: 0,
        paid_till: '2020-03-01',
        create_date: '2020-03-01',

    };
    const autopay = {};

    const clientsItemHeader = shallowToJson(shallow(
        <ClientsItemHeader
            client={ client }
            autopay={ autopay }
        />,
    ));

    expect(clientsItemHeader).toMatchSnapshot();
});

it('onObserverChange: должен вызвать getAverageAndBalance и установить корретный стейт', () => {
    const client = {
        status: 'freezed',
        id: 'clientId',
        origin: 'origin',
        name: 'name',
        average: 0,
        balance: 0,
        paid_till: '2020-03-01',
        create_date: '2020-03-01',
    };
    const autopay = {
        minValue: 20,
    };
    const getAverageAndBalance = jest.fn(() => Promise.resolve({
        balance: 200,
        average_outcome: 500,
    }));
    const clientsItemHeaderInstance = shallow(
        <ClientsItemHeader
            client={ client }
            autopay={ autopay }
            getAverageAndBalance={ getAverageAndBalance }
        />,
    ).instance();

    return clientsItemHeaderInstance.onObserverChange(true).then(() => {
        expect(getAverageAndBalance).toHaveBeenCalledWith('clientId');
        expect(clientsItemHeaderInstance.state).toEqual({
            average: 500,
            balance: 200,
        });
    });

});
