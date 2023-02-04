/* eslint-disable max-len */
import { IFeed } from 'realty-core/types/feed';

export const bannedFeed: IFeed = {
    partnerId: '1069252144',
    name: 'Забанен №2',
    url: 'http://ddl7.data.hu/get/395137/12686590/number_ban2.xml',
    extendedReason: {
        list: [
            {
                segment: 'sell-room',
                title: 'Сомнительная цена',
                reasonText:
                    'Возможно, цена указана с ошибкой. Проверьте, что в поле <price> нет лишнего знака или наоборот, нет пропущенных. Уточните валюту. Сверьте, что выбран нужный тип сделки — продажа или аренда, длительная или посуточная. Если нашли ошибку — исправьте её. Мы быстро проверим и активируем объявление. Если вы считаете, что ошибок нет — напишите в службу поддержки.',
                reasonHTML:
                    'Возможно, цена указана с ошибкой. Проверьте, что в поле &#60;price&#62; нет лишнего знака или наоборот, нет пропущенных. Уточните валюту. Сверьте, что выбран нужный тип сделки — продажа или аренда, длительная или посуточная. Если нашли ошибку — исправьте её. Мы быстро проверим и активируем объявление. Если вы считаете, что ошибок нет — напишите в службу поддержки.',
                example: '444444',
            },
            {
                segment: 'rent-house',
                title: 'Неверный тип партнера',
                reasonText:
                    'В форме загрузки фида, в поле «Информация об источнике», неверно указан тип партнера. Данные о продавце в форме загрузки фида должны быть указаны точно и без ошибок. Чтобы отредактировать «Информацию об источнике», зайдите в «Личный кабинет», нажмите на значок карандаша на верхней панели справа и в открывшемся меню выберите единственно верный вариант — «Агентство», «Застройщик» или «Агент».',
                reasonHTML: '',
                example: '',
            },
            {
                segment: 'sell-comm',
                title: 'Отказ предоставить информацию',
                reasonText:
                    'Несуществующие или закрытые для просмотра объекты. Разместив объявление о продаже или аренде, будьте готовы оперативно и точно отвечать на все вопросы покупателей и, при необходимости, организовать показ нужного объекта.',
                reasonHTML: '',
                example: '',
            },
            {
                segment: 'all',
                title: 'Объявление о поиске объекта',
                reasonText:
                    'На Яндекс.Недвижимости нельзя размещать объявления о поиске жилья. Мы принимаем только публикации о продаже и сдаче в аренду. Чтобы найти объект, воспользуйтесь поиском и фильтрами.',
                reasonHTML: '',
                example: '',
            },
            {
                segment: 'sell-lot',
                title: 'Новостройка в разделе вторичного жилья',
                reasonText:
                    'Вы разместили квартиру в новостройке как вторичное жильё. Отредактируйте элемент <deal-status> и выберите тип сделки: первичная продажа, прямая продажа или переуступка. Подробнее: https://yandex.ru/support/realty/requirements/requirements-sale-new.html#concept6.',
                reasonHTML:
                    'Вы разместили квартиру в новостройке как вторичное жильё. Отредактируйте элемент &#60;deal-status&#62; и выберите тип сделки: первичная продажа, прямая продажа или переуступка. Подробнее: <a class="link" href="https://yandex.ru/support/realty/requirements/requirements-sale-new.html#concept6" target="_blank">«Требования к фидам»</a>.',
                example: '',
            },
            {
                segment: 'sell-lot',
                title: 'Неактуальное предложение',
                reasonText:
                    'Неактуальные предложения или объекты под задатком, авансом, залогом. Если объект продан, сдан, за него внесён залог, аванс или задаток — не тяните время, удаляйте такое объявление сразу же.',
                reasonHTML: '',
                example: '',
            },
            {
                segment: 'sell-garage',
                title: 'Просмотр невозможен',
                reasonText:
                    'Объект недоступен для просмотра. Разместив объявление о продаже или аренде, будьте готовы оперативно отвечать на все сопутствующие вопросы покупателей и, при необходимости, организовать показ нужного объекта. Важно: сразу же удаляйте предложения, потерявшие свою актуальность, а также объекты под задатком, авансом или залогом.',
                reasonHTML: '',
                example: '',
            },
            {
                segment: 'all',
                title: 'Предложение услуг',
                reasonText:
                    'Рекламные призывы и слоганы. Ваш рассказ про объект должен быть точным, правдивым и касаться исключительно предмета продажи. Не стоит давать в теге description ссылки на сторонние агентства и адвокатские конторы, предложения по оформлению сделок, подбору и другим услугам.',
                reasonHTML: '',
                example: 'sdfsdf234, 44444',
            },
        ],
    },
    status: 'moderation_error',
    statusId: 22,
    removed: false,
    canChangeStatusAt: 1606557274000,
    createTime: 1606377289000,
};

export const onModerationFeed = {
    ...bannedFeed,
    status: 'moderation',
    statusId: 6,
};

