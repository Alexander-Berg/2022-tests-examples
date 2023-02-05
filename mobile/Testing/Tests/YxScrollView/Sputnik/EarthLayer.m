//
//  EarthLayer.m
//  Weather
//
//  Created by Denis Malykh on 26.03.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

#import "EarthLayer.h"

static CGFloat EarthLayout_Stiffness = 50.0f;
static CGFloat EarthLayout_Velocity = 10.0f;
static CGFloat EarthLayout_Damping = 10.0f;
static CGFloat EarthLayout_Mass = 1.0f;
static BOOL EarthLayout_Overdamping = NO;

@implementation EarthLayer

@dynamic progress;
@dynamic rotate;
@dynamic earthImage;
@dynamic orbitImage;
@dynamic sputnikImage;

#pragma mark - Public

- (void)setProgress:(CGFloat)progress velocity:(NSTimeInterval)velocity complete:(dispatch_block_t)complete {
    if(self.progressComplete != nil) {
        dispatch_block_t block = self.progressComplete;
        self.progressComplete = nil;
        block();
    }
    if(velocity > 0.0f) {
        CABasicAnimation* animation = [CABasicAnimation animationWithKeyPath:NSStringFromSelector(@selector(progress))];
        if(self.presentationLayer != nil) {
            animation.fromValue = [self.presentationLayer valueForKey:NSStringFromSelector(@selector(progress))];
        } else {
            animation.fromValue = @(self.progress);
        }
        animation.toValue = @(progress);
        animation.duration = ABS([animation.fromValue floatValue] - progress) / velocity;
        animation.fillMode = kCAFillModeBoth;
        animation.delegate = self;
        [self removeAnimationForKey:NSStringFromSelector(@selector(progress))];
        [self addAnimation:animation forKey:NSStringFromSelector(@selector(progress))];
        
        self.progressComplete = complete;
    } else {
        if(complete != nil) {
            complete();
        }
    }
    self.progress = progress;
}

- (void)setRotate:(CGFloat)rotate velocity:(NSTimeInterval)velocity complete:(dispatch_block_t)complete {
    if(self.rotateComplete != nil) {
        dispatch_block_t block = self.rotateComplete;
        self.rotateComplete = nil;
        block();
    }
    if(velocity > 0.0f) {
        [self removeAnimationForKey:NSStringFromSelector(@selector(rotate))];

        CABasicAnimation* animation = [CABasicAnimation animationWithKeyPath:NSStringFromSelector(@selector(rotate))];
        if(self.presentationLayer != nil) {
            animation.fromValue = [self.presentationLayer valueForKey:NSStringFromSelector(@selector(rotate))];
        } else {
            animation.fromValue = @(self.rotate);
        }
        animation.toValue = @(rotate);
        animation.duration = ABS([animation.fromValue floatValue] - rotate) / velocity;
        animation.fillMode = kCAFillModeForwards; // kCAFillModeBoth;
        animation.delegate = self;
        [self addAnimation:animation forKey:NSStringFromSelector(@selector(rotate))];
        
        self.rotateComplete = complete;
    } else {
        if(complete != nil) {
            complete();
        }
    }
    self.rotate = rotate;
}

- (void)reset {
    [self removeAnimationForKey:NSStringFromSelector(@selector(progress))];
    self.progressComplete = nil;
    self.progress = 0;
    
    [self removeAnimationForKey:NSStringFromSelector(@selector(rotate))];
    self.rotateComplete = nil;
    self.rotate = 0;
}

#pragma mark - CALayer

+ (BOOL)needsDisplayForKey:(NSString*)key {
    if([key isEqualToString:NSStringFromSelector(@selector(mode))] == YES) {
        return YES;
    } else if([key isEqualToString:NSStringFromSelector(@selector(progress))] == YES) {
        return YES;
    } else if([key isEqualToString:NSStringFromSelector(@selector(rotate))] == YES) {
        return YES;
    } else if([key isEqualToString:NSStringFromSelector(@selector(earthImage))] == YES) {
        return YES;
    } else if([key isEqualToString:NSStringFromSelector(@selector(orbitImage))] == YES) {
        return YES;
    } else if([key isEqualToString:NSStringFromSelector(@selector(sputnikImage))] == YES) {
        return YES;
    }
    return [super needsDisplayForKey:key];
}

