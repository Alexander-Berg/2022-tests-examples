jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
}));
jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/cookies/actions/set');

import MockDate from 'mockdate';

import contextMock from 'autoru-frontend/mocks/contextMock';

import { MINUTE } from 'auto-core/lib/consts';

import stateMock from 'auto-core/react/AppState.mock';
import configMock from 'auto-core/react/dataDomain/config/mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';
import { getItem } from 'auto-core/react/lib/localstorage';
import gateApi from 'auto-core/react/lib/gateApi';
import setCookie from 'auto-core/react/dataDomain/cookies/actions/set';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import item, { COOKIE_NAME } from './nps_notification';

const getItemMock = getItem as jest.MockedFunction<typeof getItem>;
const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const currentDate = '2020-04-20';
const defaultSession = {
    autoPopupShown: false,
    openedAt: new Date(currentDate).getTime(),
    updatedAt: new Date(currentDate).getTime(),
    visits: 2,
};

beforeEach(() => {
    getItemMock.mockReturnValue(JSON.stringify(defaultSession));

    MockDate.set(currentDate);
});

afterEach(() => {
    MockDate.reset();
});

describe('shouldRun():', () => {
    it('запуститься, если все условия соблюдены', () => {
        getItemMock.mockReturnValueOnce(JSON.stringify({
            ...defaultSession,
            openedAt: new Date(currentDate).getTime() - 2 * MINUTE,
        }));

        const shouldRun = item.shouldRun(stateMock);
        expect(shouldRun).toBe(true);
    });

    it('не запустится, если есть кука', () => {
        const state = {
            ...stateMock,
            cookies: { [COOKIE_NAME]: 'no_exp' },
        };

        const shouldRun = item.shouldRun(state);
        expect(shouldRun).toBe(false);
    });

    it('не запустится, если пользователь дилер', () => {
        getItemMock.mockReturnValueOnce(JSON.stringify({
            ...defaultSession,
            openedAt: new Date(currentDate).getTime() - 2 * MINUTE,
        }));

        const state = {
            ...stateMock,
            user: userMock.withDealer(true).value(),
        };

        const shouldRun = item.shouldRun(state);
        expect(shouldRun).toBe(false);
    });
});

describe('run():', () => {
    it('если у пользователя есть активные офферы, поставит куку и вернет undefined', async() => {
        getResource.mockImplementation((resource: string) => {
            switch (resource) {
                case 'getUserOffersCategoryCount':
                    return Promise.resolve({ status: 'SUCCESS', count: 2 });
                case 'getDraftPublicApi':
                    return Promise.resolve();
                default:
                    return Promise.resolve();
            }
        });

        const result = await item.run(stateMock, () => { }, contextMock);

        expect(result).toBeUndefined();
        expect(setCookie).toHaveBeenCalledTimes(1);
        expect(setCookie).toHaveBeenCalledWith(COOKIE_NAME, 'has_active_offers', { expires: 1 });
    });

    it('если у пользователя нет активных офферов и нет начатого драфта, поставит куку и вернет undefined', async() => {
        getResource.mockImplementation((resource: string) => {
            switch (resource) {
                case 'getUserOffersCategoryCount':
                    return Promise.resolve({ status: 'SUCCESS', count: 0 });
                case 'getDraftPublicApi':
                    return Promise.resolve({ status: 'SUCCESS', offer: {} });
                default:
                    return Promise.resolve();
            }
        });

        const result = await item.run(stateMock, () => { }, contextMock);

        expect(result).toBeUndefined();
        expect(setCookie).toHaveBeenCalledTimes(1);
        expect(setCookie).toHaveBeenCalledWith(COOKIE_NAME, 'no_draft', { expires: 1 });
    });

    it('если у пользователя есть драфт и он в экспе, отправит триггер и вернет объект', async() => {
        getResource.mockImplementation((resource: string) => {
            switch (resource) {
                case 'getUserOffersCategoryCount':
                    return Promise.resolve({ status: 'SUCCESS', count: 0 });
                case 'getDraftPublicApi':
                    return Promise.resolve({ status: 'SUCCESS', offer: offerMock });
                default:
                    return Promise.resolve();
            }
        });
        const state = {
            ...stateMock,
            config: configMock.withExperiments({ 'AUTORUFRONT-21692_nps': true }).value(),
        };

        const result = await item.run(state, () => { }, contextMock);

        expect(setCookie).toHaveBeenCalledTimes(0);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'NPS', 'd_offer_second_session', 'trigger' ]);
        expect(result).toEqual({ id: AutoPopupNames.NPS_NOTIFICATION });
    });

    it('если у пользователя есть драфт и он не  в экспе, отправит триггер и вернет undefined', async() => {
        getResource.mockImplementation((resource: string) => {
            switch (resource) {
                case 'getUserOffersCategoryCount':
                    return Promise.resolve({ status: 'SUCCESS', count: 0 });
                case 'getDraftPublicApi':
                    return Promise.resolve({ status: 'SUCCESS', offer: offerMock });
                default:
                    return Promise.resolve();
            }
        });
        const state = {
            ...stateMock,
            config: configMock.withExperiments({ 'AUTORUFRONT-21692_nps': false }).value(),
        };

        const result = await item.run(state, () => { }, contextMock);

        expect(result).toBeUndefined();
        expect(setCookie).toHaveBeenCalledTimes(1);
        expect(setCookie).toHaveBeenCalledWith(COOKIE_NAME, 'no_exp', { expires: 365 });
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'NPS', 'd_offer_second_session', 'trigger' ]);
    });
});
