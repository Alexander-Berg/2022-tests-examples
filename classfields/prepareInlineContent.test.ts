import { prepareCSS, prepareJS } from './prepareInlineContent';

describe('prepareCSS', () => {
    it.each<[string, string]>([
        [
            '.adAliasName{display: block;} @media print { .adAliasName{display: none;} }',
            '.VertisAds{display: block;} @media print { .VertisAds{display: none;} }',
        ],
        [
            '.adAlias__rtb,.adAlias__adfox{display: block;}',
            '.VertisAds__rtb,.VertisAds__adfox{display: block;}',
        ],
    ])('должен преобразовать %s', (content, result) => {
        expect(
            prepareCSS({ content, adAliasName: 'VertisAds' }),
        ).toEqual(result);
    });
});

describe('prepareJS', () => {
    it('должен заменить %adAliasName% и %isDesktop%, если isDesktop=true', () => {
        expect(
            prepareJS({
                content: 'const name="%adAliasName%";const IS_DESKTOP=Boolean("%isDesktop%");',
                isDesktop: true,
                adAliasName: 'VertisAds',
            }),
        ).toBe('const name="VertisAds";const IS_DESKTOP=Boolean("true");');
    });

    it('должен заменить %adAliasName% и %isDesktop%, если isDesktop=false', () => {
        expect(
            prepareJS({
                content: 'const name="%adAliasName%";const IS_DESKTOP=Boolean("%isDesktop%");',
                isDesktop: false,
                adAliasName: 'VertisAds',
            }),
        ).toBe('const name="VertisAds";const IS_DESKTOP=Boolean("");');
    });
});
