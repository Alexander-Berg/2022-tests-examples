import { Service } from '../../../types/common';
import { ITagAttributes } from '../../../types/tag';
import { IPostModelAttributes, PostStatus } from '../../../types/post';

export const TAG_DATA_1: ITagAttributes = {
    urlPart: 'vtorichnoe-zhilyo',
    service: Service.realty,
    title: 'Вторичное жилье',
    shortTitle: 'Втор. жилье',
    blocks: [],
    draftTitle: 'Вторичное жилье (черновик)',
    draftShortTitle: 'Втор. жилье (черновик)',
    draftBlocks: [{ type: 'text', text: 'Актуальные новости и статьи по теме: вторичное жилье' }],
    isArchived: false,
    isHot: false,
    isPartnership: false,
    partnershipImage: null,
    partnershipLink: null,
    partnershipName: 'Партнер 1',
    partnershipBadgeName: 'Бейдж партнера 1',
    metaTitle: 'Вторичное жилье - статьи',
    metaDescription: 'Все интересные статьи про вторичное жилье',
    mmm: 'TOYOTA:COROLLA:2020',
};

export const TAG_DATA_2: Partial<ITagAttributes> = {
    urlPart: 'specproekt',
    title: 'Спецпроект',
    shortTitle: 'Спецпроект',
    blocks: [],
    draftTitle: 'Спецпроект (черновик)',
    draftShortTitle: 'Спецпроект (черновик)',
    draftBlocks: [{ type: 'text', text: 'Актуальные новости и статьи по теме: спецпроект' }],
    isArchived: false,
    isHot: false,
    isPartnership: false,
    partnershipImage: null,
    partnershipLink: null,
    partnershipName: 'Партнер 2',
    partnershipBadgeName: 'Бейдж партнера 2',
    metaTitle: 'Спецпроект - статьи',
    metaDescription: 'Все интересные статьи по спецпроекту',
    mmm: 'MERCEDES:A-KLASSE:2019',
};

export const TAG_DATA_3: Partial<ITagAttributes> = {
    urlPart: 'kommercheskaya-nedvizhimost',
    title: 'Коммерческая недвижимость',
    shortTitle: 'Ком. недвижимость',
    blocks: [],
    draftTitle: 'Коммерческая недвижимость (черновик)',
    draftShortTitle: 'Ком. недвижимость (черновик)',
    draftBlocks: [{ type: 'text', text: 'Актуальные новости и статьи по теме: коммерческая недвижимость' }],
    isArchived: false,
    isHot: false,
    isPartnership: false,
    partnershipImage: null,
    partnershipLink: null,
    partnershipName: 'Партнер 3',
    partnershipBadgeName: 'Бейдж партнера 3',
    metaTitle: 'Коммерческая недвижимость - статьи',
    metaDescription: 'Все интересные статьи про коммерческую недвижимость',
    mmm: 'AUDI,A4,2018',
};

export const CREATE_TAG_DATA_1: Partial<ITagAttributes> = {
    urlPart: 'obzory',
    draftTitle: 'Обзоры',
    draftShortTitle: 'Обзоры',
    draftBlocks: [],
    isArchived: false,
    isHot: false,
    isPartnership: false,
    partnershipImage: {
        'group-id': 1399832,
        imagename: '32-citi2h.png_1618489642958',
        meta: {
            crc64: '71069454E19D5BBA',
            md5: '217b38c3c9d3796c0bf5bb145eb31ece',
            'modification-time': 1618489643,
            'orig-animated': false,
            'orig-format': 'PNG',
            'orig-orientation': '0',
            'orig-size': { x: 571, y: 64 },
            'orig-size-bytes': 5480,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
            processing: 'finished',
        },
        sizes: {
            '1200х1200': {
                height: 64,
                path: '/get-vertis-journal/1399832/32-citi2h.png_1618489642958/1200х1200',
                width: 571,
            },
            '320х320': {
                height: 36,
                path: '/get-vertis-journal/1399832/32-citi2h.png_1618489642958/320х320',
                width: 320,
            },
            '460х460': {
                height: 52,
                path: '/get-vertis-journal/1399832/32-citi2h.png_1618489642958/460х460',
                width: 460,
            },
            optimize: {
                height: 64,
                path: '/get-vertis-journal/1399832/32-citi2h.png_1618489642958/optimize',
                width: 571,
            },
            orig: { height: 64, path: '/get-vertis-journal/1399832/32-citi2h.png_1618489642958/orig', width: 571 },
        },
    },
    partnershipLink: 'https://pornhub.com/',
    partnershipName: 'Партнер',
    partnershipBadgeName: 'Бейдж партнера',
    metaTitle: 'Статьи с тегом обзоры',
    metaDescription: 'Статьи с тегом обзоры',
    mmm: 'VOLKSWAGEN,PASSAT,2021',
};

