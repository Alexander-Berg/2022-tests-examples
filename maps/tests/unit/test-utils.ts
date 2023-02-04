// Remove undefined props.
function getCopy<T extends unknown>(data: T): T {
    return JSON.parse(JSON.stringify(data));
}

export {getCopy};
