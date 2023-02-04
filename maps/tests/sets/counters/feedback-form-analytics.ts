import cssSelectors from '../../common/css-selectors';
import counterGenerator from '../../lib/counter-generator';
import {
    ADD_OBJECT_FEEDBACK_FORM_TYPES,
    ADD_ORGANIZATION_FEEDBACK_FORM_TYPES,
    EDIT_ORGANIZATION_FEEDBACK_FORM_TYPES,
    EDIT_TOPONYM_FEEDBACK_FORM_TYPES,
    EDIT_STOP_FEEDBACK_FORM_TYPES,
    EDIT_ROUTE_FEEDBACK_FORM_TYPES
} from '../../../src/feedback/common/types/feedback-common';

const CLIENT_ID = 'web';
const FORM_CONTEXT_ID = 'map.context';
const CLIENT_CONTEXT_ID = 'analytics-client-context';

const DATA = {
    addObject: {
        title: 'Добавление топонима',
        selector: cssSelectors.addToponymFeedback.buttons.submit.view,
        types: ADD_OBJECT_FEEDBACK_FORM_TYPES,
        formId: 'toponym',
        query: {},
        prepareFormForSubmit: async (browser: WebdriverIO.Browser, type: string) => {
            switch (type) {
                case 'address/add':
                    await browser.clickInMap();
                    await browser.setValueToInput(cssSelectors.addToponymFeedback.items.house, 'Дом');
                    break;

                case 'fence/add':
                case 'road/add':
                    const points = [
                        [100, 100],
                        [200, 200]
                    ];

                    for (const [x, y] of points) {
                        await browser.simulateClick({
                            x,
                            y,
                            selector: cssSelectors.map.container,
                            description: 'Рисуем забор'
                        });
                        await browser.waitForVisible(cssSelectors.routeFeedback.routePoint);
                    }
                    await browser.waitForVisible(cssSelectors.addToponymFeedback.buttons.submit.active);
                    break;

                case 'provider/add':
                    await browser.clickInMap();
                    await browser.setValueToInput(cssSelectors.addToponymFeedback.items.name, 'Название');
                    break;

                default:
                    await browser.clickInMap();
                    await browser.setValueToInput(cssSelectors.addToponymFeedback.items.comment, 'Комментарий');
            }
        }
    },
    addOrganization: {
        title: 'Добавление организации',
        selector: cssSelectors.organizationFeedback.card,
        types: ADD_ORGANIZATION_FEEDBACK_FORM_TYPES,
        formId: 'organization',
        query: {},
        prepareFormForSubmit: async (browser: WebdriverIO.Browser) => {
            await browser.waitAndClick(cssSelectors.addToponymFeedback.buttons.submit.active);
        }
    },
    editOrganization: {
        title: 'Редактирование организации',
        selector: cssSelectors.organizationFeedback.card,
        types: EDIT_ORGANIZATION_FEEDBACK_FORM_TYPES.filter((type) => type !== 'organization/location/edit'),
        formId: 'organization',
        query: {
            ol: 'biz',
            oid: 1124715036
        },
        prepareFormForSubmit: async (browser: WebdriverIO.Browser) => {
            await browser.setValueToInput(cssSelectors.organizationFeedback.comment, 'Комментарий');
            await browser.waitAndClick(cssSelectors.addToponymFeedback.buttons.submit.active);
        }
    },
    editOrganizationLocation: {
        title: 'Редактирование организации',
        selector: cssSelectors.organizationFeedback.locationEdit.view,
        types: ['organization/location/edit'],
        formId: 'organization',
        query: {
            ol: 'biz',
            oid: 1124715036
        },
        prepareFormForSubmit: async (browser: WebdriverIO.Browser) => {
            await browser.setValueToInput(cssSelectors.organizationFeedback.comment, 'Комментарий');
            await browser.waitAndClick(cssSelectors.addToponymFeedback.buttons.submit.active);
        }
    },
    ...Object.entries({
        editToponym: {
            title: 'Редактирование топонима',
            selector: cssSelectors.feedback.sidebar.view,
            types: EDIT_TOPONYM_FEEDBACK_FORM_TYPES,
            formId: 'toponym',
            query: {
                ouri:
                    'ymapsbm1://geo?ll=37.587%2C55.734&spn=0.001%2C0.001&' +
                    'text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D1%83%D0%BB%D0%B8%D1' +
                    '%86%D0%B0%20%D0%9B%D1%8C%D0%B2%D0%B0%20%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C%2016'
            },
            prepareFormForSubmit: async (browser: WebdriverIO.Browser) => {
                await browser.waitAndClick(cssSelectors.addToponymFeedback.buttons.submit.active);
            }
        },
        editPark: {
            title: 'Редактирование парка',
            selector: cssSelectors.feedback.sidebar.view,
            types: EDIT_TOPONYM_FEEDBACK_FORM_TYPES,
            formId: 'toponym',
            query: {
                ouri:
                    'ymapsbm1://geo?ll=37.585%2C55.734&spn=0.003%2C0.001&' +
                    'text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D0%BF%D0%B0%D1%80%D0' +
                    '%BA%20%D1%83%D1%81%D0%B0%D0%B4%D1%8C%D0%B1%D1%8B%20%D0%9B.%D0%9D.%20%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3' +
                    '%D0%BE%20%D0%B2%20%D0%A5%D0%B0%D0%BC%D0%BE%D0%B2%D0%BD%D0%B8%D0%BA%D0%B0%D1%85'
            },
            prepareFormForSubmit: async (browser: WebdriverIO.Browser) => {
                await browser.waitAndClick(cssSelectors.addToponymFeedback.buttons.submit.active);
            }
        },
        editEntrance: {
            title: 'Редактирование входа',
            selector: cssSelectors.feedback.sidebar.view,
            types: EDIT_TOPONYM_FEEDBACK_FORM_TYPES,
            formId: 'toponym',
            query: {
                ouri:
                    'ymapsbm1://geo?ll=37.587%2C55.734&spn=0.001%2C0.001&text=%D0%A0%D0%BE%D1%81' +
                    '%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D1%83%D0%BB%D0%B8%D1%86%D0%B0%20%D0%9B%D1%8C' +
                    '%D0%B2%D0%B0%20%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C%2016%2C%20%D0%BF%D0%BE%D0%B4%D1%8A%D0%B5%D0%' +
                    'B7%D0%B4%204%20%7B1711007348%7D&z=19'
            }
        },
        editTransitStop: {
            title: 'Редактирование остановки',
            selector: cssSelectors.feedback.sidebar.view,
            types: EDIT_STOP_FEEDBACK_FORM_TYPES,
            formId: 'toponym',
            query: {
                'masstransit[stopId]': 'stop__9648341'
            }
        }
    } as const).reduce<Record<string, Object>>((acc, [key, value]) => {
        acc[key] = {
            ...value,
            prepareFormForSubmit: async (browser: WebdriverIO.Browser, type: string) => {
                switch (type) {
                    case 'entrance/edit':
                    case 'entrance/name/edit':
                    case 'object/name/edit':
                    case 'transit/stop/name/edit':
                        await browser.setValueToInput(cssSelectors.editObjectFeedback.items.name, 'Название');
                        break;

                    case 'address/edit':
                        await browser.setValueToInput(cssSelectors.editObjectFeedback.items.house, 'Дом');
                        break;

                    case 'entrance/location/edit':
                    case 'object/location/edit':
                    case 'transit/stop/location/edit':
                        await browser.dragPointer({
                            selector: cssSelectors.feedback.map.placemark.view,
                            delta: 100,
                            description: 'Совершить драг плейсмарка в любом направлении.'
                        });
                        break;

                    default:
                        await browser.setValueToInput(cssSelectors.addToponymFeedback.items.comment, 'Комментарий');
                }

                await browser.waitAndClick(cssSelectors.addToponymFeedback.buttons.submit.active);
            }
        };

        return acc;
    }, {}),
    ...[
        {rtt: 'auto', type: 'Авто'},
        {rtt: 'mt', type: 'ОТ'},
        {rtt: 'pd', type: 'Пеший'},
        {rtt: 'bc', type: 'Вело'},
        {rtt: 'sc', type: 'Скутер'}
    ].reduce(
        (acc, {rtt, type}) =>
            ({
                ...acc,
                [`${rtt}RouteEdit`]: {
                    types: EDIT_ROUTE_FEEDBACK_FORM_TYPES,
                    title: `Редактирование маршрута. ${type}`,
                    selector: cssSelectors.routeFeedback.buttons.submit.view,
                    formId: 'route',
                    query: {
                        rtt,
                        rtext: '55.735628%2C37.587252~55.733096%2C37.588012',
                        ruri: '~'
                    },
                    prepareFormForSubmit: async (browser: WebdriverIO.Browser, type: string) => {
                        switch (type) {
                            case 'route/edit/better':
                                const points = [
                                    [100, 100],
                                    [200, 200]
                                ];

                                for (const [x, y] of points) {
                                    await browser.simulateClick({
                                        x,
                                        y,
                                        selector: cssSelectors.map.container,
                                        description: 'Рисуем забор'
                                    });
                                    await browser.waitForVisible(cssSelectors.routeFeedback.routePoint);
                                }
                                await browser.waitForVisible(cssSelectors.routeFeedback.buttons.submit.enabled);
                                break;

                            default:
                                if (browser.isPhone) {
                                    await browser.dragPointerFromCenter({
                                        delta: 10,
                                        description: 'Драгнуть карту в любом направлении'
                                    });
                                } else {
                                    const point: Point =
                                        rtt === 'auto'
                                            ? [37.58962041708024, 55.734072346621744]
                                            : [37.5871244054184, 55.73465561752411];
                                    await browser.simulateGeoClick({point, description: 'Кликнуть в нитку маршрута'});
                                }
                                await browser.setValueToInput(
                                    cssSelectors.addToponymFeedback.items.comment,
                                    'Комментарий'
                                );
                        }
                    }
                }
            } as const),
        {}
    )
} as const;

