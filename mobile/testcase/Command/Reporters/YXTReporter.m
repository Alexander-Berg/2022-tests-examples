
#import "YXTReporter.h"
#import "YXTStringFactory.h"
#import "YXTLog.h"

@interface YXTReporter ()

@property (nonatomic, copy, readonly) NSString *baseUrl;
@property (nonatomic, strong, readonly) NSOperationQueue *networkQueue;
@property (nonatomic, assign, readonly) NSTimeInterval timeout;

@end

@implementation YXTReporter

- (instancetype)initWithBaseURL:(NSString *)baseUrl
                   networkQueue:(NSOperationQueue *)networkQueue
                        timeout:(NSTimeInterval)timeout
{
    if (baseUrl.length == 0) {
        [NSException raise:NSInvalidArgumentException format:@"Empty base url"];
    }
    self = [super init];
    if (self != nil) {
        _baseUrl = [baseUrl copy];
        _networkQueue = networkQueue;
        _timeout = timeout;
    }
    return self;
}

- (void)reportResult:(NSDictionary *)result
 withQueryParameters:(NSDictionary *)queryParameters
          completion:(void(^)(NSDictionary *))completion
{
    result = result ?: @{};
    NSError *error = nil;
    NSData *resultJSONData = [NSJSONSerialization dataWithJSONObject:result options:0 error:&error];
    if (error != nil) {
        [NSException raise:NSInternalInconsistencyException format:@"Could not serialize result = %@", result];
    }
    NSURLRequest *request = [self requestWithQueryParameters:queryParameters body:resultJSONData];
    [YXTLog logFormat:@"Report command result.\nQuery: %@\nResult: %@", queryParameters, result];
    __typeof(self) __weak weakSelf = self;
    [NSURLConnection sendAsynchronousRequest:request
                                       queue:self.networkQueue
                           completionHandler:^(NSURLResponse *response, NSData *data, NSError *connectionError) {
                               [weakSelf processResponse:response data:data error:error completion:completion];
                            }];
    
}

- (NSURLRequest *)requestWithQueryParameters:(NSDictionary *)queryParameters
                                        body:(NSData *)body
{
    NSString *query = [YXTStringFactory queryForComponents:queryParameters];
    NSString *fullURL = [NSString stringWithFormat:@"%@?%@", self.baseUrl, query];
    
    NSMutableURLRequest *request =
        [NSMutableURLRequest requestWithURL:[NSURL URLWithString:fullURL]
                                cachePolicy:NSURLRequestReloadIgnoringCacheData
                            timeoutInterval:self.timeout];
    request.HTTPMethod = @"PUT";
    request.HTTPBody = body;
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    
    return [request copy];
}

- (void)processResponse:(NSURLResponse *)response
                   data:(NSData *)data
                  error:(NSError *)error
             completion:(void(^)(NSDictionary *))completion
{
    if (error != nil) {
        NSString *errorDescription = [YXTStringFactory descriptionForError:error];
        [NSException raise:NSInternalInconsistencyException
                    format:@"Could not send result to %@: error = %@", response.URL, errorDescription];
    }
    
    NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
    NSInteger statusCode = httpResponse.statusCode;
    if (statusCode >= 200 && statusCode < 300 && completion != nil) {
        [self processData:data statusCode:statusCode completion:completion];
    }
}

- (void)processData:(NSData *)data statusCode:(NSInteger)statusCode completion:(void(^)(NSDictionary *))completion
{
    NSString *resultString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    [YXTLog logFormat:@"[%lu] Recieved response: %@", statusCode, resultString];
    
    NSError *error = nil;
    NSDictionary *result = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
    if (error != nil) {
        [NSException raise:NSInternalInconsistencyException
                    format:@"Could not deserialize response %@", [YXTStringFactory descriptionForError:error]];
    }
    if ([result isKindOfClass:[NSDictionary class]] == NO) {
        [NSException raise:NSInternalInconsistencyException
                    format:@"Response parameters object is not a dictionary"];
    }
    completion(result);
}

@end
