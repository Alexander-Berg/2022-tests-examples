import getMaskPhone from 'realty-core/view/react/libs/get-phone-mask';
import { encrypt } from 'realty-core/app/lib/crypto_phone';

const phone = '+79876543210';

const imageTab = {
    id: 1,
    type: 'IMAGE_BANNER',
    webSiteUrl: '#',
    tabPhoto: {
        desktopPhoto: {
            optimize: 'http://site.com/image.png'
        },
        mobilePhoto: {
            optimize: 'http://site.com/image.png'
        }
    }
};

const phoneTab = {
    id: 2,
    type: 'CALL',
    callTradeInTitle: 'Позвоните',
    callTradeInDescription: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula dolor.',
    phone: {
        phoneWithMask: getMaskPhone(phone, '××\u00a0××'),
        phoneHash: encrypt(phone)
    }
};

const buttonTab = {
    id: 3,
    type: 'TRADE_IN',
    callTradeInTitle: 'Получите скидку',
    callTradeInDescription: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula dolor.',
    buttonText: 'Получить',
    webSiteUrl: '#'
};

export const developerWithImageTab = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    customTabs: [ imageTab, phoneTab ]
};

export const developerWithPhoneTab = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    customTabs: [ phoneTab, buttonTab ]
};

export const developerWithButtonTab = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    customTabs: [ buttonTab, imageTab ]
};
