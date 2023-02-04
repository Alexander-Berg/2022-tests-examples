import { expect } from '@jest/globals';

import config from 'internal-core/configs';
import { decode } from 'internal-core/utils/uri-component';

import { PostReplacer } from '../replacer';

describe('Autoru broken links fixer batch replacer', () => {
    const POST_URL_PART = 'someurlpart';
    const POST_LINK = `${config.links.autoruMag}/article/${POST_URL_PART}`;

    const postReplacer = new PostReplacer({ postUrlPart: POST_URL_PART });

    describe('Метод includes', () => {
        it('Определяет наличие абсолютной ссылки в строке - по полному совпадению URL', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const STRING = `Lorem ipsum ${BAD_LINK} dolor sit amet`;

            const includes = postReplacer.includes(STRING, BAD_LINK);

            expect(includes).toBeTruthy();
        });

        describe('Определяет равенство относительной ссылки и переданной строки', () => {
            it('Для ссылки без пробелов', () => {
                const BAD_RELATIVE_LINK = 'some/broken/link';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;

                const includesLink = postReplacer.includes(BAD_RELATIVE_LINK, BAD_LINK);

                expect(includesLink).toBeTruthy();
            });

            it('Для ссылки с пробелами', () => {
                const BAD_RELATIVE_LINK = 'текст в блоке поста';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;

                const includesLink = postReplacer.includes(BAD_RELATIVE_LINK, BAD_LINK);

                expect(includesLink).toBeTruthy();
            });
        });

        describe('Определяет отсутствие ссылок', () => {
            const BAD_RELATIVE_LINK_1 = 'some/broken/link';
            const BAD_RELATIVE_LINK_2 = 'текст в блоке поста';
            const BAD_LINK_1 = `${POST_LINK}/${BAD_RELATIVE_LINK_1}`;
            const BAD_LINK_2 = `${POST_LINK}/${BAD_RELATIVE_LINK_2}`;
            const BAD_LINK_3 = 'https://some.url.com/some/broken/link';

            describe('В строке с относительными ссылками (содержащей подстроки переданных строк)', () => {
                const STRING_WITH_LINKS = `Lorem ipsum ${BAD_RELATIVE_LINK_1} dolor sit amet ${POST_LINK} ipsum ${BAD_RELATIVE_LINK_2}`;

                it('Для url без пробелов', () => {
                    const includes = postReplacer.includes(STRING_WITH_LINKS, BAD_LINK_1);

                    expect(includes).toBeFalsy();
                });
                it('Для url с пробелами', () => {
                    const includes = postReplacer.includes(STRING_WITH_LINKS, BAD_LINK_2);

                    expect(includes).toBeFalsy();
                });
                it('Для случайного url', () => {
                    const includes = postReplacer.includes(STRING_WITH_LINKS, BAD_LINK_3);

                    expect(includes).toBeFalsy();
                });
            });

            describe('В строке без ссылок (не содержащей никаких подстрок переданных строк)', () => {
                const STRING_WITHOUT_LINKS = 'Lorem ipsum dolor sit amet ipsum';

                it('Для url без пробелов', () => {
                    const includes = postReplacer.includes(STRING_WITHOUT_LINKS, BAD_LINK_1);

                    expect(includes).toBeFalsy();
                });
                it('Для url с пробелами', () => {
                    const includes = postReplacer.includes(STRING_WITHOUT_LINKS, BAD_LINK_2);

                    expect(includes).toBeFalsy();
                });
                it('Для случайного url', () => {
                    const includes = postReplacer.includes(STRING_WITHOUT_LINKS, BAD_LINK_3);

                    expect(includes).toBeFalsy();
                });
                it('Для относительной ссылки без пробелов', () => {
                    const includes = postReplacer.includes(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_1);

                    expect(includes).toBeFalsy();
                });
                it('Для относительной ссылки с пробелами', () => {
                    const includes = postReplacer.includes(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_2);

                    expect(includes).toBeFalsy();
                });
            });
        });
    });

    describe('Метод includesHref', () => {
        it('Определяет наличие абсолютной ссылки внутри href в строке - по полному совпадению URL', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const STRING = `Текст со <a href="${BAD_LINK}" />ссылкой</a> 1`;

            const includes = postReplacer.includesHref(STRING, BAD_LINK);

            expect(includes).toBeTruthy();
        });

        describe('Определяет наличие относительной ссылки внутри href в строке - по частичному совпадению URL, без учёта адреса страницы', () => {
            it('Для ссылки без пробелов', () => {
                const BAD_RELATIVE_LINK = 'some/broken/link';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;
                const STRING = `<a href="${BAD_RELATIVE_LINK}">Текст</a> со ссылкой 1`;

                const includesLink = postReplacer.includesHref(STRING, BAD_LINK);

                expect(includesLink).toBeTruthy();
            });

            it('Для ссылки с пробелами', () => {
                const BAD_RELATIVE_LINK = 'текст в блоке поста';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;
                const STRING = `Текст со <a href="${BAD_RELATIVE_LINK}">ссылкой</a> 2`;

                const includesLink = postReplacer.includesHref(STRING, BAD_LINK);

                expect(includesLink).toBeTruthy();
            });
        });

        describe('Определяет отсутствие ссылок', () => {
            const BAD_RELATIVE_LINK_1 = 'some/broken/link';
            const BAD_RELATIVE_LINK_2 = 'текст в блоке поста';
            const BAD_LINK_1 = `${POST_LINK}/${BAD_RELATIVE_LINK_1}`;
            const BAD_LINK_2 = `${POST_LINK}/${BAD_RELATIVE_LINK_2}`;
            const BAD_LINK_3 = 'https://some.url.com/some/broken/link';

            describe('В строке с относительными ссылками (содержащей подстроки переданных строк)', () => {
                const STRING_WITH_LINKS = `Lorem ${BAD_RELATIVE_LINK_1} ipsum ${BAD_LINK_1} dolor ${BAD_RELATIVE_LINK_2} sit amet ${POST_LINK} ipsum ${BAD_LINK_2}`;

                it('Для url без пробелов', () => {
                    const includes = postReplacer.includesHref(STRING_WITH_LINKS, BAD_LINK_1);

                    expect(includes).toBeFalsy();
                });
                it('Для url с пробелами', () => {
                    const includes = postReplacer.includesHref(STRING_WITH_LINKS, BAD_LINK_2);

                    expect(includes).toBeFalsy();
                });
                it('Для случайного url', () => {
                    const includes = postReplacer.includesHref(STRING_WITH_LINKS, BAD_LINK_3);

                    expect(includes).toBeFalsy();
                });
                it('Для относительной ссылки без пробелов', () => {
                    const includes = postReplacer.includesHref(STRING_WITH_LINKS, BAD_RELATIVE_LINK_1);

                    expect(includes).toBeFalsy();
                });
                it('Для относительной ссылки с пробелами', () => {
                    const includes = postReplacer.includesHref(STRING_WITH_LINKS, BAD_RELATIVE_LINK_2);

                    expect(includes).toBeFalsy();
                });
            });

            describe('В строке без ссылок (не содержащей никаких подстрок переданных строк)', () => {
                const STRING_WITHOUT_LINKS = 'Lorem ipsum dolor sit amet ipsum';

                it('Для url без пробелов', () => {
                    const includes = postReplacer.includesHref(STRING_WITHOUT_LINKS, BAD_LINK_1);

                    expect(includes).toBeFalsy();
                });
                it('Для url с пробелами', () => {
                    const includes = postReplacer.includesHref(STRING_WITHOUT_LINKS, BAD_LINK_2);

                    expect(includes).toBeFalsy();
                });
                it('Для случайного url', () => {
                    const includes = postReplacer.includesHref(STRING_WITHOUT_LINKS, BAD_LINK_3);

                    expect(includes).toBeFalsy();
                });
                it('Для относительной ссылки без пробелов', () => {
                    const includes = postReplacer.includesHref(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_1);

                    expect(includes).toBeFalsy();
                });
                it('Для относительной ссылки с пробелами', () => {
                    const includes = postReplacer.includesHref(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_2);

                    expect(includes).toBeFalsy();
                });
            });
        });

        it('Определяет наличие ссылки внутри href с одинарными кавычками', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const STRING = `Текст с <a href='${BAD_LINK}' />одинарными</a> кавычками`;

            const includes = postReplacer.includesHref(STRING, BAD_LINK);

            expect(includes).toBeTruthy();
        });
    });
    describe('Метод replace', () => {
        it('Заменяет абсолютную ссылку в строке - по полному совпадению URL', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Lorem ipsum ${BAD_LINK} dolor sit amet`;
            const NEW_STRING = `Lorem ipsum ${NEW_LINK} dolor sit amet`;

            const replaced = postReplacer.replace(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        describe('Заменяет относительную ссылку в строке - в случае равенства URL и переданной строки', () => {
            it('Для ссылки без пробелов', () => {
                const BAD_RELATIVE_LINK = 'some/broken/link';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;
                const NEW_LINK = 'https://some.url.com/some/good/link-1';

                const replaced = postReplacer.replace(BAD_RELATIVE_LINK, BAD_LINK, NEW_LINK);

                expect(replaced).toEqual(NEW_LINK);
            });

            it('Для ссылки с пробелами', () => {
                const BAD_RELATIVE_LINK = 'текст в блоке поста';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;
                const NEW_LINK = 'https://some.url.com/some/good/link-2';

                const replaced = postReplacer.replace(BAD_RELATIVE_LINK, BAD_LINK, NEW_LINK);

                expect(replaced).toEqual(NEW_LINK);
            });
        });

        it('Заменяет более 1 вхождения одной и той же ссылки', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Lorem ipsum ${BAD_LINK} dolor ${BAD_LINK} sit amet`;
            const NEW_STRING = `Lorem ipsum ${NEW_LINK} dolor ${NEW_LINK} sit amet`;

            const replaced = postReplacer.replace(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        it('Заменяет ссылку со спец. символами', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/"link)';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Lorem ipsum ${BAD_LINK} dolor sit amet`;
            const NEW_STRING = `Lorem ipsum ${NEW_LINK} dolor sit amet`;

            const replaced = postReplacer.replace(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        it('Заменяет ссылку с использованием decodeUIRComponent', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/%20';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Lorem ipsum ${decode(BAD_LINK)} dolor sit amet`;
            const NEW_STRING = `Lorem ipsum ${NEW_LINK} dolor sit amet`;

            const replaced = postReplacer.replace(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        it('Заменяет ссылку без использования decodeUIRComponent', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/%20';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Lorem ipsum ${BAD_LINK} dolor sit amet`;
            const NEW_STRING = `Lorem ipsum ${NEW_LINK} dolor sit amet`;

            const replaced = postReplacer.replace(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        describe('Не меняет строку без ссылок, которые нужно заменить', () => {
            const BAD_RELATIVE_LINK_1 = 'some/broken/link';
            const BAD_RELATIVE_LINK_2 = 'текст в блоке поста';
            const BAD_LINK_1 = `${POST_LINK}/${BAD_RELATIVE_LINK_1}`;
            const BAD_LINK_2 = `${POST_LINK}/${BAD_RELATIVE_LINK_2}`;
            const BAD_LINK_3 = 'https://some.url.com/some/broken/link';
            const NEW_LINK = 'https://some.url.com/some/awesome/link';

            describe('В строке с относительными ссылками (содержащей подстроки переданных строк)', () => {
                const STRING_WITH_LINKS = `Lorem ipsum ${BAD_RELATIVE_LINK_1} dolor sit amet ${POST_LINK} ipsum ${BAD_RELATIVE_LINK_2}`;

                it('Для url без пробелов', () => {
                    const replaced = postReplacer.replace(STRING_WITH_LINKS, BAD_LINK_1, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
                it('Для url с пробелами', () => {
                    const replaced = postReplacer.replace(STRING_WITH_LINKS, BAD_LINK_2, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
                it('Для случайного url', () => {
                    const replaced = postReplacer.replace(STRING_WITH_LINKS, BAD_LINK_3, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
            });

            describe('В строке без ссылок (не содержащей никаких подстрок переданных строк)', () => {
                const STRING_WITHOUT_LINKS = 'Lorem ipsum dolor sit amet ipsum';

                it('Для url без пробелов', () => {
                    const replaced = postReplacer.replace(STRING_WITHOUT_LINKS, BAD_LINK_1, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для url с пробелами', () => {
                    const replaced = postReplacer.replace(STRING_WITHOUT_LINKS, BAD_LINK_2, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для случайного url', () => {
                    const replaced = postReplacer.replace(STRING_WITHOUT_LINKS, BAD_LINK_3, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для относительной ссылки без пробелов', () => {
                    const replaced = postReplacer.replace(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_1, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для относительной ссылки с пробелами', () => {
                    const replaced = postReplacer.replace(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_2, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
            });
        });
    });
    describe('Метод replaceHref', () => {
        it('Заменяет абсолютную ссылку внутри href в строке - по полному совпадению URL', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Текст со <a href="${BAD_LINK}" />ссылкой</a> 1`;
            const NEW_STRING = `Текст со <a href="${NEW_LINK}" />ссылкой</a> 1`;

            const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        describe('Заменяет относительную ссылку внутри href в строке - по частичному совпадению URL, без учёта адреса страницы', () => {
            it('Для ссылки без пробелов', () => {
                const BAD_RELATIVE_LINK = 'some/broken/link';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;
                const NEW_LINK = 'https://some.url.com/some/good/link-1';
                const BAD_STRING = `<a href="${BAD_RELATIVE_LINK}">Текст</a> со ссылкой 1`;
                const NEW_STRING = `<a href="${NEW_LINK}">Текст</a> со ссылкой 1`;

                const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

                expect(replaced).toEqual(NEW_STRING);
            });

            it('Для ссылки с пробелами', () => {
                const BAD_RELATIVE_LINK = 'текст в блоке поста';
                const BAD_LINK = `${POST_LINK}/${BAD_RELATIVE_LINK}`;
                const NEW_LINK = 'https://some.url.com/some/good/link-2';
                const BAD_STRING = `Текст со <a href="${BAD_RELATIVE_LINK}">ссылкой</a> 2`;
                const NEW_STRING = `Текст со <a href="${NEW_LINK}">ссылкой</a> 2`;

                const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

                expect(replaced).toEqual(NEW_STRING);
            });
        });

        it('Заменяет ссылку внутри href с одинарными кавычками', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Текст со <a href='${BAD_LINK}' />ссылкой</a> с одинарными кавычками`;
            const NEW_STRING = `Текст со <a href="${NEW_LINK}" />ссылкой</a> с одинарными кавычками`;

            const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        it('Заменяет более 1 вхождения одной и той же ссылки', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/link';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Текст со <a href="${BAD_LINK}" />ссылкой</a> и <a href="${BAD_LINK}" />ещё одной ссылкой</a>`;
            const NEW_STRING = `Текст со <a href="${NEW_LINK}" />ссылкой</a> и <a href="${NEW_LINK}" />ещё одной ссылкой</a>`;

            const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        it('Заменяет ссылку со спец. символами', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/"link)';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Текст со <a href="${BAD_LINK}" />ссылкой</a>`;
            const NEW_STRING = `Текст со <a href="${NEW_LINK}" />ссылкой</a>`;

            const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        it('Заменяет ссылку с использованием decodeUIRComponent', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/%20';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Текст со <a href="${decode(BAD_LINK)}" />ссылкой</a>`;
            const NEW_STRING = `Текст со <a href="${NEW_LINK}" />ссылкой</a>`;

            const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        it('Заменяет ссылку без использования decodeUIRComponent', () => {
            const BAD_LINK = 'https://some.url.com/some/broken/%20';
            const NEW_LINK = 'https://some.url.com/some/good/link';
            const BAD_STRING = `Текст со <a href="${BAD_LINK}" />ссылкой</a>`;
            const NEW_STRING = `Текст со <a href="${NEW_LINK}" />ссылкой</a>`;

            const replaced = postReplacer.replaceHref(BAD_STRING, BAD_LINK, NEW_LINK);

            expect(replaced).toEqual(NEW_STRING);
        });

        describe('Не меняет строку без ссылок, которые нужно заменить', () => {
            const BAD_RELATIVE_LINK_1 = 'some/broken/link';
            const BAD_RELATIVE_LINK_2 = 'текст в блоке поста';
            const BAD_LINK_1 = `${POST_LINK}/${BAD_RELATIVE_LINK_1}`;
            const BAD_LINK_2 = `${POST_LINK}/${BAD_RELATIVE_LINK_2}`;
            const BAD_LINK_3 = 'https://some.url.com/some/broken/link';
            const NEW_LINK = 'https://some.url.com/some/awesome/link';

            describe('В строке с относительными ссылками (содержащей подстроки переданных строк)', () => {
                const STRING_WITH_LINKS = `Lorem ${BAD_RELATIVE_LINK_1} ipsum ${BAD_LINK_1} dolor ${BAD_RELATIVE_LINK_2} sit amet ${POST_LINK} ipsum ${BAD_LINK_2}`;

                it('Для url без пробелов', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITH_LINKS, BAD_LINK_1, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
                it('Для url с пробелами', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITH_LINKS, BAD_LINK_2, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
                it('Для случайного url', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITH_LINKS, BAD_LINK_3, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
                it('Для относительной ссылки без пробелов', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITH_LINKS, BAD_RELATIVE_LINK_1, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
                it('Для относительной ссылки с пробелами', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITH_LINKS, BAD_RELATIVE_LINK_2, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITH_LINKS);
                });
            });

            describe('В строке без ссылок (не содержащей никаких подстрок переданных строк)', () => {
                const STRING_WITHOUT_LINKS = 'Lorem ipsum dolor sit amet ipsum';

                it('Для url без пробелов', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITHOUT_LINKS, BAD_LINK_1, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для url с пробелами', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITHOUT_LINKS, BAD_LINK_2, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для случайного url', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITHOUT_LINKS, BAD_LINK_3, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для относительной ссылки без пробелов', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_1, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
                it('Для относительной ссылки с пробелами', () => {
                    const replaced = postReplacer.replaceHref(STRING_WITHOUT_LINKS, BAD_RELATIVE_LINK_2, NEW_LINK);

                    expect(replaced).toEqual(STRING_WITHOUT_LINKS);
                });
            });
        });
    });
});
