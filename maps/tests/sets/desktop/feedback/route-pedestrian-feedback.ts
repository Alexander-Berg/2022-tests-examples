import {run} from '../../../lib/func/sidebar-feedback-generator';
import cssSelectors from '../../../common/css-selectors';

run({
    desc: 'Фидбэк. Маршрут (Пеший)',
    parentPanel: 'routes',
    precondition: async (browser) => {
        await browser.openPage('?rtext=55.734384,37.588121~55.760207,37.625325&rtt=pd');
        await browser.waitAndClick(cssSelectors.routes.editPedestrian);
        await browser.waitForVisible(cssSelectors.routeFeedback.view);
    },
    routePoint: [37.60899751324462, 55.74881592108554],
    backText: 'Возврат к карточке маршрута',
    testMenu: {
        better: 'Есть маршрут лучше',
        addObject: 'Добавить объект на карту',
        incorrectPedestrian: 'Нельзя пройти по маршруту',
        other: 'Другое'
    },
    withoutEmail: ['better', 'incorrectPedestrian', 'other']
});
