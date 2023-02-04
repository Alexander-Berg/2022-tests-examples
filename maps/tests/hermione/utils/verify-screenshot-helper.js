const path = require('path'),
    fs = require('vow-fs'),
    vowNode = require('vow-node'),
    chalk = require('chalk'),
    PngImg = require('png-img'),
    vow = require('vow'),
    chai = require('chai'),
    DEFAULT_TOLERANCE = 12,
    AssertViewResults = require('hermione/lib/browser/commands/assert-view/assert-view-results'),
    { getTestContext } = require('hermione/lib/utils/mocha'),
    { Image } = require('gemini-core'),
    { getCaptureProcessors } = require('hermione/lib/browser/commands/assert-view/capture-processors'),
    { handleImageDiff } = getCaptureProcessors(),
    config = {
        system : {
            diffColor : '#ff00ff',
            tempOpts : {}
        }
    };

module.exports = function(pluginOptions) {
    return function async(...args) {
        let params;
        if(typeof args[0] === 'number') {
            params = {
                left : args[0],
                top : args[1],
                width : args[2],
                height : args[3],
                screenshotId : args[4],
                options : args[5] || {}
            };
        } else {
            params = {
                selector : args[0],
                screenshotId : args[1],
                options : args[2] || {}
            };
        }

        const { selector, screenshotId, options, left, top, width, height } = params,
            test = getTestContext(this.executionContext),
            excludeSelectors = options.excludes,
            tolerance = options.tolerance || DEFAULT_TOLERANCE;

        let screenshot;

        test.hermioneCtx.assertViewResults = test.hermioneCtx.assertViewResults || AssertViewResults.create();
        config.tolerance = tolerance;

        return this
            .saveScreenshot()
            .then((screenshotBuffer) => {
                screenshot = new PngImg(screenshotBuffer);
            })
            .then(() => selector?
                this.isVisible(selector)
                    .then((isVisible) => isVisible?
                        true :
                        reportError('Element "' + selector + '" could not be snapped because it is not visible')
                    )
                    .getLocationInView(selector)
                    .then((location) => {
                        return this
                            .getElementSize(selector)
                            .then((elementSize) => {
                                const elementSizes = [].concat(elementSize);
                                const locations = [].concat(location);

                                return {
                                    x : locations[0].x,
                                    y : locations[0].y,
                                    width : elementSizes[0].width,
                                    height : elementSizes[0].height
                                };
                            });
                    }) :
                { x : left || 0, y : top || 0, width : width || +Infinity, height : height || +Infinity }
            )
            .then((dimensions) => {
                if(!excludeSelectors) {
                    return dimensions;
                }

                const screenshotSize = screenshot.size();
                const excludePromises = excludeSelectors.map((excludeNode) => {
                    return this
                        .getLocation(excludeNode.selector)
                        .then((location) => {
                            return this
                                .getElementSize(excludeNode.selector)
                                .then((elementSize) => {
                                    const elementSizes = [].concat(elementSize);
                                    const locations = [].concat(location).reverse();

                                    return elementSizes
                                        .map((size, i) => {
                                            return {
                                                x : locations[i].x,
                                                y : locations[i].y,
                                                width : size.width,
                                                height : size.height,
                                                color : excludeNode.color || '#ff0000'
                                            };
                                        })
                                        .filter((rect) => {
                                            return rect.x >= 0 && (rect.x + rect.width) < screenshotSize.width &&
                                                rect.y >= 0 && (rect.y + rect.height) < screenshotSize.height;
                                        })
                                        .map((rect) => {
                                            return screenshot.fill(rect.x, rect.y, rect.width, rect.height, rect.color);
                                        });
                                });
                        });
                }, this);

                return vow.all(excludePromises).then(() => dimensions);
            })
            .then((dimensions) => {
                const screenshotSize = screenshot.size(),
                    offsetX = Math.max(dimensions.x, 0),
                    offsetY = Math.max(dimensions.y, 0);

                return screenshot.crop(
                    offsetX,
                    offsetY,
                    Math.min(screenshotSize.width - offsetX, dimensions.width, screenshotSize.width),
                    Math.min(screenshotSize.height - offsetY, dimensions.height, screenshotSize.height)
                );
            })
            .then((screenshot) => {
                const referencePath = getScreenshotPath(this.executionContext, screenshotId),
                    unmatchedPath = getUnmatchedPath(referencePath),
                    browserId = this.executionContext.browserId,
                    stateName = screenshotId + '.' + browserId;

                return fs.exists(referencePath)
                    .then((referenceScreenshotExists) => {
                        if(referenceScreenshotExists) {
                            return saveScreenshot(screenshot, unmatchedPath)
                                .then(() =>
                                    Image.compare(referencePath, unmatchedPath, { tolerance : tolerance, canHaveCaret : true })
                                        .then((isEqual) => {
                                            if(!isEqual) {
                                                this.debugLog(`Screenshot "${screenshotId}" doesn't match to reference`);
                                                return handleImageDiff(unmatchedPath, referencePath, stateName, config)
                                                    .catch((e) => test.hermioneCtx.assertViewResults.add(e));
                                            }
                                            pluginOptions.verbose && console.log(
                                                chalk.gray(' ... '),
                                                chalk.green('✓'),
                                                chalk.gray('verified ', chalk.bold(screenshotId),' screenshot in ',
                                                    chalk.bold(browserId)));
                                            return test.hermioneCtx.assertViewResults.add({ stateName : stateName, refImagePath : referencePath });
                                        })
                                );
                        }
                        return saveScreenshot(screenshot, referencePath)
                            .then(() => {
                                console.warn(chalk.red(
                                    `Reference screenshot "${screenshotId}" for browser "${browserId}"
                                     does not exist`
                                ));
                                return true;
                            });
                    });
            });
    };

    function getScreenshotPath(executionContext, id) {
        return path.relative(process.cwd(), executionContext.file)
                .replace(/\.js$/, '')
                .replace(new RegExp('^' + pluginOptions.testBasePath), pluginOptions.referencePath) +
            '/' + id + '.' + executionContext.browserId + '.png';
    }

    function getUnmatchedPath(referenceScreenshot) {
        return referenceScreenshot.replace(new RegExp('^' + pluginOptions.referencePath), pluginOptions.unmatchedPath);
    }
};

function saveScreenshot(screenshot, filePath) {
    return fs.makeDir(path.dirname(filePath))
        .then(() => vowNode.invoke(screenshot.save.bind(screenshot), filePath)
        );
}

function reportError(text) {
    return chai.assert(false, text);
}