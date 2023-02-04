/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

jest.mock('auto-core/react/dataDomain/cookies/actions/setToRoot', () => {
    return {
        'default': jest.fn(() => {
            return { type: 'MOCK_ACTION' };
        }),
    };
});

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve()),
    };
});

const React = require('react');
const { Provider, useDispatch, useSelector } = require('react-redux');
const { mount, shallow } = require('enzyme');
const _ = require('lodash');
const { nbsp } = require('auto-core/react/lib/html-entities');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const getResource = require('auto-core/react/lib/gateApi').getResource;
const setToRoot = require('auto-core/react/dataDomain/cookies/actions/setToRoot').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const IndexSelector = require('./IndexSelector');
const IndexMarks = require('www-desktop/react/components/Index/IndexMarks');
const storeMock = require('./IndexSelector.mock');

const BODY_TYPE_IDS = [
    'ALLROAD',
    'SEDAN',
    'HATCHBACK',
    'LIFTBACK',
    'WAGON',
    'MINIVAN',
    'COUPE',
    'PICKUP',
    'CABRIO',
    'VAN',
];

afterEach(() => {
    contextMock.hasExperiment.mockReset();
});

describe('h1', () => {
    it('должен отрисовать заголовок без гео, если в url нет региона', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const h1 = wrapper.find('.Index__title-h1');
        expect(h1.props().children).toEqual('Легковые автомобили');
    });

    it('должен отрисовать заголовок c гео, если в url есть регион', () => {
        const store = mockStore({
            ...storeMock,
            geo: {
                ...storeMock.geo,
                geoSource: 'path',
            },
        });
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const h1 = wrapper.find('.Index__title-h1');
        expect(h1.props().children).toEqual(`Легковые автомобили в${ nbsp }Москве`);
    });

    it('должен отрисовать заголовок без гео при выборе нескольких городов', () => {
        const store = mockStore({
            ...storeMock,
            geo: {
                ...storeMock.geo,
                gidsInfo: [
                    ...storeMock.geo.gidsInfo, ...storeMock.geo.gidsInfo,
                ],
            },
        });
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const h1 = wrapper.find('.Index__title-h1');
        expect(h1.props().children).toEqual('Легковые автомобили');
    });
});

describe('tabs', () => {
    afterEach(() => {
        setToRoot.mockClear();
    });

    it('должен ставить куку и обновлять каунты при выборе "помощника"', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };

        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        const tab = wrapper.find('.IndexSelector__tabs RadioGroup');
        tab.simulate('change', 'wizard');

        expect(setToRoot.mock.calls).toHaveLength(1);
        expect(setToRoot.mock.calls[0]).toEqual([ 'index-selector-tab', 'wizard', { expires: 365 } ]);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });
    it('должен ставить куку и обновлять каунты при выборе "марки"', () => {
        const store = mockStore({
            ...storeMock,
            cookies: {
                'index-selector-tab': 'wizard',
            },
        });
        const context = {
            ...contextMock,
            store,
        };

        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        const tab = wrapper.find('.IndexSelector__tabs RadioGroup');
        tab.simulate('change', 'marks');

        expect(setToRoot.mock.calls).toHaveLength(1);
        expect(setToRoot.mock.calls[0]).toEqual([ 'index-selector-tab', 'marks', { expires: 365 } ]);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });
});

describe('sections', () => {
    it('должен обновить стэйт и счетчики при переключении табов "все" -> "новые"', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        const sectionFilter = wrapper.find('SectionFilter');
        sectionFilter.simulate('change', 'new');

        expect(wrapper.state().section).toEqual('new');
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });
});

describe('slider', () => {
    it('должен обновить стэйт и счетчики изменении цены "от"', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        const slider = wrapper.find('RangeSlider');
        slider.simulate('change', { from: 100000 });

        expect(wrapper.state().priceFrom).toEqual(100000);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });

    it('должен обновить стэйт и счетчики изменении цены "до"', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        const slider = wrapper.find('RangeSlider');
        slider.simulate('change', { to: 500000 });

        expect(wrapper.state().priceTo).toEqual(500000);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });
});

