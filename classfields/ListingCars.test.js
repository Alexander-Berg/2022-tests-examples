/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

const React = require('react');
const _ = require('lodash');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

// Mocks
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const stateSupportDataMock = require('autoru-frontend/mockData/bunker/desktop/state_support.json');

//Components
const ListingCarouselSpecial = require('www-desktop/react/components/ListingCarouselSpecial');
const ListingCarouselSpecialNew = require('www-desktop/react/components/ListingCarouselSpecialNew');
const ListingCars = require('./ListingCars');

const statApi = require('auto-core/lib/event-log/statApi').default;

let ContextProvider;

const listingFlat = Array(15).fill({}).map((item, index) => ({ id: index }));
const listingGrouped = [ { id: 1, groupping_info: { groupping_params: { configuration_id: 777 }, grouping_id: 'configuration_id=777' } } ];
const listingMixed = [ listingFlat[0], listingGrouped[0] ];

const LISTING_OUTPUT_TYPE = require('auto-core/data/listing/OutputTypes').default;

beforeEach(() => {
    ContextProvider = createContextProvider(contextMock);
});

const defaultProps = {
    setCookieForever: _.noop,
    listingRequestId: 'abc123',
    sendMarketingEventByListingOffer: _.noop,
    sellerPopupOpen: _.noop,
    presentEquipment: [ {} ],
};

it('должен отрендерить офферы в табличном виде для табличного типа вывода', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingFlat.slice(0, 1) }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.TABLE }
                category="cars"
                stateSupportData={ stateSupportDataMock }
            />
        </ContextProvider>,
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить офферы в списочном виде для типа вывода "Список"', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingFlat.slice(0, 1) }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.LIST }
                category="cars"
            />
        </ContextProvider>,
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить офферы в сгруппированном виде для типа вывода "Модель"', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingGrouped }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.MODELS }
                category="cars"
            />
        </ContextProvider>,
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить офферы в списочном или сгруппированном виде в зависимости от наличия групповой информации в них для типа вывода "Модель"', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingMixed }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.MODELS }
                category="cars"
            />
        </ContextProvider>,
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить офферы в списочном виде в для типа вывода "Модель" и любой категории, крома легковых', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingGrouped }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.MODELS }
                category="trucks"
            />
        </ContextProvider>,
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен отрендерить карусель спецпредложений для пустого листинга', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listingRequestId="abc123"
                outputType={ LISTING_OUTPUT_TYPE.LIST }
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find(ListingCarouselSpecial)).toHaveLength(0);
    expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(0);
});

it('не должен отрендерить карусель спецпредложений для табличного вида', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingFlat }
                outputType={ LISTING_OUTPUT_TYPE.TABLE }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find(ListingCarouselSpecial)).toHaveLength(0);
    expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(0);
});

it('должен отрендерить карусель спецпредложений для б/у', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingFlat }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.LIST }
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find(ListingCarouselSpecial)).toHaveLength(1);
    expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(0);
});

it('должен отрендерить карусель спецпредложений для новых', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                category="cars"
                listing={ listingFlat }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                section="new"
                outputType={ LISTING_OUTPUT_TYPE.LIST }
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find(ListingCarouselSpecial)).toHaveLength(0);
    expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(1);
});

it('должен отрендерить карусель спецпредложений после 12 блоков, если нет блока расширения радиуса', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                category="cars"
                listing={ listingFlat }
                pagination={{ page: 1, total_offers_count: 50, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.LIST }
            />
        </ContextProvider>,
    ).dive();

    expect(tree.children().at(13).is(ListingCarouselSpecial)).toBe(true);
});

it('должен рендерить блок ListingItemPromo, если офферов больше 5', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                category="cars"
                listing={ listingFlat.slice(0, 7) }
                pagination={{ page: 1, total_offers_count: 15, total_page_count: 1 }}
                section="all"
                outputType={ LISTING_OUTPUT_TYPE.LIST }
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find('ListingItemPromo')).toExist();
});

