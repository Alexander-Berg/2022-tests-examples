import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import { ImageUploaderEntityIds } from 'app/libs/house-services';

import { IUniversalStore } from 'view/modules/types';
import { ImageUploaderEntityId, ImageUploaderImageId } from 'types/imageUploader';
import {
    HouseServicesPeriodId,
    HouseServicesMeterReadingsStatus,
    HouseServicesMeterReadingsId,
    HouseServicesPeriodBillStatus,
    HouseServicesPeriodPaymentConfirmationStatus,
    HouseServicesAggregatedMeterReadingsStatus,
    HouseServicesPeriodReceiptStatus,
    HouseServicesPeriodType,
} from 'types/houseServices';
import { HouseServiceMeterTariff, HouseServiceMeterType } from 'types/houseService';
import { IHouseServicesPeriodStore } from 'view/modules/houseServicesPeriod/reducers';
import { DefaultNamespaceAliases, ImageNamespaces } from 'types/image';

const imageUrl = generateImageUrl({ width: 300, height: 300 });

const houseServicesPeriod: IHouseServicesPeriodStore = {
    periodId: '76576576' as HouseServicesPeriodId,
    period: '2021-09',
    periodType: HouseServicesPeriodType.REGULAR,
    meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
    billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
    receiptStatus: HouseServicesPeriodReceiptStatus.NOT_SENT,
    confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
    meterReadings: [
        {
            meterReadingsId: '562623435' as HouseServicesMeterReadingsId,
            status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
            meter: {
                type: HouseServiceMeterType.POWER,
                number: '64352345',
                installedPlace: 'В коридоре',
                deliverFromDay: 20,
                deliverToDay: 25,
                tariff: HouseServiceMeterTariff.DOUBLE,
                initialMeterReadings: [],
            },
            meterReadings: [
                {
                    meterValue: 100,
                    meterPhoto: {
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        imageUrls: [
                            {
                                alias: 'orig' as DefaultNamespaceAliases,
                                url: imageUrl,
                            },
                        ],
                    },
                },
                {
                    meterValue: 200,
                    meterPhoto: {
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        imageUrls: [
                            {
                                alias: 'orig' as DefaultNamespaceAliases,
                                url: imageUrl,
                            },
                        ],
                    },
                },
            ],
        },
        {
            meterReadingsId: '867373365' as HouseServicesMeterReadingsId,
            status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
            meter: {
                type: HouseServiceMeterType.HEATING,
                number: '4362345',
                installedPlace: 'На кухне',
                deliverFromDay: 20,
                deliverToDay: 25,
                tariff: HouseServiceMeterTariff.TRIPLE,
                initialMeterReadings: [],
            },
            meterReadings: [],
        },
    ],
};

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
            periodId: '76576576',
            meterReadingsId: '562623435',
        },
    },
    houseServicesPeriod,
    houseServicesMeterReadings: {
        meterReadingsId: '562623435' as HouseServicesMeterReadingsId,
        status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
        meter: {
            type: HouseServiceMeterType.POWER,
            number: '64352345',
            installedPlace: 'В коридоре',
            deliverFromDay: 20,
            deliverToDay: 25,
            tariff: HouseServiceMeterTariff.DOUBLE,
            initialMeterReadings: [],
        },
        meterReadings: [
            {
                meterValue: 100,
                meterPhoto: {
                    namespace: 'arenda' as ImageNamespaces.ARENDA,
                    groupId: 1396625,
                    name: '9f4107752ebb0b58713ca84dacde044f',
                    imageUrls: [
                        {
                            alias: 'orig' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                    ],
                },
            },
            {
                meterValue: 200,
                meterPhoto: {
                    namespace: 'arenda' as ImageNamespaces.ARENDA,
                    groupId: 1396625,
                    name: '9f4107752ebb0b58713ca84dacde044f',
                    imageUrls: [
                        {
                            alias: 'orig' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                    ],
                },
            },
        ],
    },
    imageUploader: {
        [ImageUploaderEntityIds.T1 as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: ImageUploaderEntityIds.T1 as ImageUploaderEntityId,
                    imageId: 'b1db5a44-028b-49e6-a327-3dbc7920c073' as ImageUploaderImageId,
                    previewUrl: imageUrl,
                    largeUrl: imageUrl,
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
            ],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        [ImageUploaderEntityIds.T2 as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: ImageUploaderEntityIds.T2 as ImageUploaderEntityId,
                    imageId: 'b1db5a44-028b-49e6-a327-3dbc7920c073' as ImageUploaderImageId,
                    previewUrl: imageUrl,
                    largeUrl: imageUrl,
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
            ],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        [ImageUploaderEntityIds.T3 as ImageUploaderEntityId]: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};
