import MockDate from 'mockdate';

import type { SuperGeneration } from '@vertis/schema-registry/ts-types-snake/auto/api/catalog_model';

import { ndash } from 'auto-core/react/lib/html-entities';

import getSuperGenName from './getSuperGenName';

describe('getSuperGenName', () => {
    beforeEach(() => {
        MockDate.set('2019-01-01');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('вернет пустую строку так как объект supergen не был передан', () => {
        const actual = getSuperGenName();

        expect(actual).toBe('');
    });

    it('вернет поклоение с начальным и конечным годом', () => {
        const actual = getSuperGenName({
            year_from: 2016,
            year_to: 2018,
            name: 'IV',
        } as SuperGeneration);

        expect(actual).toBe(`2016 ${ ndash } 2018 IV`);
    });

    it('вернет поклоение с начальным и текстом "н.в" если год равен текущему', () => {
        const actual = getSuperGenName({
            year_from: 2018,
            year_to: 2019,
            name: 'IV',
        } as SuperGeneration);

        expect(actual).toBe(`2018 ${ ndash } н.в. IV`);
    });
});
