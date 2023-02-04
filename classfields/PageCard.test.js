/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

const React = require('react');
const _ = require('lodash');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const MockDate = require('mockdate');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
import { showAutoclosableMessage, VIEW } from 'auto-core/react/dataDomain/notifier/actions/notifier';

const bunkerMock = getBunkerMock([ 'common/metrics', 'common/chat_preset_messages' ]);
const cardGroupComplectationsMock = require('autoru-frontend/mockData/state/cardGroupComplectations.mock');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const pageParamsMock = require('autoru-frontend/mockData/pageParams_cars.mock');
const getNumberOfViewedOffersForCurrentSession = require('auto-core/lib/viewedOffers/getNumberOfViewedOffersForCurrentSession').default;

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const { OfferStatus } = require('@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model');

const PageCard = require('./PageCard').default;

const Context = createContextProvider(contextMock);

const listing = {
    searchID: 'searchID',
};

jest.mock('auto-core/lib/viewedOffers/getNumberOfViewedOffersForCurrentSession');

let cardMock;
let defaultState;
let defaultProps;

beforeEach(() => {
    // дата важна для мока
    MockDate.set('2019-02-26T13:00:00.000+0300');
    cardMock = cloneOfferWithHelpers(require('auto-core/react/dataDomain/card/mocks/card.cars.mock'))
        .withTechParam({ id: '21221591' })
        .withComplectation({ name: 'Классик' })
        .value();

    defaultState = {
        ...cardGroupComplectationsMock,
        card: cardMock,
        config: {
            data: {},
        },
        bunker: bunkerMock,
        listing,
        user: { data: {} },
        offerStats: {},
    };

    defaultProps = {
        params: _.cloneDeep(pageParamsMock),
    };

});

afterEach(() => {
    jest.resetModules();
});

it('должен правильно передать пропы в CardViewEvents', () => {
    const props = { params: { tech_param_id: '21221591', complectation_id: '21221786' } };
    const tree = renderComponent(props, defaultState);

    expect(tree.find('CardViewEvents')).toHaveProp({
        groupingId: 'tech_param_id=21221591,complectation_id=21221786',
        offer: cardMock,
        groupSize: 3,
    });
});

it('при page_from=edit-page и заблокированном оффере должен открыть нотифаку', () => {
    const card = cloneOfferWithHelpers(require('auto-core/react/dataDomain/card/mocks/card.cars.mock'))
        .withTechParam({ id: '21221591' })
        .withComplectation({ name: 'Классик' })
        .withStatus(OfferStatus.BANNED)
        .value();

    const props = { params: { page_from: 'edit-page' } };
    const store = {
        ...defaultState,
        card,
    };

    renderComponent(props, store);

    expect(showAutoclosableMessage).toHaveBeenCalledWith({
        message: 'Спасибо. Модераторы проверят изменения и активируют объявление, если всё будет верно.',
        view: VIEW.SUCCESS });
});

it('отправит цель о просмотре 5 офферов за сессию', () => {
    getNumberOfViewedOffersForCurrentSession.mockReturnValueOnce(5);
    renderComponent(defaultProps, defaultState);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('SHOW_FIVE_CARD');

});

it('не отправит цель о просмотре 4 офферов за сессию', () => {
    getNumberOfViewedOffersForCurrentSession.mockReturnValueOnce(4);
    renderComponent(defaultProps, defaultState);
    expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('SHOW_FIVE_CARD');
});

function renderComponent(props, state) {
    const store = mockStore(state);
    const tree = shallow(
        <Provider store={ store }>
            <Context>
                <PageCard
                    { ...props }
                />
            </Context>
        </Provider>,
    ).dive().dive().dive();
    return tree;
}
