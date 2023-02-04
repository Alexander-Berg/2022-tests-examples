const React = require('react');
const ClientsItemPhones = require('./ClientsItemPhones');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

it('должен вернуть список контактов', () => {
    const phones = [
        { contact_name: 'менеджер', phone: '+7 111 111 11 11' },
        { contact_name: 'менеджер', phone: '+7 222 222 22 22' },
        { contact_name: 'менеджер 2', phone: '+7 333 333 33 33' },
    ];

    const clientsItemPhones = shallowToJson(shallow(
        <ClientsItemPhones
            phones={ phones }
        />,
    ));

    expect(clientsItemPhones).toMatchSnapshot();
});
