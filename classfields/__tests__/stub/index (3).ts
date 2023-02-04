import {
    HouseServiceMeterType,
    HouseServiceId,
    HouseServiceType,
    HouseServiceMeterTariff,
    IHouseServiceMeter,
} from 'types/houseService';

export const getCounter = (type: HouseServiceMeterType): IHouseServiceMeter => {
    return {
        houseServiceId: 'counter123' as HouseServiceId,
        createTime: '2022-07-05T11:14:17.603Z',
        updateTime: '2022-07-06T11:10:23.603Z',
        [HouseServiceType.METER]: {
            type: type,
            number: '10456',
            installedPlace: 'Кухня',
            deliverFromDay: 4,
            deliverToDay: 10,
            tariff: HouseServiceMeterTariff.SINGLE,
            initialMeterReadings: [],
        },
    };
};
