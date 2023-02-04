import engineType from './engineType';

describe('getOriginal', () => {
    it('правильно отдаст один тип двигателя', () => {
        expect(engineType.getOriginal('gasoline')).toBe('Бензин');
    });
    it('правильно отдаст несколько тип двигателя', () => {
        expect(engineType.getOriginal([ 'gasoline', 'diesel', 'electro' ])).toBe('Бензин, Дизель, Электро');
    });
});
