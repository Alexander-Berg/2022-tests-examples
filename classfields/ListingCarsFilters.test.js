const React = require('react');
const _ = require('lodash');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const questionMock = require('auto-core/react/dataDomain/autoguru/mocks/questions.mock');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ListingCarsFilters = require('./ListingCarsFilters');

const store = mockStore({
    listing: {
        data: {
            search_parameters: { section: 'used' },
            grouping: {
                groups_count: 999,
            },
        },
    },
    autoguru: {
        questions: questionMock,
        ansverValues: [],
        isInitialized: true,
        questionIndex: 0,
    },
});

const defaultProps = {
    filters: {
        seller_group: 'COMMERCIAL',
        sort: 'fresh_relevance_1-desc',
        section: 'used',
    },
    onChange: _.noop,
    searchParamsCounters: {},
    onChangeMode: _.noop,
};

describe('автогуру', () => {
    it('должен отрендерить кнопку перехода в помощник, если помощник включен', () => {
        const props = _.cloneDeep(defaultProps);
        props.isAutoGuruEnabled = true;
        const tree = shallowRenderComponent(props);

        expect(tree.find({ type: 'radio', value: 'guru' })).toHaveLength(1);
    });

    it('не должен отрендерить кнопку перехода в помощник, если помощник выключен', () => {
        const tree = shallowRenderComponent();
        expect(tree.find({ type: 'radio', value: 'guru' })).toHaveLength(0);
    });
});

it('вызывает показ кредитного попапа при переходе с главной', () => {
    const props = _.cloneDeep(defaultProps);

    props.showCreditPopup = jest.fn();
    props.prevPageType = 'index';
    props.filters = {
        ...props.filters,
        on_credit: true,
    };

    shallowRenderComponent(props);

    expect(props.showCreditPopup).toHaveBeenCalledTimes(1);
    expect(props.showCreditPopup).toHaveBeenCalledWith(true);
});

it('НЕ вызывает показ кредитного попапа при переходе с главной, но без кредитного фильтра', () => {
    const props = _.cloneDeep(defaultProps);

    props.showCreditPopup = jest.fn();
    props.prevPageType = 'index';

    shallowRenderComponent(props);

    expect(props.showCreditPopup).toHaveBeenCalledTimes(0);
});

it('НЕ вызывает показ кредитного попапа при переходе НЕ с главной', () => {
    const props = _.cloneDeep(defaultProps);

    props.showCreditPopup = jest.fn();
    props.prevPageType = 'listing';
    props.filters = {
        ...props.filters,
        on_credit: true,
    };

    shallowRenderComponent(props);

    expect(props.showCreditPopup).toHaveBeenCalledTimes(0);
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <ListingCarsFilters { ...props }/>,
        { context: { ...contextMock, store } },
    );
}
