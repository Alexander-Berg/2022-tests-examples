import glob from 'glob';
import {SeoFile} from './types';

// Позволяет найти элемент с указанным itemprop ограниченный предком с указанным itemtype.
// Например, можно найти элемент с itemprop="name", который находится в itemscope http://schema.org/LocalBusiness",
// но не в itemscope http://schema.org/Review.
function getSchemaPropSelector(itemProp: string, ancestorItemType: string): string {
    return `.//*[@itemprop="${itemProp}"]${
        ancestorItemType ? `[ancestor::*[@itemscope][1][@itemtype="http://schema.org/${ancestorItemType}"]]` : ''
    }`;
}

function getSchemaScopeSelector(itemType: string): string {
    return `.//*[@itemscope][@itemtype="http://schema.org/${itemType}"]`;
}

function getSeoTestFiles(): SeoFile[] {
    return (
        glob
            .sync('./landings-spec/**/*.ts', {cwd: __dirname})
            // eslint-disable-next-line @typescript-eslint/no-require-imports, @typescript-eslint/no-var-requires
            .reduce((spec, filename) => spec.concat(require(filename).default), [])
    );
}

export {getSchemaPropSelector, getSchemaScopeSelector, getSeoTestFiles};
