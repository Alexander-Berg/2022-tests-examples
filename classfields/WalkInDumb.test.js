jest.mock('auto-core/react/actions/scroll');

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');
const MockDate = require('mockdate');

const scrollTo = require('auto-core/react/actions/scroll');

const walkInMock = require('auto-core/react/dataDomain/walkIn/mocks/withData.mock');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const routertMock = require('autoru-frontend/mocks/routerMock');

const getDateLimits = require('www-cabinet/react/dataDomain/walkIn/selectors/getDateLimits');
const getWalkInSocialSecurityStats = require('auto-core/react/dataDomain/walkIn/selectors/getWalkInSocialSecurityStats');
const getWalkInTotalStats = require('auto-core/react/dataDomain/walkIn/selectors/getWalkInTotalStats');
const getWalkInTotalVisits = require('auto-core/react/dataDomain/walkIn/selectors/getWalkInTotalVisits');
const getWalkInEventsList = require('www-cabinet/react/dataDomain/walkIn/selectors/getWalkInEventsList');
const getWalkInPaging = require('www-cabinet/react/dataDomain/walkIn/selectors/getWalkInPaging');

const WalkInDumb = require('./WalkInDumb');

const baseProps = {
    dateLimits: getDateLimits(walkInMock),
    isEnabled: true,
    isExpandedLegend: false,
    isVisibleDescription: true,
    isEnabledSocialSecurityHistogram: true,
    isPageLoading: false,
    eventsList: getWalkInEventsList(walkInMock),
    paging: getWalkInPaging(walkInMock),
    socialSecurityStats: getWalkInSocialSecurityStats(walkInMock),
    totalStats: getWalkInTotalStats(walkInMock),
    totalVisits: getWalkInTotalVisits(walkInMock),

    onShowMoreEvents: _.noop,
    onCloseDescription: _.noop,
    onCollapseLegend: _.noop,
    onExpandLegend: _.noop,

    router: routertMock,
    routeName: 'foo',
    routeParams: {},
};

afterEach(() => {
    MockDate.reset();
});

it('должен скроллить к таблице при апдейте номера страницы', () => {
    scrollTo.mockImplementation(jest.fn());

    const tree = shallowRenderComponent(baseProps);
    const instance = tree.instance();

    instance.componentDidUpdate({ routeParams: { page: 5 } });

    expect(scrollTo).toHaveBeenCalledWith('WalkIn__table', { offset: -195 });
});

it('не должен рендерить гистограмму, если она неактивна', () => {
    const tree = shallowRenderComponent({
        ...baseProps,
        isEnabledSocialSecurityHistogram: false,
    });

    const histogramComponent = tree.find('.WalkIn__socialHistogram');

    expect(shallowToJson(histogramComponent)).toBeNull();
});

it('при переключении страницы в пагинации должен реплейсить роут со смердженными параметрами', () => {
    const router = {
        replace: jest.fn(),
    };

    const tree = shallowRenderComponent({
        ...baseProps,
        router,
        routeParams: { foo: 123 },
        routeName: 'bar',
    });

    const instance = tree.instance();

    instance.onClickPage(3);

    expect(router.replace).toHaveBeenCalledWith('linkCabinet/bar/?foo=123&page=3');
});

it('должен рендерить страницу без данных с плейсхолдером, если выбран текущий день', () => {
    MockDate.set('2015-01-12');

    const tree = shallowRenderComponent({
        ...baseProps,
        dateLimits: { from: '2015-01-12', to: '2015-01-12' },
    });

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен рендерить страницу без таблицы эвентов, если нет эвентов', () => {
    const tree = shallowRenderComponent({
        ...baseProps,
        eventsList: [],
        isEnabledSocialSecurityHistogram: false, // чтобы не засорять снэпшот
    });

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен сбрасывать номер страницы при изменениях фильтра', () => {
    const router = {
        replace: jest.fn(),
    };

    const tree = shallowRenderComponent({
        ...baseProps,
        router,
        routeParams: { page: 123 },
        routeName: 'bar',
    });

    const instance = tree.instance();

    instance.onChangeFilterValues({ from: '2015-02-09', to: '2015-02-10' });

    expect(router.replace).toHaveBeenCalledWith('linkCabinet/bar/?page=&from=2015-02-09&to=2015-02-10');
});

it('должен рендерить плейсхолдер, если страница неактивна', () => {
    const tree = shallowRenderComponent({
        ...baseProps,
        isEnabled: false,
    });

    expect(tree.find('WalkInPlaceholder')).toExist();
});

function shallowRenderComponent(props) {
    return shallow(
        <WalkInDumb { ...props }/>,
        { context: contextMock },
    );
}
