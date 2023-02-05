
import CommonCore
import Div2Kit

private let rootPath = UIElementPath("root")
private let parentPath = UIElementPath("parent")
private let cardId = DivCardID(rawValue: "ad")

class Div2BlockModelingContextTestFactory {

    static func make() -> Div2BlockModelingContext {
        return Div2BlockModelingContext(
            rootPath: rootPath,
            parentPath: parentPath,
            cardId: cardId,
            parentDivStatePath: nil,
            stateManager: DivStateManager(),
            blocksState: nil,
            visibilityCounter: DivVisibilityCounter(),
            galleryResizableInsets: nil,
            imageHolderFactory: ImageHolderFactory(localImageProvider: nil),
            customDivBlockFactory: CustomDivBlockFactory.empty,
            fontSpecifier: fontSpecifier,
            flagsInfo: DivFlagsInfo.default,
            extensionHandlers: []
        )
    }

}
