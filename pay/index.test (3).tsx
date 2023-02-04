import React from 'react';

import { fireEvent, getAllByTestId, render } from '@testing-library/react';

import { Select } from '.';

const TEST_ID = 'select';
const OPTION_TEST_ID = `${TEST_ID}-option`;

const OPTIONS = [
    { label: 'First option', value: 1 },
    { label: 'Second option', value: 2 },
    { label: 'Third option', value: 3 },
];

describe('Select', function () {
    it('should render all options', function () {
        const { getAllByTestId } = render(<Select options={OPTIONS} testId={TEST_ID} />);
        const options = getAllByTestId(OPTION_TEST_ID) as HTMLOptionElement[];

        expect(options.length).toBe(OPTIONS.length);
    });

    it('should select item by value', function () {
        const { getAllByTestId } = render(<Select options={OPTIONS} value={1} testId={TEST_ID} />);

        const options = getAllByTestId(OPTION_TEST_ID) as HTMLOptionElement[];

        expect(options[0].selected).toBeTruthy();
        expect(options[1].selected).toBeFalsy();
        expect(options[2].selected).toBeFalsy();
    });

    it('should call onChange', function () {
        const onChange = jest.fn();

        const { getByTestId } = render(
            <Select options={OPTIONS} onChange={onChange} testId={TEST_ID} />,
        );

        // NB! — триггерим '1', а ловим 1 — как в OPTIONS[0].value
        fireEvent.change(getByTestId(TEST_ID), { target: { value: '1' } });

        expect(onChange).toBeCalledWith(1);
    });
});
