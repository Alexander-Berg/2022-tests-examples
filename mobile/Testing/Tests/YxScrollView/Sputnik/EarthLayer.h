//
//  EarthLayer.h
//  Weather
//
//  Created by Denis Malykh on 26.03.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

#import <QuartzCore/QuartzCore.h>
#import <UIKit/UIKit.h>

@interface EarthLayer : CALayer < CAAnimationDelegate >

@property(nonatomic) CGFloat progress;
@property(nonatomic) CGFloat rotate;

@property(nonatomic, strong) UIImage* earthImage;
@property(nonatomic, strong) UIImage* orbitImage;
@property(nonatomic, strong) UIImage* sputnikImage;

@property(nonatomic, copy) dispatch_block_t progressComplete;
@property(nonatomic, copy) dispatch_block_t rotateComplete;

- (void)setProgress:(CGFloat)progress velocity:(NSTimeInterval)velocity complete:(dispatch_block_t)complete;
- (void)setRotate:(CGFloat)rotate velocity:(NSTimeInterval)velocity complete:(dispatch_block_t)complete;
- (void)reset;

@end
