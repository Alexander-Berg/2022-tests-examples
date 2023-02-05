//
//  FixedFrameGroup.swift
//  MetroToolbox
//
//  Created by Yury Potapov on 28.02.18.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

import CoreGraphics

public struct FixedFrameGroup {
    
    public enum Mode {
        case fromPoint(_: CGPoint, spacing: CGFloat)
        case centeredInRect(_: CGRect, spacing: CGFloat)
    }
    
    public enum Direction {
        case horizontal
        case vertical
    }
    
    public var mode: Mode
    public var direction: Direction
    
    public init(mode: Mode, direction: Direction) {
        self.mode = mode
        self.direction = direction
    }
    
    public func layout(_ sizes: [CGSize]) -> [CGRect] {
        switch mode {
        case .fromPoint(let point, spacing: let spacing):
            return makeFramesModeFromPoint(point, spacing: spacing, for: sizes)
        case .centeredInRect(let rect, spacing: let spacing):
            return makeFramesModeCenteredInRect(rect, spacing: spacing, for: sizes)
        }
    }
    
    // MARK: - Private
    
    private func makeFramesModeFromPoint(
        _ point: CGPoint, spacing: CGFloat, for sizes: [CGSize]) -> [CGRect]
    {
        var frames: [CGRect] = []
        
        if direction == .horizontal {
            var currentX = point.x
            let center = point.y
            
            for size in sizes {
                let origin = CGPoint(x: currentX, y: center - size.height / 2.0)
                frames.append(CGRect(origin: origin, size: size))
                currentX +=  size.width + spacing
            }
        } else {
            var currentY = point.y
            let center = point.x
            
            for size in sizes {
                let origin = CGPoint(x: center - size.width / 2.0, y: currentY)
                frames.append(CGRect(origin: origin, size: size))
                currentY +=  size.height + spacing
            }
        }
        
        return frames
    }
    
    private func makeFramesModeCenteredInRect(
        _ rect: CGRect, spacing: CGFloat, for sizes: [CGSize]) -> [CGRect]
    {
        if direction == .horizontal {
            let sumWidth = (sizes.reduce(0.0) { $0 + $1.width }) + CGFloat(sizes.count - 1) * spacing
            let inset = rect.minX + (rect.width - sumWidth) / 2.0
            return makeFramesModeFromPoint(CGPoint(x: inset, y: rect.midY), spacing: spacing, for: sizes)
        } else {
            let sumHeight = (sizes.reduce(0.0) { $0 + $1.height }) + CGFloat(sizes.count - 1) * spacing
            let inset = rect.minY + (rect.height - sumHeight) / 2.0
            return makeFramesModeFromPoint(CGPoint(x: rect.midX, y: inset), spacing: spacing, for: sizes)
        }
    }
}
