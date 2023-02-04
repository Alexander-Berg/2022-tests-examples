const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const CallsNumbersDumb = require('./CallsNumbersDumb');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
jest.mock('auto-core/react/actions/scroll', () => jest.fn());

const callsNumbersDumbBaseProps = {
    listing: [],
    unconfirmedListing: [],
    pagination: {
        page_num: 1,
        page_size: 5,
        total_count: 7,
        total_page_count: 2,
    },
    routeParams: {
        page: 1,
        redirect: '+71111111111',
    },
};

it('render: должен вернуть страницу звонков: описание, фильтры, листинг, пагинация, модалка жалобы', () => {
    const tree = shallow(
        <CallsNumbersDumb
            { ...callsNumbersDumbBaseProps }
        />, { context: contextMock },
    );

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('changeRouteParams: должен вызвать router.replace с корретными параметрами', () => {
    const router = {
        replace: jest.fn(),
    };
    const treeInstance = shallow(
        <CallsNumbersDumb
            { ...callsNumbersDumbBaseProps }
            router={ router }
            routeName="callsNumbers"
        />, { context: contextMock },
    ).instance();

    treeInstance.changeRouteParams({ redirect: '+72222222222' });

    expect(router.replace).toHaveBeenCalledWith('linkCabinet/callsNumbers/?page=1&redirect=%2B72222222222');
});

it('onPageButtonClick: должен вызвать this.changeRouteParams и scrollTo с кооретными параметрами', () => {
    const scrollTo = require('auto-core/react/actions/scroll');
    const treeInstance = shallow(
        <CallsNumbersDumb
            { ...callsNumbersDumbBaseProps }
        />, { context: contextMock },
    ).instance();

    treeInstance.changeRouteParams = jest.fn();
    treeInstance.onPageButtonClick(2);

    expect(scrollTo).toHaveBeenCalledWith('ServiceNavigation');
    expect(treeInstance.changeRouteParams).toHaveBeenCalledWith({ page: 2 });
});

it('showComplaintModal: должен вызвать setState с корретными параметрами', () => {
    const treeInstance = shallow(
        <CallsNumbersDumb
            { ...callsNumbersDumbBaseProps }
        />, { context: contextMock },
    ).instance();

    treeInstance.setState = jest.fn();
    treeInstance.showComplaintModal('currentCall')();

    expect(treeInstance.setState).toHaveBeenCalledWith({
        isShowingComplaintModal: true,
        currentCall: 'currentCall',
    });
});

it('onSubmitComplaintForm: должен вызвать createComplaint c корректными параметрами', () => {
    const createComplaint = jest.fn();
    const treeInstance = shallow(
        <CallsNumbersDumb
            { ...callsNumbersDumbBaseProps }
            createComplaint={ createComplaint }
        />, { context: contextMock },
    ).instance();

    treeInstance.state = {
        currentCall: '+71111111111',
    };
    treeInstance.onSubmitComplaintForm('params');

    expect(createComplaint).toHaveBeenCalledWith({
        redirect: '+71111111111',
        complaint: 'params',
    });
});

it('onCloseComplaintForm: должен вызвать setState с корретными параметрами', () => {
    const createComplaint = jest.fn();
    const treeInstance = shallow(
        <CallsNumbersDumb
            { ...callsNumbersDumbBaseProps }
            createComplaint={ createComplaint }
        />, { context: contextMock },
    ).instance();

    treeInstance.setState = jest.fn();
    treeInstance.onCloseComplaintForm();

    expect(treeInstance.setState).toHaveBeenCalledWith({
        isShowingComplaintModal: false,
        currentCall: undefined,
    });
});

it('componentDidMount: должен вызвать showErrorNotification, если hasError = true', () => {
    const showErrorNotification = jest.fn();
    const treeInstance = shallow(
        <CallsNumbersDumb
            hasError={ true }
            { ...callsNumbersDumbBaseProps }
            showErrorNotification={ showErrorNotification }
        />, { context: contextMock },
    ).instance();

    treeInstance.componentDidMount();

    expect(showErrorNotification).toHaveBeenCalledWith();
});
