const getPaymentSuccessText = require('./getPaymentSuccessText');

describe('function "getPaymentSuccessText"', () => {
    it('формирует правильный текст для пакетов', () => {
        const text = getPaymentSuccessText([ { name: 'Экспресс-продажа', service: 'package_express' } ]);
        expect(text).toMatchSnapshot();
    });

    it('формирует правильный текст для обычных опций', () => {
        const names = [
            { name: 'Выделение цветом', service: 'all_sale_color' },
            { name: 'Поднятие в ТОП', service: 'all_sale_fresh' },
            { name: 'Спецпредложение', service: 'all_sale_special' },
        ];
        names.forEach((name) => {
            const text = getPaymentSuccessText([ name ]);
            expect(text).toMatchSnapshot('подключено');
        });
    });

    it('формирует правильный текст для двух сервисов', () => {
        const text = getPaymentSuccessText([
            { name: 'Экспресс-продажа', service: 'package_express' },
            { name: 'Спецпредложение', service: 'all_sale_special' },
        ]);
        expect(text).toMatchSnapshot();
    });

    it('формирует правильный текст для трех и более сервисов', () => {
        const text = getPaymentSuccessText([
            { name: 'Экспресс-продажа', service: 'package_express' },
            { name: 'Спецпредложение', service: 'all_sale_special' },
            { name: 'Поднятие в ТОП', service: 'all_sale_fresh' },
        ]);
        expect(text).toMatchSnapshot();
    });
});
