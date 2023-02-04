import gateApi from 'auto-core/react/lib/gateApi';

import { C2BCreateApplicationSource } from '../types';

import getCurrentDateFormatted from './getCurrentDateFormatted';
import createC2BApplication from './createC2BApplication';

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const currentDate = getCurrentDateFormatted();

describe('Создание заявки Выкупа', () => {
    it('Создает заявку по id оффера, если есть id оффера, id черновика и ВИН', async() => {
        createC2BApplication({
            offerId: '1-1',
            draftId: '1-1',
            mileage: 10000,
            phoneNumber: '89652223344',
            pricePrediction: {
                from: 100000,
                to: 20000000,
            },
            vinOrLicense: 'WAUZZZ8V9KA111514',
            source: C2BCreateApplicationSource.Landing,
        });

        expect(gateApi.getResource).toHaveBeenNthCalledWith(1, 'createC2bApplicationByOfferId', {
            category: 'cars',
            date: currentDate,
            place: {
                address: 'Россия, Москва',
                comment: 'С лендинга',
                lat: 55.755705,
                lon: 37.617879,
            },
            time: '',
            phoneNumber: '89652223344',
            offerId: '1-1',
        });
    });

    it('Создает заявку по id черновика, если нет id оффера', async() => {
        createC2BApplication({
            draftId: '1-1',
            mileage: 10000,
            phoneNumber: '89652223344',
            pricePrediction: {
                from: 100000,
                to: 20000000,
            },
            vinOrLicense: 'WAUZZZ8V9KA111514',
            source: C2BCreateApplicationSource.OfferForm,
        });

        expect(gateApi.getResource).toHaveBeenNthCalledWith(1, 'createC2bApplicationAuto', {
            category: 'cars',
            date: currentDate,
            place: {
                address: 'Россия, Москва',
                comment: 'С формы размещения',
                lat: 55.755705,
                lon: 37.617879,
            },
            time: '',
            phoneNumber: '89652223344',
            draftId: '1-1',
        });
    });

    it('Создает заявку по вину или госномеру, если нет id оффера и черновика', async() => {
        createC2BApplication({
            mileage: 10000,
            phoneNumber: '89652223344',
            pricePrediction: {
                from: 100000,
                to: 20000000,
            },
            vinOrLicense: 'WAUZZZ8V9KA111514',
            source: C2BCreateApplicationSource.Landing,
        });

        expect(gateApi.getResource).toHaveBeenNthCalledWith(1, 'createC2bApplicationByVinOrLicense', {
            vin_or_license_plate: 'WAUZZZ8V9KA111514',
            mileage: 10000,
            phone: '89652223344',
            price_prediction: {
                from: 100000,
                to: 20000000,
            },
        });
    });
});
