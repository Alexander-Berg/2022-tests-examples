const React = require('react');
const { shallow } = require('enzyme');

const ListingItemServices = require('./ListingItemServices');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const userMock = require('auto-core/react/dataDomain/user/mocks').default;

let props;
let initialState;

beforeEach(() => {
    initialState = {
        bunker: getBunkerMock([ 'common/vas' ]),
        user: userMock.value(),
    };
    props = {
        category: 'cars',
        linkParams: {},
    };
});

it('для объявления с короной добавить попап с сервисом "поднятия в топ"', () => {
    props.hasCrown = true;
    const page = shallowRenderComponent({ initialState, props });
    const popup = page.find('Connect(ListingItemServicePopup)');

    expect(popup.prop('type')).toBe('top');
});

it('для пропа hasServiceFresh попап с сервисом "поднятие в поиске"', () => {
    props.hasServiceFresh = true;
    const page = shallowRenderComponent({ initialState, props });
    const popup = page.find('Connect(ListingItemServicePopup)');

    expect(popup.prop('type')).toBe('fresh');
});

it('если нет ни короны ни поднятния в поиске, вернет пустоту', () => {
    const page = shallowRenderComponent({ initialState, props });

    expect(page.isEmptyRender()).toBe(true);
});

it('если сортировка ни по дате ни по актуальности, вернет пустоту', () => {
    props.sort = 'foo';
    props.hasCrown = true;
    const page = shallowRenderComponent({ initialState, props });

    expect(page.isEmptyRender()).toBe(true);
});

function shallowRenderComponent({ initialState, props }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <ListingItemServices { ...props } store={ store }/>
        </ContextProvider>,
    ).dive();
}
