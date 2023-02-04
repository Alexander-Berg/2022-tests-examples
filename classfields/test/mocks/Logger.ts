export const Logger = {
    child: () => ({
        info: jest.fn(),
        error: jest.fn(),
    }),
};
