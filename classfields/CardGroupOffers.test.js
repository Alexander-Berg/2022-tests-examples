const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const cardGroupState = require('auto-core/react/dataDomain/cardGroup/mocks/state.mock');
const listingForGroup = require('autoru-frontend/mockData/state/listingForGroup.mock');

const CardGroupOffers = require('./CardGroupOffers');

let state;

beforeEach(() => {
    state = {
        cardGroupComplectations: {
            data: {},
        },
        listing: {
            data: {
                offers: [ cardMock, cardMock, cardMock, cardMock ],
                pagination: { total_offers_count: 4 },
            },
        },
        geo: {},
    };
});

describe('Блок ListingBestPriceMobile', () => {
    describe('рендеринг', () => {
        it('рендерит и прокидывает проп isFromCardGroup, если есть showMatchApplicationBlock', () => {
            const wrapper = shallowRenderComponent({ state, context: contextMock, showMatchApplicationBlock: true });

            const component = wrapper.find('Connect(ListingBestPriceMobile)');
            expect(component).toExist();
            expect(component).toHaveProp('isFromCardGroup', true);
        });

        it('не рендерит, если нет showMatchApplicationBlock', () => {
            const wrapper = shallowRenderComponent({ state, context: contextMock });

            const component = wrapper.find('Connect(ListingBestPriceMobile)');
            expect(component).not.toExist();
        });
    });

    describe('положение блока', () => {
        it('должен вставить провязку после 3 оффера', () => {
            const wrapper = shallowRenderComponent({ state, context: contextMock, showMatchApplicationBlock: true });

            const offersList = wrapper.find('.CardGroupOffers__offersList');

            expect(offersList.childAt(3).dive().is('Connect(ListingBestPriceMobile)')).toBe(true);
        });

        it('должен вставить провязку последней, если офферов меньше 3', () => {
            const currentState = { ...state, listing: { data: { ...state.listing.data, offers: [ cardMock ] } } };
            const wrapper = shallowRenderComponent({ state: currentState, context: contextMock, showMatchApplicationBlock: true });

            const offersList = wrapper.find('.CardGroupOffers__offersList');

            expect(offersList.childAt(1).dive().is('Connect(ListingBestPriceMobile)')).toBe(true);
        });
    });
});

describe('бесконечный листинг', () => {
    const initialState = {
        ...cardGroupState,
        geo: { gidsInfo: [] },
        listingLocatorCounters: {},
        listing: listingForGroup,
    };

    it('в карточке группы не должен рендерить БЛ при isPending', async() => {
        const stateWithPending = {
            ...initialState,
            listing: {
                ...listingForGroup,
                pending: true,
            },
        };
        const wrapper = shallowRenderComponent({ state: stateWithPending, context: contextMock });

        expect(wrapper.find('Connect(ListingInfiniteAbstract)')).not.toExist();
    });

    it('в карточке группы должен отрендерить бесконечный листинг', async() => {
        const wrapper = shallowRenderComponent({ state: initialState, context: contextMock });

        expect(wrapper.find('Connect(ListingInfiniteMobile)')).toExist();
    });

    it('с экспом должен отрендерить бесконечный листинг', async() => {
        const context = {
            ...contextMock,
            hasExperiment: (exp) => exp === 'AUTORUFRONT-18380_infinite_listing_mobile',
        };
        const wrapper = shallowRenderComponent({ state: initialState, context });

        expect(wrapper.find('Connect(ListingInfiniteMobile)')).toExist();
    });
});

const shallowRenderComponent = ({ context, showMatchApplicationBlock, state }) => {
    const pageParams = {
        transmission: 'AUTOMATIC',
        section: 'new',
        gear_type: 'ALL_WHEEL_DRIVE',
        category: 'cars',
        color: '200204',
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '345',
                complectation_name: 'Test',
            },
        ],
    };

    const Context = createContextProvider(context);
    const store = mockStore(state);

    return shallow(
        <Provider store={ store }>
            <Context>
                <CardGroupOffers pageParams={ pageParams } showMatchApplicationBlock={ showMatchApplicationBlock }/>
            </Context>
        </Provider>,
    ).dive().dive().dive();
};
