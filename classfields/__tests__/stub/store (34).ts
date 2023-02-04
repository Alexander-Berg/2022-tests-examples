import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    reviews: {
        myReview: {
            text:
                // eslint-disable-next-line max-len
                'Очень круто, профессионально, впечатлен, вначале отпугивали отзывы на яндекс недвижимости но я так понял это от конкурентов.',
            comment: {
                text: 'Спасибо за отзыв!) Рады с вами сотрудничать)))',
            },
            rating: 5,
        },
    },
};

export const badRatingStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    reviews: {
        myReview: {
            text:
                // eslint-disable-next-line max-len
                'Очень круто, профессионально, впечатлен, вначале отпугивали отзывы на яндекс недвижимости но я так понял это от конкурентов.',
            comment: {
                text: 'Спасибо за отзыв!) Рады с вами сотрудничать)))',
            },
            rating: 1,
        },
    },
};

export const withoutCommentStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    reviews: {
        myReview: {
            text:
                // eslint-disable-next-line max-len
                'Очень круто, профессионально, впечатлен, вначале отпугивали отзывы на яндекс недвижимости но я так понял это от конкурентов.',
            rating: 5,
        },
    },
};

export const longCommentStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    reviews: {
        myReview: {
            text:
                // eslint-disable-next-line max-len
                'Очень круто, профессионально, впечатлен, вначале отпугивали отзывы на яндекс недвижимости но я так понял это от конкурентов.',
            comment: {
                text:
                    // eslint-disable-next-line max-len
                    'К сожалению, не видим ваш номер, чтобы связаться. Напишите нам, пожалуйста, на почту help@arenda.yandex.ru, с радостью вас выслушаем',
            },
            rating: 5,
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
};
