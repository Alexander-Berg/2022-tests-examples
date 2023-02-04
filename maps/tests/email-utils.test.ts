import * as emailUtils from 'server/utils/email-utils';

const CORRECTED_EMAIL = 'test.email@example.ru';

const CORRECTION_SETS: Record<string, string> = {
    'Валидная после исправления': '. .. tes t.email.@. example..ry .. . ',
    'Удаление пробелов': '  t e s t . email @ example . ru  ',
    'Удаление дублирующих точек': 'test...email@example..ru',
    'Удаление точки до и после @': 'test.email..@..example.ru',
    'Удаление точек в начале и конце': '...test.email@example.ru...',
    'Исправление опечатки в ru домене': 'test.email@example.ry'
};

describe('Утилиты для работы с почтой', () => {
    describe('Корректировака почты', () => {
        test('Пустая почта', () => {
            expect(emailUtils.correctEmail(undefined)).toEqual(undefined);
        });

        Object.entries(CORRECTION_SETS).forEach(([name, incorrectEmail]) => {
            test(name, () => {
                expect(emailUtils.correctEmail(incorrectEmail)).toEqual(CORRECTED_EMAIL);
            });
        });
    });
});
