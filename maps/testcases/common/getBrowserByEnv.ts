import { BrowserEnum } from './constants';

export const getBrowserByEnvTitle = (evnTitle: string): BrowserEnum => {
  const title = evnTitle.toLowerCase();

  if (title.includes(BrowserEnum.chrome)) {
    return BrowserEnum.chrome;
  }

  return BrowserEnum.edge;
};
