const updateGetParamsInUrl = require('./updateGetParamsInUrl');

it('должен вернуть url с добавленными параметрами', () => {
    expect(updateGetParamsInUrl('http://auto.ru/?param1=1&param2=2', { newParam: '3' }))
        .toEqual('http://auto.ru/?param1=1&param2=2&newParam=3');
});

it('должен вернуть url с обновленными параметрами', () => {
    expect(updateGetParamsInUrl('http://auto.ru/?param1=1&param2=2', { param2: '3' }))
        .toEqual('http://auto.ru/?param1=1&param2=3');
});

it('должен вернуть url без undefined параметров', () => {
    expect(updateGetParamsInUrl('http://auto.ru/?param1=1&param2=2', { param2: undefined }))
        .toEqual('http://auto.ru/?param1=1');
});