Object.values(DATA).forEach(({query, selector, title, formId, types, prepareFormForSubmit}) => {
    for (const type of types) {
        const url = `/?${Object.entries(query)
            .map(([key, value]) => `${key}=${value}`)
            .join(
                '&'
            )}&feedback=${type}&feedback-metadata[client_id]=${CLIENT_ID}&feedback-context=${FORM_CONTEXT_ID}&feedback-client-context=${CLIENT_CONTEXT_ID}`;
        const formState = {
            formId,
            type: 'form',
            formType: type,
            formContextId: FORM_CONTEXT_ID,
            clientContextId: CLIENT_CONTEXT_ID,
            clientId: CLIENT_ID
        };
        const submitButtonState = {
            formId,
            type: 'submit_button',
            formType: type,
            formContextId: FORM_CONTEXT_ID,
            clientContextId: CLIENT_CONTEXT_ID,
            clientId: CLIENT_ID
        };

        counterGenerator({
            name: `Формы фидбека: ${title}.`,
            specs: [
                {
                    name: 'maps_www.feedback.form',
                    description: `Сайддбар для ${type}.`,
                    url,
                    selector,
                    events: [
                        {
                            type: 'show',
                            state: formState
                        },
                        {
                            type: 'hide',
                            state: formState,
                            setup: async (browser) => {
                                await browser.waitAndClick(cssSelectors.feedback.header.close);
                            }
                        }
                    ]
                },
                {
                    name: 'maps_www.feedback.form.sidebar.submit',
                    description: `Кнопка отправить для ${type}.`,
                    url,
                    selector,
                    events: [
                        {
                            type: 'show',
                            state: submitButtonState
                        },
                        {
                            type: 'hide',
                            state: submitButtonState,
                            setup: async (browser) => {
                                await browser.waitAndClick(cssSelectors.feedback.header.close);
                            }
                        },
                        {
                            type: 'click',
                            state: submitButtonState,
                            setup: async (browser) => prepareFormForSubmit(browser, type)
                        }
                    ]
                }
            ]
        });
    }
});
