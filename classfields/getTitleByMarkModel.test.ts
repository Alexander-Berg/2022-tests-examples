import { nbsp } from 'auto-core/react/lib/html-entities';

import getTitleByMarkModel from './getTitleByMarkModel';

describe('правильно формирует тайтл', () => {
    it('когда нет марки-модели', () => {
        expect(getTitleByMarkModel()).toEqual(`Новые автомобили по${ nbsp }лучшим ценам`);
    });

    it('когда есть только марка', () => {
        expect(getTitleByMarkModel('Audi')).toEqual(`Новые Audi по${ nbsp }лучшим ценам`);
    });

    it('когда есть марка-модель', () => {
        expect(getTitleByMarkModel('Audi', 'Q5')).toEqual(`Новые Audi Q5 по${ nbsp }лучшим ценам`);
    });
});
