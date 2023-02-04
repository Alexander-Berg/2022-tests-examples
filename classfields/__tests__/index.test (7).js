const { renderToStaticMarkup } = require('react-dom/server');
const I18N = require('../index');

describe('i18n', () => {
    describe('interpolateComponents', () => {
        const Link = ({ children, url }) => (
            <a href={url} className='Link'>{children}</a>
        );
        const Bold = ({ children }) => (
            <strong className='Bold'>{children}</strong>
        );
        const langs = {
            ru: {
                // eslint-disable-next-line max-len
                common: '<link-1>Подключите</link-1> <bold>тариф</bold> <link-2>«Расширенный»</link-2> до конца <bold>месяца</bold>',
                'tag-without-space': 'Подключите тариф «Расширенный»<br/> до конца месяца',
                'tag-with-space': 'Подключите тариф «Расширенный»<br /> до конца месяца',
                nested: 'Подключите тариф <link><bold>«Расширенный»</bold></link>',
                missed: 'Подключите тариф «Расширенный»<br /> до конца месяца',
                'params-and-tags': 'Подключите тариф <link>«Расширенный»</link> до %{time}',
                'only-params': 'Подключите тариф «Расширенный» до %{time}'
            }
        };
        const i18n = I18N.include(lang => langs[lang]);

        it('должен заменять теги на компоненты', () => {
            expect(
                renderToStaticMarkup(
                    i18n('common', null, {
                        'link-1': <Link url='url-1' />,
                        'link-2': <Link url='url-2' />,
                        bold: <Bold />
                    })
                )
            ).toMatchSnapshot();
        });

        it('должен заменять теги без пробела перед /> на компоненты', () => {
            expect(
                renderToStaticMarkup(
                    i18n('tag-without-space', null, {
                        br: <br />
                    })
                )
            ).toMatchSnapshot();
        });

        it('должен заменять теги с пробелом перед /> на компоненты', () => {
            expect(
                renderToStaticMarkup(
                    i18n('tag-with-space', null, {
                        br: <br />
                    })
                )
            ).toMatchSnapshot();
        });

        it('должен заменять вложенные теги на компоненты', () => {
            expect(
                renderToStaticMarkup(
                    i18n('nested', null, {
                        bold: <Bold />,
                        link: <Link url='#' />
                    })
                )
            ).toMatchSnapshot();
        });

        it('не должен заменять теги для которых не передан компонент', () => {
            expect(
                renderToStaticMarkup(
                    i18n('missed', null, {})
                )
            ).toMatchSnapshot();
        });

        it('должен подставлять параметры и заменять теги на компоненты', () => {
            expect(
                renderToStaticMarkup(
                    i18n('params-and-tags', { time: '10:00' }, {
                        link: <Link url='#' />
                    })
                )
            ).toMatchSnapshot();
        });

        it('должен подставлять параметры, если не передана карта с компонентами', () => {
            expect(
                renderToStaticMarkup(
                    i18n('only-params', { time: '10:00' })
                )
            ).toMatchSnapshot();
        });
    });
});
