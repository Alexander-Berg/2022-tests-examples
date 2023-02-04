jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getResource = require('auto-core/react/lib/gateApi').getResource;

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const IndexPresets = require('./IndexPresets');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const indexPresetsMock = require('autoru-frontend/mockData/state/indexPresets.mock');
const indexSearchHistoryMock = require('autoru-frontend/mockData/state/indexSearchHistory.mock');
const subscriptionsMock = require('auto-core/react/dataDomain/subscriptions/mocks/subscriptions.mock').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

let initialState;
let props;
let context;
let searchOffersParams;
let getIndexPresetMock;
beforeEach(() => {
    initialState = {
        indexPresets: _.cloneDeep(indexPresetsMock),
        indexSearchHistory: _.cloneDeep(indexSearchHistoryMock),
        subscriptions: _.cloneDeep(subscriptionsMock),
        user: { data: {} },
    };

    props = {};

    context = _.cloneDeep(contextMock);

    getIndexPresetMock = jest.fn((resourceName, params) => {
        searchOffersParams = params;
        return Promise.resolve();
    });
    getResource.mockImplementation(getIndexPresetMock);
});

describe('при запросе пресетов', () => {
    it('если никаких поисков нет, не добавит никаких доп параметров', () => {
        makeSureThatWeGetNonPersonalizedSearch();
        shallowRenderIndexPresets();

        expect(searchOffersParams).not.toHaveProperty('year_from');
        expect(searchOffersParams).not.toHaveProperty('catalog_filter');
        expect(searchOffersParams.section).toBe('all');
    });

    it('если пользователь выберет последний пресет, не добавит никаких доп параметров', () => {
        makeSureThatWeGetPersonalizedSearch();
        const page = shallowRenderIndexPresets();
        const tabSelector = page.find('.IndexPresets__tabs RadioGroup');
        tabSelector.simulate('change', initialState.indexPresets[2].alias);

        expect(getIndexPresetMock).toHaveBeenCalledTimes(2);
        expect(searchOffersParams).not.toHaveProperty('year_from');
        expect(searchOffersParams.catalog_filter).toEqual(initialState.indexPresets[2].api.params.catalog_filter);
        expect(searchOffersParams.section).toBe('all');
    });

    it('добавить ммм из двух первых сохраненных и двух первых предыдущих поисков в параметры', () => {
        initialState.indexSearchHistory[1].params.catalog_filter = [ { mark: 'JEEP' } ];
        shallowRenderIndexPresets();
        const allMmm = [
            ...initialState.indexSearchHistory.slice(0, 2).map(({ params }) => params),
            ...initialState.subscriptions.data.slice(0, 2).map(({ data }) => data.params),
        ].reduce((result, { catalog_filter: mmm = [] }) => ([ ...result, ...mmm ]), []);

        expect(getIndexPresetMock).toHaveBeenCalledTimes(1);
        expect(searchOffersParams.catalog_filter).toEqual(allMmm);
    });

    it('если в поисках есть хотя бы один без ммм, то не будет добавлять ммм в параметры', () => {
        initialState.indexSearchHistory[0].params.catalog_filter = undefined;
        shallowRenderIndexPresets();

        expect(searchOffersParams).not.toHaveProperty('catalog_filter');
    });

    it('если хотя бы у одного поиска нет "года от", не добавит год в параметры', () => {
        initialState.indexSearchHistory[1].params.year_from = undefined;
        shallowRenderIndexPresets();

        expect(searchOffersParams).not.toHaveProperty('year_from');
    });

    it('если у всех поисков есть "год от", добавит в парметры самый ранний из них', () => {
        initialState.indexSearchHistory[0].params.year_from = 2010;
        initialState.indexSearchHistory[1].params.year_from = 2008;
        initialState.subscriptions.data[0].data.params.year_from = 2012;
        initialState.subscriptions.data[1].data.params.year_from = 2015;
        shallowRenderIndexPresets();

        expect(searchOffersParams.year_from).toBe(2008);
    });

    it('если у всех поисков есть секция и она одинаковая, добавит ее в параметры', () => {
        initialState.indexSearchHistory[0].params.section = 'used';
        initialState.indexSearchHistory[1].params.section = 'used';
        initialState.subscriptions.data[0].data.params.section = 'used';
        initialState.subscriptions.data[1].data.params.section = 'used';
        shallowRenderIndexPresets();

        expect(searchOffersParams.section).toBe('used');
    });

    it('если хотя бы у одного поиска нет секции, сбросит секцию в all', () => {
        initialState.indexSearchHistory[0].params.section = undefined;
        shallowRenderIndexPresets();

        expect(searchOffersParams.section).toBe('all');
    });

    it('секции разные, сбросит секцию в all', () => {
        initialState.indexSearchHistory[0].params.section = 'used';
        initialState.indexSearchHistory[0].params.section = 'new';
        shallowRenderIndexPresets();

        expect(searchOffersParams.section).toBe('all');
    });

    it('если поиск вернул ничего, то сделает новый запрос но уже без доп параметров', () => {
        const pr1 = Promise.resolve([]);
        const pr2 = Promise.resolve([ { id: 'foo' } ]);
        getIndexPresetMock.mockImplementationOnce(() => pr1);
        getIndexPresetMock.mockImplementationOnce(() => pr2);

        makeSureThatWeGetPersonalizedSearch();
        shallowRenderIndexPresets();

        return pr2.then(() => {
            const firstReqParams = getIndexPresetMock.mock.calls[0][1];
            const secondReqParams = getIndexPresetMock.mock.calls[1][1];

            expect(getIndexPresetMock).toHaveBeenCalledTimes(2);
            expect(firstReqParams).toHaveProperty('year_from');
            expect(firstReqParams).toHaveProperty('catalog_filter');
            expect(secondReqParams).not.toHaveProperty('year_from');
            expect(secondReqParams).not.toHaveProperty('catalog_filter');
        });
    });

    it('не будет делать перезапрос, если первый поиск не был персонализированным', () => {
        const pr1 = Promise.resolve([]);
        getIndexPresetMock.mockImplementationOnce(() => pr1);

        makeSureThatWeGetNonPersonalizedSearch();
        shallowRenderIndexPresets();

        return pr1.then(() => {
            expect(getIndexPresetMock).toHaveBeenCalledTimes(1);
        });
    });

    it('передаст в IndexPresetsOffer флаг, что поиск был персонализирован', () => {
        const pr1 = Promise.resolve([ offerMock, offerMock, offerMock, offerMock, offerMock ]);
        getIndexPresetMock.mockImplementationOnce(() => pr1);

        makeSureThatWeGetPersonalizedSearch();
        const page = shallowRenderIndexPresets();

        return pr1.then(() => {
            const carousel = page.find('CarouselOffers');
            expect(carousel.props().offerProps.isPersonalized).toBe(true);
        });
    });

    it('не передаст в IndexPresetsOffer флаг, если доп параметры в поиске отсутствовали', () => {
        const pr1 = Promise.resolve([ offerMock, offerMock, offerMock, offerMock, offerMock ]);
        getIndexPresetMock.mockImplementationOnce(() => pr1);

        makeSureThatWeGetNonPersonalizedSearch();
        const page = shallowRenderIndexPresets();

        return pr1.then(() => {
            const carousel = page.find('CarouselOffers');
            expect(carousel.props().offerProps.isPersonalized).toBe(false);
        });
    });
});

