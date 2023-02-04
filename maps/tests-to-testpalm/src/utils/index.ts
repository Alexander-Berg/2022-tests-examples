function encodeHtmlEntities(input: string): string {
    return input.replace(/[\u00A0-\u9999<>&~*_]/g, (char) => `&#${char.charCodeAt(0)};`);
}

function minutesToMilliseconds(minutes: number): number {
    return minutes * 60 * 1000;
}

export {encodeHtmlEntities, minutesToMilliseconds};
