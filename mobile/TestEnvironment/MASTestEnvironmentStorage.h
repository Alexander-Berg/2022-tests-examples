
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface MASTestEnvironmentStorage : NSObject

- (nullable NSString *)testEnvironment;

- (void)saveTestEnvironment:(nullable NSString *)testEnvironment;

@end

NS_ASSUME_NONNULL_END