export const feeds = {
    list: [
        {
            partnerId: '1069252130',
            name: 'number2',
            url: 'http://s3.mdst.yandex.net/realty/suburban/number2.xml',
            lastIndexInfo: {
                indexTime: 1607924988269,
                feedLoadTime: 1607921892000,
                total: 17,
                accepted: 14,
                declined: 3,
                declinedNotInVerba: 0,
                errors: [
                    {
                        type: 'ROOMS_SPACE_NOT_MATCH_ROOMS_COUNT',
                        text: 'Ошибка в комнатах или их площадях',
                        count: 3,
                        urls: ['1234510', '1234516', '1234511'],
                    },
                ],
            },
            status: 'active',
            statusId: 12,
            removed: false,
            canChangeStatusAt: 1606388463000,
            createTime: 1606208016000,
        },
        {
            partnerId: '1069252132',
            name: 'Забаненный',
            url: 'http://s3.mdst.yandex.net/realty/suburban/number_ban.xml',
            extendedReason: {
                list: [
                    {
                        segment: 'rent-room',
                        title: 'Доски менее 80%',
                        reasonText:
                            "<strong>Доски объявлений, в которых заявлено больше 20% неактуальных предложений</strong>, на сайте запрещены. <br>Отправляя на рассмотрение новый фид, убедитесь, что в нём точно не меньше 80% рабочих объявлений. Кроме того, не стоит зашивать в офферы гиперссылки — покупатели в любом случае не смогут перейти с нашего сайта на ваш. </br><br><strong>Важно!</strong> Если вы намерены продолжать сотрудничество с <a href='https://realty.yandex.ru'>Яндекс.Недвижимость</a>, советуем подумать о том, чтобы стать прямым партнером сервиса. Это позволит вам отслеживать статистику по загрузкам, просмотрам и кликам, подключать <a href='https://yandex.ru/support/realty/agency/home/promotion-xml-housing.html'>дополнительные услуги</a> к объявлениям. Отправить заявку просто — напишите нам ответ на это письмо, и мы свяжем вас с нужным специалистом.",
                        reasonHTML: '',
                        example: '453453453, sdfsdfsdf',
                    },
                ],
            },
            status: 'moderation_error',
            statusId: 22,
            removed: false,
            canChangeStatusAt: 1606914875000,
            createTime: 1606227577000,
        },
        bannedFeed,
        {
            partnerId: '1069252148',
            name: 'off_sale',
            url: 'http://s3.mdst.yandex.net/realty/export/ren_park_open_plan1.xml',
            lastIndexInfo: {
                indexTime: 1607925018550,
                feedLoadTime: 1607925018552,
                total: 0,
                accepted: 0,
                declined: 0,
                declinedNotInVerba: 0,
                errors: [],
            },
            status: 'moderation_error',
            statusId: 5,
            removed: false,
            canChangeStatusAt: 1606566313000,
            createTime: 1606389673000,
        },
        {
            partnerId: '1069252150',
            name: 'ban3',
            url: 'http://s3.mdst.yandex.net/realty/export/zooey_test_ban3.xml',
            extendedReason: {
                list: [
                    {
                        segment: 'sell-garage',
                        title: 'Дом в разделе участка',
                        reasonText:
                            'Внимательно и точно заполняйте теги category, type, deal-status. Объявления, в которых эта важная информация не отражает или приукрашивает действительность, модерацию не пройдут.',
                        reasonHTML: '',
                        example: '',
                    },
                ],
            },
            status: 'banned',
            statusId: 16,
            removed: false,
            canChangeStatusAt: 1606641022000,
            createTime: 1606389908000,
        },
        {
            partnerId: '1069252160',
            name: 'feeds_for_test',
            url: 'https://s3.mdst.yandex.net/realty/feeds_for_test/dt/wrongdate.xml',
            status: 'moderation',
            statusId: 6,
            removed: false,
            canChangeStatusAt: 1606644442000,
            createTime: 1606467850000,
        },
        {
            partnerId: '1069252218',
            name: 'Невалидный XSD',
            url: 'http://s3.mdst.yandex.net/realty/suburban/zooey0912.xml',
            status: 'download_error',
            statusId: 3,
            removed: false,
            canChangeStatusAt: 1607956395000,
            createTime: 1607523894000,
        },
        {
            partnerId: '1069252219',
            name: 'Скачивание',
            url: 'http://s3.mdst.yandex.net/realty/suburban/zooey0912.xml',
            status: 'downloading',
            statusId: 2,
            removed: false,
            canChangeStatusAt: 1607956395000,
            createTime: 1607523894000,
        },
        {
            partnerId: '1069252223',
            name: 'Архивный',
            url: 'http://s3.mdst.yandex.net/realty/suburban/zooey0912.xml',
            status: 'archived',
            statusId: 15,
            removed: false,
            canChangeStatusAt: 1607956395000,
            createTime: 1607523894000,
        },
    ],
    status: 'loaded',
};
