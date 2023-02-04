import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import getMaskPhone from 'realty-core/view/react/libs/get-phone-mask';
import { encrypt } from 'realty-core/app/lib/crypto_phone';

const phone = '+79876543210';

const image = generateImageUrl({
    width: 200,
    height: 100
});

const workTime = {
    datePatterns: [
        {
            from: 1,
            to: 4,
            timeIntervals: [ { from: '09:00', to: '21:00' } ]
        },
        {
            from: 6,
            to: 7,
            timeIntervals: [ { from: '10:00', to: '20:00' } ]
        }
    ],
    timeZone: '+03:00'
};

export const officeDataWithoutContacts = {
    id: '1',
    workTime,
    name: 'Офис продаж',
    parking: true,
    logotype: {
        appMiddle: image
    }
};

export const officeDataWithoutLogo = {
    id: '1',
    workTime,
    userAddress: 'Москва, Мосфильмовская улица, 70',
    encryptedPhones: [ {
        phoneWithMask: getMaskPhone(phone, '××\u00a0××'),
        phoneHash: encrypt(phone)
    } ]
};

export const officeDataWithoutWorkTime = {
    id: '1',
    name: 'Офис продаж',
    parking: true,
    userAddress: 'Москва, Мосфильмовская улица, 70',
    logotype: {
        appMiddle: image
    },
    encryptedPhones: [ {
        phoneWithMask: getMaskPhone(phone, '××\u00a0××'),
        phoneHash: encrypt(phone)
    } ]
};
