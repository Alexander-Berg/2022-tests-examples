import cssSelectors from '../../common/css-selectors';
import counterGenerator, {CaseSpec, EventSpec} from '../../lib/counter-generator';
import getSelectorByText from '../../lib/func/get-selector-by-text';

const CLIENT_ID = 'web';
const FORM_CONTEXT_ID = 'map.context';
const CLIENT_CONTEXT_ID = 'analytics-client-context';

const EDIT_MAP_MENU_TYPE = 'map/edit';
const SELECT_OBJECT_MENU_TYPE = 'object/select';
const ADD_OBJECT_MENU_TYPE = 'object/add';
const EDIT_OBJECT_MENU_TYPE = 'object/edit';
const EDIT_ORGANIZATION_MENU_TYPE = 'organization/edit';
const EDIT_ORGANIZATION_STATUS_MENU_TYPE = 'organization/closed';
const EDIT_ROUTE_MENU_TYPE = 'route/edit';
const EDIT_OBJECT_MENU = {
    addObject: {
        title: 'Добавление объекта',
        selector: cssSelectors.addObjectFeedback.view,
        formId: 'toponym',
        query: {
            feedback: ADD_OBJECT_MENU_TYPE
        },
        items: {
            'organization/add': 'Организация',
            'address/add': 'Адрес',
            'entrance/add': 'Вход в здание',
            'road/add': 'Дорога',
            'barrier/add': 'Шлагбаум',
            'transit/stop/add': 'Остановка',
            'parking/add': 'Парковка',
            'crosswalk/add': 'Пешеходный переход',
            'fence/add': 'Забор',
            'gate/add': 'Калитка',
            'other/add': 'Другой объект'
        }
    },
    editToponym: {
        title: 'Редактирование топонима',
        selector: cssSelectors.editObjectFeedback.view,
        formId: 'toponym',
        query: {
            feedback: EDIT_OBJECT_MENU_TYPE,
            // топоним с провайдерами
            ouri:
                'ymapsbm1://geo?ll=37.587%2C55.733&spn=0.001%2C0.001&' +
                '&text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D0%9F%D1%83%D0%B' +
                '3%D0%BE%D0%B2%D0%B8%D1%88%D0%BD%D0%B8%D0%BA%D0%BE%D0%B2%20%D0%BF%D0%B5%D1%80%D0%B5%D1%83%D0%BB%D0%BE%D0%BA%2C%202'
        },
        items: {
            'address/edit': 'Изменить адрес',
            'entrance/edit': 'Исправить или добавить входы',
            'organization/add': 'Добавить организацию',
            'provider/add': 'Добавить провайдера',
            'object/other': 'Другое'
        }
    },
    editPark: {
        title: 'Редактирование парка',
        selector: cssSelectors.editObjectFeedback.view,
        formId: 'toponym',
        query: {
            feedback: EDIT_OBJECT_MENU_TYPE,
            ouri:
                'ymapsbm1://geo?ll=37.585%2C55.734&spn=0.003%2C0.001&' +
                'text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D0%BF%D0%B0%D1%80%D0' +
                '%BA%20%D1%83%D1%81%D0%B0%D0%B4%D1%8C%D0%B1%D1%8B%20%D0%9B.%D0%9D.%20%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3' +
                '%D0%BE%20%D0%B2%20%D0%A5%D0%B0%D0%BC%D0%BE%D0%B2%D0%BD%D0%B8%D0%BA%D0%B0%D1%85'
        },
        items: {
            'object/add': 'Добавить объект на карту',
            'object/name/edit': 'Изменить название объекта',
            'object/location/edit': 'Объект в другом месте',
            'object/not-found': 'Объекта здесь нет',
            'organization/add': 'Добавить организацию',
            'object/other': 'Другое'
        }
    },
    editEntrance: {
        title: 'Редактирование входа',
        selector: cssSelectors.editObjectFeedback.view,
        formId: 'toponym',
        query: {
            feedback: EDIT_OBJECT_MENU_TYPE,
            ouri:
                'ymapsbm1://geo?ll=37.587%2C55.734&spn=0.001%2C0.001&text=%D0%A0%D0%BE%D1%81' +
                '%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D1%83%D0%BB%D0%B8%D1%86%D0%B0%20%D0%9B%D1%8C' +
                '%D0%B2%D0%B0%20%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C%2016%2C%20%D0%BF%D0%BE%D0%B4%D1%8A%D0%B5%D0%' +
                'B7%D0%B4%204%20%7B1711007348%7D&z=19'
        },
        items: {
            'entrance/edit': 'Исправить вход',
            'organization/add': 'Добавить организацию',
            // TODO: GEOAPPGOODS-504
            // 'provider/add': 'Добавить провайдера',
            'object/other': 'Другое'
        }
    },
    editOpenedOrganization: {
        title: 'Редактирование открытой организации',
        selector: cssSelectors.organizationFeedback.menu,
        formId: 'organization',
        query: {
            feedback: EDIT_ORGANIZATION_MENU_TYPE,
            ol: 'biz',
            oid: 1124715036
        },
        items: {
            'organization/edit-info': 'Неправильная информация',
            'organization/closed': 'Закрыто или не существует',
            'organization/location/edit': 'Исправить местоположение',
            'organization/edit-accessibility': 'Доступность',
            'organization/other': 'Другое'
        }
    },
    editClosedOrganization: {
        title: 'Редактирование закрытой организации',
        selector: cssSelectors.organizationFeedback.menu,
        formId: 'organization',
        query: {
            feedback: EDIT_ORGANIZATION_MENU_TYPE,
            ol: 'biz',
            oid: 1083414973
        },
        items: {
            'organization/edit-info': 'Неправильная информация',
            'organization/opened': 'Снова открыто',
            'organization/location/edit': 'Исправить местоположение',
            'organization/other': 'Другое'
        }
    },
    editOrganizationStatus: {
        title: 'Редактирование статуса организации',
        selector: cssSelectors.organizationFeedback.menu,
        formId: 'organization',
        query: {
            feedback: EDIT_ORGANIZATION_STATUS_MENU_TYPE,
            ol: 'biz',
            oid: 1124715036
        },
        items: {
            'organization/closed/permanently': 'Закрыта навсегда',
            'organization/closed/temporary': 'Временно не работает',
            'organization/closed/unknown': 'Никогда не существовала',
            'organization/moved': 'Переехала'
        }
    },
    mapEdit: {
        title: 'Редактирование карты',
        selector: cssSelectors.editMapFeedback.view,
        formId: '*',
        query: {
            feedback: EDIT_MAP_MENU_TYPE
        },
        items: {
            'object/select': 'Исправить неточность',
            'object/add': 'Добавить новый объект'
        }
    },
    objectSelect: {
        title: 'Выбор объекта на карте',
        selector: cssSelectors.selectObjectFeedback.view,
        formId: '*',
        query: {
            feedback: SELECT_OBJECT_MENU_TYPE
        },
        items: {}
    },
    ...[
        {
            rtt: 'auto',
            type: 'Авто',
            items: {
                'route/incorrect': 'Нельзя',
                'object/add': 'Добавить объект на карту',
                'route/edit/better': 'Есть маршрут лучше',
                'route/other': 'Другое'
            }
        },
        {
            rtt: 'mt',
            type: 'ОТ',
            items: {
                'route/incorrect/other': 'Нельзя',
                'object/add': 'Добавить объект на карту',
                'route/edit/better': 'Есть маршрут лучше',
                'route/other': 'Другое'
            }
        },
        {
            rtt: 'pd',
            type: 'Пеший',
            items: {
                'route/incorrect/other': 'Нельзя',
                'object/add': 'Добавить объект на карту',
                'route/edit/better': 'Есть маршрут лучше',
                'route/other': 'Другое'
            }
        },
        {
            rtt: 'bc',
            type: 'Вело',
            items: {
                'route/incorrect/other': 'Нельзя',
                'object/add': 'Добавить объект на карту',
                'route/edit/better': 'Есть маршрут лучше',
                'route/other': 'Другое'
            }
        },
        {
            rtt: 'sc',
            type: 'Скутер',
            items: {
                'route/incorrect/other': 'Нельзя',
                'object/add': 'Добавить объект на карту',
                'route/edit/better': 'Есть маршрут лучше',
                'route/other': 'Другое'
            }
        }
    ].reduce(
        (acc, {rtt, type, items}) =>
            ({
                ...acc,
                [`${rtt}RouteEdit`]: {
                    items,
                    title: `Редактирование маршрута. ${type}`,
                    selector: cssSelectors.routeFeedback.view,
                    formId: 'route',
                    query: {
                        rtt,
                        feedback: EDIT_ROUTE_MENU_TYPE,
                        rtext: '55.735628%2C37.587252~55.733096%2C37.588012',
                        ruri: '~'
                    }
                }
            } as const),
        {}
    )
} as const;

