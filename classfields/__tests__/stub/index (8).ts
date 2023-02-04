import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { ImageUploaderEntityIdentity, ImageUploaderImageId } from 'types/imageUploader';
import { ImageNamespaces } from 'types/image';

import { initialState as fieldsInitialState } from 'view/modules/houseServicesPeriodBillForm/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/houseServicesPeriodBillForm/reducers/network';
import { IUniversalStore } from 'view/modules/types';

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
            periodId: '76576576',
        },
    },
    houseServicesPeriodBillForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    imageUploader: {
        houseServicesBillPhotos: {
            images: [],
        },
    },
    config: { isMobile: '' },
};

export const storeWithImages: DeepPartial<IUniversalStore> = {
    ...store,
    imageUploader: {
        houseServicesBillPhotos: {
            images: [
                {
                    entityId: 'houseServicesBillPhotos' as ImageUploaderEntityIdentity,
                    imageId: 'b1db5a44' as ImageUploaderImageId,
                    previewUrl: '//128x128',
                    largeUrl: '//orig',
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f41077',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
                {
                    entityId: 'houseServicesBillPhotos' as ImageUploaderEntityIdentity,
                    imageId: 'b1db5a45' as ImageUploaderImageId,
                    previewUrl: '//128x128',
                    largeUrl: '//orig',
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f41078',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
                {
                    entityId: 'houseServicesBillPhotos' as ImageUploaderEntityIdentity,
                    imageId: 'b1db5a47' as ImageUploaderImageId,
                    previewUrl: '//128x128',
                    largeUrl: '//orig',
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f41079',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
            ],
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...store,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...store,
    config: {
        isMobile: 'yes',
    },
};
