import { DeepPartial } from 'utility-types';

import { UID } from 'realty-core/types/common';

import { FlatUserRole } from 'realty-core/app/graphql/operations/types';

import { UserId } from 'types/user';
import { FlatId } from 'types/flat';

import { IUniversalStore } from 'view/modules/types';

export const getStore = (flatAddress?: string): DeepPartial<IUniversalStore> => {
    const flatId = '123456789' as FlatId;

    return {
        page: {
            params: {
                flatId,
            },
        },
        passport: {
            avatarHost: 'avatars.mdst.yandex.net',
            avatarId: '0/0-0',
            isAuth: true,
            displayName: 'Name',
        },
        user: {
            person: {
                name: 'Имя',
                surname: 'Фамилия',
            },
            uid: '000000000000' as UID,
            userId: '010101010101' as UserId,
            calculatedInfo: {
                roles: [FlatUserRole.Owner],
            },
        },
        menu: {
            isOpened: false,
        },
        userFlats: {
            order: [flatId],
            map: {
                [flatId]: {
                    address: {
                        address: flatAddress,
                    },
                },
            },
        },
    };
};

export const testProps = [
    {
        title: 'С логотипом',
        store: getStore(),
        props: {},
    },
    {
        title: 'C адресом',
        store: getStore('ул. Крутая д 34'),
        props: { withFlatAddress: true },
    },
    {
        title: 'Длинное название адреса',
        store: getStore('ул. Длинных слов  д3434 кв 23'),
        props: { withFlatAddress: true },
    },
    {
        title: 'C кнопкой поддержки',
        store: getStore('ул. Крутая д 34'),
        props: { withSupport: true },
    },
];
