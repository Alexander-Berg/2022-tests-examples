import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { OfferPhotoError } from 'realty-core/view/react/modules/offer-photo-upload/redux/reducer';

const imageUrl = generateImageUrl({ width: 600, height: 200 });

const photos = [
    {
        id: '1',
        url: imageUrl,
        isLoading: false,
    },
    {
        id: '2',
        url: imageUrl,
        isLoading: true,
    },
    {
        id: '3',
        base64:
            // eslint-disable-next-line max-len
            'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQIAdgB2AAD/4QBiRXhpZgAATU0AKgAAAAgABQESAAMAAAABAAEAAAEaAAUAAAABAAAASgEbAAUAAAABAAAAUgEoAAMAAAABAAMAAAITAAMAAAABAAEAAAAAAAAAAAB2AAAAAQAAAHYAAAAB/9sAQwADAgICAgIDAgICAwMDAwQGBAQEBAQIBgYFBgkICgoJCAkJCgwPDAoLDgsJCQ0RDQ4PEBAREAoMEhMSEBMPEBAQ/8AACwgAMgAyAQERAP/EABUAAQEAAAAAAAAAAAAAAAAAAAAJ/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAAPwCVQAAAAAAAAAAAAAAAD//Z',
        isLoading: false,
    },
    {
        id: '4',
        error: OfferPhotoError.INVALID_MIME_TYPE,
        isLoading: false,
    },
    {
        id: '5',
        error: OfferPhotoError.MAX_SIZE_EXCEEDED,
        isLoading: false,
    },
    {
        id: '6',
        error: OfferPhotoError.UNKNOWN,
        isLoading: false,
    },
];

export const state = {
    offerPhotoUpload: {
        photos,
    },
};

export const noPhotosState = {
    offerPhotoUpload: {
        photos: [],
    },
};
