import gateApi from 'auto-core/react/lib/gateApi';

import offer from '../mock/offer.json';
import draft from '../mock/draft.json';
import expectedCreateDraft from '../mock/expectedCreateDraft';

import cloneOffer from './cloneOffer';

it('Должен сходить в оффер, создать из него черновик и добавить в черновик фотографии', () => {
    gateApi.getResource = jest.fn((params) => {
        if (params === 'getUserOffer') {
            return Promise.resolve(offer);
        }
        if (params === 'createDraft') {
            return Promise.resolve(draft);
        }
        return Promise.resolve();
    }) as jest.Mock;

    const dispatch = jest.fn();
    return cloneOffer({
        offerID: '1113844940-76294749',
        category: 'cars',
        clientId: undefined,
    })(dispatch).then(() => {
        expect((gateApi.getResource as jest.Mock).mock.calls).toEqual([
            [
                'getUserOffer',
                {
                    category: 'cars',
                    dealer_id: undefined,
                    offerID: '1113844940-76294749',
                },
            ],
            [
                'createDraft',
                {
                    draft: expectedCreateDraft,
                    category: 'cars',
                    client_id: undefined,
                },
            ],
            [
                'uploadDraftPhotoFromUrlList',
                {
                    category: 'cars',
                    dealer_id: undefined,
                    offerID: '3519484208139686852-582a98bb',
                    url: 'https://images.mds-proxy.test.avto.ru/get-autoru-vos/65698/02f7929046390cfc1393428fa6991210/1200x900',
                },
            ],
        ]);
    });
});
