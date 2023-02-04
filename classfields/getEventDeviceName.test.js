const getEventDeviceName = require('./getEventDeviceName');

let event;
beforeEach(() => {
    event = {
        user_info: {
            device: {},
        },
    };
});

it('должен отдавать платформу для десктопа', () => {
    event.user_info.device.platform = 'desktop';

    expect(getEventDeviceName(event)).toBe('ПК');
});

it('должен отдавать платформу для тача', () => {
    event.user_info.device.platform = 'touch';

    expect(getEventDeviceName(event)).toBe('Мобильная версия');
});

it('должен отдавать отформатированное имя бренда для других платформ', () => {
    event.user_info.device.brand = 'APPLE';

    expect(getEventDeviceName(event)).toBe('Apple');
});
