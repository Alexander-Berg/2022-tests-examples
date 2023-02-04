import type { Metrika } from 'auto-core/react/lib/metrika';

import sendMetrikaThreeViewsCars from './sendMetrikaThreeViewsCars';

const metrika: Metrika = {
    reachGoal: jest.fn(),
    sendParams: jest.fn(),
    sendPageEvent: jest.fn(),
    susaninReactHistoryListener: jest.fn(),
    susaninReactUpdateListener: jest.fn(),
};

it('Отправит метрику, так как 3 посещения', () => {
    sendMetrikaThreeViewsCars(metrika);
    sendMetrikaThreeViewsCars(metrika);
    sendMetrikaThreeViewsCars(metrika);
    expect(metrika.reachGoal).toHaveBeenCalledTimes(1);
    expect(metrika.reachGoal).toHaveBeenLastCalledWith('VIEW_OR_CONTACT');
});

it('Не отправит метрику, так < 3 посещений', () => {
    sendMetrikaThreeViewsCars(metrika);
    sendMetrikaThreeViewsCars(metrika);
    expect(metrika.reachGoal).toHaveBeenCalledTimes(0);
});
