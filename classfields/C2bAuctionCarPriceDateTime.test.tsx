import React from 'react';
import { shallow } from 'enzyme';
import { act } from 'react-dom/test-utils';

import Select from 'auto-core/react/components/islands/Select/Select';
import Item from 'auto-core/react/components/islands/Item/Item';

import { C2bAuctionCarPriceDateTime } from './C2bAuctionCarPriceDateTime';

describe('C2bAuctionCarPriceDateTime.', () => {
    it('правильно выбирается дата в селекте', () => {
        const onChangeDateMock = jest.fn();
        const onChangeTimeMock = jest.fn();

        const wrapper = shallow(
            <C2bAuctionCarPriceDateTime
                onChangeDate={ onChangeDateMock }
                onChangeTime={ onChangeTimeMock }
                datesList={ [ '20-12-2021', '22-12-2021' ] }
                timesList={ [] }
            />,
        );

        const selectDate = wrapper.find(Select).first();
        const datesItem = selectDate.find(Item);
        expect(datesItem).toHaveLength(2);

        act(() => {
            selectDate.prop('onChange')?.([], {} as any);
            selectDate.prop('onChange')?.([ '20-12-2021' ], {} as any);
        });

        //при вызове с пустым массивом не доллжен вызывавться колбэк
        expect(onChangeDateMock).toHaveBeenCalledTimes(1);
        expect(onChangeDateMock).toHaveBeenCalledWith([ '20-12-2021' ]);
    });

    it('правильно выбирается время в селекте', () => {
        const onChangeDateMock = jest.fn();
        const onChangeTimeMock = jest.fn();

        const wrapper = shallow(
            <C2bAuctionCarPriceDateTime
                onChangeDate={ onChangeDateMock }
                onChangeTime={ onChangeTimeMock }
                datesList={ [] }
                timesList={ [ '12:00', '13:00', '15:00' ] }
            />,
        );

        const selectTime = wrapper.find(Select).at(1);
        const timesItem = selectTime.find(Item);
        expect(timesItem).toHaveLength(3);

        act(() => {
            selectTime.prop('onChange')?.([], {} as any);
            selectTime.prop('onChange')?.([ '15:00' ], {} as any);
        });

        //при вызове с пустым массивом не доллжен вызывавться колбэк
        expect(onChangeTimeMock).toHaveBeenCalledTimes(1);
        expect(onChangeTimeMock).toHaveBeenCalledWith('15:00');
    });
});
