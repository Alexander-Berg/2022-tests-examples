
#import "YXTApplicationCleaner.h"
#import "YXTLog.h"

void yxt_cleanCache()
{
    [YXTApplicationCleaner cleanCache];
}

void yxt_cleanKeychain()
{
    [YXTApplicationCleaner cleanKeychain];
}

@implementation YXTApplicationCleaner

+ (void)cleanDirectoryWithSearchPath:(NSSearchPathDirectory)searchPath
{
    NSError *error = nil;
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSArray<NSString *> *paths = NSSearchPathForDirectoriesInDomains(searchPath, NSUserDomainMask, YES);
    for (NSString *directory in paths) {
        NSArray<NSString *> *files = [fileManager contentsOfDirectoryAtPath:directory error:&error];
        NSAssert(error == nil, @"Error while removing files in %@: %@", directory, error);
        for (NSString *file in files) {
            [fileManager removeItemAtPath:[directory stringByAppendingPathComponent:file] error:&error];
            NSAssert(error == nil, @"Error while removing %@: %@", file, error);
        }
    }
}

+ (void)cleanKeychain
{
    [self deleteAllKeysForSecClass:kSecClassGenericPassword];
    [self deleteAllKeysForSecClass:kSecClassInternetPassword];
    [self deleteAllKeysForSecClass:kSecClassCertificate];
    [self deleteAllKeysForSecClass:kSecClassKey];
    [self deleteAllKeysForSecClass:kSecClassIdentity];
    [YXTLog logMessage:@"Clean: Keychain"];
}

+ (void)deleteAllKeysForSecClass:(CFTypeRef)secClass
{
    NSMutableDictionary* dict = [NSMutableDictionary dictionary];
    [dict setObject:(__bridge id)secClass forKey:(__bridge id)kSecClass];
    SecItemDelete((__bridge CFDictionaryRef) dict);
}

+ (void)cleanCache
{
    [self cleanDirectoryWithSearchPath:NSCachesDirectory];
}

@end
