jest.mock('auto-core/lib/core/isMobileApp', () => jest.fn());
const isMobileApp = require('auto-core/lib/core/isMobileApp');

const prepareState = require('./state');
const _ = require('lodash');
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;

let updatedMockOffer;

beforeEach(() => {
    updatedMockOffer = _.cloneDeep(mockOffer);

    updatedMockOffer.state.image_urls.push(...updatedMockOffer.state.image_urls.slice(0, 5), ...updatedMockOffer.state.image_urls.slice(0, 5));
});

describe('State', () => {
    it('Should return 11 images in listing', () => {
        const res = prepareState(updatedMockOffer, { type: 'listing' });
        expect(res.image_urls).toHaveLength(11);
        expect(res.images_count).toEqual(15);
    });

    it('Should return 5 images in mobile listing', () => {
        isMobileApp.mockImplementationOnce(() => true);

        const res = prepareState(updatedMockOffer, { type: 'listing' });
        expect(res.image_urls).toHaveLength(5);
        expect(res.images_count).toEqual(15);
    });

    it('Should return 7 images', () => {
        const res = prepareState(updatedMockOffer);
        expect(res.image_urls).toHaveLength(15);
        expect(res.images_count).toEqual(15);
    });

    it('должен выбрать только нужные размеры картинок', () => {
        const res = prepareState(updatedMockOffer, { type: 'listing' });
        expect(Object.keys(res.image_urls[0].sizes)).toEqual([ 'small', '320x240', '456x342', '1200x900', '1200x900n' ]);
    });

    it('должен выбрать is_hd, если он есть', () => {
        updatedMockOffer.state.image_urls[0].is_hd = true;
        const res = prepareState(updatedMockOffer, { type: 'listing' });
        expect(res.image_urls[0].is_hd).toEqual(true);
    });

    it('не должен выбрать is_hd для неактивного оффера, даже если он есть', () => {
        updatedMockOffer.status = 'INACTIVE';
        updatedMockOffer.state.image_urls[0].is_hd = true;
        const res = prepareState(updatedMockOffer, { type: 'listing' });
        expect(res.image_urls[0].is_hd).toEqual(undefined);
    });
});
