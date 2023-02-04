import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import {
    IInventoryRoom,
    IInventoryItem,
    InventoryItemClientId,
    InventoryDefectClientId,
    InventoryRoomClientId,
} from 'types/inventory';
import { ArendaNamespaceAliasesBase, IImage, ImageNamespaces } from 'types/image';

const image: IImage = {
    namespace: ImageNamespaces.ARENDA,
    groupId: 65493,
    name: '9708827f45323b9155762b1416208be9',
    imageUrls: [
        {
            alias: ArendaNamespaceAliasesBase._64x64,
            url: generateImageUrl({ width: 400, height: 400 }),
        },
        {
            alias: ArendaNamespaceAliasesBase._128x128,
            url: generateImageUrl({ width: 400, height: 400 }),
        },
    ],
};

export const item: IInventoryItem = {
    itemClientId: '1' as InventoryItemClientId,
    itemName: 'Стул',
    photos: [image],
    count: 1,
};

export const itemWithCount: IInventoryItem = {
    itemClientId: '2' as InventoryItemClientId,
    itemName: 'Стул',
    photos: [image],
    count: 3,
};

export const itemWithDefect: IInventoryItem = {
    itemClientId: '3' as InventoryItemClientId,
    itemName: 'Стул',
    photos: [image],
    count: 1,
    defectId: '1' as InventoryDefectClientId,
};

export const itemWithLongName: IInventoryItem = {
    itemClientId: '1' as InventoryItemClientId,
    itemName: 'Очень очень очень очень очень очень очень очень очень большой стол',
    photos: [image],
    count: 1,
};

export const room: IInventoryRoom = {
    roomClientId: '1' as InventoryRoomClientId,
    roomName: 'Кухня',
    items: [item, itemWithCount, itemWithDefect, itemWithLongName],
};
