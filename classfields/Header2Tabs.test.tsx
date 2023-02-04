import React from 'react';
import { render } from '@testing-library/react';

import '@testing-library/jest-dom';
import Header2Tabs from 'auto-core/react/components/mobile/Header2Tabs/Header2Tabs';

it('не должен отрендерить молнию если есть кука', () => {
    const tabs = [
        {
            id: 'electro',
            name: 'Электромобили',
            url: 'url',
            pimple: true,
        },
    ];
    const cookies = { 'navigation_dot_seen-electro-dot': 'true' };

    render(
        <Header2Tabs tabs={ tabs } cookies={ cookies }/>,
    );

    expect(document.querySelector('.Header2Tabs__tab_electro')).not.toBeInTheDocument();
});
