/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const _ = require('lodash');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const statApi = require('auto-core/lib/event-log/statApi').default;

jest.mock('../ListingItem/ListingItem', () => {
    // Сам ListingItem нам в этих тестах не интересен, поэтому мокаем его

    /* eslint-disable react/prop-types */
    const ListingItem = ({ offer }) => {
        return <div className="ListingItem">{ offer.id }</div>;
    };

    return {
        'default': ListingItem,
    };
});

jest.mock('auto-core/lib/event-log/statApi');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ListingCars = require('./ListingCars');

let props;
let state;
beforeEach(() => {
    const offers = Array(37).fill(undefined).map((value, index) => {
        return {
            id: `${ index }-hash`,
        };
    });
    const searchParameters = {
        catalog_filter: [
            { mark: 'BMW' },
        ],
        category: 'cars',
        section: 'all',
    };

    state = {
        bunker: {
            'common/metrics': {},
        },
        config: {
            data: {
                init: {},
            },
        },
        geo: {
            gids: [],
        },
        listing: {
            data: {
                offers: offers,
                request_id: 'abc123',
                search_parameters: searchParameters,
                pagination: {
                    current: 1,
                    page: 1,
                    page_size: 37,
                    total_page_count: 3,
                    total_offers_count: offers.length,
                },
            },
            searchID: 'listing-searchID',
        },
        state: {
            headerHeight: 52,
        },
    };

    props = {
        listingRequestId: 'abc123',
        mmmInfo: {
            mark: { id: 'BMW', cyrillic_name: 'БМВ', name: 'BMW' },
        },
        offers: state.listing.data.offers,
        pagination: state.listing.data.pagination,
        searchID: 'listing-searchID',
        searchParameters: state.listing.data.search_parameters,
        setCookieForever: _.noop,
    };
});

