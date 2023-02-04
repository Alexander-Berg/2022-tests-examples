const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const querystring = require('querystring');
const { shallowToJson } = require('enzyme-to-json');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const IndexMarks = require('./IndexMarks');
const props = require('./IndexMarks.mock');
const storeMock = require('../IndexSelector/IndexSelector.mock');

it('проверяет элементы в блоке с марками', () => {
    const store = mockStore(storeMock);
    const context = {
        link: (routeName, routeParams) => `${ routeName }?${ querystring.stringify(routeParams) }`,
        store,
    };
    const wrapper = shallow(
        <IndexMarks { ...props }/>,
        { context: context });
    const markLogo = wrapper.dive().find('IndexSuperMark').first();
    expect(shallowToJson(markLogo)).toMatchSnapshot();
    const markWithCount = wrapper.dive().find('.IndexMarks__item').first();
    expect(shallowToJson(markWithCount)).toMatchSnapshot();
    wrapper.setProps({ showAllMarks: true });
    const markWithoutCount = wrapper.dive().find('.IndexMarks__marks-without-counts .IndexMarks__item').first();
    expect(shallowToJson(markWithoutCount)).toMatchSnapshot();
});

it('проверяет ссылки в блоке с марками', () => {
    const store = mockStore(storeMock);
    const context = {
        link: (routeName, routeParams) => `${ routeName }?${ querystring.stringify(routeParams) }`,
        store,
    };

    const newProps = _.cloneDeep(props);

    newProps.section = 'new';
    newProps.priceFrom = 200000;
    newProps.priceTo = 500000;

    const logoUrl = `listing?category=cars&mark=VAZ&price_from=${ newProps.priceFrom }&price_to=${ newProps.priceTo }&section=${ newProps.section }`;
    const markWithCountUrl = `listing?category=cars&mark=VAZ&price_from=${ newProps.priceFrom }&price_to=${ newProps.priceTo }&section=${ newProps.section }`;
    const markWithoutCountUrl = 'catalog-model-listing?mark=AC';
    const wrapper = shallow(
        <IndexMarks { ...newProps }/>,
        { context: context });
    const markLogoLink = wrapper.dive().find('IndexSuperMark').first().dive().find('Link');
    expect(markLogoLink.props().url).toEqual(logoUrl);
    const markWithCountLink = wrapper.dive().find('.IndexMarks__item').find('MetrikaLink').first();
    expect(markWithCountLink.props().url).toEqual(markWithCountUrl);
    wrapper.setProps({ showAllMarks: true });
    const markWithoutCountLink = wrapper.dive().find('.IndexMarks__marks-without-counts .IndexMarks__item').find('MetrikaLink').first();
    expect(markWithoutCountLink.props().url).toEqual(markWithoutCountUrl);
});

it('должен "засерить" марку, у которой в props каунт 0, но в state не 0', () => {
    const store = mockStore(storeMock);
    const context = {
        link: () => '',
        store,
    };

    const newProps = _.cloneDeep(props);

    const newMarksCounts = newProps.marksCounts;
    newMarksCounts.AUDI = 0;

    newProps.marksCounts = newMarksCounts;

    const wrapper = shallow(
        <IndexMarks { ...newProps }/>,
        { context: context });
    expect(wrapper.dive().find('.IndexMarks__item_no-count')).toHaveLength(1);
});

it('если все счетчики марок === 0, должен показать 34 марки и 0 каунтов', () => {
    const newMarksCounts = _.mapValues(props.marksCounts, () => 0);

    const newProps = _.cloneDeep(props);
    const newStoreMock = _.cloneDeep(storeMock);

    newProps.marksCounts = newMarksCounts;

    const newMarks = newStoreMock.indexBreadcrumbs.marks;
    newMarks.forEach(e => e.count = 0);
    newStoreMock.indexBreadcrumbs.marks = newMarks;

    const store = mockStore(newStoreMock);
    const context = {
        link: () => '',
        store,
    };
    const wrapper = shallow(
        <IndexMarks { ...newProps }/>,
        { context: context });
    //Должно быть 34 ссылки на марки
    expect(wrapper.dive().find('.IndexMarks__item')).toHaveLength(34);
    //не должно быть счетчиков
    expect(wrapper.dive().find('.IndexMarks__item-count')).toHaveLength(0);
    //ссылки не должны быть "засерены"
    expect(wrapper.dive().find('.IndexMarks__item_no-count')).toHaveLength(0);
});

describe('кнопка "Свернуть"', () => {
    const store = mockStore(storeMock);
    const context = {
        link: () => '',
        store,
    };
    const onCollapseClickMock = jest.fn();

    it('должен отобразить кнопку в развернутом списке  марок', () => {
        const wrapper = shallow(
            <IndexMarks { ...props } showAllMarks={ true }/>,
            { context: context });
        expect(wrapper.dive().find('.IndexMarks__collapse')).toMatchSnapshot();
    });

    it('не должен отобразить кнопку в свернутом списке марок', () => {
        const wrapper = shallow(
            <IndexMarks { ...props } showAllMarks={ false }/>,
            { context: context });
        expect(wrapper.dive().find('.IndexMarks__collapse')).toHaveLength(0);
    });

    it('должен вызвать метод onCollapseClick при клике', () => {
        const wrapper = shallow(
            <IndexMarks { ...props } showAllMarks={ true } onCollapseClick={ onCollapseClickMock }/>,
            { context: context });
        wrapper.dive().find('.IndexMarks__collapse').simulate('click');
        expect(onCollapseClickMock).toHaveBeenCalled();
    });
});

describe('super popular marks', () => {
    it('должен выбрать самые популярные марки в регионе', () => {
        const store = mockStore(storeMock);
        const context = {
            link: () => '',
            store,
        };
        const wrapper = shallow(
            <IndexMarks { ...props }/>,
            { context: context },
        ).dive();

        expect(wrapper.instance().superPopularMarks.map(item => item.id)).toEqual(
            [ 'VAZ', 'BMW', 'FORD', 'HYUNDAI', 'KIA', 'MERCEDES', 'NISSAN', 'RENAULT', 'TOYOTA', 'VOLKSWAGEN' ],
        );
    });

    it('должен добавить супер-популярные марки, если в регионе менее 10 марок', () => {
        // типа есть всего две марки с объявами
        const newStoreMock = _.cloneDeep(storeMock);
        newStoreMock.indexBreadcrumbs.marks.forEach(mark => {
            if (mark.id !== 'AUDI' && mark.id !== 'ACURA') {
                mark.count = 0;
            }
        });

        const store = mockStore(newStoreMock);
        const context = {
            link: () => '',
            store,
        };

        const wrapper = shallow(
            <IndexMarks { ...props }/>,
            { context: context },
        ).dive();

        expect(wrapper.instance().superPopularMarks.map(item => item.id)).toEqual(
            [ 'VAZ', 'ACURA', 'AUDI', 'BMW', 'FORD', 'HYUNDAI', 'KIA', 'MERCEDES', 'MITSUBISHI', 'TOYOTA' ],
        );
    });
});
