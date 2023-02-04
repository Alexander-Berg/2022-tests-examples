import React from 'react';

import { newbuildingOffer } from '../../__tests__/stubs/offer';
import { alfaBankMortgageInitial } from '../../__tests__/stubs/alfabank';

export { newbuildingOffer as offer };

export const disabledOffer = {
    ...newbuildingOffer,
    active: false,
};

export const baseState = {
    user: {
        favoritesMap: {},
    },
    offerPhones: ['+79998887766'],
    alfaBankMortgage: alfaBankMortgageInitial,
    offerCard: {},
};

export const actionsRef = React.createRef<HTMLDivElement>();

export const onActionsClick = () => null;
