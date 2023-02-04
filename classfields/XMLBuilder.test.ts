import type { YogaNode } from 'app/server/tmpl/YogaNode';

import { yogaToXml } from 'app/server/tmpl/XMLBuilder';

it('должен вернуть простой JSON для простого XML', () => {
    const nodes: Array<YogaNode> = [
        {
            name: 'stack',
            children: [
                {
                    name: 'stack',
                    text: 'Ребёнок',
                },
            ],
        },
    ];
    const result = yogaToXml(nodes);

    expect(result).toEqual('<list><stack><stack text="Ребёнок"/></stack></list>');
});

describe('эскейпить значений в атрибутах', () => {
    it('должен эскейпить кавычки', () => {
        const nodes: Array<YogaNode> = [
            {
                name: 'stack',
                children: [
                    {
                        name: 'stack',
                        text: 'значение с " кавычкой',
                    },
                ],
            },
        ];
        const result = yogaToXml(nodes);

        expect(result).toEqual('<list><stack><stack text="значение с &quot; кавычкой"/></stack></list>');
    });

    it('должен эскейпить <>', () => {
        const nodes = [
            {
                name: 'stack',
                children: [
                    {
                        name: 'stack',
                        text: 'значение с <> больше меньше',
                    },
                ],
            },
        ];
        const result = yogaToXml(nodes);

        expect(result).toEqual('<list><stack><stack text="значение с &lt;&gt; больше меньше"/></stack></list>');
    });

    it('должен эскейпить \\n \\r', () => {
        const nodes: Array<YogaNode> = [
            {
                name: 'stack',
                children: [
                    {
                        name: 'stack',
                        text: 'значение с \r \n переносом строк',
                    },
                ],
            },
        ];
        const result = yogaToXml(nodes);

        expect(result).toEqual('<list><stack><stack text="значение с &#13; &#10; переносом строк"/></stack></list>');
    });

    it('должен эскейпить nbsp', () => {
        const nodes: Array<YogaNode> = [
            {
                name: 'stack',
                children: [
                    {
                        name: 'stack',
                        text: 'значение с неразрывным пробелом',
                    },
                ],
            },
        ];
        const result = yogaToXml(nodes);

        expect(result).toEqual('<list><stack><stack text="значение с&#160;неразрывным пробелом"/></stack></list>');
    });

    it('должен пропустить атрибут со значением undefined', () => {
        const nodes: Array<YogaNode> = [
            {
                name: 'stack',
                children: [
                    {
                        name: 'stack',
                        height: 10,
                        text: undefined,
                    },
                ],
            },
        ];
        const result = yogaToXml(nodes);

        expect(result).toEqual('<list><stack><stack height="10"/></stack></list>');
    });
});
