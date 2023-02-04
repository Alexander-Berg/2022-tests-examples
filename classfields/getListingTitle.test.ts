import getListingTitle from './getListingTitle';
import type { Props } from './getListingTitle';

let props: Props;

beforeEach(() => {
    props = {
        title: 'Новости',
        listingType: 'category',
        subsection: {
            urlPart: 'pro',
            title: 'Про бизнес',
            type: 'category',
        },
    };
});

it('возвращает исходный заголовок, если нет подсекции', () => {
    delete props.subsection;
    const result = getListingTitle(props);

    expect(result).toBe(props.title);
});

it('возвращает заголовок для листинга и подсекции category', () => {
    const result = getListingTitle(props);

    expect(result).toBe('Материалы по теме Новости и Про бизнес');
});

it('возвращает заголовок для листинга category', () => {
    props.subsection = {
        urlPart: 'bmw',
        title: 'BMW',
        type: 'tag',
    };
    const result = getListingTitle(props);

    expect(result).toBe('Новости по теме');
});

it('возвращает заголовок, если листинг tag и подсекции category', () => {
    props.listingType = 'tag';
    const result = getListingTitle(props);

    expect(result).toBe('Про бизнес по теме');
});

it('возвращает заголовок, если листинг и подсекция tag', () => {
    props.listingType = 'tag';
    props.subsection = {
        urlPart: 'bmw',
        title: 'BMW',
        type: 'tag',
    };
    const result = getListingTitle(props);

    expect(result).toBe('Материалы по теме');
});
