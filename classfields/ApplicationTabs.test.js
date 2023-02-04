const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ApplicationTabs = require('./ApplicationTabs');

const TEST_CASES_CATALOG = [
    {
        name: 'cars',
        params: { category: 'cars' },
    },
    {
        name: 'cars с маркой',
        params: {
            category: 'cars',
            mark: 'bmw',
        },
    },
    {
        name: 'cars с кучей всего',
        params: {
            category: 'cars',
            mark: 'bmw',
        },
    },
];

const TEST_CASES_VIDEO = [
    {
        name: 'cars',
        params: { category: 'cars' },
    },
    {
        name: 'cars с маркой',
        params: {
            category: 'cars',
            mark: 'bmw',
        },
    },
    {
        name: 'cars с кучей всего',
        params: {
            category: 'cars',
            complectation_id: '21397498_21397672_21397560',
            configuration_id: '21397498',
            mark: 'mitsubishi',
            model: 'outlander',
            super_gen: '21397304',
        },
    },
];

const TEST_CASES_REVIEWS = [
    {
        name: 'cars',
        params: {
            category: 'cars',
            geo_id: '213',
            parent_category: 'cars',
        },
    },
    {
        name: 'cars с маркой',
        params: {
            geo_id: '213',
            mark: 'audi',
            parent_category: 'cars',
        },
    },
    {
        name: 'cars с маркой и моделью',
        params: {
            geo_id: '213',
            mark: 'audi',
            model: 'a3',
            parent_category: 'cars',
        },
    },
    {
        name: 'moto',
        params: {
            parent_category: 'moto',
        },
    },
    {
        name: 'moto scooters',
        params: {
            category: 'scooters',
            parent_category: 'moto',
        },
    },
    {
        name: 'moto scooters марка модель',
        params: {
            category: 'scooters',
            parent_category: 'moto',
            mark: 'bmw',
            model: 'c650',
        },
    },
];

it('не должно быть табов в вебвью', () => {
    const tree = createTree({ onlyContent: true });
    expect(shallowToJson(tree)).toBe('');
});

describe('каталог', () => {
    TEST_CASES_CATALOG.forEach((testCase) => {
        it(`${ testCase.name }`, () => {
            const tree = createTree({ params: testCase.params, activeTab: 'catalog' });
            expect(shallowToJson(tree)).toMatchSnapshot();
        });
    });
});

describe('видео', () => {
    TEST_CASES_VIDEO.forEach((testCase) => {
        it(`${ testCase.name }`, () => {
            const tree = createTree({ params: testCase.params, activeTab: 'video' });
            expect(shallowToJson(tree)).toMatchSnapshot();
        });
    });
});

describe('отзывы', () => {
    TEST_CASES_REVIEWS.forEach((testCase) => {
        it(`${ testCase.name }`, () => {
            const tree = createTree({ params: testCase.params, activeTab: 'reviews' });
            expect(shallowToJson(tree)).toMatchSnapshot();
        });
    });
});

describe('Ссылка Выкупа', () => {
    it('Показывает ссылку, если передан параметр', () => {
        const tree = createTree({ isBuyoutLinkVisible: true });

        const buyoutLink = tree
            .dive()
            .find('.AppTabs__item')
            .filterWhere((wrapper) => wrapper.dive().text() === 'Выкуп');

        expect(buyoutLink.exists()).toBe(true);
    });

    it('Не показывает ссылку, если соответствующий параметр не передан', () => {
        const tree = createTree({});

        const buyoutLink = tree
            .dive()
            .find('.AppTabs__item')
            .filterWhere((wrapper) => wrapper.dive().text() === 'Выкуп');

        expect(buyoutLink.exists()).toBe(false);
    });
});

//  на странице статистики этот компонент не используется, хз почему.

const createTree = function({ activeTab, isBuyoutLinkVisible, onlyContent, params }) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <ApplicationTabs
                activeTab={ activeTab || '' }
                onlyContent={ onlyContent }
                params={ params }
                isBuyoutLinkVisible={ isBuyoutLinkVisible }
            />
        </ContextProvider>,
    ).dive();
};