export const UPDATE_TAG_DATA_1: Partial<ITagAttributes> = {
    draftTitle: 'Спецпроект (черновик)',
    draftShortTitle: 'Спецпроект (черновик)',
    draftBlocks: [{ type: 'text', text: 'Актуальные новости и статьи по теме: спецпроект' }],
    isArchived: true,
    isHot: true,
    isPartnership: true,
    partnershipImage: {
        'group-id': 1398410,
        imagename: 'tinkoff.png_1618491876338',
        meta: {
            crc64: 'A02E02415EF37BE4',
            md5: 'ce92ece3cacdb7e2cd39b4db7d1c7312',
            'modification-time': 1618491876,
            'orig-animated': false,
            'orig-format': 'PNG',
            'orig-orientation': '0',
            'orig-size': { x: 474, y: 64 },
            'orig-size-bytes': 4970,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
            processing: 'finished',
        },
        sizes: {
            '1200х1200': {
                height: 64,
                path: '/get-vertis-journal/1398410/tinkoff.png_1618491876338/1200х1200',
                width: 474,
            },
            '320х320': {
                height: 43,
                path: '/get-vertis-journal/1398410/tinkoff.png_1618491876338/320х320',
                width: 320,
            },
            '460х460': {
                height: 62,
                path: '/get-vertis-journal/1398410/tinkoff.png_1618491876338/460х460',
                width: 460,
            },
            optimize: {
                height: 64,
                path: '/get-vertis-journal/1398410/tinkoff.png_1618491876338/optimize',
                width: 474,
            },
            orig: { height: 64, path: '/get-vertis-journal/1398410/tinkoff.png_1618491876338/orig', width: 474 },
        },
    },
    partnershipLink:
        'https://www.tinkoff.ru/loans/cash-loan/realty/?utm_source=yandexrealty0321_knz&utm_medium=ntv.fix&utm_campaign=loans.cashloan_realty.yandex',
    partnershipName: 'Хороший партнер',
    partnershipBadgeName: 'Бейдж хорошего партнера',
    metaTitle: 'Спецпроект - статьи',
    metaDescription: 'Все интересные статьи по спецпроекту',
    mmm: 'SKODA,RAPID,2005',
};

