
#import <Foundation/Foundation.h>

@class YXTCommandInput;
@class YXTCommandDescriptor;

typedef void(^YXTCommandBlock)(YXTCommandInput *input);

void __FRANKENSTAIN_EXECUTING_COMMAND__(dispatch_block_t block);

#define REGISTER_COMMAND(CLASS_NAME, COMMAND_NAME, CMD) \
    [YXTCommandsRegistry registerCommandClass:CLASS_NAME \
                                         name:COMMAND_NAME \
                                        block:^(YXTCommandInput *input) { \
                                            { __FRANKENSTAIN_EXECUTING_COMMAND__(^{ CMD ; }) ; }; \
                                        }];
#define REGISTER_CLASS(CLASS_NAME, COMMANDS) { \
    NSString *currentCommandClassName = CLASS_NAME; \
    { COMMANDS ; }; \
}
#define WITH_COMMAND(COMMAND_NAME, CMD) REGISTER_COMMAND(currentCommandClassName, COMMAND_NAME, CMD)

@interface YXTCommandsRegistry : NSObject

+ (void)registerCommandClass:(NSString *)commandClass
                        name:(NSString *)name
                       block:(YXTCommandBlock)block;

+ (YXTCommandBlock)commandBlockForCommandDescriptor:(YXTCommandDescriptor *)commandDescriptor;

@end
