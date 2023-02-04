import { AnyObject } from 'realty-core/types/utils';

const getDefaultReview = (props: AnyObject) => ({
    ...props,
    type: 'YANDEX',
    text: 'Отличный Жк буду жить в нем вечно',
    rating: 4,
    isAnonymous: false,
    updatedTime: '2020-02-14T11:36:18.592Z',
    author: {
        name: 'Vasily P.',
        avatarUrl: '',
        profileUrl: 'https://reviews.yandex.ru/user/mqhj9n71p0ngnh8dvqcke58ytm',
    },
    moderation: {
        status: 'success',
    },
    orgId: '125771270495',
});

export const initialState = {
    newbuildingCardPage: {
        card: {
            fullName: 'ЖК Тестовый',
            locativeFullName: 'в ЖК Тестовом',
            location: {},
            permalink: '123456789012',
        },
    },
    similar: {},
    user: {
        isAuth: true,
    },
    reviews: {
        entries: [
            getDefaultReview({ id: 1 }),
            getDefaultReview({ id: 2 }),
            getDefaultReview({ id: 3 }),
            getDefaultReview({ id: 4 }),
            getDefaultReview({ id: 5 }),
            getDefaultReview({ id: 6 }),
            getDefaultReview({ id: 7 }),
            getDefaultReview({ id: 8 }),
        ],
        rating: {
            value: 4.2,
            count: 12,
        },
        pager: {
            rating: {
                Value: 4.2,
                Count: 12,
            },
            page: 1,
            totalPages: 3,
            count: 20,
        },
    },
    breadcrumbs: [
        {
            title: 'Я.Недвижимость',
            link: '/',
        },
        {
            title: 'Санкт-Петербург и ЛО',
            link: '/sankt-peterburg_i_leningradskaya_oblast/',
        },
        {
            title: 'Купить квартиру в новостройке',
            link: '/sankt-peterburg_i_leningradskaya_oblast/kupit/novostrojka//kvartiry/',
        },
        {
            title: 'ЖК Ultra City',
            link: '/sankt-peterburg/kupit/novostrojka/ultra-city-57790/',
        },
        {
            title: 'Отзывы — ЖК Ultra City',
            link: '',
        },
    ],
};
