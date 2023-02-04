import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { HouseServicesSettingsStatus } from 'app/libs/house-services/settings-form';

import { IUniversalStore } from 'view/modules/types';
import { HouseServiceMeterTariff, HouseServiceMeterType, HouseServiceId } from 'types/houseService';

export const houseServices = [
    {
        houseServiceId: '1' as HouseServiceId,
        meter: {
            type: HouseServiceMeterType.GAS,
            number: '584215',
            installedPlace: 'На кухне',
            deliverFromDay: 20,
            deliverToDay: 25,
            tariff: HouseServiceMeterTariff.SINGLE,
        },
    },
    {
        houseServiceId: '2' as HouseServiceId,
        meter: {
            type: HouseServiceMeterType.WATER_COLD,
            number: '5458456',
            installedPlace: 'В ванной',
            deliverFromDay: 15,
            deliverToDay: 18,
            tariff: HouseServiceMeterTariff.SINGLE,
        },
    },
    {
        houseServiceId: '3' as HouseServiceId,
        meter: {
            type: HouseServiceMeterType.WATER_HOT,
            number: '9932485465',
            installedPlace: 'В ванной',
            deliverFromDay: 15,
            deliverToDay: 18,
            tariff: HouseServiceMeterTariff.SINGLE,
        },
    },
    {
        houseServiceId: '4' as HouseServiceId,
        meter: {
            type: HouseServiceMeterType.POWER,
            number: '821654',
            installedPlace: 'В коридоре',
            deliverFromDay: 20,
            deliverToDay: 25,
            tariff: HouseServiceMeterTariff.DOUBLE,
        },
    },
];

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
        },
    },
    houseServicesSettings: {
        settings: {
            settingsStatus: HouseServicesSettingsStatus.NEW,
        },
        houseServices,
        updateSettingsStatus: RequestStatus.LOADED,
    },
};

export const storeWithoutHouseServices: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
        },
    },
    houseServicesSettings: {
        settings: {
            settingsStatus: HouseServicesSettingsStatus.NEW,
        },
        houseServices: [],
        updateSettingsStatus: RequestStatus.LOADED,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...store,
    spa: {
        status: RequestStatus.PENDING,
    },
};
