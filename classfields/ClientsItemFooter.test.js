const React = require('react');
const ClientsItemFooter = require('./ClientsItemFooter');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const ContextProvider = createContextProvider({
    linkCabinet: () => 'linkToCabinet',
    linkOld: () => 'linkOld',
});

it('Должен вернуть набор кнопок для объявления', () => {
    const client = {
        id: 'clientId',
    };

    const clientsItemFooter = shallowToJson(shallow(
        <ContextProvider>
            <ClientsItemFooter
                client={ client }
                userId="userId"
                offersCount={ 45 }
                isFreezed={ false }
            />
        </ContextProvider>,
    ).dive());

    expect(clientsItemFooter).toMatchSnapshot();
});

it('deactivateClient: должен вызвать changeStatus с набором параметров', () => {
    const changeStatus = jest.fn();
    const client = {
        agent_id: 'agentId',
        id: 'clientId',
    };

    const clientsItemFooterInstance = shallow(
        <ContextProvider>
            <ClientsItemFooter
                agentId="agentId"
                client={ client }
                userId="userId"
                offersCount={ 45 }
                isFreezed={ false }
                changeStatus={ changeStatus }
            />
        </ContextProvider>,
    ).dive().instance();

    clientsItemFooterInstance.deactivateClient();

    expect(changeStatus).toHaveBeenCalledWith({
        userId: 'userId',
        agency_id: 'agentId',
        client_id: 'clientId',
        status: 'stopped',
    });
});

it('activateClient: должен вызвать changeStatus с набором параметров', () => {
    const changeStatus = jest.fn();
    const client = {
        agent_id: 'agentId',
        id: 'clientId',
    };

    const clientsItemFooterInstance = shallow(
        <ContextProvider>
            <ClientsItemFooter
                agentId="agentId"
                client={ client }
                userId="userId"
                offersCount={ 45 }
                isFreezed={ false }
                changeStatus={ changeStatus }
            />
        </ContextProvider>,
    ).dive().instance();

    clientsItemFooterInstance.activateClient();

    expect(changeStatus).toHaveBeenCalledWith({
        userId: 'userId',
        agency_id: 'agentId',
        client_id: 'clientId',
        status: 'active',
    });
});

describe('renderActionButton тесты', () => {
    it('должен вернуть кнопку декативации, если client.status === active', () => {
        const client = {
            status: 'active',
        };

        const clientsItemFooterInstance = shallow(
            <ContextProvider>
                <ClientsItemFooter
                    client={ client }
                />
            </ContextProvider>,
        ).dive().instance();

        expect(clientsItemFooterInstance.renderActionButton()).toMatchSnapshot();
    });

    it('должен вернуть кнопку активации, если client.status === stopped', () => {
        const client = {
            status: 'stopped',
        };

        const clientsItemFooterInstance = shallow(
            <ContextProvider>
                <ClientsItemFooter
                    client={ client }
                />
            </ContextProvider>,
        ).dive().instance();

        expect(clientsItemFooterInstance.renderActionButton()).toMatchSnapshot();
    });

    it('должен вернуть пустой контейнер для других статусов клиентов', () => {
        const client = {
            status: 'disabled',
        };

        const clientsItemFooterInstance = shallow(
            <ContextProvider>
                <ClientsItemFooter
                    client={ client }
                />
            </ContextProvider>,
        ).dive().instance();

        expect(clientsItemFooterInstance.renderActionButton()).toMatchSnapshot();
    });
});

it('renderMimicButton: должен нарисовать Button, если у клиента несколько юзеров', () => {
    const client = {
        users: [
            { id: 111, email: '111@yandex.ru' },
            { id: 222, email: '222@yandex.ru' },
        ],
    };

    const clientsItemFooterInstance = shallow(
        <ContextProvider>
            <ClientsItemFooter
                client={ client }
            />
        </ContextProvider>,
    ).dive().instance();

    expect(clientsItemFooterInstance.renderMimicButton()).toMatchSnapshot();
});
