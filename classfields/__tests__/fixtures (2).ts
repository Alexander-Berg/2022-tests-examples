import { IMainAttributes } from '../../../types/main';
import { ICategoryAttributes } from '../../../types/category';
import { ITagAttributes } from '../../../types/tag';
import { IPostModelAttributes, PostStatus } from '../../../types/post';
import { Service } from '../../../types/common';

export const MAIN_DATA_1: IMainAttributes = {
    service: Service.realty,
    blocks: [
        {
            type: 'text',
            text: 'stil-khay-tek-v-interere-kak-sozdat-dom-iz-buduschego-v-obychnoy-kvartire',
        },
        {
            type: 'text',
            text: 'stil-khay-tek-v-interere-kak-sozdat-dom-iz-buduschego-v-obychnoy-kvartire',
        },
    ],
    draftBlocks: [
        {
            type: 'text',
            text: 'arenda-nuzhen-li-mne-rieltor',
        },
    ],
};

export const UPDATE_MAIN_DATA_1 = {
    draftBlocks: [
        {
            type: 'material',
            material: 'novostroyki-i-stalinki-protiv-brezhnevok-i-khruschevok-kak-dorozhayut-moskovskie-kvartiry',
        },
    ],
};

export const UPDATE_MAIN_DATA_2 = {
    draftBlocks: [
        {
            type: 'section',
            section: {
                blocks: [
                    {
                        type: 'threeRight',
                        threeRight: ['v-kazahstane-prodayut-betmobil-po-cene-desyati-horoshih-mersedesov'],
                    },
                    null,
                    undefined,
                    false,
                ],
            },
        },
    ],
};

export const CATEGORY_DATA_1: ICategoryAttributes = {
    urlPart: 'kommercheskaya-nedvizhimost',
    service: Service.realty,
    title: 'Коммерческая недвижимость',
    shortTitle: 'Ком. недвижимость',
    blocks: [],
    draftTitle: 'Коммерческая недвижимость (черновик)',
    draftShortTitle: 'Ком. недвижимость (черновик)',
    draftBlocks: [{ type: 'text', text: 'Актуальные новости и статьи по теме: коммерческая недвижимость' }],
    isArchived: false,
    metaTitle: 'Коммерческая недвижимость - статьи',
    metaDescription: 'Все интересные статьи про коммерческую недвижимость',
};

export const CREATE_CATEGORY_DATA_1: Partial<ICategoryAttributes> = {
    urlPart: 'obzory',
    service: Service.autoru,
    draftTitle: 'Обзоры',
    draftShortTitle: 'Обзоры',
    draftBlocks: [{ type: 'text', text: 'Новые обзоры' }],
    isArchived: false,
    metaTitle: 'Статьи по категории обзоры',
    metaDescription: 'Статьи по категории обзоры',
};

export const UPDATE_CATEGORY_DATA_1 = {
    service: Service.realty,
    draftTitle: 'Ремонт и дизайн',
    draftShortTitle: 'Ремонт',
    draftBlocks: [{ type: 'text', text: 'Статьи на тему ремонта и дизайна' }],
    tags: [],
    isArchived: true,
    metaTitle: 'Статьи по категории "ремонт и дизайн"',
    metaDescription: 'Статьи по категории "ремонт и дизайн"',
};

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
    partnershipLink:
        'https://www.tinkoff.ru/loans/cash-loan/realty/?utm_source=yandexrealty0321_knz&utm_medium=ntv.fix&utm_campaign=loans.cashloan_realty.yandex',
    partnershipImage: null,
    metaTitle: 'Вторичное жилье - статьи',
    metaDescription: 'Все интересные статьи про вторичное жилье',
    mmm: 'TOYOTA:COROLLA:2020',
};

export const TAG_DATA_2: ITagAttributes = {
    urlPart: 'specproekt',
    service: Service.realty,
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
    partnershipLink:
        'https://www.tinkoff.ru/loans/cash-loan/realty/?utm_source=yandexrealty0321_knz&utm_medium=ntv.fix&utm_campaign=loans.cashloan_realty.yandex',
    metaTitle: 'Спецпроект - статьи',
    metaDescription: 'Все интересные статьи по спецпроекту',
    mmm: 'MERCEDES:A-KLASSE:2019',
};

