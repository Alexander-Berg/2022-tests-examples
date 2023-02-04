import { DeepPartial } from 'utility-types';

import { ICoreStore } from 'realty-core/view/react/common/reducers/types';

export interface IGetStoreArg {
    geo?: DeepPartial<ICoreStore['geo']>;
    user?: DeepPartial<ICoreStore['user']>;
}

export const getStore = ({ geo, user }: IGetStoreArg = {}): DeepPartial<ICoreStore> => ({
    geo: {
        rgid: 0,
        isInMO: false,
        isInLO: false,
        ...geo,
    },
    user: {
        isJuridical: false,
        isAuth: false,
        ...user,
    },
});
