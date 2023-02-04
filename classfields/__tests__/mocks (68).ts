import { CommunicationChannels, IOfferCardMobile } from 'realty-core/types/offerCard';

export const onPhoneClick = () => {
    /**/
};

export const item = {
    offerId: 'mockId',
    uid: 'itemUid',
    author: {
        encryptedPhones: ['KzcF5OHTkJyMLTYN2MPjERy'],
        allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
    },
} as IOfferCardMobile;

export const baseProps = {
    onPhoneClick,
    item,
    myUid: 'myUid',
    onOpenOfferChat: () => null,
    onNoteAdd: () => null,
};
