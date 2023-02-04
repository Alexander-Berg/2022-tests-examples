import {run} from '../../../lib/func/sidebar-feedback-generator';
import cssSelectors from '../../../common/css-selectors';

run({
    desc: 'Фидбэк. Маршрут (Вело)',
    parentPanel: 'routes',
    precondition: async (browser) => {
        await browser.openPage('?rtext=55.734384,37.588121~55.760207,37.625325&rtt=bc');
        await browser.waitAndClick(cssSelectors.routes.editBicycle);
        await browser.waitForVisible(cssSelectors.routeFeedback.view);
    },
    backText: 'Возврат к карточке маршрута',
    routePoint: [37.61159106353759, 55.74774488158421],
    testMenu: {
        better: 'Есть маршрут лучше',
        addObject: 'Добавить объект на карту',
        incorrectBicycle: 'Нельзя проехать по маршруту',
        other: 'Другое'
    },
    withoutEmail: ['better', 'incorrectBicycle', 'other']
});
