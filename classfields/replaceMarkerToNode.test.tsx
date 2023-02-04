import React from 'react';

import Link from 'auto-core/react/components/islands/Link/Link';

import replaceMarkerToNode from './replaceMarkerToNode';

const string = 'Здесь может быть какой-то текст из бункера и ∮MARKER∮';
const MARKER = 'MARKER';
const link = <Link onClick={ () => {} } key="link">живая ссылка</Link>;

it('корректно заменит подстроку-маркер на jsx-элемент', () => {
    const tree = (
        <div>
            { replaceMarkerToNode(string, [ { marker: MARKER, node: link } ]) }
        </div>
    );

    expect(tree).toMatchSnapshot();
});

it('корректно заменит несколько маркеров на jsx-элемент', () => {
    const tree = (
        <div>
            { replaceMarkerToNode(`${ string } и ещё ∮${ MARKER }∮`, [ { marker: MARKER, node: link } ]) }
        </div>
    );

    expect(tree).toMatchSnapshot();
});

it('корректно заменит несколько разных маркеров на разные jsx-элементы', () => {
    const OTHER = 'OTHER';
    const otherLink = <Link onClick={ () => {} } key="other_link">другая ссылка</Link>;

    const tree = (
        <div>
            {
                replaceMarkerToNode(`${ string } и ещё ∮${ OTHER }∮`, [
                    { marker: MARKER, node: link },
                    { marker: OTHER, node: otherLink },
                ])
            }
        </div>
    );

    expect(tree).toMatchSnapshot();
});

it('вернет строку без изменений, если в ней нет маркера', () => {
    const stringWithoutMarker = 'Текст без маркера';
    expect(replaceMarkerToNode(stringWithoutMarker, [ { marker: MARKER, node: link } ])).toEqual(stringWithoutMarker);
});