- (void)drawInContext:(CGContextRef)context {
    [super drawInContext:context];
    
    UIGraphicsPushContext(context);
    
    CGFloat earthScale = [self _earthScaleInsterpolate:self.progress];
    CGFloat orbitScale = [self _orbitScaleInsterpolate:self.progress];
    CGFloat sputnikScale = [self _sputnikScaleInsterpolate:self.progress];
    
    CGRect bounds = self.bounds;
    CGPoint boundsCenter = CGPointMake(CGRectGetMidX(bounds), CGRectGetMidY(bounds));
    CGSize earthSize = CGSizeMake(self.earthImage.size.width * earthScale, self.earthImage.size.height * earthScale);
    CGSize earthSize2 = CGSizeMake(earthSize.width * 0.5f, earthSize.height * 0.5f);
    CGSize orbitSize = CGSizeMake(self.orbitImage.size.width * orbitScale, self.orbitImage.size.height * orbitScale);
    CGSize orbitSize2 = CGSizeMake(orbitSize.width * 0.5f, orbitSize.height * 0.5f);
    CGSize sputnikSize = CGSizeMake(self.sputnikImage.size.width * sputnikScale, self.sputnikImage.size.height * sputnikScale);
    CGSize sputnikSize2 = CGSizeMake(sputnikSize.width * 0.5f, sputnikSize.height * 0.5f);
    
    CGContextSaveGState(context);
    CGContextTranslateCTM(context, boundsCenter.x, boundsCenter.y);
    [self.orbitImage drawInRect:CGRectMake(-orbitSize2.width, -orbitSize2.height, orbitSize.width, orbitSize.height)];
    CGContextRestoreGState(context);
    
    CGContextSaveGState(context);
    CGContextTranslateCTM(context, boundsCenter.x, boundsCenter.y);
    CGContextRotateCTM(context, [self _rotateInsterpolate:self.rotate] * M_PI / 180.0);
    [self.sputnikImage drawInRect:CGRectMake(-sputnikSize2.width, -sputnikSize2.height, sputnikSize.width, sputnikSize.height)];
    CGContextRestoreGState(context);
    
    CGContextSaveGState(context);
    CGContextTranslateCTM(context, boundsCenter.x, boundsCenter.y);
    [self.earthImage drawInRect:CGRectMake(-earthSize2.width, -earthSize2.height, earthSize.width, earthSize.height)];
    CGContextRestoreGState(context);
    
    UIGraphicsPopContext();
}

#pragma mark - Private

- (CGFloat)_earthScaleInsterpolate:(CGFloat)progress {
    return [self _springInterpolateProgress:progress min:0.0f max:0.8f from:0.0f to:1.0f];
}

- (CGFloat)_orbitScaleInsterpolate:(CGFloat)progress {
    return [self _springInterpolateProgress:progress min:0.2f max:1.0f from:0.0f to:1.0f];
}

- (CGFloat)_sputnikScaleInsterpolate:(CGFloat)progress {
    return [self _springInterpolateProgress:progress min:0.4f max:1.0f from:0.0f to:1.0f];
}

- (CGFloat)_rotateInsterpolate:(CGFloat)angle {
    return angle;
}

- (CGFloat)_springInterpolateProgress:(CGFloat)progress min:(CGFloat)min max:(CGFloat)max from:(CGFloat)from to:(CGFloat)to {
    if(progress > min) {
        if(progress < max) {
            return [self _springInterpolateTime:(progress - min) / (max - min) from:from to:to damping:EarthLayout_Damping mass:EarthLayout_Mass stiffness:EarthLayout_Stiffness velocity:EarthLayout_Velocity overdamping:EarthLayout_Overdamping];
        } else {
            return to;
        }
    }
    return from;
}

- (CGFloat)_springInterpolateTime:(CGFloat)time from:(CGFloat)from to:(CGFloat)to damping:(CGFloat)damping mass:(CGFloat)mass stiffness:(CGFloat)stiffness velocity:(CGFloat)velocity overdamping:(BOOL)overdamping {
    CGFloat b = damping / (2 * mass);
    CGFloat o1 = sqrtf(stiffness / mass);
    CGFloat o2 = sqrtf((o1 * o1) - (b * b));
    CGFloat o3 = sqrtf((b * b) - (o1 * o1));
    CGFloat x0 = -1;
    if((overdamping == NO) && (b > o1)) {
        b = o1;
    }
    CGFloat fraction;
    if(b < o1) {
        CGFloat envelope = expf(-b * time);
        fraction = (-x0 + envelope * (x0 * cosf(o2 * time) + ((b * x0 + velocity) / o2) * sinf(o2 * time)));
    } else if(b == o1) {
        CGFloat envelope = expf(-b * time);
        fraction = (-x0 + envelope * (x0 + (b * x0 + velocity) * time));
    } else {
        CGFloat envelope = expf(-b * time);
        fraction = (-x0 + envelope * (x0 * coshf(o3 * time) + ((b * x0 + velocity) / o3) * sinhf(o3 * time)));
    }
    return (from + fraction) * (to - from);
}

#pragma mark - CAAnimationDelegate

- (void)animationDidStop:(CAAnimation*)animation finished:(BOOL)finished {
    if([animation isKindOfClass:CAPropertyAnimation.class] == YES) {
        CAPropertyAnimation* propertyAnimation = (CAPropertyAnimation*)animation;
        if([propertyAnimation.keyPath isEqualToString:NSStringFromSelector(@selector(progress))] == YES) {
            if(_progressComplete != nil) {
                dispatch_block_t block = _progressComplete;
                _progressComplete = nil;
                block();
            }
        } else if([propertyAnimation.keyPath isEqualToString:NSStringFromSelector(@selector(rotate))] == YES) {
            if(_rotateComplete != nil) {
                dispatch_block_t block = _rotateComplete;
                _rotateComplete = nil;
                block();
            }
        }
    }
}

@end
