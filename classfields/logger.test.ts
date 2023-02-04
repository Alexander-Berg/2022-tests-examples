jest.mock('auto-core/lib/event-log/statApi', () => {
    return {
        log: jest.fn(),
    };
});

import _ from 'lodash';

import type { VasEvent } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';

import { VAS_EVENTS } from 'auto-core/lib/util/vas/dicts';
import statApi from 'auto-core/lib/event-log/statApi';

import { TBillingFrom, TWalletRefillService } from 'auto-core/types/TBilling';
import type { TOfferCategory } from 'auto-core/types/proto/auto/api/api_offer_model';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

let frontLogParams: Partial<VasEvent>;
const eventLogPromise = Promise.resolve();

import type { Params } from './logger';
import VasLogger from './logger';

const statApiLog = statApi.log as jest.MockedFunction<typeof statApi.log>;
statApiLog.mockImplementation((params: Partial<VasEvent>) => {
    frontLogParams = params;
    return eventLogPromise;
});

const metrikaMock = contextMock.metrika;
let metrikaParams: Array<string>;
metrikaMock.sendParams.mockImplementation((params: Array<string>) => {
    metrikaParams = params;
});

let params: Partial<Params>;
const DEFAULT_PARAMS = {
    event: VAS_EVENTS.show,
    serviceId: TOfferVas.TOP,
    price: 999,
    originalPrice: 1499,
    offerId: '12345678-abcdef',
    from: TBillingFrom.DESKTOP_LK,
    category: 'cars' as TOfferCategory,
};

beforeEach(() => {
    params = _.cloneDeep(DEFAULT_PARAMS);

    metrikaMock.sendParams.mockClear();
    statApiLog.mockClear();
});

it('правильно формирует параметры для логов', () => {
    const logger = new VasLogger(metrikaMock);
    logger.logVasEvent(params);

    expect(metrikaMock.sendParams).toHaveBeenCalledTimes(1);
    expect(statApi.log).toHaveBeenCalledTimes(1);

    expect(metrikaParams).toMatchSnapshot();
    expect(frontLogParams).toMatchSnapshot();
});

it('для неавторизованного пользователя ничего не отправит', () => {
    const logger = new VasLogger(metrikaMock);
    params.isAuth = false;
    logger.logVasEvent(params);

    expect(metrikaMock.sendParams).toHaveBeenCalledTimes(0);
    expect(statApi.log).toHaveBeenCalledTimes(0);
});

describe('умеет запоминать события показов', () => {
    let logger: VasLogger;
    beforeEach(() => {
        logger = new VasLogger(metrikaMock);
    });

    it('для "объявы + место + сервис" не будет логировать повторно', () => {
        logger.logVasEvent(params);

        return eventLogPromise
            .then(() => {
                logger.logVasEvent(params);
                logger.logVasEvent(params);
                logger.logVasEvent(params);
            })
            .then(() => {
                expect(metrikaMock.sendParams).toHaveBeenCalledTimes(1);
                expect(statApi.log).toHaveBeenCalledTimes(1);
            });
    });

    it('для "объявы + место" сделает два лога', () => {
        logger.logVasEvent(params);
        return eventLogPromise
            .then(() => {
                params.from = TBillingFrom.DESKTOP_CARD_VAS;
                logger.logVasEvent(params);
            })
            .then(() => {
                expect(metrikaMock.sendParams).toHaveBeenCalledTimes(2);
                expect(statApi.log).toHaveBeenCalledTimes(2);
            });
    });

    it('для "сервис + место" сделает два лога', () => {
        logger.logVasEvent(params);

        return eventLogPromise
            .then(() => {
                params.offerId = 'fooooooo-baaar';
                logger.logVasEvent(params);
            })
            .then(() => {
                expect(metrikaMock.sendParams).toHaveBeenCalledTimes(2);
                expect(statApi.log).toHaveBeenCalledTimes(2);
            });
    });

    it('для фронт лога ориентируется на from а для метрики на place', () => {
        params.serviceId = TOfferVas.REPORTS;
        params.from = TBillingFrom.DESKTOP_CARD_BUNDLE;
        logger.logVasEvent(params);

        return eventLogPromise
            .then(() => {
                params.from = TBillingFrom.DESKTOP_CARD_FREE_REPORT;
                logger.logVasEvent(params);
            })
            .then(() => {
                expect(metrikaMock.sendParams).toHaveBeenCalledTimes(1);
                expect(statApi.log).toHaveBeenCalledTimes(2);
            });
    });

    it('для других событий ничего не запоминает', () => {
        params.event = VAS_EVENTS.click;
        logger.logVasEvent(params);
        return eventLogPromise
            .then(() => {
                logger.logVasEvent(params);
            })
            .then(() => {
                expect(metrikaMock.sendParams).toHaveBeenCalledTimes(2);
                expect(statApi.log).toHaveBeenCalledTimes(2);
            });
    });
});

it('не будет отправлять метрику для не-васа', () => {
    const logger = new VasLogger(metrikaMock);
    params.serviceId = TWalletRefillService.BIND_CARD;
    logger.logVasEvent(params);

    expect(metrikaMock.sendParams).toHaveBeenCalledTimes(0);
    expect(statApi.log).toHaveBeenCalledTimes(1);
});

it('правильно отправит метрику для мобилки', () => {
    const logger = new VasLogger(metrikaMock);
    params.isMobile = true;
    logger.logVasEvent(params);

    expect(metrikaParams).toMatchSnapshot();
});

it('умеет запоминать дефолтные параметры и переопределять их', () => {
    const logger = new VasLogger(metrikaMock, {
        from: TBillingFrom.FORM_EDIT,
        category: 'moto',
    });
    delete params.from;
    logger.logVasEvent(params);

    expect(frontLogParams).toMatchSnapshot();
    expect(metrikaParams).toMatchSnapshot();
});

it('не будет отправлять фронт-лог листинга или со спец-блока', () => {
    params.from = TBillingFrom.SPECIALS_BLOCK;
    const logger = new VasLogger(metrikaMock);
    logger.logVasEvent(params);

    expect(metrikaMock.sendParams).toHaveBeenCalledTimes(1);
    expect(statApi.log).toHaveBeenCalledTimes(0);
});

describe('для события отмены', () => {
    it('если есть код ошибки отправит метрику с ним', () => {
        const logger = new VasLogger(metrikaMock);
        params.errorCode = 'my_error';
        params.event = VAS_EVENTS.error;
        logger.logVasEvent(params);

        expect(metrikaParams).toMatchSnapshot();
    });

    it('если нет кода ошибки отправит метрику с NA', () => {
        const logger = new VasLogger(metrikaMock);
        params.event = VAS_EVENTS.error;
        logger.logVasEvent(params);

        expect(metrikaParams).toMatchSnapshot();
    });
});
