//TODO: удалить пропуск тестов через skip, когда добавят ручки для гаража
/* eslint-disable jest/no-disabled-tests */
import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';

import type { THttpResponse, THttpRequest } from 'auto-core/http';

import { getC2bLandingOffer } from './getC2bLandingOffer';

let context: ReturnType<typeof createContext>;
let req: THttpRequest;
let res: THttpResponse;

interface OfferPartial {
    id: string;
    category: string;
}

const DEFAULT_CATEGORY = 'cars';

const DRAFT_ID = '1-2';
const USER_OFFER_ID = '3';
const GARAGE_OFFER_ID = '4';

const draftDefault = {
    id: DRAFT_ID,
    category: DEFAULT_CATEGORY,
};
const userOfferDefault = {
    id: USER_OFFER_ID,
    category: DEFAULT_CATEGORY,
};
const garageOfferDefault = {
    id: GARAGE_OFFER_ID,
    category: DEFAULT_CATEGORY,
};

const appraisalDefaultValue = {
    can_apply: true,
    price_range: {
        from: 1000000,
        to: 12000000,
    },
    price_prediction: 1000000,
    car_info: {},
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
    jest.clearAllMocks();
});

describe('блок для получения офера/черновика/гаража и их оценки подходящих к аукциону', () => {
    describe('успешное выполнение запросов', () => {
        it('должен вернуть данные оффера', () => {
            mockResponses();

            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: userOfferDefault,
                        offerId: userOfferDefault.id,
                    });
                });
        });

        it('должен вернуть данные черновика, если оффер не подходит', () => {
            mockResponses({
                offers: [
                    { offer: userOfferDefault, canApply: false },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: draftDefault,
                        draftId: draftDefault.id,
                    });
                });
        });

        it.skip('должен вернуть данные офера из гаража, если оффер юзера и черновик не подходят', () => {
            mockResponses({
                offers: [
                    { offer: userOfferDefault, canApply: false },
                ],
                draft: { canApply: false },
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: garageOfferDefault,
                        garageCardId: garageOfferDefault.id,
                    });
                });
        });

        it.skip('должен вернуть данные первого подходящего оффера из гаража', () => {
            mockResponses({
                draft: { canApply: false },
                offers: [
                    { offer: userOfferDefault, canApply: false },
                ],
                garage: [
                    { offer: garageOfferDefault, canApply: false },
                    { offer: garageOfferDefault, canApply: false },
                    {
                        offer: {
                            ...garageOfferDefault,
                            id: '345',
                        },
                        canApply: true,
                    },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: {
                            ...garageOfferDefault,
                            id: '345',
                        },
                        garageCardId: '345',
                    });
                });
        });

        it('должен вернуть данные первого подходящего оффера из всех офферов юзера', () => {
            mockResponses({
                offers: [
                    { offer: userOfferDefault, canApply: false },
                    {
                        offer: {
                            ...userOfferDefault,
                            id: '323',
                        },
                        canApply: true,
                    },
                ],
            });
            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: {
                            ...userOfferDefault,
                            id: '323',
                        },
                        offerId: '323',
                    });
                });
        });

        it('возвращает ошибку, если все сущности не подходят', () => {
            mockResponses({
                draft: { canApply: false },
                offers: [
                    { offer: userOfferDefault, canApply: false },
                ],
                garage: [
                    { offer: garageOfferDefault, canApply: false },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .catch(error => error)
                .then((error) => {
                    expect(de.is_error(error)).toBe(true);
                });
        });
    });

    describe('выполнение запросов с ошибкой', () => {
        it('должен вернуть данные оффера черновика, если получение офферов юзера упало с ошибкой', () => {
            mockResponses({
                offersIsError: true,
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: draftDefault,
                        draftId: draftDefault.id,
                    });
                });
        });

        it('должен вернуть данные оффера черновика, если оценка офера упала с ошибкой', () => {
            mockResponses({
                canApplyOffersIsError: true,
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: draftDefault,
                        draftId: draftDefault.id,
                    });
                });
        });

        it.skip('должен вернуть данные офера гаража, если получение офферов юзера и черновика упали с ошибкой', () => {
            mockResponses({
                offersIsError: true,
                draftIsError: true,
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: garageOfferDefault,
                        garageCardId: garageOfferDefault.id,
                    });
                });
        });

        it('возвращает ошибку, если не удалось получить оценку ни по одной сущности', () => {
            mockResponses({
                offersIsError: true,
                garageIsError: true,
                canApplyDraftIsError: true,
            });

            return de.run(getC2bLandingOffer, {
                context,
            })
                .catch(error => error)
                .then((error) => {
                    expect(de.is_error(error)).toBe(true);
                });
        });
    });

    describe('запросы конкретных сущностей через параметры', () => {
        it('должен вернуть данные оффера', () => {
            mockResponses({
                offers: [
                    { offer: { ...userOfferDefault, id: '57' }, canApply: true },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    offer_id: '57',
                },
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: { ...userOfferDefault, id: '57' },
                        offerId: '57',
                    });
                });
        });

        it('должен вернуть данные оффера, даже если он не подходит к аукциону', () => {
            mockResponses({
                offers: [
                    { offer: { ...userOfferDefault, id: '57' }, canApply: false },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    offer_id: '57',
                },
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: { ...userOfferDefault, id: '57' },
                        can_apply: false,
                        offerId: '57',
                    });
                });
        });

        it('возвращает ошибку, если не удалось получить оценку по офферу', () => {
            mockResponses({
                canApplyOffersIsError: true,
                offers: [
                    { offer: { ...userOfferDefault, id: '57' }, canApply: false },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    offer_id: '57',
                },
            })
                .catch(error => error)
                .then(error => {
                    expect(de.is_error(error)).toBe(true);
                });
        });

        it('должен вернуть данные черновика', () => {
            mockResponses({
                draft: {
                    offer: { ...draftDefault, id: '3232' },
                },
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    draft_id: '3232',
                },
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: { ...draftDefault, id: '3232' },
                        draftId: '3232',
                    });
                });
        });

        it('возвращает ошибку, если черновик не удалось получить', () => {
            mockResponses({
                canApplyDraftIsError: true,
                draft: {
                    offer: { ...draftDefault, id: '3232' },
                },
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    draft_id: '3232',
                },
            })
                .catch(error => error)
                .then(error => {
                    expect(de.is_error(error)).toBe(true);
                });
        });

        it('должен вернуть данные черновика, даже если он не подходит к аукциону', () => {
            mockResponses({
                draft: {
                    offer: { ...draftDefault, id: '3232' },
                    canApply: false,
                },
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    draft_id: '3232',
                },
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        can_apply: false,
                        car_info: { ...draftDefault, id: '3232' },
                        draftId: '3232',
                    });
                });
        });

        it.skip('должен вернуть данные карточки из гаража', () => {
            mockResponses({
                garage: [
                    { offer: { ...garageOfferDefault, id: '322' }, canApply: true },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    garage_card_id: '322',
                },
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        car_info: { ...garageOfferDefault, id: '322' },
                        garageCardId: '322',
                    });
                });
        });

        it.skip('должен вернуть данные офера из гаража, даже если он не подходит к аукциону', () => {
            mockResponses({
                garage: [
                    { offer: { ...garageOfferDefault, id: '322' }, canApply: false },
                ],
            });

            return de.run(getC2bLandingOffer, {
                context,
                params: {
                    garage_card_id: '322',
                },
            })
                .then((result) => {
                    expect(result).toMatchObject({
                        ...appraisalDefaultValue,
                        can_apply: false,
                        car_info: { ...userOfferDefault, id: '322' },
                        garageCardId: '322',
                    });
                });
        });
    });
});

