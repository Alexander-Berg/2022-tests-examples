const React = require('react');
const { shallow } = require('enzyme');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const Context = createContextProvider(contextMock);

const SaleCarousel = require('./SaleCarousel');

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const getResource = require('auto-core/react/lib/gateApi').getResource;

const PAGE_PARAMS_CARS = {
    category: 'CARS',
};

function delayedExpect(func, timeout = 100) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            try {
                func();
                resolve();
            } catch (e) {
                reject(e);
            }
        }, timeout);
    });
}

let responseMock;
beforeEach(() => {
    responseMock = {
        search_parameters: {
            body_type_group: [ 'SEDAN' ],
            catalog_filter: [
                { mark: 'ACURA', model: 'RDX', generation: '20079928' },
            ],
            category: 'cars',
            section: 'all',
        },
        offers: Array(12).fill({ id: '1' }),
    };
});

it('должен запросить объявления с правильными параметрами, если есть catalog_filter', () => {
    getResource.mockImplementation(() => Promise.resolve());
    const pageParams = {
        ...PAGE_PARAMS_CARS,
        catalog_filter: [ { mark: 'AUDI', model: 'A1', generation: '111' } ],
    };
    const tree = shallow(
        <Context>
            <SaleCarousel pageParams={ pageParams } searchID="SEARCH_ID"/>
        </Context >,
    ).dive().dive().dive();
    tree.find('InView').simulate('change', true);
    return delayedExpect(() => {
        expect(getResource).toHaveBeenCalledWith('getRelatedOffersForCurrentOrPreviousGeneration', {
            body_type_group: '',
            catalog_filter: [ { generation: '111', mark: 'AUDI', model: 'A1' } ],
            category: 'cars',
            mark: 'AUDI',
            model: 'A1',
            sub_category: undefined,
            super_gen: '111',
        });
    });
});

it('должен запросить объявления с правильными параметрами, если нет catalog_filter', () => {
    // например в каталоге так
    getResource.mockImplementation(() => Promise.resolve());
    const pageParams = {
        ...PAGE_PARAMS_CARS,
        mark: 'audi',
        model: 'a1',
        super_gen: '111',
    };
    const tree = shallow(
        <Context>
            <SaleCarousel pageParams={ pageParams } searchID="SEARCH_ID"/>
        </Context >,
    ).dive().dive().dive();

    tree.find('InView').simulate('change', true);

    return delayedExpect(() => {
        expect(getResource).toHaveBeenCalledWith('getRelatedOffersForCurrentOrPreviousGeneration', {
            body_type_group: '',
            catalog_filter: [ { generation: '111', mark: 'AUDI', model: 'A1' } ],
            category: 'cars',
            mark: 'AUDI',
            model: 'A1',
            sub_category: undefined,
            super_gen: '111',
        });
    });
});

describe('ссылки на листинг', () => {
    let context;
    let Context;
    beforeAll(() => {
        context = {
            ...contextMock,
            link: jest.fn(),
        };
        Context = createContextProvider(context);
    });

    it('должен правильно строить ссылки на листинг', () => {
        getResource.mockImplementation(() => Promise.resolve(responseMock));
        const tree = shallow(
            <Context>
                <SaleCarousel
                    pageParams={{
                        category: 'CARS',
                        mark: 'audi',
                        model: 'a4',
                        super_gen: [ '123', '456' ],
                    }}
                    searchID="SEARCH_ID"
                />
            </Context >,
        ).dive().dive().dive();
        tree.find('InView').simulate('change', true);
        return delayedExpect(() => {
            tree.find('InView').dive().find('CarouselOffers').dive();
            expect(context.link).toHaveBeenCalledWith('listing', {
                category: 'cars',
                body_type_group: [ 'SEDAN' ],
                section: 'all',
                catalog_filter: [
                    { mark: 'ACURA', model: 'RDX', generation: '20079928' },
                ],
            });
        });
    });
});

describe('фильтры', () => {
    it('не должен отрендерить карусель c фильтрами, если все объявления бу', () => {
        // делаем вид, что все объявления бу
        responseMock.offers = responseMock.offers.map((offer) => ({ ...offer, section: 'used' }));

        getResource.mockImplementation(() => Promise.resolve(responseMock));
        const tree = shallow(
            <Context>
                <SaleCarousel pageParams={ PAGE_PARAMS_CARS } searchID="SEARCH_ID"/>
            </Context >,
        ).dive().dive().dive();

        tree.find('InView').simulate('change', true);
        return delayedExpect(() => {
            expect(
                tree.find('InView').dive().find('CarouselOffers').dive().dive().find('Memo(CarouselOffersFilters)'),
            ).not.toExist();
        });
    });

    it('не должен отрендерить карусель c фильтрами, если все объявления новые', () => {
        // делаем вид, что все объявления новые
        responseMock.offers = responseMock.offers.map((offer) => ({ ...offer, section: 'new' }));

        getResource.mockImplementation(() => Promise.resolve(responseMock));
        const tree = shallow(
            <Context>
                <SaleCarousel pageParams={ PAGE_PARAMS_CARS } searchID="SEARCH_ID"/>
            </Context >,
        ).dive().dive().dive();

        tree.find('InView').simulate('change', true);
        return delayedExpect(() => {
            expect(
                tree.find('InView').dive().find('CarouselOffers').dive().dive().find('Memo(CarouselOffersFilters)'),
            ).not.toExist();
        });
    });

    describe('фильтр есть', () => {
        beforeEach(() => {
            // делаем вид, что первое объявление новое, остальные бу
            responseMock.offers[0] = { ...responseMock.offers[0], section: 'new' };

            getResource.mockImplementation(() => Promise.resolve(responseMock));
        });

        it('должен отрендерить карусель c фильтрами, если есть и новые и бу объявления', () => {
            const tree = shallow(
                <Context>
                    <SaleCarousel pageParams={ PAGE_PARAMS_CARS } searchID="SEARCH_ID"/>
                </Context >,
            ).dive().dive().dive();

            tree.find('InView').simulate('change', true);

            return delayedExpect(() => {
                expect(
                    tree.find('InView').dive().find('CarouselOffers').dive().dive().find('Memo(CarouselOffersFilters)'),
                ).toExist();
            });
        });

        it('должен обработать клик по секции', () => {
            const tree = shallow(
                <Context>
                    <SaleCarousel pageParams={ PAGE_PARAMS_CARS } searchID="SEARCH_ID"/>
                </Context >,
            ).dive().dive().dive();

            tree.find('InView').simulate('change', true);

            return delayedExpect(() => {
                const filter = tree.find('InView').dive().find('CarouselOffers').dive().dive().find('Memo(CarouselOffersFilters)');

                // кликаем в "C пробегом"
                filter.dive().find('Memo(CarouselLazyOffersFilterButton)').last()
                    .dive().find('Button')
                    .simulate('click');

                expect(
                    tree.find('InView').dive().find('CarouselOffers').dive().dive().find('.SaleCarousel__footerLink'),
                ).toHaveProp(
                    'url',
                    'link/listing/?body_type_group=SEDAN&catalog_filter=mark%3DACURA%2Cmodel%3DRDX%2Cgeneration%3D20079928&category=cars&section=all',
                );
            });
        });
    });
});