export const POST_DATA_1: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 211,
    urlPart: 'pereplanirovka-i-remont-v-novostroyke',
    service: Service.realty,
    title: 'Перепланировка и ремонт в новостройке',
    draftTitle: 'Перепланировка и ремонт в новостройке (draft)',
    titleRss: 'Перепланировка и ремонт в новостройке (rss)',
    draftTitleRss: 'Перепланировка и ремонт в новостройке (draft rss)',
    titleApp: 'Перепланировка и ремонт в новостройке (app)',
    draftTitleApp: 'Перепланировка и ремонт в новостройке (draft app)',
    lead: 'В ожидании своей квартиры вы наверняка не раз всё обдумали: какие стены снесёте, какие комнаты объедините, а какие — расширите',
    blocks: [
        {
            text: '<h2><strong>Когда приступать к&nbsp;ремонту</strong></h2>',
            type: 'text',
        },
    ],
    draftBlocks: [
        {
            text: '<h2><strong>Когда приступать к&nbsp;ремонту (draft)</strong></h2>',
            type: 'text',
        },
    ],
    mainImage: {
        meta: {
            md5: 'd44176585904b2cbee0d96cde82c8e41',
            crc64: '5475ED31C205030C',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 900900,
            'orig-orientation': '0',
            'modification-time': 1604661536,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Pereplanirovka_i_remont_v_novostroyke_1604661527613/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Pereplanirovka_i_remont_v_novostroyke_1604661527613',
    },
    author: 'user1',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'user2',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-06T11:18:57.000Z',
    createdAt: '2020-11-06T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_2: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 212,
    urlPart: 'malogabaritnye-kvartiry',
    service: Service.realty,
    title: 'Малогабаритные квартиры',
    draftTitle: 'Малогабаритные квартиры (draft)',
    titleRss: 'Малогабаритные квартиры (rss)',
    draftTitleRss: 'Малогабаритные квартиры (draft rss)',
    titleApp: 'Малогабаритные квартиры (app)',
    draftTitleApp: 'Малогабаритные квартиры (draft app)',
    lead: 'Малогабаритки были очень популярны в СССР, когда требовалось расселить максимум людей на минимуме площади.',
    blocks: [
        {
            text: '<p><span style="font-size:24px"><strong>Что такое малогабаритки?</strong></span></p>',
            type: 'text',
        },
    ],
    draftBlocks: [
        {
            text: '<p><span style="font-size:24px"><strong>Что такое малогабаритки? (draft)</strong></span></p>',
            type: 'text',
        },
    ],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'dcversus',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'dcversus',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_3: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 213,
    urlPart: 'url-part-213',
    service: Service.realty,
    title: 'Пост номер 3',
    draftTitle: 'Пост номер 3 (draft)',
    titleRss: 'Пост номер 3 (rss)',
    draftTitleRss: 'Пост номер 3 (draft rss)',
    titleApp: 'Пост номер 3 (app)',
    draftTitleApp: 'Пост номер 3 (draft app)',
    lead: 'Пост номер 3 Пост номер 3 Пост номер 3',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_4: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 214,
    urlPart: 'url-part-214',
    service: Service.realty,
    title: 'Пост номер 4',
    draftTitle: 'Пост номер 4 (draft)',
    titleRss: 'Пост номер 4 (rss)',
    draftTitleRss: 'Пост номер 4 (draft rss)',
    titleApp: 'Пост номер 4 (app)',
    draftTitleApp: 'Пост номер 4 (draft app)',
    lead: 'Пост номер 4 Пост номер 4 Пост номер 4',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_5: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 215,
    urlPart: 'url-part-215',
    service: Service.realty,
    title: 'Пост номер 5',
    draftTitle: 'Пост номер 5 (draft)',
    titleRss: 'Пост номер 5 (rss)',
    draftTitleRss: 'Пост номер 5 (draft rss)',
    titleApp: 'Пост номер 5 (app)',
    draftTitleApp: 'Пост номер 5 (draft app)',
    lead: 'Пост номер 5 Пост номер 5 Пост номер 5',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_6: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 216,
    urlPart: 'url-part-216',
    service: Service.realty,
    title: 'Пост номер 6',
    draftTitle: 'Пост номер 6 (draft)',
    titleRss: 'Пост номер 6 (rss)',
    draftTitleRss: 'Пост номер 6 (draft rss)',
    titleApp: 'Пост номер 6 (app)',
    draftTitleApp: 'Пост номер 6 (draft app)',
    lead: 'Пост номер 6 Пост номер 6 Пост номер 6',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_7: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 217,
    urlPart: 'url-part-217',
    service: Service.realty,
    title: 'Пост номер 7',
    draftTitle: 'Пост номер 7 (draft)',
    titleRss: 'Пост номер 7 (rss)',
    draftTitleRss: 'Пост номер 7 (draft rss)',
    titleApp: 'Пост номер 7 (app)',
    draftTitleApp: 'Пост номер 7 (draft app)',
    lead: 'Пост номер 7 Пост номер 7 Пост номер 7',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_8: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 218,
    urlPart: 'url-part-218',
    service: Service.realty,
    title: 'Пост номер 8',
    draftTitle: 'Пост номер 8 (draft)',
    titleRss: 'Пост номер 8 (rss)',
    draftTitleRss: 'Пост номер 8 (draft rss)',
    titleApp: 'Пост номер 8 (app)',
    draftTitleApp: 'Пост номер 8 (draft app)',
    lead: 'Пост номер 8 Пост номер 8 Пост номер 8',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_9: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 219,
    urlPart: 'url-part-219',
    service: Service.realty,
    title: 'Пост номер 9',
    draftTitle: 'Пост номер 9 (draft)',
    titleRss: 'Пост номер 9 (rss)',
    draftTitleRss: 'Пост номер 9 (draft rss)',
    titleApp: 'Пост номер 9 (app)',
    draftTitleApp: 'Пост номер 9 (draft app)',
    lead: 'Пост номер 9 Пост номер 9 Пост номер 9',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_10: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 220,
    urlPart: 'url-part-220',
    service: Service.realty,
    title: 'Пост номер 10',
    draftTitle: 'Пост номер 10 (draft)',
    titleRss: 'Пост номер 10 (rss)',
    draftTitleRss: 'Пост номер 10 (draft rss)',
    titleApp: 'Пост номер 10 (app)',
    draftTitleApp: 'Пост номер 10 (draft app)',
    lead: 'Пост номер 10 Пост номер 10 Пост номер 10',
    blocks: [],
    draftBlocks: [],
    mainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImage4x3: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageRss: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    mainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    draftMainImageApp: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    cover: {
        meta: {
            'orig-format': 'PNG',
            'orig-size-bytes': 900900,
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/orig',
                width: 1880,
                height: 700,
            },
            optimize: {
                path: '/get-vertis-journal/1398410/Malogabaritnye_kvartiry_1604661526745/optimize',
                width: 1880,
                height: 700,
            },
        },
        'group-id': 1398410,
        imagename: 'Malogabaritnye_kvartiry_1604661526745',
    },
    author: 'robot',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'robot',
    publishAt: '2021-01-20T20:10:30.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
    lastEditedAt: '2020-11-05T11:18:56.000Z',
    createdAt: '2020-11-05T10:30:33.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};