describe('текст ссылки на список всех предложений', () => {
    const initialState = {
        indexSearchHistory: _.cloneDeep(indexSearchHistoryMock),
        subscriptions: _.cloneDeep(subscriptionsMock),
        indexPresets: [
            {
                alias: 'new-cars-with-discount',
                api: {
                    params: {},
                },
            },
            {
                alias: 'special',
                api: {
                    params: {},
                },
            },
        ],
        user: { data: {} },
    };

    const store = mockStore(initialState);

    it('должен вывести текст ссылки "Все новые автомобили" для пресета "Новые со скидкой"', () => {
        const page = shallowRenderIndexPresets(store);
        const allLink = page.find('CarouselOffers').props().footer;
        expect(allLink().props.children).toBe('Все новые автомобили');
    });

    it('должен вывести текст ссылки "Смотреть все" для любого пресета, кроме "Новые со скидкой"', () => {
        const page = shallowRenderIndexPresets(store);
        const tabSelector = page.find('.IndexPresets__tabs RadioGroup');
        tabSelector.simulate('change', initialState.indexPresets[1].alias);
        const allLink = page.find('CarouselOffers').props().footer;
        expect(allLink().props.children).toBe('Смотреть все');
    });
});

it('если в пресете офферов меньше 5 - кнопка обновить должна быть задизейблена', () => {
    const pr1 = Promise.resolve([ offerMock, offerMock, offerMock, offerMock ]);
    getIndexPresetMock.mockImplementationOnce(() => pr1);

    makeSureThatWeGetNonPersonalizedSearch();
    const page = shallowRenderIndexPresets();

    return pr1.then(() => {
        const button = page.find('Button');
        expect(button.props().disabled).toBe(true);
    });
});

it('если офферы не загрузились, остаются превью', () => {
    const pr1 = Promise.reject([]);
    getIndexPresetMock.mockImplementation(() => pr1);

    makeSureThatWeGetNonPersonalizedSearch();
    const wrapper = shallowRenderIndexPresets();

    return pr1.then(() => {}, () => {
        const carousel = wrapper.find('CarouselOffers');
        expect(carousel.props().classNameList).toContain('IndexPresets__offers_loading');
    });
});

function shallowRenderIndexPresets(store = mockStore(initialState)) {
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <IndexPresets { ...props } store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}

function makeSureThatWeGetPersonalizedSearch() {
    initialState.indexSearchHistory[1].params.catalog_filter = [ { mark: 'JEEP' } ];
    initialState.indexSearchHistory[0].params.year_from = 2010;
    initialState.indexSearchHistory[1].params.year_from = 2008;
    initialState.subscriptions.data[0].data.params.year_from = 2012;
    initialState.subscriptions.data[1].data.params.year_from = 2015;
}

function makeSureThatWeGetNonPersonalizedSearch() {
    initialState.indexSearchHistory = [];
    initialState.subscriptions.data = [];
}
