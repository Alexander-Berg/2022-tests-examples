
#import "YXTCommandsRegistry.h"
#import "YXTCommandDescriptor.h"

static NSString *const kYXTClassKey = @"class";
static NSString *const kYXTCommandsKey = @"commands";

static volatile int count = 0; // To prevent __FRANKENSTAIN_EXECUTING_COMMAND__ from optimization

void __FRANKENSTAIN_EXECUTING_COMMAND__(dispatch_block_t block)
{
    block();
    count++;
}

@implementation YXTCommandsRegistry

+ (NSMutableDictionary *)commandBlocks
{
    static NSMutableDictionary *commandBlocks = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        commandBlocks = [NSMutableDictionary dictionary];
    });
    return commandBlocks;
}

+ (NSString *)keyForCommandClass:(NSString *)commandClass commandName:(NSString *)commandName
{
    return [NSString stringWithFormat:@"%@.%@", commandClass, commandName];
}

+ (void)registerCommandClass:(NSString *)commandClass
                        name:(NSString *)name
                       block:(YXTCommandBlock)block
{
    if (commandClass.length == 0) {
        [NSException raise:NSInvalidArgumentException format:@"Empty command class"];
    }
    if (name.length == 0) {
        [NSException raise:NSInvalidArgumentException format:@"Empty command name"];
    }
    @synchronized (self) {
        NSString *commandKey = [self keyForCommandClass:commandClass commandName:name];
        NSMutableDictionary *commandBlocks = [self commandBlocks];
        if (commandBlocks[commandKey] != nil) {
            [NSException raise:NSInternalInconsistencyException
                        format:@"Command '%@' already registered", commandKey];
        }
        commandBlocks[commandKey] = block;
    }
}

+ (YXTCommandBlock)commandBlockForCommandDescriptor:(YXTCommandDescriptor *)commandDescriptor
{
    NSString *commandKey = [self keyForCommandClass:commandDescriptor.className
                                        commandName:commandDescriptor.name];
    return [self commandBlocks][commandKey];

}

@end
