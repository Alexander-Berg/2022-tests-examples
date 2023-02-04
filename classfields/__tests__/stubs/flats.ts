import { DeepPartial } from 'utility-types';

import { IUniversalStore } from 'view/modules/types';

import { IManagerFlat } from 'types/flat';

import { contractData } from '../../ManagerSearchFlatsItem/__test__/stubs/flats';

const generateStore = (map: Record<string, IManagerFlat>): DeepPartial<IUniversalStore> => {
    const order = [];
    for (const key of Object.keys(map)) order.push(key);
    const paging = {
        page: {
            num: 1,
            size: 5,
        },
        total: order.length,
        pageCount: Math.floor(order.length / 5),
    };
    return {
        managerSearchFlats: {
            order,
            map,
            paging,
        },
    };
};

export const getFlatsStore = (mapSize: number, pageNumber: number): DeepPartial<IUniversalStore> => {
    const map = {};
    for (let i = 0; i < mapSize; i++) map[`${i}`] = contractData;
    const store = generateStore(map);
    return {
        ...store,
        managerSearchFlats: {
            ...store.managerSearchFlats,
            paging: {
                page: {
                    size: 5,
                    num: pageNumber,
                },
                total: mapSize,
                pageCount: pageNumber,
            },
        },
    };
};
