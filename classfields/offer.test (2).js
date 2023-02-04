jest.mock('auto-core/react/lib/gateApi');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const gateApi = require('auto-core/react/lib/gateApi');
const actions = require('./offer');

describe('activateOffer()', () => {
    let store;
    beforeEach(() => {
        store = mockStore({
            sales: {
                items: [ { saleId: 'id', offerFromStare: true } ],
            },
        });
        gateApi.getResource.mockReset();
    });

    it('должен отправить правильный ajax-запрос', () => {
        gateApi.getResource.mockImplementation(() => Promise.resolve({ status: 'SUCCESS' }));

        store.dispatch(actions.activateOffer({ offerIDHash: 'id', category: 'cars' }));
        expect(gateApi.getResource).toHaveBeenCalledWith(
            'offerActivate',
            { offerID: 'id', category: 'cars' },
            null,
            { processError: false },
        );
    });

    it('должен обновить объявление после успешного ответа, если есть updateOffer', async() => {
        const promiseActivate = Promise.resolve({ status: 'SUCCESS' });
        const promiseGetOffer = Promise.resolve({ getUserOfferResponse: true });
        gateApi.getResource
            .mockImplementationOnce(() => promiseActivate)
            .mockImplementationOnce(() => promiseGetOffer);

        const result = store.dispatch(actions.activateOffer({
            category: 'cars',
            offerIDHash: 'id',
            updateOffer: {
                category: 'card',
                offerID: 'offerIdToUpdate',
            },
        }));

        await result;
        expect(store.getActions()).toEqual([
            { type: 'SALES_UPDATE_OFFER', payload: { offerID: 'offerIdToUpdate', offer: { getUserOfferResponse: true } } },
            { type: 'SALES_SET_UNFOLDED_STATE', payload: { offerId: 'id', isUnfolded: true } },
        ]);
    });

    it('не должен обновить объявление после успешного ответа, если нет updateOffer', async() => {
        const promiseActivate = Promise.resolve({ status: 'SUCCESS' });
        const promiseGetOffer = Promise.resolve({ getUserOfferResponse: true });
        gateApi.getResource
            .mockImplementationOnce(() => promiseActivate)
            .mockImplementationOnce(() => promiseGetOffer);

        store.dispatch(actions.activateOffer({
            category: 'cars',
            offerIDHash: 'id',
        }));

        await promiseActivate;
        await promiseGetOffer;
        expect(store.getActions()).toEqual([]);
    });

    it('должен вернуть ошибку, если в ответе status !== "SUCCESS"', () => {
        const promiseActivate = Promise.resolve({});
        gateApi.getResource.mockImplementationOnce(() => promiseActivate);

        return store.dispatch(actions.activateOffer({
            category: 'cars',
            offerIDHash: 'id',
        })).then(
            () => {
                throw new Error('resolved');
            },
            (error) => {
                expect(error).toEqual({ error: 'UNEXPECTED_ERROR' });
            },
        );
    });

    it('должен вызвать диалог активации, если она платная', () => {
        const promiseActivate = Promise.resolve({
            status: 'ERROR',
            error: 'PAYMENT_NEEDED',
            paid_reason: 'paid_reason_value',
        });
        gateApi.getResource.mockImplementationOnce(() => promiseActivate);

        return store.dispatch(actions.activateOffer({
            category: 'cars',
            offerIDHash: 'id',
        })).then(() => {
            expect(store.getActions()).toEqual([
                {
                    type: 'OPEN_ACTIVATE_OFFER_MODAL',
                    payload: {
                        paidReason: 'paid_reason_value',
                        offerId: 'id',
                        category: 'cars',
                        from: 'new-lk-tab',
                        shouldUpdateOfferAfter: true,
                        shouldUnfoldOfferAfter: true,
                        shouldShowSuccessTextAfter: true,
                        successText: 'Объявление успешно активировано',
                        services: [ { service: 'all_sale_activate' } ],
                        returnUrl: undefined,
                        isOpened: true,
                    },
                },
            ]);
        });
    });

    it('должен вызвать диалог похожего объявления, если оно есть', () => {
        const promiseActivate = Promise.resolve({
            status: 'ERROR',
            error: 'HAVE_SIMILAR_OFFER',
            similar_offer: { similar_offer_response: true },
            price_info: { price_info_response: true },
        });
        gateApi.getResource.mockImplementationOnce(() => promiseActivate);

        return store.dispatch(actions.activateOffer({
            category: 'cars',
            offerIDHash: 'id',
        })).then(() => {
            expect(store.getActions()).toEqual([
                {
                    type: 'OPEN_SAME_OFFER_MODAL',
                    payload: {
                        currentOffer: { saleId: 'id', offerFromStare: true },
                        similarOffer: { similar_offer_response: true },
                        price: { price_info_response: true },
                    },
                },
            ]);
        });
    });

    it('должен вернуть ошибку, если в объявлении нет телефона', () => {
        const promiseActivate = Promise.resolve({
            status: 'ERROR',
            error: 'NO_PHONE',
        });
        gateApi.getResource.mockImplementationOnce(() => promiseActivate);

        return store.dispatch(actions.activateOffer({
            category: 'cars',
            offerIDHash: 'id',
        })).then(
            () => {
                throw new Error();
            },
            (error) => {
                expect(error).toEqual({
                    error: 'NO_PHONE',
                    message: 'В объявлении не хватает номера телефона — добавьте его, чтобы активировать публикацию.',
                });
            },
        );
    });
});
