import de from 'descript';

import type { PredictPrice } from '@vertis/schema-registry/ts-types-snake/auto/api/stats_model';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';

import type { THttpResponse, THttpRequest } from 'auto-core/http';

import { EvaluationErrorName } from './consts/evaluationErrorName';
import getEvaluation from './getEvaluation';

const DEFAULT_PREDICT = {
    prices: {
        autoru: {
            from: 1,
            to: 2,
        },
        tradein_dealer_matrix_new: {
            from: 4,
            to: 5,
        },
        tradein_dealer_matrix_used: {
            from: 6,
            to: 7,
        },
        tradein_dealer_matrix_buyout: {
            from: 8,
            to: 9,
        },
    } as PredictPrice,
};

let context: ReturnType<typeof createContext>;
let req: THttpRequest;
let res: THttpResponse;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
    jest.clearAllMocks();
});

describe('getEvaluation получает оценку стоимости, возможность c2b Выкупа и Трейд-ина', () => {
    it('возвращает данные предикта и Выкупа, если доступны все данные', async() => {
        mockResponses({
            predict: DEFAULT_PREDICT,
            buyout: {
                can_apply: true,
                price_range: {
                    from: 2,
                    to: 3,
                },
            },
            tradeIn: {
                is_available: true,
            },
        });

        await de.run(getEvaluation, { context })
            .then((result) => {
                expect(result).toEqual({
                    selfSale: {
                        price: {
                            from: 1,
                            to: 2,
                        },
                    },
                    buyout: {
                        price: {
                            from: 2,
                            to: 3,
                        },
                    },
                });
            });
    });

    it('возвращает данные предикта и Трейд-ина, если Выкуп недоступен', async() => {
        mockResponses({
            predict: DEFAULT_PREDICT,
            buyout: {
                can_apply: false,
                price_range: {
                    from: 0,
                    to: 0,
                },
            },
            tradeIn: {
                is_available: true,
            },
        });

        await de.run(getEvaluation, { context })
            .then((result) => {
                expect(result).toEqual({
                    selfSale: {
                        price: {
                            from: 1,
                            to: 2,
                        },
                    },
                    tradeIn: {
                        tradein_dealer_matrix_new: {
                            from: 4,
                            to: 5,
                        },
                        tradein_dealer_matrix_used: {
                            from: 6,
                            to: 7,
                        },
                        tradein_dealer_matrix_buyout: {
                            from: 8,
                            to: 9,
                        },
                    },
                });
            });
    });

    it('возвращает только данные предикта, если Выкуп и Трейд-ин недоступны', async() => {
        mockResponses({
            predict: DEFAULT_PREDICT,
            buyout: {
                can_apply: false,
                price_range: {
                    from: 0,
                    to: 0,
                },
            },
            tradeIn: {
                is_available: false,
            },
        });

        await de.run(getEvaluation, { context })
            .then((result) => {
                expect(result).toEqual({
                    selfSale: {
                        price: {
                            from: 1,
                            to: 2,
                        },
                    },
                });
            });
    });

    it('возвращает только предикт, если блоки Выкупа и Трейд-ина упали с ошибкой', async() => {
        mockResponses({
            predict: DEFAULT_PREDICT,
            isBuyoutError: true,
            isTradeIntError: true,
        });

        await de.run(getEvaluation, { context })
            .then((result) => {
                expect(result).toEqual({
                    selfSale: {
                        price: {
                            from: 1,
                            to: 2,
                        },
                    },
                });
            });
    });

    it('возвращает ошибку UNPROCESSABLE_ENTITY, если предикт ответил с 422 ошибкой', async() => {
        mockResponses({
            predict: {
                error: EvaluationErrorName.UNPROCESSABLE_ENTITY,
            },
            predictErrorCode: 422,
        });

        await de.run(getEvaluation, { context })
            .then((result) => {
                expect(result).toEqual({
                    errorName: 'UNPROCESSABLE_ENTITY',
                });
            });
    });

    it('возвращает ошибку OTHER, если предикт упал с другой ошибкой', async() => {
        mockResponses({
            predict: {
                error: EvaluationErrorName.OTHER,
            },
            predictErrorCode: 400,
        });

        await de.run(getEvaluation, { context })
            .then((result) => {
                expect(result).toEqual({
                    errorName: 'OTHER',
                });
            });
    });
});

interface ResponseParams {
    predict: {
        prices?: PredictPrice;
        error?: EvaluationErrorName;
    };
    buyout?: {
        can_apply: boolean;
        price_range: {
            from: number;
            to: number;
        };
    };
    tradeIn?: {
        is_available: boolean;
    };

    predictErrorCode?: number;
    isBuyoutError?: boolean;
    isTradeIntError?: boolean;
}

function mockResponses({ buyout, isBuyoutError, isTradeIntError, predict, predictErrorCode, tradeIn }: ResponseParams) {
    publicApi
        .post('/1.0/stats/predict')
        .reply(predictErrorCode ? predictErrorCode : 200, predict);

    publicApi
        .post('/1.0/c2b-auction/application/can_apply')
        .reply(isBuyoutError ? 400 : 200, buyout);

    publicApi
        .post('/1.0/trade-in/is_available')
        .reply(isTradeIntError ? 400 : 200, tradeIn);
}