describe('button title', () => {
    it('должен быть title "показать n объявлений", если count есть', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const button = wrapper.find('.IndexSelector__submit Button');
        expect(button.props().children).toEqual('Показать 28 574 предложения');
    });

    it('должен быть title "ничего не найдено", если count=0', () => {
        const newStoreMock = _.cloneDeep(storeMock);

        newStoreMock.indexBreadcrumbs.count = 0;

        const store = mockStore(newStoreMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const button = wrapper.find('.IndexSelector__submit Button');
        expect(button.props().children).toEqual('Ничего не найдено');
    });

    //на самом деле никто не помнит, зачем этот кейс
    it('должен быть title "Найти объявления", если count нет', () => {
        const newStoreMock = _.cloneDeep(storeMock);

        newStoreMock.indexBreadcrumbs.count = undefined;

        const store = mockStore(newStoreMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const button = wrapper.find('.IndexSelector__submit Button');
        expect(button.props().children).toEqual('Найти объявления');
    });

    it('должен быть title "показать n объявлений и еще n в других городах", если есть count и другие города', () => {
        const newStoreMock = _.cloneDeep(storeMock);

        newStoreMock.listingGeoRadiusCounters = {
            data: [
                {
                    radius: 1000,
                    count: 120312,
                },
            ],
        };

        contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-19905_listing_link');

        const store = mockStore(newStoreMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const button = wrapper.find('.IndexSelector__submit Button').find('div');

        const children = button.children().children();
        const childrenValues = children.map(title => title.text());

        expect(childrenValues).toEqual([ 'Показать 28 574 предложения', 'ещё 91 738 в других городах' ]);
    });

    it('должен быть title "ничего не найдено и еще n в других городах", если count=0, но есть другие города', () => {
        const newStoreMock = _.cloneDeep(storeMock);

        newStoreMock.indexBreadcrumbs.count = 0;
        newStoreMock.listingGeoRadiusCounters = {
            data: [
                {
                    radius: 1000,
                    count: 120312,
                },
            ],
        };

        contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-19905_listing_link');

        const store = mockStore(newStoreMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const button = wrapper.find('.IndexSelector__submit Button').find('div');

        const children = button.children().children();
        const childrenValues = children.map(title => title.text());

        expect(childrenValues).toEqual([ 'Ничего не найдено', 'ещё 120 312 в других городах' ]);
    });
});

describe('button url, должен вызвать link с правильными параметрами', () => {
    let originalWindowLocation;
    beforeEach(() => {
        originalWindowLocation = global.window.location;
        delete global.window.location;
        global.window.location = { href: 'https://test.br' };
    });
    afterEach(() => {
        global.window.location = originalWindowLocation;
    });

    it('дефолтные параметры', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        const button = wrapper.find('.IndexSelector__submit Button');
        button.simulate('click');
        expect(global.window.location.href).toEqual('link/listing/?category=cars&section=all');
    });
    it('section=new', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({ section: 'new' });
        const button = wrapper.find('.IndexSelector__submit Button');
        button.simulate('click');
        expect(global.window.location.href).toEqual('link/listing/?category=cars&section=new');
    });
    it('priceFrom=100000', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({ priceFrom: 100000 });
        const button = wrapper.find('.IndexSelector__submit Button');
        button.simulate('click');
        expect(global.window.location.href).toEqual('link/listing/?category=cars&section=all&price_from=100000');
    });
    it('tab=wizard', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({ tab: 'wizard' });
        const button = wrapper.find('.IndexSelector__submit Button');
        button.simulate('click');
        expect(global.window.location.href).toEqual('link/listing/?category=cars&section=all&from=old_guru');
    });
    it('tab=wizard + кузова', () => {
        const bodyTypeId = 'HATCHBACK';
        const bodyTypeParams = 'body_type_group=HATCHBACK_3_DOORS&body_type_group=HATCHBACK_5_DOORS&body_type_group=LIFTBACK';
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({
            tab: 'wizard',
            bodyType: [ bodyTypeId ],
        });
        const button = wrapper.find('.IndexSelector__submit Button');
        button.simulate('click');
        expect(global.window.location.href).toEqual('link/listing/?category=cars&section=all&' + bodyTypeParams + '&from=old_guru');
    });
    it('tab=wizard + все пва', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({
            tab: 'wizard',
            bodyType: [ BODY_TYPE_IDS ],
        });
        const button = wrapper.find('.IndexSelector__submit Button');
        button.simulate('click');
        expect(global.window.location.href).toEqual('link/listing/?category=cars&section=all&from=old_guru');
    });
    it('tab=wizard + пресет', () => {
        const presetId = 'offroad';
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({
            tab: 'wizard',
            presets: [ presetId ],
        });
        const button = wrapper.find('.IndexSelector__submit Button');
        button.simulate('click');
        expect(global.window.location.href)
            .toEqual('link/listing/?category=cars&section=all&clearance_from=200&gear_type=ALL_WHEEL_DRIVE&from=old_guru');
    });
});

