
#import "MASTestEnvironmentStorage.h"

static NSString *const kMASTestEnvironmentStorageKey = @"MASTestEnvironmentStorageKey";

@implementation MASTestEnvironmentStorage

- (nullable NSString *)testEnvironment
{
    return [[NSUserDefaults standardUserDefaults] objectForKey:kMASTestEnvironmentStorageKey];
}

- (void)saveTestEnvironment:(nullable NSString *)testEnvironment
{
    [[NSUserDefaults standardUserDefaults] setObject:testEnvironment forKey:kMASTestEnvironmentStorageKey];
}

@end
