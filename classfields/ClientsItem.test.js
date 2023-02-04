const React = require('react');
const ClientsItem = require('./ClientsItem');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

it('должен вернуть компонент c ClientsItemHeader, ClientsItemContent и ClientsItemFooter', () => {
    const client = {
        id: 'clientId',
    };
    const clientsItem = shallowToJson(shallow(
        <ClientsItem
            client={ client }
        />,
    ));

    expect(clientsItem).toMatchSnapshot();
});
