import React from 'react';

import '@testing-library/jest-dom';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import AuctionTableItem from 'www-cabinet/react/components/Auction/AuctionTableItem/AuctionTableItem';

import AuctionTableItemProps from './mocks/AuctionTableItemProps';

it('должен установить класс disabled для кнопки с крестиком, если нет current_item', async() => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    const { findAllByRole } = await renderComponent(<AuctionTableItem { ...AuctionTableItemProps }/>);
    const buttons = await findAllByRole('button');
    expect(buttons[4]).toHaveAttribute('disabled');
});