counterGenerator({
    name: 'Формы меню фидбека.',
    specs: Object.values(EDIT_OBJECT_MENU).reduce<CaseSpec[]>((acc, {query, items, selector, title, formId}) => {
        const url = `/?${Object.entries(query)
            .map(([key, value]) => `${key}=${value}`)
            .join(
                '&'
            )}&feedback-metadata[client_id]=${CLIENT_ID}&feedback-context=${FORM_CONTEXT_ID}&feedback-client-context=${CLIENT_CONTEXT_ID}`;
        acc.push(
            {
                name: 'maps_www.feedback.form',
                description: `Меню «${title}».`,
                url,
                selector,
                events: [
                    {
                        type: 'show',
                        state: {
                            formId,
                            type: 'form',
                            formType: query.feedback,
                            formContextId: FORM_CONTEXT_ID,
                            clientContextId: CLIENT_CONTEXT_ID,
                            clientId: CLIENT_ID
                        }
                    },
                    {
                        type: 'hide',
                        state: {
                            formId,
                            type: 'form',
                            formType: query.feedback,
                            formContextId: FORM_CONTEXT_ID,
                            clientContextId: CLIENT_CONTEXT_ID,
                            clientId: CLIENT_ID
                        },
                        setup: async (browser) => {
                            await browser.waitAndClick(cssSelectors.feedback.header.close);
                        }
                    },
                    ...Object.entries(items).map<EventSpec>(([, description]) => ({
                        type: 'hide',
                        description: `Клик в пункт меню «${description}».`,
                        state: {
                            formId,
                            type: 'form',
                            formType: query.feedback,
                            formContextId: FORM_CONTEXT_ID,
                            clientContextId: CLIENT_CONTEXT_ID,
                            clientId: CLIENT_ID
                        },
                        setup: async (browser) => {
                            await browser.waitAndClick(
                                getSelectorByText(description, selector, {seekOccurrence: true})
                            );
                        }
                    }))
                ]
            },
            ...Object.entries(items).map<CaseSpec>(([type, description]) => ({
                url,
                description: `Пункт «${description}» в меню «${title}».`,
                selector: getSelectorByText(description, selector, {seekOccurrence: true}),
                name: `maps_www.feedback.form.sidebar.menu_item`,
                events: [
                    {
                        type: 'show',
                        options: {
                            multiple: true
                        },
                        state: {
                            formType: query.feedback,
                            openForm: '*'
                        }
                    },
                    {
                        type: 'click',
                        state: {
                            formType: query.feedback,
                            openForm: type
                        }
                    }
                ]
            }))
        );
        return acc;
    }, [])
});