describe('блок кузовов', () => {
    it('должен правильно обрабатывать выбор кузова', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        wrapper.setState({ tab: 'wizard' });
        const sedan = wrapper.find('IndexBodyTypes').dive().find('div[data-id="SEDAN"]');
        sedan.simulate('click', { currentTarget: { getAttribute: () => 'SEDAN' } });
        expect(wrapper.state().bodyType).toEqual([ 'SEDAN' ]);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });

    it('должен обрабатывать сброс кузова', () => {
        const bodyType = 'SEDAN';
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        wrapper.setState({
            tab: 'wizard',
            bodyType: [ bodyType ],
        });
        const body = wrapper.find('IndexBodyTypes').dive().find(`div[data-id="${ bodyType }"]`);
        body.simulate('click', { currentTarget: { getAttribute: () => bodyType } });
        expect(wrapper.state().bodyType).toEqual(BODY_TYPE_IDS);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });

    it('должен правильно обрабатывать выбор нескольких кузовов', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        wrapper.setState({ tab: 'wizard' });
        const sedan = wrapper.find('IndexBodyTypes').dive().find('div[data-id="SEDAN"]');
        sedan.simulate('click', { currentTarget: { getAttribute: () => 'SEDAN' } });
        const allroad = wrapper.find('IndexBodyTypes').dive().find('div[data-id="ALLROAD"]');
        allroad.simulate('click', { currentTarget: { getAttribute: () => 'ALLROAD' } });
        expect(wrapper.state().bodyType).toEqual([ 'SEDAN', 'ALLROAD' ]);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(2);
    });
});

describe('блок пресетов', () => {
    it('должен обрабатывать выбор пресета', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        wrapper.setState({ tab: 'wizard' });
        const sedan = wrapper.find('div[data-id="seat"]');
        sedan.simulate('click', { currentTarget: { getAttribute: () => 'seat' } });
        expect(wrapper.state().presets).toEqual([ 'seat' ]);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });

    it('должен обрабатывать сброс пресета', () => {
        const presetId = 'offroad';
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        wrapper.setState({
            tab: 'wizard',
            presets: [ presetId ],
        });
        const preset = wrapper.find(`div[data-id="${ presetId }"]`);
        preset.simulate('click', { currentTarget: { getAttribute: () => presetId } });
        expect(wrapper.state().presets).toEqual([]);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(1);
    });

    it('должен обрабатывать выбор нескольких пресетов', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.instance().updateCounts = jest.fn();
        wrapper.setState({ tab: 'wizard' });
        const sedan = wrapper.find('div[data-id="seat"]');
        sedan.simulate('click', { currentTarget: { getAttribute: () => 'seat' } });
        const offroad = wrapper.find('div[data-id="offroad"]');
        offroad.simulate('click', { currentTarget: { getAttribute: () => 'offroad' } });
        expect(wrapper.state().presets).toEqual([ 'seat', 'offroad' ]);
        expect(wrapper.instance().updateCounts.mock.calls).toHaveLength(2);
    });
});

