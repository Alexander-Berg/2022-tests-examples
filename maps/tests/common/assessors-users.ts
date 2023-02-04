import {usersData} from './users';

// Пользователи в данном файле используются для автоматической выгрузки тестов в TestPalm.
// Данные учетки используются для ручного прогона кейсов вместо аналогичных из файла ./users.ts

const assessorsUsersData: typeof usersData = {
    /**
     * Закладок нет.
     * Не подписан на Рассылятор.
     *
     * Конструктор:
     * - `um=constructor:6d05c3b921fec896d033698456a980ef4ad0d31d01cd7e20944a52c604a81844`
     */
    common: {
        login: 'yndx-test.common',
        password: 'commonTEST123'
    },

    /**
     * Подписан на рассылку в Рассыляторе.
     */
    subscriptions: {
        login: 'yndx-test.Subscriptions',
        password: 'subscriptions123'
    },

    /**
     * Закладки:
     * 1. Работа.
     * 2. Одна в "Избранном".
     * 3. Две в списках "Первый" и "Третий".
     */
    bookmarks: {
        login: 'yndx-test.bookmarks',
        password: 'yndxbookmarks1234!'
    },

    /**
     * Закладки:
     * 1. Активный стейт закладки.
     */
    bookmarksCustomList: {
        login: 'yndx-test.with-custom-list',
        password: 'pass-testing'
    },

    /**
     * Закладки:
     * 1. Работа.
     * 2. Дом.
     */
    withHomeAndWork: {
        login: 'yndx-test.withHomeAndWork',
        password: 'withHomeAndWork'
    },

    /**
     * 7 закладок в "Избранном", списков нет.
     */
    bookmarksWithoutLists: {
        login: 'yndx-test.WithoutLists',
        password: 'WithoutLists123'
    },

    /**
     * Закладки ОТ:
     * 1. Три избранных транспорта.
     * 2. Три избранные остановки.
     */
    masstransitBookmarks: {
        login: 'yndx-test.masstransit',
        password: 'masstransit123'
    },

    /**
     * Закладки ОТ:
     * 1. Избранная трамвайная остановка
     * 2. Избранная сдвоенная остановка (автобус + трамвай)
     */
    masstransitBookmarks2: {
        login: 'yndx-test.masstransit.2',
        password: 'masstransit123'
    },

    /**
     * Закладки ОТ:
     * 1. Избранный маршрут н9 (ночной)
     * 2. Четыре избранные остановки (с 1, 3, >3 и избранным маршрутами)
     */
    masstransitBookmarks3: {
        login: 'yndx-test.masstransit.3',
        password: 'masstransit123'
    },

    /**
     * Персональный саджест:
     * 1. «Mission» на Спартаковской
     */
    personalSuggest: {
        login: 'yndx-test.personalSuggest',
        password: 'personalSuggest123'
    },

    /**
     * 11 пустых списков с разными иконками.
     */
    manyFolders: {
        login: 'yndx-test.many-folders',
        password: 'manyFolders123'
    },

    /**
     * Зарегано на английской локали.
     */
    enBookmarks: {
        login: 'yndx-test.en',
        password: 'enUser123'
    },

    /**
     * Пользователь с данными из личного кабинета.
     */
    ugcProfile: {
        login: 'yndx-test.ugc',
        password: 'pass-testing'
    },

    /**
     * Пользователь с данными по орге из личного кабинета.
     */
    ugcProfileOrg: {
        login: 'yndx-test.ugcOrg',
        password: 'pass-testing'
    },

    /**
     * Пользователь с данными из личного кабинета c загруженными фотками с разными состояниями.
     */
    ugcProfileStatuses: {
        login: 'yndx-test.ugc.statuses',
        password: 'pass-testing'
    },

    /**
     * Пользователь с данными по бронированиям.
     */
    booking: {
        login: 'yndx-test.booking',
        password: 'pass-testing'
    },

    /**
     * Пользователь у которого пустой список способов оплаты в вебвью оплате парковок.
     */
    parkingPaymentEmptyPaymentMethods: {
        login: 'user.with.home.and.work',
        password: 'pass-testing'
    },

    /**
     * Пользователь у которого в закладках организация которая входит в подборки дискавери
     */
    'bookmark-and-discovery': {
        login: 'yndx-test.bkmrk-discovery',
        password: 'pass-testing'
    },

    /**
     * Для тестов призумов на список закладок
     * Закладки:
     * Мир - спан на весь мир
     * Район - три места в одном доме - зум = 15
     * Точка - точка на карте - меняется только центр карты
     * Места - спан вмещает все точки
     */

    bookmarksZooms: {
        login: 'yndx-test.bookmark-zooms',
        password: 'pass-testing'
    },

    /**
     * Для тестов призумов на поднятие список закладок при добавлении закладки.
     * Закладки:
     * 1, 2, 3 -- все изначально пустые
     */

    bookmarksLists: {
        login: 'yndx-test.bookmark-lists',
        password: 'pass-testing'
    },

    /**
     * Для тестов на мультидобавление закладок.
     * Закладки:
     * 1, 2, 3 -- в каждой есть одинаковая закладка
     */
    bookmarksMultiAdd: {
        login: 'yndx-test.bookmarkMultiAdd',
        password: 'pass-testing'
    },

    /**
     * Для тестов, где надо менять язык в паспорте
     */
    langSwitch: {
        login: 'yndx-test.langSwitch',
        password: 'pass-testing'
    },

    /**
     * Для тестов во вкладке "Отзывы, фото и исправления"
     * взял из текущих кейсов
     */
    ugcFeedbacks: {
        login: 'ugc.test',
        password: 'test.ugc'
    },

    /**
     * Для тестов на сторис
     */
    stories: {
        login: 'yndx-test.stories',
        password: 'pass-testing'
    }
};

export {assessorsUsersData};
