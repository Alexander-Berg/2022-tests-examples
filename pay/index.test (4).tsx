import React from 'react';

import { fireEvent, render } from '@testing-library/react';

import { Toggle } from '.';

const TEST_ID = 'toggle';
const CHECKBOX_TEST_ID = `${TEST_ID}-checkbox`;
const SWITCH_TEST_ID = `${TEST_ID}-switch`;

describe('Toggle', function () {
    it('should be checked', function () {
        const { getByTestId } = render(<Toggle checked={true} testId={TEST_ID} />);
        const checkbox = getByTestId(CHECKBOX_TEST_ID) as HTMLInputElement;

        expect(checkbox.checked).toBe(true);
    });

    it('should be unchecked', function () {
        const { getByTestId } = render(<Toggle checked={false} testId={TEST_ID} />);
        const checkbox = getByTestId(CHECKBOX_TEST_ID) as HTMLInputElement;

        expect(checkbox.checked).toBe(false);
    });

    it('should call onChange', function () {
        const onChange = jest.fn();

        const { getByTestId } = render(
            <Toggle checked={false} onChange={onChange} testId={TEST_ID} />,
        );

        fireEvent.click(getByTestId(TEST_ID));
        expect(onChange).toBeCalledTimes(1);
    });
});
