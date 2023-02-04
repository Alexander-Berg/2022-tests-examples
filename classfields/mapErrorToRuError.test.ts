import mapErrorToRuError from './mapErrorToRuError';

describe('mapErrorToRuError показывает понятную ошибку на русском', () => {
    it('если текст ошибки VIN_CODE_NOT_FOUND', () => {
        expect(mapErrorToRuError('VIN_CODE_NOT_FOUND')).toBe('Не удалось найти машину по ВИНу. Заполните форму самостоятельно');
    });

    it('если текст ошибки LICENSE_PLATE_NOT_FOUND', () => {
        expect(mapErrorToRuError('LICENSE_PLATE_NOT_FOUND')).toBe('Не удалось найти машину по госномеру. Заполните форму самостоятельно');
    });

    it('если текст ошибки другой', () => {
        expect(mapErrorToRuError('HELP_EVERYTHING_IS_DOWN')).toBe('Неизвестная ошибка. Попробуйте еще раз или заполните форму самостоятельно');
    });
});
