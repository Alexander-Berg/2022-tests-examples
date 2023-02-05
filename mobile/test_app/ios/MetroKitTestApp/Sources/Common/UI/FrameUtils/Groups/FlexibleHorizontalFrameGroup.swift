import CoreGraphics

public struct FlexibleHorizontalFrameGroup {
    
    public struct Item {
        public struct Priority {
            public var value: Int
            
            public init(value: Int) {
                self.value = value
            }
            
            public static let veryHigh: Priority = Priority(value: 1000)
            public static let high: Priority = Priority(value: 750)
            public static let medium: Priority = Priority(value: 500)
            public static let low: Priority = Priority(value: 250)
            public static let veryLow: Priority = Priority(value: 100)
        }
    
        public var content: BoundedContent
        public var maxWidth: CGFloat
        public var priority: Priority
        
        public init(content: BoundedContent, maxWidth: CGFloat = .greatestFiniteMagnitude, priority: Priority = .low) {
            self.content = content
            self.maxWidth = maxWidth
            self.priority = priority
        }
    }
    
    public var spacing: CGFloat
    public var leftInset: CGFloat
    public var rightInset: CGFloat
    public var boundingSize: CGSize
    
    public init(spacing: CGFloat, leftInset: CGFloat, rightInset: CGFloat, boundingSize: CGSize) {
        self.spacing = spacing
        self.leftInset = leftInset
        self.rightInset = rightInset
        self.boundingSize = boundingSize
    }
    
    public func layout(_ items: [Item]) -> [CGRect] {
        return makeFrames(from: makeBoundedItems(from: items))
    }
    
    // MARK: - Private
    
    private struct BoundedItem {
        var item: Item
        var size: CGSize
    }
    
    private func makeBoundedItems(from items: [Item]) -> [BoundedItem] {
        let reservedWidth: CGFloat = leftInset + rightInset + spacing * CGFloat(max(items.count - 1, 0))
        var availableWidth: CGFloat = boundingSize.width - reservedWidth
        
        var resultItems = items.map { BoundedItem(item: $0, size: .zero) }
        
        let sortedItems = items.enumerated().sorted(by: { $0.element.priority.value > $1.element.priority.value })
        
        for (offset, item) in sortedItems.enumerated() {
            assert(availableWidth > 0)
        
            let maxWidth = min(availableWidth, item.element.maxWidth)
            
            let itemSize: CGSize
            
            if offset == sortedItems.count - 1 { // last
                itemSize = item.element.content.boundingSize(fixedWidth: maxWidth, maxHeight: boundingSize.width)
            } else {
                itemSize = item.element.content.boundingSize(maxWidth: maxWidth, maxHeight: boundingSize.width)
            }
            
            resultItems[item.offset].size = itemSize
            availableWidth -= itemSize.width
        }
        
        return resultItems
    }
    
    private func makeFrames(from boundedItems: [BoundedItem]) -> [CGRect] {
        var frames: [CGRect] = []
        
        var currentX = leftInset
        
        for item in boundedItems {
            frames.append(CGRect(origin: CGPoint(x: currentX, y: 0.0), size: item.size))
            currentX += spacing + item.size.width
        }
        
        return frames
    }
    
}
