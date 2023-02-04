import noop from 'lodash/noop';

import { createRootReducer } from 'realty-core/view/react/libs/test-helpers';
import { backCallApiReducer } from 'realty-core/view/react/modules/back-call/redux/reducer';

export interface IGate {
    create(): Promise<void>;
}

export const pendingGate: IGate = {
    create: () => new Promise(noop),
};

export const errorGate: IGate = {
    create: () => Promise.reject(),
};

export const errorAlreadyCreatedGate: IGate = {
    create: () => Promise.reject({ code: 'ALREADY_CREATED' }),
};

export const defaultPhone = '+71234567890';

export const userPayload = {
    defaultPhone,
};

export const unauthorizedUserPayload = {
    defaultPhone: null,
};

export const rootReducer = createRootReducer({
    backCallApi: backCallApiReducer,
});