describe('Провязки', () => {
    describe('Новые авто у дилера в наличии', () => {
        it('не рисует провязку на пустом листинге', () => {
            props.offers = [];
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('WidgetNewOffers')).toHaveLength(0);
        });

        it('рисует провязку с правильными пропами', () => {
            props.pagination = {
                current: 2,
                page: 2,
                page_size: 19,
                total_page_count: 3,
                total_offers_count: 50,
            };
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('WidgetNewOffers')).toHaveProp('resourceName', 'listingCarsNewWithFallback');
            expect(wrapper.find('WidgetNewOffers')).toHaveProp('resourceParams', props.searchParameters);
        });

        it('рисует провязку после 20 объявления, если объявлений > 20', () => {
            expect.assertions(1);

            props.offers = props.offers.slice(0, 21);
            props.pagination = {
                current: 2,
                page: 2,
                page_size: 19,
                total_page_count: 3,
                total_offers_count: 50,
            };
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            let prevChild;
            wrapper.find('.Listing__page').children().forEach((child) => {
                if (child.is('WidgetNewOffers')) {
                    // eslint-disable-next-line jest/no-conditional-expect
                    expect(prevChild.find('ListingItem')).toHaveProp('index', 19);
                } else {
                    prevChild = child;
                }
            });
        });

        it('не рисует провязку на листинге новых легковых', () => {
            props.searchParameters.section = 'new';
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('WidgetNewOffers')).toHaveLength(0);
        });

        it('не рисует провязку на листинге мото', () => {
            props.searchParameters.category = 'moto';
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('WidgetNewOffers')).toHaveLength(0);
        });

        it('не рисует провязку на листинге комтс', () => {
            props.searchParameters.category = 'trucks';
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('WidgetNewOffers')).toHaveLength(0);
        });
    });

    describe('Видео', () => {
        it('не рисует провязку, если объявлений <= 25', () => {
            props.offers = props.offers.slice(0, 25);
            props.pagination = {
                current: 2,
                page: 2,
                page_size: 19,
                total_page_count: 3,
                total_offers_count: 50,
            };
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('RelatedVideo')).toHaveLength(0);
        });

        it('рисует провязку после 25 объявления', () => {
            expect.assertions(1);
            props.pagination = {
                current: 2,
                page: 2,
                page_size: 19,
                total_page_count: 3,
                total_offers_count: 50,
            };

            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            let prevChild;
            wrapper.find('.Listing__page').children().forEach((child) => {
                if (child.is('RelatedVideo')) {
                    // eslint-disable-next-line jest/no-conditional-expect
                    expect(prevChild.find('ListingItem')).toHaveProp('index', 24);
                } else {
                    prevChild = child;
                }
            });
        });

        it('должен отрисовать для CARS, передать значения МММ и категорию', () => {
            props.pagination = {
                current: 2,
                page: 2,
                page_size: 19,
                total_page_count: 3,
                total_offers_count: 50,
            };
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('RelatedVideo')).toHaveProp('mmmInfo', props.mmmInfo);
            expect(wrapper.find('RelatedVideo')).toHaveProp('topCategory', 'cars');
            expect(wrapper.find('RelatedVideo')).toHaveProp('from', 'listing');
        });

        it('должен отрисовать для MOTO, передать значения МММ и категорию', () => {
            props.pagination = {
                current: 2,
                page: 2,
                page_size: 19,
                total_page_count: 3,
                total_offers_count: 50,
            };
            props.searchParameters.category = 'moto';
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('RelatedVideo')).toHaveProp('mmmInfo', props.mmmInfo);
            expect(wrapper.find('RelatedVideo')).toHaveProp('topCategory', 'moto');
            expect(wrapper.find('RelatedVideo')).toHaveProp('from', 'listing');
        });
    });

    describe('Блок перелинковки', () => {
        it('рисует блок в конце листинга', () => {
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('.Listing').children().at(0).is('.Listing__page')).toEqual(true);
            expect(wrapper.find('.Listing').children().at(2).is('Connect(CrossLinks)')).toEqual(true);
        });

        it('не рисует на странице дилера', () => {
            props.searchParameters.dealer_code = 'code';
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('Connect(CrossLinks)')).toHaveLength(0);
        });

        it.each([
            [ 'moto' ],
            [ 'trucks' ],
        ])('не должен отрисовать блок для category=%s', (category) => {
            props.searchParameters.category = category;
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('Connect(CrossLinks)')).toHaveLength(0);
        });

        it.each([
            [ 'all' ],
            [ 'new' ],
            [ 'used' ],
        ])('должен передать правильные пропы для section=%s', (section) => {
            props.searchParameters.section = section;
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('Connect(CrossLinks)')).toMatchSnapshot();
        });

        it('должен передать правильные пропы для пустого листинга', () => {
            props.offers = [];
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('Connect(CrossLinks)')).toMatchSnapshot();
        });
    });

    describe('Измените параметры поиска', () => {

        it('должен нарисовать блок, если это легковые и в листинге всего одна страница', () => {
            props.searchParameters.category = 'cars';
            state.listing.data.pagination.total_page_count = 1;
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveLength(1);
            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveProp('params', props.searchParameters);
            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveProp('offersCount', 37);
        });

        it('не должен нарисовать блок, если для мото', () => {
            state.listing.data.pagination.total_page_count = 1;
            props.searchParameters.category = 'moto';
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveLength(0);
        });

        it('не должен нарисовать блок, если для комтс', () => {
            state.listing.data.pagination.total_page_count = 1;
            props.searchParameters.category = 'trucks';
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveLength(0);
        });

        it('должен нарисовать блок, если это легковые и нет офферов', () => {
            props.searchParameters.category = 'cars';
            props.offers = [];
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveLength(1);
            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveProp('params', props.searchParameters);
            expect(wrapper.find('ListingResetFiltersSuggest')).toHaveProp('offersCount', 37);
        });
    });

    describe('Спецпредложения', () => {
        it('должен передать контекст поиска и параметры для ресурсов', () => {
            props.pagination = {
                current: 2,
                page: 2,
                page_size: 19,
                total_page_count: 3,
                total_offers_count: 50,
            };
            props.offers = Array(38).fill(undefined).map((value, index) => {
                return {
                    id: `${ index }-hash`,
                };
            });
            const wrapper = shallowRenderListing(
                props,
                state,
                contextMock,
            );

            const widget = wrapper.find('WidgetSpecialOffers');
            expect(widget).toHaveProp({
                contextPage: 'PAGE_LISTING',
                resourceName: 'listingSpecial',
                resourceParams: state.listing.data.search_parameters,
                searchID: 'listing-searchID',
            });
        });
    });
});

