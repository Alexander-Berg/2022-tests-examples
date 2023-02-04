import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import VinReportValidatorFileUploader from './VinReportValidatorFileUploader';

it('должен нарисовать VinReportValidatorFileUploader', () => {
    const vinReportValidatorFileUploader = shallow(
        <VinReportValidatorFileUploader
            isFileUploaded={ false }
            isFileUploading={ false }
            fileNames="1 файл"
            fileUploadingChange={ jest.fn() }
            fileDescription=""
            fileUploadingError="Размер файла до 40МБ, форматы: XML, JSON, CSV"
            removeFile={ jest.fn() }
        />,
    );

    expect(shallowToJson(vinReportValidatorFileUploader)).toMatchSnapshot();
});

it('должен нарисовать VinReportValidatorFileUploader, во время загрузки файла', () => {
    const vinReportValidatorFileUploader = shallow(
        <VinReportValidatorFileUploader
            isFileUploaded={ false }
            isFileUploading={ true }
            fileNames="1 файл"
            fileUploadingChange={ jest.fn() }
            fileDescription=""
            fileUploadingError=""
            removeFile={ jest.fn() }
        />,
    );

    expect(shallowToJson(vinReportValidatorFileUploader)).toMatchSnapshot();
});

it('должен нарисовать VinReportValidatorFileUploader с ошибкой, если что-то пошло не так', () => {
    const vinReportValidatorFileUploader = shallow(
        <VinReportValidatorFileUploader
            isFileUploaded={ false }
            isFileUploading={ false }
            fileNames="1 файл"
            fileUploadingChange={ jest.fn() }
            fileDescription=""
            fileUploadingError="Что-то пошло не так"
            removeFile={ jest.fn() }
        />,
    );

    expect(shallowToJson(vinReportValidatorFileUploader)).toMatchSnapshot();
});
