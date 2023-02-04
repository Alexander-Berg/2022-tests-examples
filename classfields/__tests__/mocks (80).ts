import { IRefinement } from '..';

export const spbRgid = 741965;
export const mskRgid = 741964;
export const highwayRefinement: IRefinement = {
    distanceKm: 2,
    name: 'Тестовое шоссе',
    type: 'highway',
};

export const railwayRefinement: IRefinement = {
    distanceKm: 2,
    name: 'Тестовая жд станция',
    type: 'railway',
};

export const highwayRefinementWithoutName: IRefinement = {
    distanceKm: 2,
    type: 'highway',
};
