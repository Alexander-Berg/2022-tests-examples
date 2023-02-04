const usersData = {
    /**
     * Закладок нет.
     * Не подписан на Рассылятор.
     *
     * Конструктор:
     * - `um=constructor:6d05c3b921fec896d033698456a980ef4ad0d31d01cd7e20944a52c604a81844`
     * - `um=constructor:7165a28f5f5c833607453078636c4864200a73f6ebe671c063280182561673bd`
     */
    common: {
        login: 'geo.auto.test',
        password: 'pass-testing'
    },

    /**
     * Подписан на рассылку в Рассыляторе.
     */
    subscriptions: {
        login: 'geo.auto.test.subscriptions',
        password: 'pass-testing'
    },

    /**
     * Закладки:
     * 1. Работа.
     * 2. Одна в "Избранном".
     * 3. Две в списках "Первый" и "Третий".
     * 4. Всего 4 списка.
     */
    bookmarks: {
        login: 'user.with.4.lists',
        password: 'pass-testing'
    },

    /**
     * Закладки:
     * 1. Активный стейт закладки.
     */
    bookmarksCustomList: {
        login: 'geo.auto.test.with-custom-list',
        password: 'pass-testing'
    },

    /**
     * Закладки:
     * 1. Работа.
     * 2. Дом.
     */
    withHomeAndWork: {
        login: 'user.with.home.and.work',
        password: 'pass-testing'
    },

    /**
     * 7 закладок в "Избранном", списков нет.
     */
    bookmarksWithoutLists: {
        login: 'user.without.lists',
        password: 'pass-testing'
    },

    /**
     * Закладки ОТ:
     * 1. Три избранных транспорта.
     * 2. Три избранные остановки.
     */
    masstransitBookmarks: {
        login: 'user.masstransit.bookmarks',
        password: 'pass-testing'
    },

    /**
     * Закладки ОТ:
     * 1. Избранная трамвайная остановка
     * 2. Избранная сдвоенная остановка (автобус + трамвай)
     */
    masstransitBookmarks2: {
        login: 'user.masstransit.bookmarks.2',
        password: 'pass-testing'
    },

    /**
     * Закладки ОТ:
     * 1. Избранный маршрут н9 (ночной)
     * 2. Четыре избранные остановки (с 1, 3, >3 и избранным маршрутами)
     */
    masstransitBookmarks3: {
        login: 'user.masstransit.bookmarks.3',
        password: 'pass-testing'
    },

    /**
     * Персональный саджест:
     * 1. «Mission» на Спартаковской
     */
    personalSuggest: {
        login: 'geo.auto.test.personal.suggest',
        password: 'pass-testing'
    },

    /**
     * 11 пустых списков с разными иконками.
     */
    manyFolders: {
        login: 'geo.auto.text.many-folders',
        password: 'pass-testing'
    },

    /**
     * Зарегано на английской локали.
     */
    enBookmarks: {
        login: 'geo.auto.test.en',
        password: 'pass-testing'
    },

    /**
     * Пользователь с данными из личного кабинета.
     */
    ugcProfile: {
        login: 'geo.auto.test.ugc',
        password: 'pass-testing'
    },

    /**
     * Пользователь с данными по орге из личного кабинета.
     */
    ugcProfileOrg: {
        login: 'geo.auto.test.ugcOrg',
        password: 'pass-testing'
    },

    /**
     * Пользователь с данными из личного кабинета c загруженными фотками с разными состояниями.
     */
    ugcProfileStatuses: {
        login: 'geo.auto.test.ugc.statuses',
        password: 'pass-testing'
    },

    /**
     * Пользователь с данными по бронированиям.
     */
    booking: {
        login: 'geo.auto.test.booking',
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
        login: 'geo.auto.test.bkmrk-discovery',
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
        login: 'geo.auto.test.bookmark-zooms',
        password: 'pass-testing'
    },

    /**
     * Для тестов призумов на поднятие список закладок при добавлении закладки.
     * Закладки:
     * 1, 2, 3 -- все изначально пустые
     */

    bookmarksLists: {
        login: 'geo.auto.test.bookmark-lists',
        password: 'pass-testing'
    },

    /**
     * Для тестов на мультидобавление закладок.
     * Закладки:
     * 1, 2, 3 -- в каждой есть одинаковая закладка
     */
    bookmarksMultiAdd: {
        login: 'geo.auto.test.bookmarkMultiAdd',
        password: 'pass-testing'
    },

    /**
     * Для тестов, где надо менять язык в паспорте
     */
    langSwitch: {
        login: 'geo.auto.test.langSwitch',
        password: 'pass-testing'
    },

    /**
     * Для тестов во вкладке "Отзывы, фото и исправления"
     */
    ugcFeedbacks: {
        login: 'geo.auto.test.ugcFeedbacks',
        password: 'pass-testing'
    },

    /**
     * Для тестов на сторис
     *
     * Пользователь владеет оргой "Кофемашина", которая используется для тестирования сторис
     * - https://yandex.ru/maps/org/kofemashina/175016252281/
     * - https://altay.yandex-team.ru/cards/175016252281
     *
     * Права на организацию никому не раздаются.
     * Коллекция сторис в организации не меняется.
     * Сторисы в тестовом окружении точно такие же, как в проде.
     *
     * Не хватает существующих сторис для покрытия какого-то тесткейса? Просим в документацию:
     * ./tests/README.md#Как-отредактировать-сторис-для-автотестов
     */
    stories: {
        login: 'geo.auto.test.stories',
        password: 'pass-testing'
    }

    /**
     * Не нашёл подходящего юзера? Создай своего! Просим в документацию:
     * ./tests/README.md#Нужно-создать-пользователя-для-автотестов
     *
     * Всегда нужно создавать два пользователя: один для ./users.ts, другой для ./assessors-users.ts
     * Подробности https://st.yandex-team.ru/MAPSUI-16716
     */
};

type UserId = keyof typeof usersData;

export {usersData, UserId};