it('не должен рендерить блок ListingItemPromo, если офферов 5 или меньше', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                category="cars"
                listing={ listingFlat.slice(0, 5) }
                pagination={{ page: 1, total_offers_count: 5, total_page_count: 2 }}
                section="all"
                outputType={ LISTING_OUTPUT_TYPE.LIST }
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find('Connect(ListingItemPromo)')).not.toExist();
});

it('прокинет реф только тому айтему, который указан в params.scrollToPosition', function() {
    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                listing={ listingFlat.slice(0, 2) }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.LIST }
                category="cars"
                params={{ scrollToPosition: 1 }}
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find('ListingItem').at(0).props().snippetRef).not.toBeNull();
    expect(tree.find('ListingItem').at(1).props().snippetRef).toBeNull();
});

describe('эксперимент ABT_VS_678_PESSIMIZATION_BEATEN', () => {
    const expListingFlat = Array(3).fill({}).map((item, index) => {
        if (index === 0) {
            return { id: index.toString(), hash: 'hash', state: { state_not_beaten: false }, documents: {} };
        }
        if (index === 1) {
            return { id: index.toString(), hash: 'hash', state: {}, documents: { custom_cleared: false } };
        }
        return { id: index.toString(), hash: 'hash', state: {}, documents: {} };
    });

    it('покажет тултипы, если все условия выполнены', () => {
        const searchParams = { category: 'cars', section: 'used', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' };
        const ContextProvider = createContextProvider({ ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' });
        const setCookieForeverMock = jest.fn();

        const tree = shallow(
            <ContextProvider>
                <ListingCars
                    { ...defaultProps }
                    listing={ expListingFlat }
                    pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                    outputType={ LISTING_OUTPUT_TYPE.LIST }
                    category="cars"
                    searchParams={ searchParams }
                    setCookieForever={ setCookieForeverMock }
                />
            </ContextProvider>,
        ).dive();

        expect(tree.state()).toEqual({
            firstBeatenOfferId: '0-hash',
            firstNotClearedCustomOfferId: '1-hash',
            viewedOffers: {},
        });
        expect(tree.find('ListingItem').at(0).props().hideButtonPopupType).toEqual('damage_group');
        expect(tree.find('ListingItem').at(1).props().hideButtonPopupType).toEqual('customs_state_group');
        expect(setCookieForeverMock).toHaveBeenCalledTimes(2);
        expect(setCookieForeverMock).toHaveBeenCalledWith('listing-tooltip-beaten-shown', 'true');
        expect(setCookieForeverMock).toHaveBeenCalledWith('listing-tooltip-customs-not-cleared-shown', 'true');
    });

    it('не покажет тултипы, если секция не та', () => {
        const searchParams = { category: 'cars', section: 'new', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' };
        const ContextProvider = createContextProvider({ ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' });

        const tree = shallow(
            <ContextProvider>
                <ListingCars
                    { ...defaultProps }
                    listing={ expListingFlat }
                    pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                    outputType={ LISTING_OUTPUT_TYPE.LIST }
                    category="cars"
                    searchParams={ searchParams }
                />
            </ContextProvider>,
        ).dive();

        expect(tree.find('ListingItem').at(0).props().hideButtonPopupType).toBeUndefined();
        expect(tree.find('ListingItem').at(1).props().hideButtonPopupType).toBeUndefined();
    });

    it('не покажет тултипы, если не в экспе', () => {
        const searchParams = { category: 'cars', section: 'used', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' };
        const ContextProvider = createContextProvider(contextMock);

        const tree = shallow(
            <ContextProvider>
                <ListingCars
                    { ...defaultProps }
                    listing={ expListingFlat }
                    pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                    outputType={ LISTING_OUTPUT_TYPE.LIST }
                    category="cars"
                    searchParams={ searchParams }
                />
            </ContextProvider>,
        ).dive();

        expect(tree.find('ListingItem').at(0).props().hideButtonPopupType).toBeUndefined();
        expect(tree.find('ListingItem').at(1).props().hideButtonPopupType).toBeUndefined();
    });

    it('не покажет тултипы, если нет нужных customs_state_group и damage_group', () => {
        const searchParams = { category: 'cars', section: 'used', damage_group: 'BEATEN', customs_state_group: 'CLEARED' };
        const ContextProvider = createContextProvider({ ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' });

        const tree = shallow(
            <ContextProvider>
                <ListingCars
                    { ...defaultProps }
                    listing={ expListingFlat }
                    pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                    outputType={ LISTING_OUTPUT_TYPE.LIST }
                    category="cars"
                    searchParams={ searchParams }
                />
            </ContextProvider>,
        ).dive();

        expect(tree.find('ListingItem').at(0).props().hideButtonPopupType).toBeUndefined();
        expect(tree.find('ListingItem').at(1).props().hideButtonPopupType).toBeUndefined();
    });

    it('покажет тултипы, если после обновления фильтров компонент стал подходить условиям', () => {
        const searchParams = { category: 'cars', section: 'used' };
        const ContextProvider = createContextProvider({ ...contextMock, hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN' });

        const tree = shallow(
            <ContextProvider>
                <ListingCars
                    { ...defaultProps }
                    listing={ expListingFlat }
                    pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                    outputType={ LISTING_OUTPUT_TYPE.LIST }
                    category="cars"
                    searchParams={ searchParams }
                    searchID="321"
                />
            </ContextProvider>,
        ).dive();

        expect(tree.find('ListingItem').at(0).props().hideButtonPopupType).toBeUndefined();
        expect(tree.find('ListingItem').at(1).props().hideButtonPopupType).toBeUndefined();

        tree.setProps({
            searchID: '123',
            searchParams: {
                ...searchParams,
                damage_group: 'ANY',
                customs_state_group: 'DOESNT_MATTER',
            },
        });

        tree.update();

        expect(tree.find('ListingItem').at(0).props().hideButtonPopupType).toEqual('damage_group');
        expect(tree.find('ListingItem').at(1).props().hideButtonPopupType).toEqual('customs_state_group');
    });
});

it('должен отрендерить баннер раздела электромобилей в списке, если выбраны нужные двигатели', () => {
    const searchParams = { category: 'cars', section: 'all', engine_group: [ 'ELECTRO' ] };

    const tree = shallow(
        <ContextProvider>
            <ListingCars
                { ...defaultProps }
                params={ searchParams }
                listing={ listingFlat }
                pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                outputType={ LISTING_OUTPUT_TYPE.LIST }
                category="cars"
            />
        </ContextProvider>,
    ).dive();

    expect(tree.find('Memo(ElectroBannerDesktop)')).toExist();
});

describe('эвентлог', () => {

    it('не отправит лог, если searchID одинаковый', () => {
        const searchParams = { category: 'cars', section: 'used' };
        const tree = shallow(
            <ContextProvider>
                <ListingCars
                    { ...defaultProps }
                    params={ searchParams }
                    listing={ listingFlat }
                    pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                    outputType={ LISTING_OUTPUT_TYPE.LIST }
                    category="cars"
                    searchID="searchID"
                />
            </ContextProvider>,
        ).dive();

        expect(statApi.log).toHaveBeenCalledTimes(1);
        tree.update();
        expect(statApi.log).toHaveBeenCalledTimes(1);
    });

    it('отправит лог, если searchID отличается от предыдущего', () => {
        const searchParams = { category: 'cars', section: 'used' };
        const tree = shallow(
            <ContextProvider>
                <ListingCars
                    { ...defaultProps }
                    params={ searchParams }
                    listing={ listingFlat }
                    pagination={{ page: 1, total_offers_count: 60, total_page_count: 2 }}
                    outputType={ LISTING_OUTPUT_TYPE.LIST }
                    category="cars"
                    searchID="searchID"
                />
            </ContextProvider>,
        ).dive();

        expect(statApi.log).toHaveBeenCalledTimes(1);
        tree.setProps({ searchID: 'ne to' });
        tree.update();
        expect(statApi.log).toHaveBeenCalledTimes(2);
    });

});
