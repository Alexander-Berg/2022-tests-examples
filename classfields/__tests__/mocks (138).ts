import { newbuildingOffer } from '../../__tests__/stubs/offer';

export const offer = newbuildingOffer;
export const offerWithShortDescription = {
    ...newbuildingOffer,
    description: 'Без переуступки',
};
