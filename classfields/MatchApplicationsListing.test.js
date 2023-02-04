jest.mock('auto-core/react/actions/scroll', () => {
    return jest.fn();
});

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const matchApplicationsMock = require('www-cabinet/react/dataDomain/matchApplications/mocks/withApplications.mock');

const scrollTo = require('auto-core/react/actions/scroll');

const MatchApplicationsListing = require('./MatchApplicationsListing');

const BILLING_STATUSES = require('www-cabinet/data/matchApplications/billing-statuses');

const baseProps = {
    onShowMore: _.noop,
    onChangePage: _.noop,
    isLoading: false,
    applicationsPhones: {},
    paging: matchApplicationsMock.applicationsList.paging,
    items: matchApplicationsMock.applicationsList.match_applications,
    fetchApplicationPhone: _.noop,
};

it('должен вызывать экшн скролла, если изменилась страница', () => {
    const tree = shallow(
        <MatchApplicationsListing { ...baseProps }/>,
    );

    const newPaging = _.cloneDeep(baseProps.paging);
    newPaging.page.num = 3;

    const newItems = baseProps.items.slice(0, 1);

    const newProps = {
        ...baseProps,
        items: newItems,
        paging: newPaging,
    };

    tree.setProps(newProps);

    expect(scrollTo).toHaveBeenCalledWith('MatchApplicationsListing', { offset: -100 });
});

it('должен вызывать загрузку следующей страницы при переключении заявки, если это последний элемент на странице, и страница не последняя', () => {
    const onChangePage = jest.fn();

    const tree = shallow(
        <MatchApplicationsListing
            { ...baseProps }
            onChangePage={ onChangePage }
        />,
    );

    const instance = tree.instance();
    instance.setState({ activeIndex: 2 });
    instance.onClickNextApplication();

    expect(onChangePage).toHaveBeenCalledWith(3);
});

it('должен вызывать загрузку предыдущей страницы при переключении заявки, если это первый элемент на странице, и страница не первая', () => {
    const onChangePage = jest.fn();

    const tree = shallow(
        <MatchApplicationsListing
            { ...baseProps }
            onChangePage={ onChangePage }
        />,
    );

    const instance = tree.instance();
    instance.setState({ activeIndex: 0 });
    instance.onClickPrevApplication();

    expect(onChangePage).toHaveBeenCalledWith(1);
});

it('не должен вызывать экшены загрузки страниц при переключении заявки, если это не крайний элемент на странице', () => {
    const onChangePage = jest.fn();

    const tree = shallow(
        <MatchApplicationsListing
            { ...baseProps }
            onChangePage={ onChangePage }
        />,
    );

    const instance = tree.instance();

    instance.setState({ activeIndex: 1 });
    instance.onClickNextApplication();

    instance.setState({ activeIndex: 1 });
    instance.onClickPrevApplication();

    expect(onChangePage).not.toHaveBeenCalled();
});

const TEST_CASES = [
    { status: BILLING_STATUSES.PENDING, 'case': 'должен писать вместо стоимости плейсхолдер "В обработке", если статус "обрабатывается"', text: 'В обработке' },
    { status: BILLING_STATUSES.NEW, 'case': 'должен писать вместо стоимости плейсхолдер "В обработке", если статус "новый"', text: 'В обработке' },
    { status: BILLING_STATUSES.PAID, 'case': 'должен писать стоимость заявки для оплаченного статуса', text: '250 ₽' },
];

TEST_CASES.forEach((testCase) => {
    it(testCase.case, () => {
        const items = _.cloneDeep(matchApplicationsMock.applicationsList.match_applications).slice(0, 1);
        items[0].billing_status = testCase.status;

        const tree = shallow(
            <MatchApplicationsListing
                { ...baseProps }
                items={ items }
            />,
        );

        const price = tree.find('.MatchApplicationsListing__item .MatchApplicationsListing__price');

        expect(price.find('.MatchApplicationsListing__price').text()).toEqual(testCase.text);
    });
});
