import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { OutstaffRoles } from 'types/outstaff';
import { FlatId, IOutstaffFlat } from 'types/flat';

export const outStaffFlats: DeepPartial<IOutstaffFlat>[] = [
    {
        flat: {
            flatId: '0' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, пр‑кт Энергетиков, д 30 к 1',
                flatNumber: '1',
            },
            code: '19-COVID',
        },
    },
    {
        flat: {
            flatId: '1' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '2',
            },
            code: '20-COVID',
        },
    },
    {
        flat: {
            flatId: '2' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, ул Новосибирская, д 1, кв 9',
                flatNumber: '3',
            },
            code: '21-COVID',
        },
    },
    {
        flat: {
            flatId: '3' as FlatId,
            address: {
                address: 'г Москва, ул Шаболовка, д 28/11 стр 4, кв 1',
                flatNumber: '4',
            },
            code: '22-COVID',
        },
    },
    {
        flat: {
            flatId: '4' as FlatId,
            address: {
                address: 'г. Москва, ул. Народного ополчения, 39к1, кв 10',
                flatNumber: '5',
            },
            code: '23-COVID',
        },
    },
    {
        flat: {
            flatId: '5' as FlatId,
            address: {
                address: 'г Москва, ул Гризодубовой, д 3, кв 38',
                flatNumber: '6',
            },
            code: '24-COVID',
        },
    },
    {
        flat: {
            flatId: '6' as FlatId,
            address: {
                address: 'г. Москва, ул. Народного ополчения, 39к1, кв 10',
                flatNumber: '7',
            },
            code: '25-COVID',
        },
    },
    {
        flat: {
            flatId: '7' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Ланское шоссе, д 20 к 4, кв 44',
                flatNumber: '8',
            },
            code: '26-COVID',
        },
    },
    {
        flat: {
            flatId: '8' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Приморский пр‑кт, д 9, кв 8',
                flatNumber: '9',
            },
            code: '27-COVID',
        },
    },
    {
        flat: {
            flatId: '9' as FlatId,
            address: {
                address: 'г. Москва, ул. Народного ополчения, 39к1, кв 10',
                flatNumber: '10',
            },
            code: '28-COVID',
        },
    },
];

export const baseStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },

    outstaffSearchFlats: {
        order: [],
        map: {},
        paging: {
            page: {
                num: 1,
                size: 10,
            },
            total: 0,
            pageCount: 0,
        },
    },

    page: {
        params: {
            role: OutstaffRoles.retoucher,
        },
    },
};

export const withSkeletonStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const getStoreWithRole = (role: OutstaffRoles): DeepPartial<IUniversalStore> => {
    return {
        spa: {
            status: RequestStatus.LOADED,
        },

        outstaffSearchFlats: {
            order: [],
            map: {},
            paging: {
                page: {
                    num: 1,
                    size: 10,
                },
                total: 0,
                pageCount: 0,
            },
        },

        page: {
            params: {
                role,
            },
        },
    };
};

export const getStoreWithFlats = (total: number, pageNum = 1, pageSize?: number): DeepPartial<IUniversalStore> => {
    const order: string[] = [];
    const map: Record<FlatId, IOutstaffFlat> = {};

    Array(pageSize || total)
        .fill(0)
        .forEach((_, index) => {
            const currentIndex = index % 10;

            order[currentIndex] = outStaffFlats[currentIndex].flat?.flatId || '0';
            map[order[currentIndex]] = outStaffFlats[currentIndex];
        });

    const outstaffSearchFlats = {
        ...baseStore.outstaffSearchFlats,
        order,
        map,
        paging: {
            page: {
                num: pageNum,
                size: 10,
            },
            total: total,
            pageCount: Math.ceil(total / 10),
        },
    };

    return {
        ...baseStore,
        outstaffSearchFlats,
    };
};
