const React = require('react');
const { shallow } = require('enzyme');

const CatalogEquipmentFilter = require('./CatalogEquipmentFilter');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const groupMock = require('auto-core/models/equipment/mocks/group.mock').default;

let defaultProps;

beforeEach(() => {
    defaultProps = {
        isAlwaysEnabled: false,
        group: {},
        onChange: jest.fn(),
        value: [],
        useMultiplicityFlag: false,
    };
});

describe('тип селекта', () => {
    it('если передан флаг useMultiplicityFlag и в группе доступен мультиселект, будет check', () => {
        const props = {
            ...defaultProps,
            useMultiplicityFlag: true,
            group: groupMock.withMultipleChoice(true).value(),
        };
        const page = shallowRenderComponent({ props });
        const menu = page.find('Menu');

        expect(menu.prop('mode')).toBe('check');
    });

    it('если передан флаг useMultiplicityFlag и в группе не доступен мультиселект, будет radio-check', () => {
        const props = {
            ...defaultProps,
            useMultiplicityFlag: true,
            group: groupMock.withMultipleChoice(false).value(),
        };
        const page = shallowRenderComponent({ props });
        const menu = page.find('Menu');

        expect(menu.prop('mode')).toBe('radio-check');
    });

    it('если не передан флаг useMultiplicityFlag, будет check', () => {
        const props = {
            ...defaultProps,
            useMultiplicityFlag: false,
            group: groupMock.withMultipleChoice(false).value(),
        };
        const page = shallowRenderComponent({ props });
        const menu = page.find('Menu');

        expect(menu.prop('mode')).toBe('check');
    });
});

function shallowRenderComponent({ context, props }) {
    const ContextProvider = createContextProvider(context || contextMock);

    return shallow(
        <ContextProvider>
            <CatalogEquipmentFilter { ...props }/>
        </ContextProvider>,
    ).dive().dive();
}