describe('Вы посмотрели все предложения', () => {
    it('рисует блок после последнего снипета, если мы рисуем одну страницу', () => {
        state.listing.data.pagination.current = 1;
        state.listing.data.pagination.total_page_count = 1;
        const wrapper = shallowRenderListing(
            { ...props, type: ListingCars.TYPE.DEALER_PAGE },
            state,
            contextMock,
        );

        expect(wrapper.find('ListingEnding')).toHaveLength(1);

        let prevChild;
        wrapper.find('.Listing__page').children().forEach((child) => {
            if (child.is('ListingEnding')) {
                // eslint-disable-next-line jest/no-conditional-expect
                expect(prevChild.find('ListingItem')).toHaveProp('index', 36);
            } else {
                prevChild = child;
            }
        });
    });

    it('не рисует блок, если мы не последнюю страницу', () => {
        state.listing.data.pagination.current = 1;
        state.listing.data.pagination.total_page_count = 2;
        const wrapper = shallowRenderListing(
            { ...props, type: ListingCars.TYPE.DEALER_PAGE },
            state,
            contextMock,
        );

        expect(wrapper.find('ListingEnding')).toHaveLength(0);
    });

    it('рисует блок после последнего снипета, если мы рисуем много страниц', () => {
        const moreOffers = Array(37 * 2).fill(undefined).map((value, index) => {
            return {
                id: `${ index + 37 }-hash`,
            };
        });
        state.listing.data.offers.push(...moreOffers);
        state.listing.data.pagination.current = 3;
        state.listing.data.pagination.total_page_count = 3;
        state.listing.data.pagination.total_offers_count = state.listing.data.offers.length;
        const wrapper = shallowRenderListing(
            { ...props, type: ListingCars.TYPE.DEALER_PAGE },
            state,
            contextMock,
        );

        expect(wrapper.find('ListingEnding')).toHaveLength(1);
        let prevChild;
        wrapper.find('.Listing__page').children().forEach((child) => {
            if (child.is('ListingEnding')) {
                // eslint-disable-next-line jest/no-conditional-expect
                expect(prevChild.find('ListingItem')).toHaveProp('index', 37 * 3 - 1);
            } else {
                prevChild = child;
            }
        });
    });
});

describe('Бесконечный листинг', () => {
    beforeEach(() => {
        props.searchParameters.category = 'cars';
        props.searchParameters.section = 'used';
        state.listing.data.pagination.total_page_count = 1;
    });

    it('должен нарисовать блок, если это легковые, б/у и в листинге всего одна страница', () => {
        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(ListingInfiniteMobile)')).toHaveLength(1);
    });

    it('не должен нарисовать блок, если это легковые и в листинге больше одной страница', () => {
        state.listing.data.pagination.total_page_count = 2;
        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(ListingInfiniteMobile)')).toHaveLength(0);
    });

    it('не должен нарисовать блок для мото', () => {
        props.searchParameters.category = 'moto';
        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(ListingInfiniteMobile)')).toHaveLength(0);
    });

    it('не должен нарисовать блок для комтс', () => {
        props.searchParameters.category = 'trucks';
        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(ListingInfiniteMobile)')).toHaveLength(0);
    });

    it('не должен нарисовать блок на странице дилера', () => {
        props.searchParameters.dealer_code = 'code';
        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(ListingInfiniteMobile)')).toHaveLength(0);
    });
});

describe('отрисовка рекламы', () => {
    it('рисует рекламу, если офферов больше 3', () => {
        props.offers = props.offers.slice(0, 5);

        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(Ad)')).toExist();
    });

    it('рисует рекламу в экспе, если офферов больше 3', () => {
        props.offers = props.offers.slice(0, 5);

        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(Ad)')).toExist();
    });

    it('рисует рекламу, если офферов 3 или меньше', () => {
        props.offers = props.offers.slice(0, 3);

        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(wrapper.find('Connect(Ad)')).not.toExist();
    });
});

