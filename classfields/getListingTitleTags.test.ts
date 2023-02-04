import getListingTitleTags from './getListingTitleTags';
import type { Props } from './getListingTitleTags';

let props: Props;

beforeEach(() => {
    props = {
        title: 'Новости',
        listingType: 'category',
        listingUrlPart: 'news',
        subsection: {
            urlPart: 'bmw',
            title: 'BMW',
            type: 'tag',
        },
    };
});

it('не возвращает теги, если если нет подсекции', () => {
    delete props.subsection;
    const result = getListingTitleTags(props);

    expect(result).toHaveLength(0);
});

it('возвращает теги если листинг и подсекция тегов', () => {
    props.listingType = 'tag';
    const result = getListingTitleTags(props);

    expect(result).toEqual([
        {
            isHot: false,
            title: 'Новости',
            urlPart: 'news',
        },
        {
            isHot: false,
            title: 'BMW',
            urlPart: 'bmw',
        },
    ]);
});
