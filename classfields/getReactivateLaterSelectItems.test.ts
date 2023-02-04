import MockDate from 'mockdate';

import getReactivateLaterSelectItems from './getReactivateLaterSelectItems';

MockDate.set('2020-05-10');

it('правильно отдаёт элементы для селекта при выборе Продам позже при снятии оффера', () => {
    expect(getReactivateLaterSelectItems()).toMatchSnapshot();
});
