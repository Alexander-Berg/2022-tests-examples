/**
 * @jest-environment node
 */
const _ = require('lodash');
const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./getOrderVinReportRaw');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;

const HISTORY_ENTITY_ID = 'SOME_ID';

const FULL_REPORT_SERVER_RESULT = {
    order: {
        status: 'SUCCESS',
        report_type: 'FULL_REPORT',
    },
    full_report: {
        status: 'SUCCESS',
    },
};

const CM_EXPERT_REPORT_SERVER_RESULT = {
    order: {
        status: 'SUCCESS',
        report_type: 'CM_EXPERT_REPORT',
    },
    cm_expert_report: {
        status: 'SUCCESS',
    },
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('report_type == FULL_REPORT', () => {
    it('должен нормально отработать нормальный ответ', () => {
        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, FULL_REPORT_SERVER_RESULT);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ report: FULL_REPORT_SERVER_RESULT.full_report });
            });
    });

    it('должен отдать ошибку, если report_type !== FULL_REPORT или не CM_EXPERT_REPORT', () => {
        const result = _.cloneDeep(FULL_REPORT_SERVER_RESULT);
        result.order.report_type = 'EMPTY_REPORT';

        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, result);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ error: 'HTTP_403', errorBody: 'Report type is invalid' });
            });
    });

    it('должен отдать ошибку, если отчет не наполнен (пустой)', () => {
        const result = _.cloneDeep(FULL_REPORT_SERVER_RESULT);
        result.full_report = {};

        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, result);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ error: 'HTTP_403', errorBody: 'Report is empty' });
            });
    });

    it('должен отдать ошибку, если отчет отстуствует', () => {
        const result = _.cloneDeep(FULL_REPORT_SERVER_RESULT);
        delete result.full_report;

        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, result);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ error: 'HTTP_403', errorBody: 'Report is empty' });
            });
    });

    it('должен отдать ошибку, если status === UNTRUSTED', () => {
        const result = _.cloneDeep(FULL_REPORT_SERVER_RESULT);
        result.full_report.status = 'UNTRUSTED';

        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, result);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ error: 'HTTP_403', errorBody: 'Report is untrusted' });
            });
    });
});

describe('report_type == CM_EXPERT_REPORT', () => {
    it('должен нормально отработать нормальный ответ', () => {
        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, CM_EXPERT_REPORT_SERVER_RESULT);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ report: CM_EXPERT_REPORT_SERVER_RESULT.cm_expert_report });
            });
    });

    it('должен отдать ошибку, если report_type не cm_expert_report', () => {
        const result = _.cloneDeep(CM_EXPERT_REPORT_SERVER_RESULT);
        result.order.report_type = 'EMPTY_REPORT';

        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, result);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ error: 'HTTP_403', errorBody: 'Report type is invalid' });
            });
    });

    it('должен отдать ошибку, если отчет не наполнен (пустой)', () => {
        const result = _.cloneDeep(CM_EXPERT_REPORT_SERVER_RESULT);
        result.cm_expert_report = {};

        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, result);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ error: 'HTTP_403', errorBody: 'Report is empty' });
            });
    });

    it('должен отдать ошибку, если отчет отстуствует', () => {
        const result = _.cloneDeep(CM_EXPERT_REPORT_SERVER_RESULT);
        delete result.cm_expert_report;

        publicApi
            .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
            .reply(200, result);

        return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
            .then((result) => {
                expect(result).toEqual({ error: 'HTTP_403', errorBody: 'Report is empty' });
            });
    });
});
