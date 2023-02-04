/**
 * Returns a new promise that fulfilled if the given promise will be rejected.
 *
 * @param promise Promise that must be rejected.
 * @param onError Function for additional checks of rejection reason.
 */
export async function expectRejection(
    promise: Promise<any>,
    onError?: (err: any) => void
): Promise<void> {
    try {
        await promise;
    } catch (err) {
        if (onError) {
            onError(err);
        }
        return;
    }
    throw new Error('Expected promise to be rejected but it was fulfilled');
}
