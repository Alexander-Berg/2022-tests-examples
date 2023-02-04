import noop from 'lodash/noop';

import { IOfferCardMobile } from 'realty-core/types/offerCard';

interface IGate {
    create(): Promise<unknown>;
}

export const GateSuccess: IGate = {
    create() {
        return Promise.resolve();
    },
};

export const GatePending: IGate = {
    create() {
        return new Promise(noop);
    },
};

const userNote = 'Хата огонь! Надо брать';

export const baseOffer = {} as IOfferCardMobile;

export const offer = {
    ...baseOffer,
    userNote,
} as IOfferCardMobile;

export const offerWithOverflowingNote = {
    ...baseOffer,
    userNote: `${userNote} Когда накоплю денег`,
} as IOfferCardMobile;

export const baseProps = {
    onHide: noop,
    onNoteClick: noop,
    item: offer,
    addToFavorites: noop,
    modalVisible: false,
    eventPlace: 'blank',
};
