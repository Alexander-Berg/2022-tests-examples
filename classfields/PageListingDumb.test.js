/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

const PageListingDumb = require('./PageListingDumb');

jest.mock('auto-core/lib/util/asyncDebounce', () => ({
    __esModule: true,
    'default': (func) => func,
}));

const ListingCarouselSpecial = require('../ListingCarouselSpecial');
const ListingCarouselSpecialNew = require('../ListingCarouselSpecialNew');
const ListingCarouselNewestUsed = require('www-desktop/react/components/ListingCarouselNewestUsed');
const ListingPopularMarksWithModels = require('www-desktop/react/components/ListingPopularMarksWithModels/ListingPopularMarksWithModels').default;

const BASE_PROPS = {
    listingRequestId: 'abc123',
    params: {
        category: 'cars',
        section: 'all',
    },
    sendMarketingEventByListingOffer: jest.fn(),
    onSearchLineSelect: jest.fn(),
    fetchBreadcrumbs: jest.fn(),
    fetchCount: jest.fn(),
    fetchPriceRanges: jest.fn(),
    fetchGeoRadiusCounters: jest.fn(),
    fetchGroupsCountWithTopOffer: jest.fn(),
    fetchNew4newCount: jest.fn(),
    fetchGroupedModelsCount: jest.fn(),
    fetchCrossLinks: jest.fn(),
    fetchEquipmentFilters: jest.fn(),
    fetchMoreCars: jest.fn(),
    fetchNewestUsedCarsCounter: jest.fn(),
    fetchPhones: jest.fn(),
    fetchTrucksCount: jest.fn(),
    searchParams: { category: 'cars' },
    pagination: {
        total_page_count: 10,
        total_offers_count: 370,
    },
    filteredOffersCount: 10,
    outputType: 'list',
};

class PageListing extends PageListingDumb {
    renderFilters() {
        return <div className="PageListingDumb__filters" onChange={ this.handleFiltersChange }/>;
    }
}

