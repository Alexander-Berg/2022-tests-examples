import type { DealView as Deal } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/model';

import mockStore from 'autoru-frontend/mocks/mockStore';
import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';

import uploadDkp from 'auto-core/react/dataDomain/safeDeal/actions/uploadDkp';
import { SAFE_DEAL_UPLOAD_DKP_REJECTED } from 'auto-core/react/dataDomain/safeDeal/types';
import type { SafeDeal } from 'auto-core/react/dataDomain/safeDeal/types';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

interface State {
    safeDeal: SafeDeal;
}

let store: ThunkMockStore<State>;
beforeEach(() => {
    store = mockStore({
        safeDeal: {
            deal: {} as Deal,
            offer: {} as Offer,
            dkpPhotosPending: [],
            dkpPhotosError: [],
            pending: false,
            error: false,
            isDealCreatePending: false,
        },
    });
});

it('должен отправить SAFE_DEAL_UPLOAD_DKP_REJECTED, если тип файла не png/jpeg', () => {
    const expectedActions = [ {
        type: SAFE_DEAL_UPLOAD_DKP_REJECTED,
        name: 'test',
    } ];

    const params = {
        dealId: '123123',
        file: {
            type: 'application/pdf',
        } as File,
        name: 'test',
        uploadUrl: '',
    };

    store.dispatch(uploadDkp(params));

    expect(store.getActions()).toEqual(expectedActions);
});