describe('блок марок', () => {
    const store = mockStore(storeMock);
    const Context = createContextProvider(contextMock);
    let originalWindowScrollTo;

    beforeEach(() => {
        originalWindowScrollTo = global.window.scrollTo;
        global.window.scrollTo = jest.fn();
    });

    afterEach(() => {
        global.window.scrollTo = originalWindowScrollTo;
    });

    it('должен правильно обрабатывать клик на "все марки', () => {
        mockRedux();
        const wrapper = mount(
            <Context>
                <Provider store={ store }>
                    <IndexSelector/>
                </Provider>
            </Context>,
        );
        const indexSelector = wrapper.find('IndexSelector');
        const showAll = wrapper.find('.IndexMarks__show-all');
        showAll.simulate('click');
        expect(indexSelector.state().showAllMarks).toBe(true);
    });

    it('при клике на кнопку свернуть должен свернуть блок марок и выполнить подскролл', () => {
        const wrapper = shallow(
            <Context>
                <IndexSelector store={ store }/>
            </Context>,
        ).dive().dive();

        wrapper.setState({ showAllMarks: true });
        wrapper.find(IndexMarks).simulate('collapseClick');
        expect(wrapper.state().showAllMarks).toBe(false);
        expect(global.window.scrollTo).toHaveBeenCalledWith({
            behavor: 'instant',
            top: 0,
        });
    });
});

describe('update counts', () => {
    it('должен обновить только каунт на вкладке помощника', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const audiCount = storeMock.indexBreadcrumbs.marks.find(e => e.id === 'AUDI').count;

        const mockResponse = Promise.resolve({
            count: 100,
            marks: [ {
                id: 'AUDI',
                count: 100,
            } ],
        });
        getResource.mockImplementation(() => mockResponse);
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({ tab: 'wizard' });
        const sedan = wrapper.find('div[data-id="seat"]');
        sedan.simulate('click', { currentTarget: { getAttribute: () => 'seat' } });

        return mockResponse.then(() => {
            expect(wrapper.update().state('count')).toEqual(100);
            expect(wrapper.update().state('marksCounts').AUDI).toEqual(audiCount);
        });
    });

    it('должен обновить каунт и марки на вкладке марок', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const mockResponse = Promise.resolve({
            count: 100,
            marks: [ {
                id: 'AUDI',
                count: 100,
            } ],
        });
        getResource.mockImplementation(() => mockResponse);
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();
        wrapper.setState({ tab: 'wizard' });
        const tab = wrapper.find('.IndexSelector__tabs RadioGroup');
        tab.simulate('change', 'marks');

        return mockResponse.then(() => {
            expect(wrapper.update().state('count')).toEqual(100);
            expect(wrapper.update().state('marksCounts').find(e => e.id === 'AUDI').count).toEqual(100);
        });
    });
});

describe('блок с эксклюзивом', () => {
    it('покажется если кол-во эксклюзивных офферов превышает трешолд', () => {
        const initialState = _.cloneDeep(storeMock);
        initialState.indexBreadcrumbs.exclusiveOffersNum = 2345;
        const store = mockStore(initialState);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();

        expect(wrapper.find('.IndexSelector__exclusive').isEmptyRender()).toBe(false);
    });

    it('не покажется если кол-во эксклюзивных офферов не превышает трешолд', () => {
        const initialState = _.cloneDeep(storeMock);
        initialState.indexBreadcrumbs.exclusiveOffersNum = 1234;
        const store = mockStore(initialState);
        const context = {
            ...contextMock,
            store,
        };
        const wrapper = shallow(<IndexSelector/>, { context: context }).dive();

        expect(wrapper.find('.IndexSelector__exclusive').isEmptyRender()).toBe(true);
    });

    it('отправит метрику при показе', () => {
        const initialState = _.cloneDeep(storeMock);
        initialState.indexBreadcrumbs.exclusiveOffersNum = 2345;
        const store = mockStore(initialState);
        const Context = createContextProvider(contextMock);

        shallow(
            <Context>
                <IndexSelector store={ store }/>
            </Context>,
        ).dive().dive();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'exclusive', 'shows' ]);
    });
});

function mockRedux(state = storeMock) {
    const store = mockStore(state);

    useDispatch.mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    useSelector.mockImplementation(
        (selector) => selector(store.getState()),
    );
}
