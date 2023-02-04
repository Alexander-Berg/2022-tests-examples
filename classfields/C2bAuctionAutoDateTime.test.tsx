import React from 'react';
import { shallow } from 'enzyme';
import { act } from 'react-dom/test-utils';

import DateMock from 'autoru-frontend/mocks/components/DateMock';

import TextArea from 'auto-core/react/components/islands/TextArea/TextArea';

import { C2bAuctionAutoDateTime } from './C2bAuctionAutoDateTime';

describe('C2bAuctionAutoDateTime.', () => {
    it('выбирает новую дату', () => {
        const onChangeDateMock = jest.fn();
        const onChangeTimeMock = jest.fn();

        const wrapper = shallow(
            <DateMock date="2021-11-10">
                <C2bAuctionAutoDateTime
                    onChangeDate={ onChangeDateMock }
                    onChangeTime={ onChangeTimeMock }
                />
            </DateMock>,
        ).dive();

        const datesChips = wrapper.find('.C2bAuctionAutoDateTime__item');
        expect(datesChips).toHaveLength(5);

        act(() => {
            datesChips.first().simulate('click');
        });
        expect(onChangeDateMock).toHaveBeenCalledWith([ '10-11-2021' ]);
    });

    it('удаляет выбранную дату', () => {
        const onChangeDateMock = jest.fn();
        const onChangeTimeMock = jest.fn();

        const wrapper = shallow(
            <DateMock date="2021-11-10">
                <C2bAuctionAutoDateTime
                    onChangeDate={ onChangeDateMock }
                    onChangeTime={ onChangeTimeMock }
                    date={ [ '10-11-2021', '11-11-2021' ] }
                />
            </DateMock>,
        ).dive();

        const datesChips = wrapper.find('.C2bAuctionAutoDateTime__item');
        expect(datesChips).toHaveLength(5);

        act(() => {
            datesChips.first().simulate('click');
        });
        expect(onChangeDateMock).toHaveBeenCalledWith([ '11-11-2021' ]);
    });

    it('меняет текст в поле времени', () => {
        const onChangeDateMock = jest.fn();
        const onChangeTimeMock = jest.fn();

        const wrapper = shallow(
            <C2bAuctionAutoDateTime
                onChangeDate={ onChangeDateMock }
                onChangeTime={ onChangeTimeMock }
            />,
        );

        const input = wrapper.find(TextArea).first();

        act(() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            //@ts-ignore
            input.prop('onChange')?.('12:00');
        });
        expect(onChangeTimeMock).toHaveBeenCalledWith('12:00');
    });
});
