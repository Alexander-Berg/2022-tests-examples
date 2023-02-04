import type { HumanTechInfo, HumanTechInfoGroup, HumanTechInfoEntity } from '@vertis/schema-registry/ts-types-snake/auto/catalog/api_model';
import { HumanTechInfoGroupId } from '@vertis/schema-registry/ts-types-snake/auto/catalog/api_model';

import prepareTechInfo from './index';

type TestCase = {
    name: string;
    techInfoGroup: Array<Partial<HumanTechInfoGroup>>;
    result: Array<Partial<HumanTechInfoGroup>>;
}

const entity = (id: string): HumanTechInfoEntity => ({ id, name: '', value: '', units: '' });

const TEST_CASES: Array<TestCase> = [
    {
        name: 'должен добавить группу АККУМУЛЯТОРНАЯ БАТАРЕЯ, не трогая параметры из NO_GROUP',
        techInfoGroup: [ {
            id: HumanTechInfoGroupId.NO_GROUP,
            entity: [ entity('electric_range'), entity('acceleration') ],
        } ],
        result: [ {
            id: HumanTechInfoGroupId.NO_GROUP,
            entity: [ entity('electric_range'), entity('acceleration') ],
        }, {
            // в схеме кажется ошибка - тип ACCUMULATOR_BATTERY отсутствует
            id: 'ACCUMULATOR_BATTERY' as unknown as HumanTechInfoGroupId,
            name: 'Аккумуляторная батарея',
            entity: [ entity('electric_range') ],
        } ],
    },
    {
        name: 'должен добавить группу АККУМУЛЯТОРНАЯ БАТАРЕЯ и удалить входящие в нее параметры из других групп',
        techInfoGroup: [ {
            id: HumanTechInfoGroupId.PERFORMANCE_INDICATORS,
            entity: [ entity('charge_time'), entity('battery_capacity'), entity('consumption') ],
        } ],
        result: [ {
            id: HumanTechInfoGroupId.PERFORMANCE_INDICATORS,
            entity: [ entity('consumption') ],
        }, {
            // в схеме кажется ошибка - тип ACCUMULATOR_BATTERY отсутствует
            id: 'ACCUMULATOR_BATTERY' as unknown as HumanTechInfoGroupId,
            name: 'Аккумуляторная батарея',
            entity: [ entity('charge_time'), entity('battery_capacity') ],
        } ],
    },
    {
        name: 'не должен добавлять группу АККУМУЛЯТОРНАЯ БАТАРЕЯ, если нет нужных параметров',
        techInfoGroup: [ {
            id: HumanTechInfoGroupId.PERFORMANCE_INDICATORS,
            entity: [ entity('acceleration'), entity('consumption') ],
        } ],
        result: [ {
            id: HumanTechInfoGroupId.PERFORMANCE_INDICATORS,
            entity: [ entity('acceleration'), entity('consumption') ],
        } ],
    },
];

TEST_CASES.forEach(({ name, result, techInfoGroup }) => {
    it(name, () => {
        const techInfo = { result: { result: { data: { tech_info_group: techInfoGroup } as HumanTechInfo } } };
        expect(prepareTechInfo(techInfo).data.tech_info_group).toEqual(result);
    });
});