export const TAG_DATA_3: ITagAttributes = {
    urlPart: 'kommercheskaya-nedvizhimost',
    service: Service.realty,
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
    partnershipLink:
        'https://www.tinkoff.ru/loans/cash-loan/realty/?utm_source=yandexrealty0321_knz&utm_medium=ntv.fix&utm_campaign=loans.cashloan_realty.yandex',
    metaTitle: 'Коммерческая недвижимость - статьи',
    metaDescription: 'Все интересные статьи про коммерческую недвижимость',
    mmm: 'AUDI,A4,2018',
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
    mainImage4x3: {
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
    draftMainImage4x3: {
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
    mainImageRss: {
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
    draftMainImageRss: {
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
    mainImageApp: {
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
    draftMainImageApp: {
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
    cover: {
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
            md5: 'b3dfdecb906e54a4f3490dcc6b816d18',
            crc64: '030D469467D6FA71',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 449089,
            'orig-orientation': '0',
            'modification-time': 1604661535,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
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
    urlPart: 'evrodvushka-i-evrotreshka-chto-eto',
    service: Service.realty,
    title: 'Евродвушка и евротрёшка: что это?',
    draftTitle: 'Евродвушка и евротрёшка: что это? (draft)',
    titleRss: 'Евродвушка и евротрёшка: что это? (rss)',
    draftTitleRss: 'Евродвушка и евротрёшка: что это? (draft rss)',
    titleApp: 'Евродвушка и евротрёшка: что это? (app)',
    draftTitleApp: 'Евродвушка и евротрёшка: что это? (draft app)',
    lead: 'В рекламе новостроек и объявлениях о продаже жилья теперь часто пишут «евродвушка» или «евротрёшка».',
    blocks: [
        {
            text: '<p><span style="font-size:24px"><strong>Самая важная комната&nbsp;&mdash; это кухня</strong></span></p>',
            type: 'text',
        },
    ],
    draftBlocks: [
        {
            text: '<p><span style="font-size:24px"><strong>Самая важная комната&nbsp;&mdash; это кухня (draft)</strong></span></p>',
            type: 'text',
        },
    ],
    mainImage: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    draftMainImage: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    mainImage4x3: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    draftMainImage4x3: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    mainImageRss: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    draftMainImageRss: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    mainImageApp: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    draftMainImageApp: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    cover: {
        meta: {
            md5: 'd78fb4aea95a8a4ec52ccd4f58a478e7',
            crc64: '28E0C664B0B1E669',
            'orig-size': {
                x: 1880,
                y: 700,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 197366,
            'orig-orientation': '0',
            'modification-time': 1604661534,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/orig',
                width: 1880,
                height: 700,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/optimize',
                width: 1880,
                height: 700,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Evrodvushka_i_evrotryoshka_chto_eto_1604661525966/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Evrodvushka_i_evrotryoshka_chto_eto_1604661525966',
    },
    author: 'dcversus',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
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
    lastEditLogin: 'dcversus',
    publishAt: '2018-04-04T12:00:00.000Z',
    lastEditedAt: '2020-11-04T11:18:55.000Z',
    createdAt: '2020-11-04T10:30:32.000Z',
    lastOnlineLogin: 'editor-1',
    lastOnlineTime: '2020-11-06T11:18:57.000Z',
};

export const POST_DATA_4: IPostModelAttributes = {
    status: PostStatus.publish,
    id: 214,
    urlPart: 'vse-chto-nuzhno-znat-o-privatizatsii-zhilya',
    service: Service.realty,
    title: 'Всё, что нужно знать о приватизации жилья',
    draftTitle: 'Всё, что нужно знать о приватизации жилья',
    draftTitleRss: 'Всё, что нужно знать о приватизации жилья',
    titleRss: 'Всё, что нужно знать о приватизации жилья',
    draftTitleApp: 'Всё, что нужно знать о приватизации жилья',
    titleApp: 'Всё, что нужно знать о приватизации жилья',
    lead: 'В СССР частной собственности практически не было.',
    blocks: [
        {
            type: 'quota',
            quota: {
                text: 'текст цитаты',
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612374685,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1398410,
                    imagename: '8182292-1.jpg_1612374685275',
                },
                title: 'Умная цитата',
                author: 'я',
            },
        },
        {
            card: {
                url: 'http://some.test.com',
                text: '<p><br></p>',
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612439517,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1398410,
                    imagename: '8182292-1.jpg_1612439516806',
                },
                title: 'Заголовок карточки',
            },
            type: 'card',
        },
        {
            type: 'image',
            image: {
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612439534,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1399832,
                    imagename: '8182292-1.jpg_1612439534760',
                },
                height: '500',
            },
        },
        {
            type: 'gallery',
            gallery: [
                {
                    image: {
                        meta: {
                            md5: 'cf98d0960d5caee44686b08c31bdc946',
                            crc64: '55D5E8A0277D2842',
                            'orig-size': {
                                x: 900,
                                y: 1200,
                            },
                            processing: 'finished',
                            'orig-format': 'JPEG',
                            'orig-animated': false,
                            'orig-size-bytes': 61098,
                            'orig-orientation': '0',
                            'modification-time': 1612439606,
                            processed_by_computer_vision: false,
                            processed_by_computer_vision_description: 'computer vision is disabled',
                        },
                        sizes: {
                            orig: {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/orig',
                                width: 900,
                                height: 1200,
                            },
                            '320х320': {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/320х320',
                                width: 240,
                                height: 320,
                            },
                            '460х460': {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/460х460',
                                width: 345,
                                height: 460,
                            },
                            optimize: {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/optimize',
                                width: 900,
                                height: 1200,
                            },
                            '1200х1200': {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/1200х1200',
                                width: 900,
                                height: 1200,
                            },
                        },
                        'group-id': 1398410,
                        imagename: '8182292-1.jpg_1612439606200',
                    },
                    title: 'заголовок картинки',
                    sourceUrl: 'источник картинки',
                    description: 'описание картинки',
                },
            ],
        },
        {
            type: 'imageWithDescription',
            imageWithDescription: {
                type: 'normal',
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612440059,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1399832,
                    imagename: '8182292-1.jpg_1612440059764',
                },
                description: '<p><br></p>',
            },
        },
        {
            quiz: {
                results: [
                    {
                        image: {
                            meta: {
                                md5: 'cf98d0960d5caee44686b08c31bdc946',
                                crc64: '55D5E8A0277D2842',
                                'orig-size': {
                                    x: 900,
                                    y: 1200,
                                },
                                processing: 'finished',
                                'orig-format': 'JPEG',
                                'orig-animated': false,
                                'orig-size-bytes': 61098,
                                'orig-orientation': '0',
                                'modification-time': 1612440102,
                                processed_by_computer_vision: false,
                                processed_by_computer_vision_description: 'computer vision is disabled',
                            },
                            sizes: {
                                orig: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/orig',
                                    width: 900,
                                    height: 1200,
                                },
                                '320х320': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/320х320',
                                    width: 240,
                                    height: 320,
                                },
                                '460х460': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/460х460',
                                    width: 345,
                                    height: 460,
                                },
                                optimize: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/optimize',
                                    width: 900,
                                    height: 1200,
                                },
                                '1200х1200': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/1200х1200',
                                    width: 900,
                                    height: 1200,
                                },
                            },
                            'group-id': 1398410,
                            imagename: '8182292-1.jpg_1612440102885',
                        },
                        title: 'Неплохо!',
                        rangeMax: '1',
                    },
                ],
                questions: [
                    {
                        image: {
                            meta: {
                                md5: 'cf98d0960d5caee44686b08c31bdc946',
                                crc64: '55D5E8A0277D2842',
                                'orig-size': {
                                    x: 900,
                                    y: 1200,
                                },
                                processing: 'finished',
                                'orig-format': 'JPEG',
                                'orig-animated': false,
                                'orig-size-bytes': 61098,
                                'orig-orientation': '0',
                                'modification-time': 1612440078,
                                processed_by_computer_vision: false,
                                processed_by_computer_vision_description: 'computer vision is disabled',
                            },
                            sizes: {
                                orig: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/orig',
                                    width: 900,
                                    height: 1200,
                                },
                                '320х320': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/320х320',
                                    width: 240,
                                    height: 320,
                                },
                                '460х460': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/460х460',
                                    width: 345,
                                    height: 460,
                                },
                                optimize: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/optimize',
                                    width: 900,
                                    height: 1200,
                                },
                                '1200х1200': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/1200х1200',
                                    width: 900,
                                    height: 1200,
                                },
                            },
                            'group-id': 1398410,
                            imagename: '8182292-1.jpg_1612440078586',
                        },
                        answers: [
                            {
                                image: {
                                    meta: {
                                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                                        crc64: '55D5E8A0277D2842',
                                        'orig-size': {
                                            x: 900,
                                            y: 1200,
                                        },
                                        processing: 'finished',
                                        'orig-format': 'JPEG',
                                        'orig-animated': false,
                                        'orig-size-bytes': 61098,
                                        'orig-orientation': '0',
                                        'modification-time': 1612440083,
                                        processed_by_computer_vision: false,
                                        processed_by_computer_vision_description: 'computer vision is disabled',
                                    },
                                    sizes: {
                                        orig: {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/orig',
                                            width: 900,
                                            height: 1200,
                                        },
                                        '320х320': {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/320х320',
                                            width: 240,
                                            height: 320,
                                        },
                                        '460х460': {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/460х460',
                                            width: 345,
                                            height: 460,
                                        },
                                        optimize: {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/optimize',
                                            width: 900,
                                            height: 1200,
                                        },
                                        '1200х1200': {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/1200х1200',
                                            width: 900,
                                            height: 1200,
                                        },
                                    },
                                    'group-id': 1398410,
                                    imagename: '8182292-1.jpg_1612440083183',
                                },
                            },
                        ],
                        rightAnswer: {
                            image: {
                                meta: {
                                    md5: 'cf98d0960d5caee44686b08c31bdc946',
                                    crc64: '55D5E8A0277D2842',
                                    'orig-size': {
                                        x: 900,
                                        y: 1200,
                                    },
                                    processing: 'finished',
                                    'orig-format': 'JPEG',
                                    'orig-animated': false,
                                    'orig-size-bytes': 61098,
                                    'orig-orientation': '0',
                                    'modification-time': 1612440088,
                                    processed_by_computer_vision: false,
                                    processed_by_computer_vision_description: 'computer vision is disabled',
                                },
                                sizes: {
                                    orig: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/orig',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '320х320': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/320х320',
                                        width: 240,
                                        height: 320,
                                    },
                                    '460х460': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/460х460',
                                        width: 345,
                                        height: 460,
                                    },
                                    optimize: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/optimize',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '1200х1200': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/1200х1200',
                                        width: 900,
                                        height: 1200,
                                    },
                                },
                                'group-id': 1398410,
                                imagename: '8182292-1.jpg_1612440088870',
                            },
                        },
                        incorrectAnswer: {
                            image: {
                                meta: {
                                    md5: 'cf98d0960d5caee44686b08c31bdc946',
                                    crc64: '55D5E8A0277D2842',
                                    'orig-size': {
                                        x: 900,
                                        y: 1200,
                                    },
                                    processing: 'finished',
                                    'orig-format': 'JPEG',
                                    'orig-animated': false,
                                    'orig-size-bytes': 61098,
                                    'orig-orientation': '0',
                                    'modification-time': 1612440095,
                                    processed_by_computer_vision: false,
                                    processed_by_computer_vision_description: 'computer vision is disabled',
                                },
                                sizes: {
                                    orig: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/orig',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '320х320': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/320х320',
                                        width: 240,
                                        height: 320,
                                    },
                                    '460х460': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/460х460',
                                        width: 345,
                                        height: 460,
                                    },
                                    optimize: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/optimize',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '1200х1200': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/1200х1200',
                                        width: 900,
                                        height: 1200,
                                    },
                                },
                                'group-id': 1398410,
                                imagename: '8182292-1.jpg_1612440095513',
                            },
                        },
                    },
                ],
            },
            type: 'quiz',
        },
    ],
    draftBlocks: [
        {
            type: 'quota',
            quota: {
                text: 'текст цитаты',
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612374685,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612374685275/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1398410,
                    imagename: '8182292-1.jpg_1612374685275',
                },
                title: 'Умная цитата',
                author: 'я',
            },
        },
        {
            card: {
                url: 'http://some.test.com',
                text: '<p><br></p>',
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612439517,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439516806/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1398410,
                    imagename: '8182292-1.jpg_1612439516806',
                },
                title: 'Заголовок карточки',
            },
            type: 'card',
        },
        {
            type: 'image',
            image: {
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612439534,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612439534760/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1399832,
                    imagename: '8182292-1.jpg_1612439534760',
                },
                height: '500',
            },
        },
        {
            type: 'gallery',
            gallery: [
                {
                    image: {
                        meta: {
                            md5: 'cf98d0960d5caee44686b08c31bdc946',
                            crc64: '55D5E8A0277D2842',
                            'orig-size': {
                                x: 900,
                                y: 1200,
                            },
                            processing: 'finished',
                            'orig-format': 'JPEG',
                            'orig-animated': false,
                            'orig-size-bytes': 61098,
                            'orig-orientation': '0',
                            'modification-time': 1612439606,
                            processed_by_computer_vision: false,
                            processed_by_computer_vision_description: 'computer vision is disabled',
                        },
                        sizes: {
                            orig: {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/orig',
                                width: 900,
                                height: 1200,
                            },
                            '320х320': {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/320х320',
                                width: 240,
                                height: 320,
                            },
                            '460х460': {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/460х460',
                                width: 345,
                                height: 460,
                            },
                            optimize: {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/optimize',
                                width: 900,
                                height: 1200,
                            },
                            '1200х1200': {
                                path: '/get-vertis-journal/1398410/8182292-1.jpg_1612439606200/1200х1200',
                                width: 900,
                                height: 1200,
                            },
                        },
                        'group-id': 1398410,
                        imagename: '8182292-1.jpg_1612439606200',
                    },
                    title: 'заголовок картинки',
                    sourceUrl: 'источник картинки',
                    description: 'описание картинки',
                },
            ],
        },
        {
            type: 'imageWithDescription',
            imageWithDescription: {
                type: 'normal',
                image: {
                    meta: {
                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                        crc64: '55D5E8A0277D2842',
                        'orig-size': {
                            x: 900,
                            y: 1200,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 61098,
                        'orig-orientation': '0',
                        'modification-time': 1612440059,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/orig',
                            width: 900,
                            height: 1200,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/320х320',
                            width: 240,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/460х460',
                            width: 345,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/optimize',
                            width: 900,
                            height: 1200,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/8182292-1.jpg_1612440059764/1200х1200',
                            width: 900,
                            height: 1200,
                        },
                    },
                    'group-id': 1399832,
                    imagename: '8182292-1.jpg_1612440059764',
                },
                description: '<p><br></p>',
            },
        },
        {
            quiz: {
                results: [
                    {
                        image: {
                            meta: {
                                md5: 'cf98d0960d5caee44686b08c31bdc946',
                                crc64: '55D5E8A0277D2842',
                                'orig-size': {
                                    x: 900,
                                    y: 1200,
                                },
                                processing: 'finished',
                                'orig-format': 'JPEG',
                                'orig-animated': false,
                                'orig-size-bytes': 61098,
                                'orig-orientation': '0',
                                'modification-time': 1612440102,
                                processed_by_computer_vision: false,
                                processed_by_computer_vision_description: 'computer vision is disabled',
                            },
                            sizes: {
                                orig: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/orig',
                                    width: 900,
                                    height: 1200,
                                },
                                '320х320': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/320х320',
                                    width: 240,
                                    height: 320,
                                },
                                '460х460': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/460х460',
                                    width: 345,
                                    height: 460,
                                },
                                optimize: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/optimize',
                                    width: 900,
                                    height: 1200,
                                },
                                '1200х1200': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440102885/1200х1200',
                                    width: 900,
                                    height: 1200,
                                },
                            },
                            'group-id': 1398410,
                            imagename: '8182292-1.jpg_1612440102885',
                        },
                        title: 'Неплохо!',
                        rangeMax: '1',
                    },
                ],
                questions: [
                    {
                        image: {
                            meta: {
                                md5: 'cf98d0960d5caee44686b08c31bdc946',
                                crc64: '55D5E8A0277D2842',
                                'orig-size': {
                                    x: 900,
                                    y: 1200,
                                },
                                processing: 'finished',
                                'orig-format': 'JPEG',
                                'orig-animated': false,
                                'orig-size-bytes': 61098,
                                'orig-orientation': '0',
                                'modification-time': 1612440078,
                                processed_by_computer_vision: false,
                                processed_by_computer_vision_description: 'computer vision is disabled',
                            },
                            sizes: {
                                orig: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/orig',
                                    width: 900,
                                    height: 1200,
                                },
                                '320х320': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/320х320',
                                    width: 240,
                                    height: 320,
                                },
                                '460х460': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/460х460',
                                    width: 345,
                                    height: 460,
                                },
                                optimize: {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/optimize',
                                    width: 900,
                                    height: 1200,
                                },
                                '1200х1200': {
                                    path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440078586/1200х1200',
                                    width: 900,
                                    height: 1200,
                                },
                            },
                            'group-id': 1398410,
                            imagename: '8182292-1.jpg_1612440078586',
                        },
                        answers: [
                            {
                                image: {
                                    meta: {
                                        md5: 'cf98d0960d5caee44686b08c31bdc946',
                                        crc64: '55D5E8A0277D2842',
                                        'orig-size': {
                                            x: 900,
                                            y: 1200,
                                        },
                                        processing: 'finished',
                                        'orig-format': 'JPEG',
                                        'orig-animated': false,
                                        'orig-size-bytes': 61098,
                                        'orig-orientation': '0',
                                        'modification-time': 1612440083,
                                        processed_by_computer_vision: false,
                                        processed_by_computer_vision_description: 'computer vision is disabled',
                                    },
                                    sizes: {
                                        orig: {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/orig',
                                            width: 900,
                                            height: 1200,
                                        },
                                        '320х320': {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/320х320',
                                            width: 240,
                                            height: 320,
                                        },
                                        '460х460': {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/460х460',
                                            width: 345,
                                            height: 460,
                                        },
                                        optimize: {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/optimize',
                                            width: 900,
                                            height: 1200,
                                        },
                                        '1200х1200': {
                                            path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440083183/1200х1200',
                                            width: 900,
                                            height: 1200,
                                        },
                                    },
                                    'group-id': 1398410,
                                    imagename: '8182292-1.jpg_1612440083183',
                                },
                            },
                        ],
                        rightAnswer: {
                            image: {
                                meta: {
                                    md5: 'cf98d0960d5caee44686b08c31bdc946',
                                    crc64: '55D5E8A0277D2842',
                                    'orig-size': {
                                        x: 900,
                                        y: 1200,
                                    },
                                    processing: 'finished',
                                    'orig-format': 'JPEG',
                                    'orig-animated': false,
                                    'orig-size-bytes': 61098,
                                    'orig-orientation': '0',
                                    'modification-time': 1612440088,
                                    processed_by_computer_vision: false,
                                    processed_by_computer_vision_description: 'computer vision is disabled',
                                },
                                sizes: {
                                    orig: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/orig',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '320х320': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/320х320',
                                        width: 240,
                                        height: 320,
                                    },
                                    '460х460': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/460х460',
                                        width: 345,
                                        height: 460,
                                    },
                                    optimize: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/optimize',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '1200х1200': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440088870/1200х1200',
                                        width: 900,
                                        height: 1200,
                                    },
                                },
                                'group-id': 1398410,
                                imagename: '8182292-1.jpg_1612440088870',
                            },
                        },
                        incorrectAnswer: {
                            image: {
                                meta: {
                                    md5: 'cf98d0960d5caee44686b08c31bdc946',
                                    crc64: '55D5E8A0277D2842',
                                    'orig-size': {
                                        x: 900,
                                        y: 1200,
                                    },
                                    processing: 'finished',
                                    'orig-format': 'JPEG',
                                    'orig-animated': false,
                                    'orig-size-bytes': 61098,
                                    'orig-orientation': '0',
                                    'modification-time': 1612440095,
                                    processed_by_computer_vision: false,
                                    processed_by_computer_vision_description: 'computer vision is disabled',
                                },
                                sizes: {
                                    orig: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/orig',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '320х320': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/320х320',
                                        width: 240,
                                        height: 320,
                                    },
                                    '460х460': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/460х460',
                                        width: 345,
                                        height: 460,
                                    },
                                    optimize: {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/optimize',
                                        width: 900,
                                        height: 1200,
                                    },
                                    '1200х1200': {
                                        path: '/get-vertis-journal/1398410/8182292-1.jpg_1612440095513/1200х1200',
                                        width: 900,
                                        height: 1200,
                                    },
                                },
                                'group-id': 1398410,
                                imagename: '8182292-1.jpg_1612440095513',
                            },
                        },
                    },
                ],
            },
            type: 'quiz',
        },
    ],
    mainImage: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    draftMainImage: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    draftMainImage4x3: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    mainImage4x3: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    mainImageRss: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    draftMainImageRss: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    mainImageApp: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    draftMainImageApp: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    cover: {
        meta: {
            md5: '263fa8ff864f36caa238c6d851906f48',
            crc64: 'C8CC29FE83EB37AA',
            'orig-size': {
                x: 7834,
                y: 2918,
            },
            processing: 'finished',
            'orig-format': 'PNG',
            'orig-animated': false,
            'orig-size-bytes': 226248,
            'orig-orientation': '0',
            'modification-time': 1604661441,
            processed_by_computer_vision: false,
            processed_by_computer_vision_description: 'computer vision is disabled',
        },
        sizes: {
            orig: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/orig',
                width: 7834,
                height: 2918,
            },
            '320х320': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/320х320',
                width: 320,
                height: 119,
            },
            '460х460': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/460х460',
                width: 460,
                height: 171,
            },
            optimize: {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/optimize',
                width: 7834,
                height: 2918,
            },
            '1200х1200': {
                path: '/get-vertis-journal/1399832/Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542/1200х1200',
                width: 1200,
                height: 447,
            },
        },
        'group-id': 1399832,
        imagename: 'Vsyo_chto_nuzhno_znat_o_privatizacii_zhilya_1604661432542',
    },
    author: 'dcversus',
    commentsOff: false,
    rssOff: false,
    advertisementOff: false,
    subscribeOff: false,
    indexOff: false,
    shouldHaveFaqPage: false,
    lastEditLogin: 'avlyalin',
    publishAt: '2019-06-27T14:56:04.000Z',
    lastEditedAt: '2021-02-04T11:49:58.000Z',
    createdAt: '2020-11-06T10:20:19.000Z',
    after: null,
    authorHelp: null,
    before: null,
    illustratorHelp: null,
    lastOnlineLogin: null,
    lastOnlineTime: null,
    mainCategory: null,
    mainImageDescription: null,
    mainImageTitle: null,
    metaDescription: null,
    metaTitle: null,
    needPublishAt: null,
    photographHelp: null,
};
