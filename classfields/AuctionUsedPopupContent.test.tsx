import React from 'react';
import { shallow } from 'enzyme';

import AuctionUsedPopupContentDumb from './AuctionUsedPopupContentDumb';
import props from './mocks/props.mock.js';

it('устанавливает значение ставки при клике на сегмент', () => {
    const tree = shallow(
        <AuctionUsedPopupContentDumb { ...props as any }/>,
    );
    tree.find('AuctionUsedGraph').simulate('segmentClick', props.auctionInfo.segments[0]);
    expect(tree.state()).toEqual({ newBid: 1500 });
});

it('не устанавливает значение ставки, если аукцион задизейблен', () => {
    const tree = shallow(
        <AuctionUsedPopupContentDumb { ...props as any } disabled={ true }/>,
    );
    tree.find('AuctionUsedGraph').simulate('segmentClick', props.auctionInfo.segments[1], props.auctionInfo.segments[0]);
    expect(tree.state()).toEqual({ newBid: 1 });
});

it('не устанавливает значение ставки, если объявление находится в РК', () => {
    const tree = shallow(
        <AuctionUsedPopupContentDumb { ...props as any } disabled={ true } isCampaign={ true }/>,
    );
    tree.find('AuctionUsedGraph').simulate('segmentClick', props.auctionInfo.segments[1], props.auctionInfo.segments[0]);
    expect(tree.state()).toEqual({ newBid: 1 });
});
