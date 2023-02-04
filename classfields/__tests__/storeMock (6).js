import * as r from 'view/constants/requestStatuses';

export default {
    pending: {
        clientErrors: {
            modal: {
                isOpened: true,
                clientId: '123',
                errors: []
            },
            network: {
                fetchClientErrorsStatus: r.REQUEST_STATUS_PENDING
            }
        }
    },
    loaded: {
        clientErrors: {
            modal: {
                isOpened: true,
                clientId: '123',
                errors: [
                    {
                        count: 42653,
                        text: 'Объявления «от застройщика» могут быть размещены только авторизованными партнерами. Подробнее: <a class="link" href="https://clck.ru/ANXCn" target="_blank">https://clck.ru/ANXCn</a>',
                        type: 'INVALID_PRIMARY_SALE'
                    }
                ]
            },
            network: {
                fetchClientErrorsStatus: r.REQUEST_STATUS_LOADED
            }
        }
    },
    loadedSeveral: {
        clientErrors: {
            modal: {
                isOpened: true,
                clientId: '123',
                errors: [
                    {
                        type: 'LOCATION_NOT_FOUND',
                        text: 'Ошибка в адресе или координатах',
                        count: 2
                    },
                    {
                        type: 'PRICE_NOT_SET', text: 'Не указана цена', count: 1
                    }, {
                        type: 'CHECK_OFFER_SOLD_OUT',
                        text: 'CHECK_OFFER_SOLD_OUT',
                        count: 1
                    },
                    {
                        type: 'CHECK_UNKNOWN_REASON',
                        text: 'Объявление заблокировано по жалобе пользователя',
                        count: 88
                    },
                    {
                        type: 'CHECK_WRONG_ADDRESS',
                        text: 'Неверный адрес',
                        count: 17
                    },
                    {
                        type: 'CHECK_UNAVAILABLE',
                        text: 'CHECK_UNAVAILABLE',
                        count: 2
                    },
                    {
                        type: 'CHECK_WRONG_DATA',
                        text: 'CHECK_WRONG_DATA',
                        count: 2
                    },
                    {
                        type: 'INVALID_PRIMARY_SALE',
                        text: 'Объявления «от застройщика» могут быть размещены только авторизованными партнерами. Подробнее: <a class="link" href="https://clck.ru/ANXCn" target="_blank">https://clck.ru/ANXCn</a>',
                        count: 56757
                    },
                    {
                        type: 'NEW_IN_OLD',
                        text: 'Новостройка в разделе вторичного жилья',
                        count: 1
                    },
                    {
                        type: 'PARTIALLY_ADDRESS', text: 'Нет точного адреса', count: 3
                    }, {
                        type: 'WRONG_PIN',
                        text: 'Метка на карте не в том месте',
                        count: 3
                    },
                    {
                        type: 'WRONG_ROOMS', text: 'Комнатность', count: 1
                    }, {
                        type: 'WRONG_AREA',
                        text: 'Ошибки площади',
                        count: 1
                    },
                    {
                        type: 'PHONE_UNAVAILABLE', text: 'Недоступен', count: 1
                    }, {
                        type: 'PHONE_NOT_EXIST',
                        text: 'Неверный номер / Недоступен / Не существует',
                        count: 2
                    },
                    {
                        type: 'PHOTO_STEAL', text: 'Чужие снимки', count: 1
                    }, {
                        type: 'CONTACT_IN_DESCRIPTION',
                        text: 'В описании указан e-mail, телефон или активная ссылка',
                        count: 1
                    },
                    {
                        type: 'WRONG_CATEGORY',
                        text: 'Ошибка в типе объявления',
                        count: 18
                    }
                ]
            },
            network: {
                fetchClientErrorsStatus: r.REQUEST_STATUS_LOADED
            }
        }
    },
    failed: {
        clientErrors: {
            modal: {
                isOpened: true,
                clientId: '123',
                errors: []
            },
            network: {
                fetchClientErrorsStatus: r.REQUEST_STATUS_ERRORED
            }
        }
    }
};
