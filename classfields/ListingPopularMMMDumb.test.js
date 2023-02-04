/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const breadcrumbsPublicApi = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ListingPopularMMM = require('./ListingPopularMMMDumb');

const MARK_LEVEL = breadcrumbsPublicApi.data.find(item => item.level === 'MARK_LEVEL');
const MODEL_LEVEL = breadcrumbsPublicApi.data.find(item => item.level === 'MODEL_LEVEL');

const TEST_CASES = [
    {
        name: 'пустой массив items',
        props: {
            items: [],
            mark: '',
            searchParameters: { catalog_filter: [ { mark: 'AUDI' }, { mark: 'BMW' } ] },
        },
    },
    {
        name: 'марки',
        props: {
            items: MARK_LEVEL.entities,
            mark: '',
            type: ListingPopularMMM.TYPE.MARKS,
            searchParameters: { catalog_filter: [] },
        },
    },
    {
        name: 'модели',
        props: {
            searchParameters: { catalog_filter: [ { mark: 'FORD' } ] },
            items: MODEL_LEVEL.entities,
            mark: 'FORD',
            type: ListingPopularMMM.TYPE.MODELS,
        },
    },
];

const defaultProps = {
    metrika: '',
    onClick: _.noop,
    pending: false,
    mark: '',
    searchParameters: { catalog_filter: [] },
    type: ListingPopularMMM.TYPE.MARKS,
    pageType: 'listing',
    listingPriceRanges: [],
};

TEST_CASES.forEach((testCase) => {
    const link = jest.fn();
    const ContextProvider = createContextProvider({ ...contextMock, link });
    const props = { ...defaultProps, ... testCase.props };
    it(`должен правильно отрендерить ListingPopularMMM, ${ testCase.name }`, () => {
        const tree = shallow(
            <ContextProvider>
                <ListingPopularMMM { ...props }/>
            </ContextProvider>,
        ).dive();
        expect(shallowToJson(tree)).toMatchSnapshot();
        expect(link.mock.calls[0]).toMatchSnapshot();
    });
});

it('не должен отобразить кнопку разворачивания списка, если все элементы отображены в списке популярных', () => {
    const ContextProvider = createContextProvider({ ...contextMock });
    const items = [
        { count: 10, id: 'mark_1', name: 'Mark 1' },
        { count: 10, id: 'mark_2', name: 'Mark 2' },
    ];
    const tree = shallow(
        <ContextProvider>
            <ListingPopularMMM
                { ...defaultProps }
                count={ 2 }
                items={ items }
            />
        </ContextProvider>,
    ).dive();
    expect(tree.findWhere(node => node.key() === 'linkToExpanded')).toHaveLength(0);
});

it('должен отобразить кнопку "Нет в продаже", если в расширенном списке нет других марок', () => {
    const ContextProvider = createContextProvider({ ...contextMock });
    const items = [
        { count: 10, id: 'mark_1', name: 'Mark 1' },
        { count: 0, id: 'mark_without_offers', name: 'Mark without offers' },
    ];
    const tree = shallow(
        <ContextProvider>
            <ListingPopularMMM
                { ...defaultProps }
                count={ 2 }
                items={ items }
            />
        </ContextProvider>,
    ).dive();
    expect(tree.findWhere(node => node.key() === 'linkToExpanded')).toMatchSnapshot();
});

it('не должен отобразить кнопку "Нет в продаже", если передан параметр hideEmpty', () => {
    const ContextProvider = createContextProvider({ ...contextMock });
    const items = [
        { count: 10, id: 'mark_1', name: 'Mark 1' },
        { count: 10, id: 'mark_2', name: 'Mark 2' },
        { count: 0, id: 'mark_3', name: 'Mark 3' },
    ];
    const tree = shallow(
        <ContextProvider>
            <ListingPopularMMM
                { ...defaultProps }
                count={ 2 }
                items={ items }
                hideEmpty={ true }
            />
        </ContextProvider>,
    ).dive();
    tree.findWhere(node => node.key() === 'linkToExpanded').simulate('click');
    expect(tree).toMatchSnapshot();
});

it('не должен отобразить кнопку "Нет в продаже", если есть фильтры в урле', () => {
    const ContextProvider = createContextProvider({ ...contextMock });
    const props = { ...defaultProps };
    props.searchParameters = { catalog_filter: [], body_type_group: [ 'SEDAN' ] };
    const items = [
        { count: 10, id: 'mark_1', name: 'Mark 1' },
        { count: 10, id: 'mark_2', name: 'Mark 2' },
        { count: 10, id: 'mark_3', name: 'Mark 3' },
        { count: 10, id: 'mark_4', name: 'Mark 4' },
        { count: 0, id: 'mark_5', name: 'Mark 5' },
        { count: 0, id: 'mark_6', name: 'Mark 6' },
    ];
    const tree = shallow(
        <ContextProvider>
            <ListingPopularMMM
                { ...props }
                count={ 2 }
                items={ items }
            />
        </ContextProvider>,
    ).dive();

    tree.findWhere(node => node.key() === 'linkToExpanded').simulate('click');
    expect(tree.findWhere(node => node.key() === 'linkToExpanded')).toHaveLength(0);
});

it('ссылки должны вести в каталог если нет офферов', () => {
    const ContextProvider = createContextProvider({ ...contextMock });
    const items = [
        { count: 10, id: 'mark_1', name: 'Mark 1' },
        { count: 10, id: 'mark_2', name: 'Mark 2' },
        { count: 10, id: 'mark_3', name: 'Mark 3' },
        { count: 10, id: 'mark_4', name: 'Mark 4' },
        { count: 0, id: 'mark_5', name: 'Mark 5' },
    ];
    const tree = shallow(
        <ContextProvider>
            <ListingPopularMMM
                { ...defaultProps }
                count={ 2 }
                items={ items }
            />
        </ContextProvider>,
    ).dive();

    tree.findWhere(node => node.key() === 'linkToExpanded').simulate('click');
    tree.findWhere(node => node.key() === 'linkToExpanded').simulate('click');
    expect(tree.find('ListingPopularMMMItem').last().props().url).toContain('catalog');
});

