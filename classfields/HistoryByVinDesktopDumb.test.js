/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const _ = require('lodash');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const HistoryByVinDesktop = require('./HistoryByVinDesktopDumb');
const VinHistory = require('auto-core/react/components/common/VinHistory');

const vinReportMock = require('auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock');
const paidReportMock = _.cloneDeep(vinReportMock);
paidReportMock.report.report_type = 'PAID_REPORT';

const defaultProps = {
    pageParams: { offer_id: '777' },
    offer: { id: '777' },
    resetOffer: jest.fn(),
    resetReport: jest.fn(),
    vinReport: paidReportMock,
    refetchVinReport: jest.fn(() => Promise.resolve()),
};

beforeEach(() => {
    defaultProps.refetchVinReport.mockClear();
    jest.useFakeTimers();
});

describe('дисклеймер для модераторов', () => {
    describe('будет показан', () => {
        it('будет показан, если юзер - модератор', () => {
            const tree = shallowRenderComponent({ ...defaultProps, isModerator: true });
            expect(tree.find(VinHistory).dive().dive().find('.HistoryByVin__moderator-disclaimer')).toExist();
        });

        it('будет показан, если юзер - модератор и есть ошибка', () => {
            const tree = shallowRenderComponent({ ...defaultProps, isModerator: true, vinReport: { error: 'all your base are belong to us' } });
            expect(tree.find('Connect(VinHistoryDumb)').dive().dive().find('.HistoryByVin__moderator-disclaimer')).toExist();
        });

    });

    it('не будет показан, если юзер - не модератор', () => {
        const tree = shallowRenderComponent();
        expect(tree.find(VinHistory).dive().dive().find('.HistoryByVin__moderator-disclaimer')).not.toExist();
    });
});

it('перезапросит отчет, если сервер ответил 202', () => {
    const tree = shallowRenderComponent();

    expect(tree.instance().state.refetchTimer).toBeNull();
    expect(defaultProps.refetchVinReport).toHaveBeenCalledTimes(0);

    tree.setProps({ vinReport: { error: 'IN_PROGRESS' } });

    expect(tree.instance().state.refetchTimer).not.toBeNull();
    jest.runOnlyPendingTimers();
    expect(defaultProps.refetchVinReport).toHaveBeenCalledTimes(1);
});

it('в любой непонятной ситуации показывает лоадер', () => {
    const tree = shallowRenderComponent({ ...defaultProps, vinReport: {} });
    expect(tree.find('Connect(VinHistoryDumb)').dive().dive().find('Loader')).toExist();
});

// Этот тест нужен, чтобы ошибку не рендерили вне VinReport, который
// следит за изменением params.history_entity_id. Без VinReport не происходит
// запрос за новым отчетов
it('ошибка выводится внутри VinReport', () => {
    const tree = shallowRenderComponent({
        ...defaultProps,
        vinReport: {
            ...defaultProps.vinReport,
            error: 'VIN_CODE_NOT_FOUND',
        },
    });

    expect(tree.find('Connect(VinHistoryDumb)').dive().dive().find('.HistoryByVin__error')).toExist();
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <HistoryByVinDesktop { ...props }/>,
        { context: {
            ...contextMock,
            store: mockStore({
                card: { category: 'cars' },
                user: { data: {} },
            }),
        } },
    );
}
