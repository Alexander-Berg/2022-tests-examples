interface Options {
    seekOccurrence?: boolean;
}

function getSelectorByText(elementText: string, parentSelector = 'body', options: Options = {}): string {
    if (!elementText) {
        throw new Error('Не задан текст элемента для поиска.');
    }

    // getParentXPath умеет создавать валидный xpath для селекторов состоящих их любых сочетаний
    // классов и элементов ('.class', '.class div .child', '.class._mod_val .child'), но выдаст неверный xpath для
    // любых селекторов содержащих псевдоклассы или xpath.

    const parent = getParentXPath(parentSelector);
    if (options.seekOccurrence) {
        return `${parent}//*[contains((text() | @aria-label), '${elementText}')]`;
    }
    return `${parent}//*[(text() | @aria-label)='${elementText}']`;
}

function getParentXPath(selector: string): string {
    const xpathToParent = selector
        .split(' ')
        .map((substring) => (substring.match(/^[a-z]/) ? substring : getXPathForClassString(substring)))
        .join('//');

    return `//${xpathToParent}`;
}

const LAST_CHILD_REGEX = /:last-child$/;
const FIRST_CHILD_REGEX = /:first-child$/;
const NTH_CHILD_REGEX = /:nth-child\(\d+\)$/;

function getXPathForClassString(classString: string): string {
    if (!classString.startsWith('.')) {
        throw new Error('Неверный формат родительского селектора.');
    }
    const containsString = classString
        .replace(LAST_CHILD_REGEX, '')
        .replace(FIRST_CHILD_REGEX, '')
        .replace(NTH_CHILD_REGEX, '')
        .split('.')
        .filter((className) => Boolean(className))
        .map(getClassEqualExpression)
        .join(' and ');

    let postfix = '';
    if (FIRST_CHILD_REGEX.test(classString)) {
        postfix = '[1]';
    }
    if (LAST_CHILD_REGEX.test(classString)) {
        postfix = '[last()]';
    }
    if (NTH_CHILD_REGEX.test(classString)) {
        const [, index] = NTH_CHILD_REGEX.exec(classString)!;
        postfix = `[${index}]`;
    }

    return `*[${containsString}]${postfix}`;
}

function getClassEqualExpression(className: string): string {
    return `contains(concat(' ',normalize-space(@class),' '), ' ${className} ')`;
}

export default getSelectorByText;