it('должен отобразить кнопку "Свернуть", если список полностью развернут', () => {
    const ContextProvider = createContextProvider({ ...contextMock });
    const items = [
        { count: 10, id: 'mark_1', name: 'Mark 1' },
        { count: 0, id: 'mark_without_offers', name: 'Mark without offers' },
    ];
    const tree = shallow(
        <ContextProvider>
            <ListingPopularMMM
                { ...defaultProps }
                count={ 2 }
                items={ items }
            />
        </ContextProvider>,
    ).dive();
    tree.setState({ stage: ListingPopularMMM.STAGE.ALL });
    expect(tree.findWhere(node => node.key() === 'linkToCollapseAll')).toHaveLength(1);
});

describe('сворачивание/разворачивание списка', () => {

    const ContextProvider = createContextProvider({ ...contextMock });

    const items = [
        { count: 10, id: 'mark_1', name: 'Mark 1' },
        { count: 10, id: 'mark_2', name: 'Mark 2' },
        { count: 0, id: 'mark_without_offers', name: 'Mark without offers' },
    ];

    let tree;
    let originalWindowScrollTo;
    beforeEach(() => {
        tree = shallow(
            <ContextProvider>
                <ListingPopularMMM
                    { ...defaultProps }
                    count={ 2 }
                    items={ items }
                />
            </ContextProvider>,
        ).dive();
        originalWindowScrollTo = global.window.scrollTo;
        global.window.scrollTo = jest.fn();
    });

    afterEach(() => {
        global.window.scrollTo = originalWindowScrollTo;
    });

    it('должен развернуть список по клику на ссылку "Все марки" и показать все марки, у которых есть офферы', () => {
        tree.findWhere(node => node.key() === 'linkToExpanded').simulate('click');
        expect(tree.state().stage).toBe(ListingPopularMMM.STAGE.WITH_OFFERS);
        expect(tree.find('ListingPopularMMMItem')).toHaveLength(2);
    });

    it(`должен развернуть список по клику на ссылку "Нет в продаже"
        и показать все марки, в том числе те, которых нет в продаже`, () => {
        tree.setState({ stage: ListingPopularMMM.STAGE.WITH_OFFERS });
        tree.findWhere(node => node.key() === 'linkToExpanded').simulate('click');
        expect(tree.state().stage).toBe(ListingPopularMMM.STAGE.ALL);
        expect(tree.find('ListingPopularMMMItem')).toHaveLength(3);
    });

    it(`должен свернуть список из полностью развернутого состояния на исходное, если нет других марок с офферами, а только популярные`, () => {
        tree.setProps({
            items: [
                { count: 10, id: 'mark_1', name: 'Mark 1' },
                { count: 0, id: 'mark_without_offers', name: 'Mark without offers' },
            ],
        });
        tree.setState({ stage: ListingPopularMMM.STAGE.ALL });
        tree.findWhere(node => node.key() === 'linkToCollapseAll').simulate('click');
        expect(tree.state().stage).toBe(ListingPopularMMM.STAGE.POPULAR);
        expect(tree.find('ListingPopularMMMItem')).toHaveLength(1);
    });

    it(`должен свернуть полностью развернутый список  в исходное состояние по клику на кнопку свернуть c подскроллом`, () => {
        React.createRef.mockReturnValueOnce = { current: { offsetTop: 0 } };
        tree.setState({ stage: ListingPopularMMM.STAGE.ALL });
        tree.setProps({
            isScrollToTopEnabled: true,
        });
        const instance = tree.instance();
        jest.spyOn(instance, 'scrollToTop');
        tree.setState({ shouldScroll: true });
        tree.findWhere(node => node.key() === 'linkToCollapseAll').simulate('click');
        expect(tree.state().stage).toBe(ListingPopularMMM.STAGE.POPULAR);
        expect(tree.find('ListingPopularMMMItem')).toHaveLength(1);
        expect(instance.scrollToTop).toHaveBeenCalled();
    });
});

describe('сортировки в ссылках', () => {
    it('должен удалить дефолтную сортировку из ссылок', () => {
        const props = {
            ...defaultProps,
            searchParameters: { catalog_filter: [], sort: 'fresh_relevance_1-desc' },
        };
        const ContextProvider = createContextProvider({ ...contextMock });
        const items = [
            { count: 10, id: 'mark_1', name: 'Mark 1' },
        ];
        const tree = shallow(
            <ContextProvider>
                <ListingPopularMMM
                    { ...props }
                    count={ 2 }
                    items={ items }
                />
            </ContextProvider>,
        ).dive();
        expect(tree.find('ListingPopularMMMItem').props().url).not.toContain('sort=');
    });

    it('должен сохранить недефолтную сортировку в ссылках', () => {
        const props = {
            ...defaultProps,
            searchParameters: { catalog_filter: [], sort: 'cr_date-desc' },
        };
        const ContextProvider = createContextProvider({ ...contextMock });
        const items = [
            { count: 10, id: 'mark_1', name: 'Mark 1' },
        ];
        const tree = shallow(
            <ContextProvider>
                <ListingPopularMMM
                    { ...props }
                    count={ 2 }
                    items={ items }
                />
            </ContextProvider>,
        ).dive();
        expect(tree.find('ListingPopularMMMItem').props().url).toContain('sort=cr_date-desc');
    });
});
