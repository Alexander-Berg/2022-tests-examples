import { IImage } from 'realty-core/types/common';

export interface IGenerateImageParams {
    width?: number;
    height?: number;
    size?: number;
}

export function generateImageUrl(params?: IGenerateImageParams): string;
export function generateImageAliases(params?: IGenerateImageParams): IImage;
