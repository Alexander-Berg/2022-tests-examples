type PuppeteerPerformCommand = (callback: () => void | Promise<void>, description?: string) => Promise<void>;

export {PuppeteerPerformCommand};
