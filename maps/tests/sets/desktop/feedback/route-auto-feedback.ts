import {run} from '../../../lib/func/sidebar-feedback-generator';
import cssSelectors from '../../../common/css-selectors';

run({
    desc: 'Фидбэк. Маршрут (авто)',
    parentPanel: 'routes',
    additionalSelector: cssSelectors.routes.detailedRoute.panel,
    precondition: async (browser) => {
        await browser.openPage('?rtext=55.734384,37.588121~55.760207,37.625325&rtt=auto');
        await browser.waitAndClick(cssSelectors.routes.routeList.activeSnippet);
        await browser.waitAndClick(cssSelectors.routes.detailedRoute.edit);
        await browser.waitForVisible(cssSelectors.routeFeedback.view);
    },
    routePoint: [37.61217886114501, 55.74779382399363],
    backText: 'Возврат к карточке маршрута',
    testMenu: {
        better: 'Есть маршрут лучше',
        incorrect: {
            title: 'Нельзя проехать',
            items: {
                obstruction: 'Препятствие',
                prohibitingSign: 'Запрещающая разметка или знак',
                poorCondition: 'Неровная дорога',
                roadClosed: 'Здесь нет дороги',
                other: 'Другое'
            }
        },
        addObject: 'Добавить объект на карту',
        other: 'Другое'
    },
    withoutEmail: ['better', 'obstruction', 'prohibitingSign', 'poorCondition', 'roadClosed', 'other']
});
