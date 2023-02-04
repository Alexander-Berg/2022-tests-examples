import type { YaImagesBlock, YaImagesItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import type { YogaNode } from 'app/server/tmpl/YogaNode';
import { last } from 'lodash';
import { YaImagesCell } from './Cell';
import { YaImagesModel } from './Model';

const TIMESTAMP = 1587143959831;
describe('ya images cell', () => {
    it('block shows expand cell when more than 5', () => {
        const model = createYaImageModel(6, {
            external_uri: 'https://preview.redd.it/2l2av8at5sn31.jpg',
            size: '250x300',
            title: 'Reddit.com',
        });
        const cell = renderCellWithModel(model);
        const collapsed = cell[0].children?.[1];
        const expanded = cell[0].children?.[2];
        const collapsedChildren = collapsed?.children;
        const expandedChildren = expanded?.children;
        expect(last(collapsedChildren)?.children?.[0].text).toBe('Ещё 1\xa0фотография');
        expect(last(expandedChildren)?.children?.[0].text).toBe('Свернуть');
        expect(expanded).toMatchSnapshot();
        expect(collapsed).toMatchSnapshot();
    });
    it('block does not show expand/collapse when 5 or less', () => {
        const model = createYaImageModel(5, {
            external_uri: 'https://preview.redd.it/2l2av8at5sn31.jpg',
            size: '250x300',
            title: 'Reddit.com',
        });
        const cell = renderCellWithModel(model);
        expect(cell[0]?.children?.[1].children?.length).toBe(9);
        expect(cell).toMatchSnapshot();
    });
});

function createYaImageModel(count: number, yaImageRecord: YaImagesItem) {
    return {
        header: {
            title: 'Фотографии из Яндекс.Картинок',
            is_updating: false,
            timestamp_update: new Date(TIMESTAMP).toISOString(),
        },
        status: Status.OK,
        record_count: count,
        ya_images_records: Array(count).fill(yaImageRecord),
    };
}

function renderCellWithModel(model: YaImagesBlock): Array<YogaNode> {
    const yaImagesCell = new YaImagesCell();
    const identifier = new YaImagesModel().identifier;
    return yaImagesCell.nodes(identifier, model);
}
