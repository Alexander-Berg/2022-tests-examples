import { newbuildingOffer } from '../../__tests__/stubs/offer';

export const getOffer = ({ isEditable = false, userNote = '', deletedByUser = false } = {}) => ({
    ...newbuildingOffer,
    offerId: '1',
    isEditable,
    userNote,
    deletedByUser,
});

export const getInitialState = ({ favoritesMap = {} } = {}) => ({
    user: {
        favoritesMap,
    },
    page: {
        params: {
            id: '1',
        },
    },
});

export const getProps = ({
    isEditable = false,
    userNote = '',
    deletedByUser = false,
    isOwner = false,
    comparison = [] as string[],
} = {}) => ({
    comparison,
    isOwner,
    offer: getOffer({ isEditable, userNote, deletedByUser }),
    linkDesktop: () => '',
});

export const context = {
    addMailEvent: () => null,
};
