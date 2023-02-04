import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import { IBreadcrumb } from 'types/breadcrumbs';

import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';

import {
    InventoryActions,
    InventoryDefectClientId,
    InventoryItemClientId,
    InventoryRoomClientId,
} from 'types/inventory';

import { IUniversalStore } from 'view/modules/types';

import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';
import { IInventoryStore } from 'view/modules/inventory/reducers';

const imageUrl = generateImageUrl({ width: 1000, height: 1000, size: 10 });

const breadcrumbs: IBreadcrumbsStore = {
    crumbs: [
        {
            route: 'user-flat',
            params: {
                flatId: 'dd59b158f52442dea2ac6c38c901d0e2',
            },
        } as IBreadcrumb,
    ],
    current: {
        route: 'owner-inventory-rooms',
        params: {
            flatId: 'dd59b158f52442dea2ac6c38c901d0e2',
            ownerRequestId: 'e7739be65de548c7bbb918c85c85c9fe',
        },
    },
};

const photos = [
    {
        namespace: 'arenda' as ImageNamespaces.ARENDA,
        groupId: 1396625,
        name: 'b481e54d76dd09442f00071599e29b5c',
        imageUrls: [
            {
                alias: 'orig' as DefaultNamespaceAliases,
                url: imageUrl,
            },
            {
                alias: '64x64' as DefaultNamespaceAliases,
                url: imageUrl,
            },
            {
                alias: '128x128' as DefaultNamespaceAliases,
                url: imageUrl,
            },
            {
                alias: '280x210' as DefaultNamespaceAliases,
                url: imageUrl,
            },
            {
                alias: '560x420' as DefaultNamespaceAliases,
                url: imageUrl,
            },
            {
                alias: '1024x1024' as DefaultNamespaceAliases,
                url: imageUrl,
            },
        ],
    },
];

const inventory: IInventoryStore = {
    rooms: [
        {
            roomName: 'Кухня',
            items: [
                {
                    itemName: 'Стол',
                    photos,
                    count: 2,
                    defectId: '64cd46f3-2d35-481d-ad77-2127e0bbe898' as InventoryDefectClientId,
                    itemClientId: 'f39d5f18-9a0d-4f2d-860e-4171f6f4f0a9' as InventoryItemClientId,
                },
                {
                    itemName: 'Стул',
                    count: 4,
                    itemClientId: 'eff99a95-d7ae-4360-b188-610feb7668a4' as InventoryItemClientId,
                    photos: [],
                },
            ],
            roomClientId: '1' as InventoryRoomClientId,
        },
        {
            roomName: 'Коридор',
            items: [],
            roomClientId: '2' as InventoryRoomClientId,
        },
        {
            roomName: 'Спальня',
            items: [
                {
                    itemName: 'Кровать',
                    count: 1,
                    itemClientId: 'e0fa4895-784c-44bc-8616-b8f645e7b2f9' as InventoryItemClientId,
                    photos,
                },
                {
                    itemName: 'Шкаф',
                    count: 1,
                    itemClientId: '0887232a-17a8-4e72-abb4-5166cfb13861' as InventoryItemClientId,
                    photos: [],
                },
                {
                    itemName: 'Кресло',
                    count: 1,
                    itemClientId: '5f8fa17e-150a-4f2f-8198-2ec9cec61392' as InventoryItemClientId,
                    photos,
                },
            ],
            roomClientId: '3' as InventoryRoomClientId,
        },
    ],
    defects: [
        {
            defectClientId: '64cd46f3-2d35-481d-ad77-2127e0bbe898' as InventoryDefectClientId,
            description: ' Сломана ножка',
            photos: [
                {
                    namespace: 'arenda' as ImageNamespaces.ARENDA,
                    groupId: 1396625,
                    name: 'd9f1156e78a43d950d644f7c3c7b1dfb',
                    imageUrls: [
                        {
                            alias: 'orig' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                        {
                            alias: '64x64' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                        {
                            alias: '128x128' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                        {
                            alias: '280x210' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                        {
                            alias: '560x420' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                        {
                            alias: '1024x1024' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                    ],
                },
            ],
        },
        {
            defectClientId: '64cd46f3-2d35-481d-ad77-2127e0bbe898' as InventoryDefectClientId,
            description: ' Сломан стул',
            photos: [],
        },
    ],
    confirmedByOwner: false,
    confirmedByTenant: false,
    version: 0,
};

export const filledStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    inventory,
    inventoryActions: ['EDIT' as InventoryActions],
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    inventoryActions: ['EDIT' as InventoryActions],
    spa: {
        status: RequestStatus.PENDING,
    },
};
