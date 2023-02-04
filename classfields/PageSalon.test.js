/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const pageParamsMock = require('autoru-frontend/mockData/pageParams_cars.mock');
const bunkerMock = getBunkerMock([ 'common/metrics' ]);
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

const PageSalon = require('./PageSalon');

let state;
let props;
beforeEach(() => {
    state = {
        config: {
            data: {},
        },
        autoguru: {
            answerValues: [],
        },
        matchApplication: {},
        bunker: bunkerMock,
        geo: {
            gids: [],
        },
        listing: {
            data: {
                filteredOffersCount: 4,
                pagination: {},
                request_id: 'abc123',
                search_parameters: { },
            },
        },
        salonInfo: {
            data: {
                totalOffersCount: 1,
            },
        },
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        user: { data: {} },
    };
    props = {
        params: _.cloneDeep(pageParamsMock),
    };
});

it('должен отрендерить баннер на отсутствие авто у дилера', () => {
    state.salonInfo = {
        data: {
            totalOffersCount: 0,
        },
    };

    const page = shallowRenderComponent(state, props).dive().dive();
    const emptyListPlaceholder = page.find('SalonEmptyPlaceholder');

    expect(emptyListPlaceholder).toExist();
});

it('не должен рендерить баннер на отсутствие авто у дилера, если офферы есть', () => {

    const page = shallowRenderComponent(state, props).dive().dive();
    const emptyListPlaceholder = page.find('SalonEmptyPlaceholder');

    expect(emptyListPlaceholder).not.toExist();
});

it('не должен рендерить баннер на отсутствие авто у дилера, если категория не cars', () => {
    props.params.category = 'moto';

    const page = shallowRenderComponent(state, props).dive().dive();
    const emptyListPlaceholder = page.find('SalonEmptyPlaceholder');

    expect(emptyListPlaceholder).not.toExist();
});

function shallowRenderComponent(state, props) {
    const store = mockStore(state);
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <PageSalon
                { ...props }
                store={ store }
            />
        </ContextProvider>,
    );
}
