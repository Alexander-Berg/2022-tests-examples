export const getBaseUrl = (options?: UrlOptions): string => {
  const initialBasePath = '/courier/';
  const baseUrlWithPath = Cypress.env('BASE_URL').replace(
    initialBasePath,
    options?.basePath ?? initialBasePath,
  );

  const BASE_URL = options?.tld ? baseUrlWithPath.replace('ru', options?.tld) : baseUrlWithPath;

  const retpathURL = new URL(`${BASE_URL}${options?.link ?? ''}`);
  // for run in local stand
  retpathURL.searchParams.append('api-version', 'testing');

  return retpathURL.toString();
};
