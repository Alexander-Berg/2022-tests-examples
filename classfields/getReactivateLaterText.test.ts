import MockDate from 'mockdate';

import getReactivateLaterSelectItems from './getReactivateLaterSelectItems';
import getReactivateLaterText from './getReactivateLaterText';

MockDate.set('2020-05-10');

it('для каждого айтема селекта при выборе Продам позже при снятии оффера вернет правильную строку', () => {
    const items = getReactivateLaterSelectItems();

    const results = items.map((item) => {
        return getReactivateLaterText(item.date);
    });

    expect(results).toMatchSnapshot();
});
