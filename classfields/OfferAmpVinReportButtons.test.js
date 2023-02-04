const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const pageParamsMock = require('autoru-frontend/mockData/pageParams_cars.mock.js');

const cardVinReportFree = require('auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const offer = _.cloneDeep(offerMock);

const OfferAmpVinReportButtons = require('./OfferAmpVinReportButtons');

it('должен в AMP нарисовать кнопку "смотреть в приложении", если отчёт оплачен', () => {
    const store = mockStore({ state: {} });
    const cardVinReportPaid = _.cloneDeep(cardVinReportFree);
    cardVinReportPaid.report.report_type = 'PAID_REPORT';
    const wrapper = shallow(
        <OfferAmpVinReportButtons
            offer={ offer }
            vinReport={ cardVinReportPaid }
            pageParams={ pageParamsMock }
        />, { context: { ...contextMock, store } },
    );

    const fullReportButtons = wrapper.find('.OfferAmpVinReportButtons');
    expect(shallowToJson(fullReportButtons)).toMatchSnapshot();
});

it('должен в AMP нарисовать кнопку "купить отчёт", если нет квоты и нет покета', () => {
    const store = mockStore({ state: {} });
    const cardVinReportFree1 = _.cloneDeep(cardVinReportFree);
    const wrapper = shallow(
        <OfferAmpVinReportButtons
            offer={ offer }
            vinReport={ cardVinReportFree1 }
            pageParams={ pageParamsMock }
        />, { context: { ...contextMock, store } },
    );

    const fullReportButtons = wrapper.find('.OfferAmpVinReportButtons');
    expect(shallowToJson(fullReportButtons)).toMatchSnapshot();
});
