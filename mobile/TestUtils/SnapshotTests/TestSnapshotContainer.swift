import UIKit

public final class TestSnapshotContainer: UIView {

    public struct Style {
        public let lineWidth: CGFloat
        public let lineColors: [UIColor]
        
        public init(lineWidth: CGFloat = 8, lineColors: [UIColor]) {
            self.lineWidth = lineWidth
            self.lineColors = lineColors
        }
        
        // MARK: Presets
        
        public static var red: Style {
            return Style(lineColors: [.red, UIColor(red: 0.5, green: 0.0, blue: 0.0, alpha: 1.0)])
        }
        
        public static var green: Style {
            return Style(lineColors: [.green, UIColor(red: 0.0, green: 0.5, blue: 0.0, alpha: 1.0)])
        }
        
        public static var blue: Style {
            return Style(lineColors: [.blue, UIColor(red: 0.0, green: 0.0, blue: 0.5, alpha: 1.0)])
        }

        public static var light: Style {
            return Style(lineColors: [.white, UIColor(white: 0.95, alpha: 1.0)])
        }
        
        public static var nice: Style {
            return Style(lineColors:[
                UIColor(rgb: 0xE40303),
                UIColor(rgb: 0xFF8C00),
                UIColor(rgb: 0xFFED00),
                UIColor(rgb: 0x008026),
                UIColor(rgb: 0x004DFF),
                UIColor(rgb: 0x750787)
            ])
        }
        
        public static var great: Style {
            // by artist Martiros Saryan
            return Style(lineColors:[
                UIColor(rgb: 0xB11F38),
                UIColor(rgb: 0xE77A39),
                UIColor(rgb: 0xEBD524),
                UIColor(rgb: 0x4AA77A),
                UIColor(rgb: 0x685B87),
                UIColor(rgb: 0xA24C57)
            ])
        }
        
    }
    
    public let style: Style
    
    public init(frame: CGRect, style: Style) {
        self.style = style
        
        super.init(frame: frame)
        
        backgroundColor = .black
    }

    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public override func draw(_ rect: CGRect) {
    
        let ctx = UIGraphicsGetCurrentContext()!

        // flip y-axis of context, so (0,0) is the bottom left of the context
        ctx.scaleBy(x: 1, y: -1)
        ctx.translateBy(x: 0, y: -bounds.size.height)

        // generate a slightly larger rect than the view,
        // to allow the lines to appear seamless
        let renderRect = bounds.insetBy(dx: -style.lineWidth * 0.5, dy: -style.lineWidth * 0.5)

        // the total distance to travel when looping (each line starts at a point that
        // starts at (0,0) and ends up at (width, height)).
        let totalDistance = renderRect.size.width + renderRect.size.height
        
        // divide by cos(45º) to convert from diagonal length
        let transformedLineWidth = style.lineWidth / cos(.pi / 4)

        for (index, color) in style.lineColors.enumerated() {
            let lineOffset = CGFloat(index) * transformedLineWidth
            
            // loop through distances in the range 0 ... totalDistance
            for distance in stride(from: lineOffset, through: totalDistance,
                                   by: transformedLineWidth * CGFloat(style.lineColors.count)) {

                // the start of one of the stripes
                ctx.move(to: CGPoint(
                    // x-coordinate based on whether the distance is less than the width of the
                    // rect (it should be fixed if it is above, and moving if it is below)
                    x: distance < renderRect.width ?
                        renderRect.origin.x + distance :
                        renderRect.origin.x + renderRect.width,

                    // y-coordinate based on whether the distance is less than the width of the
                    // rect (it should be moving if it is above, and fixed if below)
                    y: distance < renderRect.width ?
                        renderRect.origin.y :
                        distance - (renderRect.width - renderRect.origin.x)
                ))

                // the end of one of the stripes
                ctx.addLine(to: CGPoint(
                    // x-coordinate based on whether the distance is less than the height of
                    // the rect (it should be moving if it is above, and fixed if it is below)
                    x: distance < renderRect.height ?
                        renderRect.origin.x :
                        distance - (renderRect.height - renderRect.origin.y),

                    // y-coordinate based on whether the distance is less than the height of
                    // the rect (it should be fixed if it is above, and moving if it is below)
                    y: distance < renderRect.height ?
                        renderRect.origin.y + distance :
                        renderRect.origin.y + renderRect.height
                ))
            }

            // stroke all of the lines added
            ctx.setStrokeColor(color.cgColor)
            ctx.setLineWidth(style.lineWidth)
            ctx.strokePath()
        }
    }
    
}
