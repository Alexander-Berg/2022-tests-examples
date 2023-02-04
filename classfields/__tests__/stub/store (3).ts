import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IInventoryDefect, InventoryDefectClientId } from 'types/inventory';
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

export const defect: IInventoryDefect = {
    defectClientId: '1' as InventoryDefectClientId,
    description: 'Пятно',
    photos: [image],
};

export const defectWithLongName: IInventoryDefect = {
    defectClientId: '2' as InventoryDefectClientId,
    description: 'Очень очень грязное все, очень грязное все, очень грязное все, очень грязное все, очень грязное все',
    photos: [image],
};

export const defects = [defect, defectWithLongName];
