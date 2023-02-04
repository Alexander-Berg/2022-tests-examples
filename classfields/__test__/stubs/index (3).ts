import { DeepPartial } from 'utility-types';

import { IUniversalStore } from 'view/modules/types';

export const storeWithEmptyReviews: DeepPartial<IUniversalStore> = {
    reviews: {
        entries: [],
    },
};

export const baseStore: DeepPartial<IUniversalStore> = {
    reviews: {
        entries: [
            {
                type: 'YANDEX',
                id: 'comment_1',
                text: 'Сдал квартиру за три дня без какого либо геморроя!!',
                rating: 5,
                isAnonymous: false,
                updatedTime: '2021-06-22T14:18:46.773Z',
                orgId: '',
                author: {
                    name: 'Вася Пупкин',
                    avatarUrl: '',
                    profileUrl: 'https://reviews.yandex.ru/user/eq6dgk44qab37pgupm0z6u610m',
                },
                comment: {
                    id: '',
                    text: 'Ольга, спасибо за приятный отклик! Вдохновляете нас на дальнейшие свершения 😉',
                    updatedTime: '2021-06-07T08:50:49.121Z',
                },
            },
        ],
        rating: {
            value: 4.4,
            count: 1,
        },
    },
};

export const manyReviewStore: DeepPartial<IUniversalStore> = {
    reviews: {
        entries: [
            {
                type: 'YANDEX',
                id: 'comment_1',
                text: 'Сдал квартиру за три дня без какого либо геморроя!!',
                rating: 5,
                isAnonymous: false,
                updatedTime: '2021-06-22T14:18:46.773Z',
                orgId: '',
                author: {
                    name: 'Имя Фамилия',
                    avatarUrl: '',
                    profileUrl: 'https://reviews.yandex.ru/user/eq6dgk44qab37pgupm0z6u610m',
                },
                comment: {
                    id: '',
                    text: 'Ольга, спасибо за приятный отклик! Вдохновляете нас на дальнейшие свершения 😉',
                    updatedTime: '2021-06-07T08:50:49.121Z',
                },
            },
            {
                type: 'YANDEX',
                id: 'comment_2',
                text:
                    'Ну очень выгодное предложение! Во-первых доверие к объектам представленным Яндекс аренда.' +
                    'Во-вторых никаких 50% риэлтору И как вишенка на торте' +
                    ' бесплатный клининг при заселении!',
                rating: 5,
                isAnonymous: false,
                updatedTime: '2021-06-22T14:18:46.773Z',
                orgId: '',
                author: {
                    name: 'Имя Фамилия',
                    avatarUrl: '',
                    profileUrl: 'https://reviews.yandex.ru/user/eq6dgk44qab37pgupm0z6u610m',
                },
                comment: {
                    id: '',
                    text: 'Спасибо, что оценили! Нам очень приятно 😊',
                    updatedTime: '2021-06-07T08:50:49.121Z',
                },
            },
            {
                type: 'YANDEX',
                id: 'comment_2',
                text: 'Все , замечательно , я очень довольна!',
                rating: 5,
                isAnonymous: false,
                updatedTime: '2021-06-22T14:18:46.773Z',
                orgId: '',
                author: {
                    name: 'Имя Фамилия',
                    avatarUrl: '',
                    profileUrl: 'https://reviews.yandex.ru/user/eq6dgk44qab37pgupm0z6u610m',
                },
                comment: {
                    id: '',
                    text: 'дорово! Мы рады, что вам понравилось 😊',
                    updatedTime: '2021-06-07T08:50:49.121Z',
                },
            },
        ],
        rating: {
            value: 4.4,
            count: 3,
        },
    },
};

export const largeTextStore: DeepPartial<IUniversalStore> = {
    reviews: {
        entries: [
            {
                type: 'YANDEX',
                id: 'comment_1',
                text:
                    'очень круто, профессионально, впечатлен, вначале отпугивали отзывы на яндекс недвижимости' +
                    ' но я так понял это от конкурентов. все оперативно, помогли отфоткать квартиру, также сделали' +
                    ' панорамный онлайн просмотр. жильцов нашли быстро, очень удобно что помогают во всех сложностях,' +
                    ' то есть между мной и жильцом есть консъерж. также удобно для арендаторов тк не нужно платить' +
                    ' второй месяц. а риски как я понял покрываются страховкой, но надеюсь до них не дойдет😁😁 ' +
                    'вообщем удачи, судя по тому как работает сервис яндекс хочет захватить рынок аренды,' +
                    ' как уже сделал это с такси и доставкой) уровень',
                rating: 5,
                isAnonymous: false,
                updatedTime: '2021-06-22T14:18:46.773Z',
                orgId: '',
                author: {
                    name: 'Вася Пупкин',
                    avatarUrl: '',
                    profileUrl: 'https://reviews.yandex.ru/user/eq6dgk44qab37pgupm0z6u610m',
                },
                comment: {
                    id: '',
                    text:
                        'Спасибо за такой подробный отзыв! Учтём все ваши пожелания и замечания, в частности,' +
                        'уже к концу месяца надеемся сделать так, чтобы дата подписания договора и дата платежа ' +
                        'могли быть разными 😊 Как только новая функция появится, сразу же с вами свяжемся. ' +
                        'Извините за некоторые шероховатости, сейчас мы активно набираем высоту ✈️ Обещаем,' +
                        'что дальше сотрудничать с нами будет комфортнее.',
                    updatedTime: '2021-06-07T08:50:49.121Z',
                },
            },
        ],
    },
};

export const withRating = (rating: number): DeepPartial<IUniversalStore> => {
    return {
        reviews: {
            entries: [
                {
                    type: 'YANDEX',
                    id: 'comment_1',
                    text: 'Сдал квартиру за три дня без какого либо геморроя!!',
                    rating: rating,
                    isAnonymous: false,
                    updatedTime: '2021-06-22T14:18:46.773Z',
                    orgId: '',
                    author: {
                        name: 'Вася Пупкин',
                        avatarUrl: '',
                        profileUrl: 'https://reviews.yandex.ru/user/eq6dgk44qab37pgupm0z6u610m',
                    },
                    comment: {
                        id: '',
                        text: 'Ольга, спасибо за приятный отклик! Вдохновляете нас на дальнейшие свершения 😉',
                        updatedTime: '2021-06-07T08:50:49.121Z',
                    },
                },
            ],
            rating: {
                value: 4.4,
                count: 1,
            },
        },
    };
};
