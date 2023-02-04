import { ChatPresetsCategory } from 'realty-core/types/chat';
import { CommunicationChannels, IOfferCard } from 'realty-core/types/offerCard';

import { newbuildingOffer, rentOffer } from '../../__tests__/stubs/offer';

const rentOfferWithChat = {
    ...rentOffer,
    author: {
        ...rentOffer.author,
        allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
    },
};

export const disabledOffer: IOfferCard = {
    ...newbuildingOffer,
    active: false,
};

export { newbuildingOffer, rentOfferWithChat as rentOffer };

export const stateWithCouplePresets = {
    page: {
        name: 'offer',
    },
    chatPresets: {
        presets: [
            { id: '1', text: 'Первый пресет' },
            { id: '2', text: 'Второй пресет новостройки' },
        ],
        category: ChatPresetsCategory.NEWBUILDING,
    },
};

export const stateWithManyPresets = {
    page: {
        name: 'offer',
    },
    chatPresets: {
        presets: [
            { id: '1', text: 'Первый пресет' },
            { id: '2', text: 'Второй пресет аренды' },
            { id: '3', text: 'Третий пресет аренды' },
            { id: '4', text: 'Четвёртый пресет аренды' },
        ],
        category: ChatPresetsCategory.RENT,
    },
};
