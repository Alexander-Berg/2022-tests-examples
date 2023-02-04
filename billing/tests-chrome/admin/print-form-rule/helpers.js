module.exports.fillElement = async function (
    browser,
    elementNum,
    context,
    attribute,
    comparison,
    value,
    blockNum = 1
) {
    await browser.scroll('.yb-rule-blocks');
    await browser.click(
        `.yb-rule-blocks__rule-blocks-container > div:nth-child(${blockNum}) .yb-rule-elements__add-rule-element`
    );
    await browser.ybLcomSelect(
        `.yb-rule-blocks__rule-blocks-container > div:nth-child(${blockNum}) .yb-rule-elements > div:nth-child(${elementNum}) .yb-rule-element__rule-item-container:nth-child(1)`,
        context
    );
    await browser.ybLcomSelect(
        `.yb-rule-blocks__rule-blocks-container > div:nth-child(${blockNum}) .yb-rule-elements > div:nth-child(${elementNum}) .yb-rule-element__rule-item-container:nth-child(2)`,
        attribute
    );
    await browser.ybLcomSelect(
        `.yb-rule-blocks__rule-blocks-container > div:nth-child(${blockNum}) .yb-rule-elements > div:nth-child(${elementNum}) .yb-rule-element__rule-item-container:nth-child(3)`,
        comparison
    );
    await browser.ybLcomSelect(
        `.yb-rule-blocks__rule-blocks-container > div:nth-child(${blockNum}) .yb-rule-elements > div:nth-child(${elementNum}) .yb-rule-element__rule-item-container:nth-child(4)`,
        value
    );
};
