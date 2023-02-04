jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(() => new Promise(() => {})),
    };
});
jest.mock('auto-core/react/lib/proofOfWork');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';

import vinReportMock from 'auto-core/react/dataDomain/defaultVinReport/mocks/defaultVinReport.mock';
import gateApi from 'auto-core/react/lib/gateApi';
import { showAutoclosableMessage, VIEW } from 'auto-core/react/dataDomain/notifier/actions/notifier';

import VinReportExampleCurtain from './VinReportExampleCurtain';

const getResourcePublicApi = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResource>;
const showAutoclosableMessageMock = showAutoclosableMessage as jest.MockedFunction<typeof showAutoclosableMessage>;

beforeEach(() => {
    getResourcePublicApi.mockClear();
    showAutoclosableMessageMock.mockClear();
});

it('не должен запрашивать данные, если пример не открыт', () => {
    shallow(
        <VinReportExampleCurtain/>,
        { context: { store: mockStore({ state: {} }) } },
    ).dive();

    return Promise.resolve().then(() => {
        expect(getResourcePublicApi).not.toHaveBeenCalled();
    });
});

it('должен запрашивать данные, если пример открыт', () => {
    shallow(
        <VinReportExampleCurtain/>,
        { context: { store: mockStore({ state: { isReportExampleCurtainOpen: true } }) } },
    ).dive();

    return Promise.resolve().then(() => {
        expect(getResourcePublicApi).toHaveBeenCalledWith('getRichVinReport', {
            vin_or_license_plate: 'Z0NZWE00054341234',
            pow: {
                client_timestamp: 1,
                hash: 'hash',
                payload: 'Z0NZWE00054341234',
                time: 2,
                timestamp: 3,
            },
        });
    });
});

it('должен отрисовать лоадер пока данные грузятся', () => {
    const wrapper = shallow(
        <VinReportExampleCurtain/>,
        { context: { store: mockStore({ state: { isReportExampleCurtainOpen: true } }) } },
    );

    return Promise.resolve().then(() => {
        expect(wrapper.dive().find('Loader')).toExist();
    });
});

it('должен отрисовать отчет, если данные загрузились', () => {
    return new Promise<void>((done) => {
        getResourcePublicApi.mockResolvedValue({ report: vinReportMock });

        const wrapper = shallow(
            <VinReportExampleCurtain/>,
            { context: { store: mockStore({ state: { isReportExampleCurtainOpen: true } }) } },
        ).dive();

        setTimeout(() => {
            expect(wrapper.find('VinReportDesktop')).toExist();
            done();
        }, 500);
    });
});

it('должен закрыть шторку и вызвать нотифайку при ошибке', () => {
    getResourcePublicApi.mockRejectedValue('error');
    shallow(
        <VinReportExampleCurtain/>,
        { context: { store: mockStore({ state: { isReportExampleCurtainOpen: true } }) } },
    ).dive();

    return new Promise<void>((done) => {
        setTimeout(() => {
            expect(showAutoclosableMessageMock).toHaveBeenCalledWith({
                message: 'Не удалось загрузить пример отчёта',
                view: VIEW.ERROR,
            });
            done();
        }, 300);
    });
});
