import {run} from '../../../lib/func/sidebar-feedback-generator';
import cssSelectors from '../../../common/css-selectors';

run({
    desc: 'Фидбэк. Маршрут (ОТ)',
    parentPanel: 'routes',
    additionalSelector: cssSelectors.routes.detailedRoute.panel,
    precondition: async (browser) => {
        await browser.openPage('?rtext=55.734384,37.588121~55.760207,37.625325&rtt=mt');
        await browser.waitAndClick(cssSelectors.routes.routeList.activeSnippet);
        await browser.waitAndClick(cssSelectors.routes.detailedRoute.edit);
        await browser.waitForVisible(cssSelectors.routeFeedback.view);
    },
    routePoint: [37.60226246284715, 55.74416823219137],
    backText: 'Возврат к карточке маршрута',
    testMenu: {
        incorrectMasstransit: 'Нельзя проехать',
        better: 'Есть маршрут лучше',
        addObject: 'Добавить объект на карту',
        other: 'Другое'
    },
    withoutEmail: ['incorrectMasstransit', 'better', 'other']
});
