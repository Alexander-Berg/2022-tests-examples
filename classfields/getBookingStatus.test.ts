import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import STATUSES from 'auto-core/react/dataDomain/booking/dicts/booking_interface_statuses';

import getBookingStatus from './getBookingStatus';

let offer: TOfferMock;

beforeEach(() => {
    offer = cloneOfferWithHelpers(offerMock);
});

it('должен вернуть статус NOT_ALLOWED', () => {
    const mock = offer.withBookingStatus(STATUSES.NOT_ALLOWED).withIsOwner(false).value();
    expect(getBookingStatus(mock)).toBe(STATUSES.NOT_ALLOWED);
});

it('должен вернуть статус ALLOWED', () => {
    const mock = offer.withBookingStatus(STATUSES.ALLOWED).withIsOwner(false).value();
    expect(getBookingStatus(mock)).toBe(STATUSES.ALLOWED);
});

it('должен вернуть статус BOOKED', () => {
    const mock = offer.withBookingStatus(STATUSES.BOOKED).withIsOwner(false).value();
    expect(getBookingStatus(mock)).toBe(STATUSES.BOOKED);
});

it('должен вернуть статус BOOKED, даже если убрали возможность бронирования', () => {
    const mock = offer.withBookingStatus(STATUSES.BOOKED).withIsOwner(false).value();
    mock.additional_info!.booking = {
        ...(mock.additional_info?.booking || {}),
        allowed: false,
    };

    expect(getBookingStatus(mock)).toBe(STATUSES.BOOKED);
});

it('должен вернуть статус BOOKED_BY_YOU', () => {
    const mock = offer.withBookingStatus(STATUSES.BOOKED_BY_YOU).withIsOwner(false).value();
    expect(getBookingStatus(mock)).toBe(STATUSES.BOOKED_BY_YOU);
});

it('должен вернуть статус BOOKED_BY_YOU_LAST_DAY', () => {
    const mock = offer.withBookingStatus(STATUSES.BOOKED_BY_YOU_LAST_DAY).withIsOwner(false).value();
    expect(getBookingStatus(mock)).toBe(STATUSES.BOOKED_BY_YOU_LAST_DAY);
});

it('должен вернуть статус YOUR_CAR_BOOKED', () => {
    const mock = offer.withBookingStatus(STATUSES.YOUR_CAR_BOOKED).withIsOwner(true).value();
    expect(getBookingStatus(mock)).toBe(STATUSES.YOUR_CAR_BOOKED);
});