describe('пустой листинг', () => {
    const props = {
        ...BASE_PROPS,
        offers: [],
        pagination: {
            total_page_count: 0,
            total_offers_count: 0,
        },
        filteredOffersCount: 0,
    };

    it('должен отрендерить блоки в правильном порядке в смешанном листинге', () => {
        const tree = shallow(
            <PageListingDumb
                { ...props }
                withProvenOwnerSortEnabled={ true }
            />,
            { context: contextMock },
        );
        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен отрендерить блоки в правильном порядке в листинге новых', () => {
        const params = { ...BASE_PROPS.params, section: 'new' };
        const tree = shallow(
            <PageListingDumb
                { ...props }
                params={ params }
                newestUsedCarsCount={ 10 }
                shouldShowNewestUsedCarsBlock={ true }
            />,
            { context: contextMock },
        );
        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});

describe('блок "почти новая"', () => {
    const props = {
        ...BASE_PROPS,
        params: { ...BASE_PROPS.params, section: 'new' },
        offers: [],
        pagination: {
            total_page_count: 0,
            total_offers_count: 0,
        },
        filteredOffersCount: 0,
    };

    it('не должен отрендерить, если отображение блока запрещено', () => {
        const tree = shallow(
            <PageListingDumb
                { ...props }
                filteredOffersCount={ 0 }
                newestUsedCarsCount={ 10 }
                shouldShowNewestUsedCarsBlock={ false }
            />,
            { context: contextMock },
        );
        expect(tree.find(ListingCarouselNewestUsed)).toHaveLength(0);
    });
    it('не должен отрендерить, если нет офферов для блока', () => {
        const tree = shallow(
            <PageListingDumb
                { ...props }
                filteredOffersCount={ 0 }
                newestUsedCarsCount={ 0 }
                shouldShowNewestUsedCarsBlock={ true }
            />,
            { context: contextMock },
        );
        expect(tree.find(ListingCarouselNewestUsed)).toHaveLength(0);
    });
    it('должен отрендерить, если отображение блока разрешено и есть офферы для него', () => {
        const tree = shallow(
            <PageListingDumb
                { ...props }
                filteredOffersCount={ 0 }
                newestUsedCarsCount={ 10 }
                shouldShowNewestUsedCarsBlock={ true }
            />,
            { context: contextMock },
        );
        expect(tree.find(ListingCarouselNewestUsed)).toHaveLength(1);
    });
});

describe('карусель спецпредложений под листингом', () => {
    const props = {
        ...BASE_PROPS,
        offers: [ {} ],
        pagination: {
            total_page_count: 0,
            total_offers_count: 0,
        },
        filteredOffersCount: 0,
    };

    it('не должен отрендерить, если количество офферов больше 37 (в таком случае карусель рендерится внутри списка офферов)', () => {
        const pagination = {
            total_page_count: 2,
            total_offers_count: 38,
        };
        const offers = Array(38).fill({});
        const tree = shallow(
            <PageListingDumb
                { ...props }
                offers={ offers }
                pagination={ pagination }
            />,
            { context: contextMock },
        );
        expect(tree.find(ListingCarouselSpecial)).toHaveLength(0);
        expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(0);
    });

    it('не должен отрендерить, если листинг пуст', () => {
        const pagination = {
            total_page_count: 0,
            total_offers_count: 0,
        };
        const offers = [];
        const tree = shallow(
            <PageListingDumb
                { ...props }
                offers={ offers }
                pagination={ pagination }
            />,
            { context: contextMock },
        );
        expect(tree.find(ListingCarouselSpecial)).toHaveLength(0);
        expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(0);
    });

    it('должен отрендерить правильную карусель в смешанном листинге', () => {
        const pagination = {
            total_page_count: 1,
            total_offers_count: 10,
        };
        const offers = Array(38).fill({});
        const tree = shallow(
            <PageListingDumb
                { ...props }
                offers={ offers }
                pagination={ pagination }
            />,
            { context: contextMock },
        );
        expect(tree.find(ListingCarouselSpecial)).toHaveLength(1);
        expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(0);
    });

    it('должен отрендерить правильную карусель в листинге новых', () => {
        const pagination = {
            total_page_count: 1,
            total_offers_count: 10,
        };
        const offers = Array(38).fill({});
        const params = { ...BASE_PROPS.params, section: 'new' };
        const tree = shallow(
            <PageListingDumb
                { ...props }
                params={ params }
                offers={ offers }
                pagination={ pagination }
            />,
            { context: contextMock },
        );
        expect(tree.find(ListingCarouselSpecial)).toHaveLength(0);
        expect(tree.find(ListingCarouselSpecialNew)).toHaveLength(1);
    });
});

describe('блок популярных марок с моделями', () => {
    const props = {
        ...BASE_PROPS,
        offers: [ {} ],
        pagination: {
            total_page_count: 0,
            total_offers_count: 0,
        },
        filteredOffersCount: 0,
        searchParams: {
            category: 'cars',
            search_tag: [ 'new4new' ],
            catalog_filter: [ { mark: 'AUDI' } ],
        },
    };
    it('должен отрендерить в листинге по тегу new4new, если не выбрана модель', () => {
        const context = contextMock;
        const tree = shallow(
            <PageListingDumb
                { ...props }
            />,
            { context },
        );
        expect(tree.find(ListingPopularMarksWithModels)).toHaveLength(1);
    });

    it('не должен отрендерить в листинге по тегу new4new, если выбрана модель', () => {
        const context = contextMock;
        const searchParams = {
            ...props.searchParams,
            catalog_filter: [ { mark: 'AUDI', model: 'E_TRON' } ],
        };
        const tree = shallow(
            <PageListingDumb
                { ...props }
                searchParams={ searchParams }
            />,
            { context },
        );
        expect(tree.find(ListingPopularMarksWithModels)).toHaveLength(0);
    });

    it('не должен отрендерить, если это не листинг по тегу new4new', () => {
        const context = contextMock;
        const searchParams = {
            ...props.searchParams,
            search_tag: null,
        };
        const tree = shallow(
            <PageListingDumb
                { ...props }
                searchParams={ searchParams }
            />,
            { context },
        );
        expect(tree.find(ListingPopularMarksWithModels)).toHaveLength(0);
    });

});

describe('параметры урла при изменении фильтров', () => {
    describe('has_image и damage_group на странице дилера', () => {
        const searchParams = {
            category: 'moto',
            has_image: false,
            damage_group: 'ANY',
            dealer_id: '123456',
            dealer_code: 'foo',
        };
        const defaultProps = {
            ...BASE_PROPS,
            offers: [],
            searchParams,
            params: {
                dealer_id: '123456',
                dealer_code: 'foo',
            },
            pageParams: {},
            pageType: 'dealer-page',
            handleChangedControl: () => ({ ...searchParams, price_to: 240000 }),
        };

        it('в мото уберет из урла has_image и damage_group', async() => {
            const props = {
                ...defaultProps,
                searchParams: {
                    ...searchParams,
                    category: 'moto',
                },
            };

            const tree = shallow(
                <PageListing { ...props }/>,
                { context: contextMock },
            );

            const filters = tree.find('.PageListingDumb__filters');
            filters.simulate('change', 240000, { name: 'price_to' });
            await flushPromises();
            expect(contextMock.pushState).toHaveBeenCalledTimes(1);
            expect(contextMock.pushState).toHaveBeenCalledWith('link/dealer-page/?category=moto&dealer_id=123456&dealer_code=foo&price_to=240000');
        });

        it('для оф дилера из урла has_image и damage_group', async() => {
            const props = {
                ...defaultProps,
                searchParams: {
                    ...searchParams,
                    category: 'moto',
                },
                pageType: 'dealer-page-official',
            };

            const tree = shallow(
                <PageListing { ...props }/>,
                { context: contextMock },
            );

            const filters = tree.find('.PageListingDumb__filters');
            filters.simulate('change', 240000, { name: 'price_to' });
            await flushPromises();
            expect(contextMock.pushState).toHaveBeenCalledTimes(1);
            expect(contextMock.pushState).toHaveBeenCalledWith('link/dealer-page-official/?category=moto&dealer_id=123456&dealer_code=foo&price_to=240000');
        });

        it('в легковых не будет трогать параметры has_image и damage_group', async() => {
            const newSearchParams = { ...searchParams, category: 'cars' };
            const props = {
                ...defaultProps,
                searchParams: newSearchParams,
                handleChangedControl: () => ({ ...newSearchParams, price_to: 240000 }),
            };

            const tree = shallow(
                <PageListing { ...props }/>,
                { context: contextMock },
            );

            const filters = tree.find('.PageListingDumb__filters');
            filters.simulate('change', 240000, { name: 'price_to' });
            await flushPromises();
            expect(contextMock.pushState).toHaveBeenCalledTimes(1);
            // eslint-disable-next-line max-len
            expect(contextMock.pushState).toHaveBeenCalledWith('link/dealer-page/?category=cars&has_image=false&damage_group=ANY&dealer_id=123456&dealer_code=foo&price_to=240000');
        });

        it('для страницы листинга не будет трогать параметры has_image и damage_group', async() => {
            const props = {
                ...defaultProps,
                pageType: 'listing',
            };

            const tree = shallow(
                <PageListing { ...props }/>,
                { context: contextMock },
            );

            const filters = tree.find('.PageListingDumb__filters');
            filters.simulate('change', 240000, { name: 'price_to' });
            await flushPromises();
            expect(contextMock.pushState).toHaveBeenCalledTimes(1);
            // eslint-disable-next-line max-len
            expect(contextMock.pushState).toHaveBeenCalledWith('link/listing/?category=moto&has_image=false&damage_group=ANY&dealer_id=123456&dealer_code=foo&price_to=240000');
        });
    });

    it('не должен строить ЧПУ для невалидных параметров', async() => {
        const searchParams = {
            category: 'cars',
            section: 'all',
            body_type_group: [ 'SEDAN' ],
            catalog_filter: [ { mark: 'AUDI' } ],
        };
        const props = {
            ...BASE_PROPS,
            offers: [],
            searchParams,
            params: {},
            pageParams: {},
            pageType: 'listing',
            handleChangedControl: () => ({ ...searchParams, catalog_filter: [ { mark: 'AUDI', model: 'R8' } ] }),
        };

        const tree = shallow(
            <PageListing { ...props }/>,
            { context: contextMock },
        );

        const filters = tree.find('.PageListingDumb__filters');
        filters.simulate('change', [ { mark: 'AUDI', model: 'R8' } ], { name: 'catalog_filter' });
        await flushPromises();
        expect(contextMock.pushState).toHaveBeenCalledTimes(1);
        // eslint-disable-next-line max-len
        expect(contextMock.pushState).toHaveBeenCalledWith('link/listing/?category=cars&section=all&body_type_group=SEDAN&catalog_filter=mark%3DAUDI%2Cmodel%3DR8');
    });
});

describe('блок с бесконечным листингом', () => {
    const props = {
        ...BASE_PROPS,
        offers: [],
        pagination: {
            current: 10,
            total_page_count: 10,
        },
        filteredOffersCount: 0,
    };

    it('не должен отрендерить, если это страница дилера', () => {
        const tree = shallow(
            <PageListingDumb
                { ...props }
                params={{ ...props.params, dealer_code: 'code' }}
            />,
            { context: contextMock },
        );
        expect(tree.find('Connect(ListingInfiniteDesktop)')).not.toExist();
    });

    it('не должен отрендерить, если это не последняя страница листинга', () => {
        const tree = shallow(
            <PageListingDumb
                { ...props }
                pagination={{
                    current: 2,
                    total_page_count: 10,
                    total_offers_count: 100,
                }}
            />,
            { context: contextMock },
        );
        expect(tree.find('Connect(ListingInfiniteDesktop)')).not.toExist();
    });

    it('должен отрендерить', () => {
        const tree = shallow(
            <PageListingDumb
                { ...props }
            />,
            { context: contextMock },
        );
        expect(tree.find('Connect(ListingInfiniteDesktop)')).toExist();
    });
});

describe('ABT_VS_678_PESSIMIZATION_BEATEN', () => {
    const searchParams = {
        category: 'cars',
        section: 'used',
        damage_group: 'ANY',
        customs_state_group: 'DOESNT_MATTER',
    };
    const defaultProps = {
        ...BASE_PROPS,
        offers: [],
        searchParams,
        params: {
            dealer_id: '123456',
            dealer_code: 'foo',
        },
        pageParams: {},
        pageType: 'dealer-page',
        handleChangedControl: () => ({ ...searchParams, price_to: 240000 }),
    };

    it('должен сделать push в историю со всеми параметрами вне экспа', async() => {
        const props = {
            ...defaultProps,
            pageType: 'listing',
        };

        const tree = shallow(
            <PageListing { ...props }/>,
            { context: contextMock },
        );

        const filters = tree.find('.PageListingDumb__filters');
        filters.simulate('change', 240000, { name: 'price_to' });
        await flushPromises();
        expect(contextMock.pushState).toHaveBeenCalledTimes(1);
        expect(contextMock.pushState)
            .toHaveBeenCalledWith('link/listing/?category=cars&section=used&damage_group=ANY&customs_state_group=DOESNT_MATTER&price_to=240000');
    });

    it('должен сделать push в историю без эксповых дефолтных параметров в экспе', async() => {
        const context = {
            ...contextMock,
            hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN',
        };

        const props = {
            ...defaultProps,
            pageType: 'listing',
        };

        const tree = shallow(
            <PageListing { ...props }/>,
            { context },
        );

        const filters = tree.find('.PageListingDumb__filters');
        filters.simulate('change', 240000, { name: 'price_to' });
        await flushPromises();
        expect(contextMock.pushState).toHaveBeenCalledTimes(1);
        expect(contextMock.pushState)
            .toHaveBeenCalledWith('link/listing/?category=cars&section=used&price_to=240000');
    });
});
