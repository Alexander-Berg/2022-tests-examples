
#import "YXTCommandCore.h"
#import "YXTApplicationCleaner.h"

@interface YXTApplicationCleanerCommand : NSObject

@end

@implementation YXTApplicationCleanerCommand

+ (void)load
{
    REGISTER_CLASS(@"ApplicationCleaner", {
        WITH_COMMAND(@"cleanCache", [self cleanCache:input]);
        WITH_COMMAND(@"cleanKeychain", [self cleanKeychain:input]);
    });
}

#pragma mark - Commands

+ (void)cleanCache:(YXTCommandInput *)input
{
    [YXTApplicationCleaner cleanCache];
    
    [input complete];
}

+ (void)cleanKeychain:(YXTCommandInput *)input
{
    [YXTApplicationCleaner cleanKeychain];
    
    [input complete];
}

@end