describe('эксперимент ABT_VS_678_PESSIMIZATION_BEATEN', () => {
    const expListing = [
        { id: '0', hash: 'hash', state: { state_not_beaten: false }, documents: {} },
        { id: '1', hash: 'hash', state: {}, documents: { custom_cleared: false } },
        { id: '2', hash: 'hash', state: {}, documents: {} },
    ];

    it('покажет тултипы, если все условия выполнены', () => {
        const searchParameters = { category: 'cars', section: 'used', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' };

        const wrapper = shallowRenderListing(
            { ...props, searchParameters, offers: expListing },
            state,
            { ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' },
        );

        expect(wrapper.state()).toEqual({
            adVisibility: {},
            firstBeatenOfferId: '0-hash',
            firstNotClearedCustomOfferId: '1-hash',
            searchID: 'listing-searchID',
        });
        expect(wrapper.find('ListingItem').at(0).props().hideButtonType).toEqual('damage_group');
        expect(wrapper.find('ListingItem').at(1).props().hideButtonType).toEqual('customs_state_group');
    });

    it('не покажет тултипы, если секция new', () => {
        const searchParameters = { category: 'cars', section: 'new', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' };

        const wrapper = shallowRenderListing(
            { ...props, searchParameters, offers: expListing },
            state,
            { ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' },
        );

        expect(wrapper.state()).toEqual({
            adVisibility: {},
            firstBeatenOfferId: null,
            firstNotClearedCustomOfferId: null,
            searchID: 'listing-searchID',
        });
        expect(wrapper.find('ListingItem').at(0).props().hideButtonType).toBeUndefined();
        expect(wrapper.find('ListingItem').at(1).props().hideButtonType).toBeUndefined();
    });

    it('не покажет тултипы, если не в экспе', () => {
        const searchParameters = { category: 'cars', section: 'used', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' };

        const wrapper = shallowRenderListing(
            { ...props, searchParameters, offers: expListing },
            state,
            contextMock,
        );

        expect(wrapper.state()).toEqual({
            adVisibility: {},
            firstBeatenOfferId: null,
            firstNotClearedCustomOfferId: null,
            searchID: 'listing-searchID',
        });
        expect(wrapper.find('ListingItem').at(0).props().hideButtonType).toBeUndefined();
        expect(wrapper.find('ListingItem').at(1).props().hideButtonType).toBeUndefined();
    });

    it('не покажет тултипы, если нет нужных customs_state_group и damage_group', () => {
        const searchParameters = { category: 'cars', section: 'used', damage_group: 'BEATEN', customs_state_group: 'CLEARED' };

        const wrapper = shallowRenderListing(
            { ...props, searchParameters, offers: expListing },
            state,
            { ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' },
        );

        expect(wrapper.state()).toEqual({
            adVisibility: {},
            firstBeatenOfferId: null,
            firstNotClearedCustomOfferId: null,
            searchID: 'listing-searchID',
        });
        expect(wrapper.find('ListingItem').at(0).props().hideButtonType).toBeUndefined();
        expect(wrapper.find('ListingItem').at(1).props().hideButtonType).toBeUndefined();
    });

    it('покажет тултипы, если после обновления фильтров компонент стал подходить условиям', () => {
        const searchParameters = { category: 'cars', section: 'used' };

        const wrapper = shallowRenderListing(
            { ...props, searchParameters, offers: expListing },
            state,
            { ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' },
        );

        expect(wrapper.state()).toEqual({
            adVisibility: {},
            firstBeatenOfferId: null,
            firstNotClearedCustomOfferId: null,
            searchID: 'listing-searchID',
        });
        expect(wrapper.find('ListingItem').at(0).props().hideButtonType).toBeUndefined();
        expect(wrapper.find('ListingItem').at(1).props().hideButtonType).toBeUndefined();

        wrapper.setProps({
            searchID: '123',
            searchParameters: {
                ...searchParameters,
                damage_group: 'ANY',
                customs_state_group: 'DOESNT_MATTER',
            },
        });

        wrapper.update();

        expect(wrapper.state()).toEqual({
            adVisibility: {},
            firstBeatenOfferId: '0-hash',
            firstNotClearedCustomOfferId: '1-hash',
            searchID: '123',
        });
        expect(wrapper.find('ListingItem').at(0).props().hideButtonType).toEqual('damage_group');
        expect(wrapper.find('ListingItem').at(1).props().hideButtonType).toEqual('customs_state_group');
    });
});

describe('эвентлог', () => {

    it('не отправит лог, если searchID одинаковый', () => {
        props.searchID = 'searchID';

        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(statApi.log).toHaveBeenCalledTimes(1);
        wrapper.update();
        expect(statApi.log).toHaveBeenCalledTimes(1);
    });

    it('отправит лог, если searchID отличается от предыдущего', () => {
        props.searchID = 'searchID';

        const wrapper = shallowRenderListing(
            props,
            state,
            contextMock,
        );

        expect(statApi.log).toHaveBeenCalledTimes(1);
        wrapper.setProps({ searchID: 'ne to' });
        wrapper.update();
        expect(statApi.log).toHaveBeenCalledTimes(2);
    });

});

function shallowRenderListing(props, state, context) {
    const store = mockStore(state);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <ListingCars { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
