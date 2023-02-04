import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardBus from 'auto-core/react/dataDomain/card/mocks/card.bus.mock';
import cardCars from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import cardCarsElectro from 'auto-core/react/dataDomain/card/mocks/card.cars.electro.mock';
import cardCrane from 'auto-core/react/dataDomain/card/mocks/card.crane.mock';
import cardLcv from 'auto-core/react/dataDomain/card/mocks/card.lcv.mock';
import cardMotorcycle from 'auto-core/react/dataDomain/card/mocks/card.motorcycle.mock';
import cardTrailer from 'auto-core/react/dataDomain/card/mocks/card.trailer.mock';
import cardTruck from 'auto-core/react/dataDomain/card/mocks/card.truck.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import techSummaryDesktop from './desktop';

it('должен вернуть колонки для BUS', () => {
    const offer = cloneOfferWithHelpers(cardBus as unknown as Offer);

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для CARS/USED', () => {
    const offer = cloneOfferWithHelpers(cardCars as unknown as Offer)
        .withSection('used');

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для CARS/NEW', () => {
    const offer = cloneOfferWithHelpers(cardCars as unknown as Offer)
        .withComplectation({ name: 'Название' })
        .withSection('new');

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для CARS/USED электрокар', () => {
    const offer = cloneOfferWithHelpers(cardCarsElectro as unknown as Offer)
        .withSection('used');

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для CARS/USED электрокар без запаса хода', () => {
    const offer = cloneOfferWithHelpers(cardCarsElectro as unknown as Offer)
        .withSection('used').value();

    offer.vehicle_info.tech_param!.electric_range = 0;

    expect(techSummaryDesktop(offer)).toMatchSnapshot();
});

it('должен вернуть колонки для CARS/NEW электрокар', () => {
    const offer = cloneOfferWithHelpers(cardCarsElectro as unknown as Offer)
        .withComplectation({ name: 'Название' })
        .withSection('new');

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для CARS/NEW электрокар без запаса хода', () => {
    const offer = cloneOfferWithHelpers(cardCarsElectro as unknown as Offer)
        .withComplectation({ name: 'Название' })
        .withSection('new')
        .value();

    offer.vehicle_info.tech_param!.electric_range = 0;

    expect(techSummaryDesktop(offer)).toMatchSnapshot();
});

it('должен вернуть колонки для CRANE', () => {
    const offer = cloneOfferWithHelpers(cardCrane as unknown as Offer);

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для LCV', () => {
    const offer = cloneOfferWithHelpers(cardLcv as unknown as Offer);

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для MOTORCYCLE', () => {
    const offer = cloneOfferWithHelpers(cardMotorcycle as unknown as Offer);

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для TRAILER', () => {
    const offer = cloneOfferWithHelpers(cardTrailer as unknown as Offer);

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});

it('должен вернуть колонки для TRUCK', () => {
    const offer = cloneOfferWithHelpers(cardTruck as unknown as Offer);

    expect(techSummaryDesktop(offer.value())).toMatchSnapshot();
});