interface ResponseParams {
    draft?: {
        offer?: OfferPartial;
        canApply?: boolean;
    };
    offers?: Array<{
        offer: OfferPartial;
        canApply: boolean;
    }>;
    garage?: Array<{
        offer: OfferPartial;
        canApply: boolean;
    }>;

    draftIsError?: boolean;
    offersIsError?: boolean;
    garageIsError?: boolean;
    canApplyDraftIsError?: boolean;
    canApplyOffersIsError?: boolean;
    canApplyGarageIsError?: boolean;
}

// мокает запросы получения списка и конкретных сущностей
// черновиков/оферов/карточек гаража и соответствующие им ручки оценки
function mockResponses(params?: ResponseParams) {
    publicApi
        .get(`/1.0/user/draft/cars`)
        .reply(params?.draftIsError ? 404 : 200, {
            offer: params?.draft?.offer || draftDefault,
        });
    publicApi
        .get(`/1.0/user/draft/cars/${ params?.draft?.offer?.id || DRAFT_ID }`)
        .reply(params?.draftIsError ? 404 : 200, {
            offer: params?.draft?.offer || draftDefault,
        });
    publicApi
        .get(`/1.0/user/draft/cars/${ params?.draft?.offer?.id || DRAFT_ID }/c2b_application_info`)
        .reply(params?.canApplyDraftIsError ? 404 : 200, {
            ...appraisalDefaultValue,
            can_apply: typeof params?.draft?.canApply === 'boolean' ? params?.draft?.canApply : appraisalDefaultValue.can_apply,
            car_info: params?.draft?.offer || draftDefault,
        });

    const userOffers = params?.offers?.map(item => item.offer) || [ { ...userOfferDefault } ];
    publicApi
        .get(`/1.0/user/offers/cars?status=ACTIVE`)
        .reply(params?.offersIsError ? 404 : 200, {
            offers: userOffers,
        });
    if (params?.offers) {
        params?.offers.forEach(item => {
            publicApi
                .get(`/1.0/user/offers/cars/${ item.offer.id }`)
                .reply(params?.offersIsError ? 404 : 200, {
                    offer: item.offer,
                });
            publicApi
                .get(`/1.0/user/offers/cars/${ item.offer.id }/c2b_can_apply`)
                .reply(params?.canApplyOffersIsError ? 404 : 200, {
                    ...appraisalDefaultValue,
                    can_apply: item.canApply,
                    car_info: item.offer || userOfferDefault,
                });
        });
    } else {
        publicApi
            .get(`/1.0/user/offers/cars/${ USER_OFFER_ID }`)
            .reply(params?.offersIsError ? 404 : 200, {
                offer: userOfferDefault,
            });
        publicApi
            .get(`/1.0/user/offers/cars/${ USER_OFFER_ID }/c2b_can_apply`)
            .reply(params?.canApplyOffersIsError ? 404 : 200, {
                ...appraisalDefaultValue,
                car_info: userOfferDefault,
            });
    }

    const garageOffers = params?.garage?.map(item => item.offer) || [ { ...garageOfferDefault } ];
    publicApi
        .post(`/1.0/garage/user/cards`)
        .reply(params?.garageIsError ? 404 : 200, {
            listing: garageOffers,
        });

    if (params?.garage) {
        params?.garage.forEach(item => {
            publicApi
                .get(`/1.0/garage/user/card/${ item.offer.id }`)
                .reply(params?.garageIsError ? 404 : 200, {
                    card: item.offer,
                });
            publicApi
                //TODO: изменить url
                .get(`/1.0/user/draft/cars/garage/${ item.offer.id }/c2b_can_apply_garage_application`)
                .reply(params?.canApplyGarageIsError ? 404 : 200, {
                    ...appraisalDefaultValue,
                    can_apply: item.canApply,
                    car_info: item.offer || garageOfferDefault,
                });
        });
    } else {
        publicApi
            .get(`/1.0/garage/user/card/${ GARAGE_OFFER_ID }`)
            .reply(params?.garageIsError ? 404 : 200, {
                card: garageOfferDefault,
            });
        publicApi
            //TODO: изменить url
            .get(`/1.0/user/draft/cars/garage/${ GARAGE_OFFER_ID }/c2b_can_apply_garage_application`)
            .reply(params?.canApplyGarageIsError ? 404 : 200, {
                ...appraisalDefaultValue,
                car_info: garageOfferDefault,
            });
    }
}
