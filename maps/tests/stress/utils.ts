export const ENDPOINTS = [
    'GET /v2/maps',
    'GET /v2/maps/{sid}',
    'GET /v2/public_maps/{sid}',
    'GET /v2/public_maps/{sid}/export',
    'GET /v2/maps/{sid}/objects/{id}',
    'GET /v2/public_maps/{sid}/objects/{id}',
    'GET /v2/maps/{sid}/objects_metadata',
    'GET /v2/public_maps/{sid}/objects_metadata'
] as const;

export type EndpointName = typeof ENDPOINTS[number];

export const S3_BUCKET_NAME = 'constructor-int-ammo';

export function getAvailableEndpoints(): string {
    let res = 'Available endpoints:\n';
    ENDPOINTS.forEach((name) => {
        res += `  "${name}"\n`;
    });
    return res;
}

export function endpointToAmmoFile(endpoint: string): string {
    return endpoint.trim().replace(/\s+|\//g, '-') + '.txt';
}

export function getEndpointsFromArgs(args: any): ReadonlySet<EndpointName> {
    const endpoints = args.endpoint ? args.endpoint : ENDPOINTS;
    const res = new Set<EndpointName>();
    for (const endpoint of endpoints) {
        if (!ENDPOINTS.includes(endpoint)) {
            // tslint:disable-next-line:no-console
            console.error(`Endpoint "${endpoint}" not found`);
            process.exit(1);
        }
        res.add(endpoint);
    }
    return res;
}
