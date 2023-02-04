jest.mock('auto-core/react/dataDomain/cookies/actions/set');

import contextMock from 'autoru-frontend/mocks/contextMock';

import stateMock from 'auto-core/react/AppState.mock';
import configMock from 'auto-core/react/dataDomain/config/mock';
import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';
import setCookie from 'auto-core/react/dataDomain/cookies/actions/set';

import item, { COOKIE_NAME } from './nps_modal';

describe('shouldRun():', () => {
    it('запустится, если есть кука со значением offer_published', () => {
        const state = {
            ...stateMock,
            cookies: { [COOKIE_NAME]: 'offer_published' },
        };

        const shouldRun = item.shouldRun(state);
        expect(shouldRun).toBe(true);
    });

    it('не запустится, если значение куки другое', () => {
        const state = {
            ...stateMock,
            cookies: { [COOKIE_NAME]: 'no_exp' },
        };

        const shouldRun = item.shouldRun(state);
        expect(shouldRun).toBe(false);
    });

    it('не запустится, если нет куки', () => {
        const shouldRun = item.shouldRun(stateMock);
        expect(shouldRun).toBe(false);
    });
});

describe('run():', () => {
    it('в экспе отправит метрику триггера и вернет объект', async() => {
        const state = {
            ...stateMock,
            config: configMock.withExperiments({ 'AUTORUFRONT-21692_nps': true }).value(),
        };

        const result = await item.run(state, () => {}, contextMock);

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'NPS', 'd_offer_success_publish', 'trigger' ]);
        expect(result).toEqual({ id: AutoPopupNames.NPS_MODAL });
    });

    it('вне экспа отправит метрику триггера и вернет undefined', async() => {
        const state = {
            ...stateMock,
            config: configMock.withExperiments({ 'AUTORUFRONT-21692_nps': false }).value(),
        };

        const result = await item.run(state, () => {}, contextMock);

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'NPS', 'd_offer_success_publish', 'trigger' ]);
        expect(setCookie).toHaveBeenCalledTimes(1);
        expect(setCookie).toHaveBeenCalledWith(COOKIE_NAME, 'no_exp', { expires: 365 });
        expect(result).toBeUndefined();
    });
});
