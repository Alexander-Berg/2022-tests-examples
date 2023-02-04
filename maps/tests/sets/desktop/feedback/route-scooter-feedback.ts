import {run} from '../../../lib/func/sidebar-feedback-generator';
import cssSelectors from '../../../common/css-selectors';

run({
    desc: 'Фидбэк. Маршрут (Самокат)',
    parentPanel: 'routes',
    precondition: async (browser) => {
        await browser.openPage('?rtext=55.734384,37.588121~55.760207,37.625325&rtt=sc');
        await browser.waitAndClick(cssSelectors.routes.editBicycle);
        await browser.waitForVisible(cssSelectors.routeFeedback.view);
    },
    routePoint: [37.61116472321734, 55.75236443855799],
    backText: 'Возврат к карточке маршрута',
    testMenu: {
        better: 'Есть маршрут лучше',
        addObject: 'Добавить объект на карту',
        incorrectBicycle: 'Нельзя проехать по маршруту',
        other: 'Другое'
    },
    withoutEmail: ['better', 'incorrectBicycle', 'other']
});
