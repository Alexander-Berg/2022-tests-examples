import React from 'react';

import { fireEvent, getAllByTestId, render } from '@testing-library/react';

import { RadioButton } from '.';

const TEST_ID = 'radio';
const LABEL_TEST_ID = `${TEST_ID}-label`;
const INPUT_TEST_ID = `${TEST_ID}-input`;

const OPTIONS = [
    { label: 'First option', value: 1 },
    { label: 'Second option', value: 2 },
    { label: 'Third option', value: 3 },
];

describe('RadioButton', function () {
    it('should render all options', function () {
        const { getAllByTestId } = render(<RadioButton options={OPTIONS} testId={TEST_ID} />);
        const options = getAllByTestId(LABEL_TEST_ID) as HTMLInputElement[];

        expect(options.length).toBe(OPTIONS.length);
    });

    it('should select item by value', function () {
        const { getAllByTestId } = render(
            <RadioButton options={OPTIONS} value={1} testId={TEST_ID} />,
        );
        const options = getAllByTestId(INPUT_TEST_ID) as HTMLInputElement[];

        expect(options[0].checked).toBeTruthy();
        expect(options[1].checked).toBeFalsy();
        expect(options[2].checked).toBeFalsy();
    });

    it('should call onChange', function () {
        const onChange = jest.fn();

        const { getAllByTestId } = render(
            <RadioButton options={OPTIONS} onChange={onChange} testId={TEST_ID} />,
        );

        fireEvent.click(getAllByTestId(LABEL_TEST_ID)[0]);
        expect(onChange).toBeCalledWith('1');
    });
});
