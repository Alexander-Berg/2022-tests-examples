const React = require('react');

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const vinReport = require('auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock');

jest.mock('auto-core/react/dataDomain/vinReport/actions/refreshVinReport', () => ({
    'default': jest.fn(() => () => { }),
}));

const refreshVinReport = require('auto-core/react/dataDomain/vinReport/actions/refreshVinReport').default;

const VinReportRefresh = require('./VinReportRefresh');

it('должен обновлять обновляемый отчёт по клику', () => {
    const state = {
        vinReport,
    };

    const vinReportData = {
        ...vinReport,
        reload_params: { allow_reload: true },
    };

    const store = mockStore(state);

    const wrapper = shallow(
        <VinReportRefresh vinReport={ vinReportData }/>,
        { context: { ...contextMock, store } },
    ).dive();
    wrapper.find('.VinReportRefresh__refresh').simulate('click');
    expect(refreshVinReport).toHaveBeenCalledWith({ category: 'cars', offerID: '1084368429-e9a4c888' });
});

it('не должен ничего рендерить, если обновлять нельзя', () => {
    const state = {
        vinReport,
    };
    const store = mockStore(state);

    const wrapper = shallow(
        <VinReportRefresh vinReport={ vinReport }/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(wrapper.isEmptyRender()).toBe(true);
});
