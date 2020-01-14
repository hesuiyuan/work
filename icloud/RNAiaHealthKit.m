#import "RNAiaHealthKit.h"
#import <HealthKit/HealthKit.h>
#import <UIKit/UIKit.h>


@interface RNAiaHealthKit ()
@property (nonatomic, strong) HKHealthStore *store;
@property (nonatomic, strong) NSString *_tenantId;
@property (nonatomic, strong) NSDateFormatter *_formatter;
@property (nonatomic, strong) NSString *_appleDeviceName;
@end

@implementation RNAiaHealthKit

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (NSOperationQueue*)requrestQueue
{
    NSOperationQueue *queue = [NSOperationQueue currentQueue];
    return queue;
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(initHealthKit: (NSString *)env options: (NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    NSLog(@"in RNAiaHealthKit- initHealthKit");
    NSLog(@"env: %@", env);
    NSLog(@"options: %@", options);
    
    @try{
        // Check running env
        #if TARGET_IPHONE_SIMULATOR
                NSLog(@"Running in Simulator - no app store or giro");
                self._appleDeviceName = @"com.apple.Health";
        #else
                NSLog(@"Running on the Device");
                self._appleDeviceName = @"com.apple.health.";
        #endif
        
        self._tenantId = options[env][@"XVitalityLegalEntityId"];
        NSLog(@"tenantId: %@", options[env][@"XVitalityLegalEntityId"]);
        self.store = [[HKHealthStore alloc] init];
        self._formatter = [[NSDateFormatter alloc] init];
        [self._formatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss"];
        [self._formatter setTimeZone: [NSTimeZone timeZoneWithName:@"Asia/Hong_Kong"]];
        
        resolve(@(YES));
    }
    @catch( NSException *exception){
        resolve(@(NO));
    }
}

RCT_EXPORT_METHOD(isHealthSDKAvailable: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    BOOL isAvailable = [HKHealthStore isHealthDataAvailable];
    resolve(@(isAvailable));
}

RCT_EXPORT_METHOD(uploadData: (NSString *)url headers:(NSDictionary *)headers data:(NSDictionary *)data resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    NSMutableURLRequest * urlRequest = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]];
    [urlRequest setHTTPMethod:@"POST"];
    [urlRequest addValue:@"application/json; charset=utf-8" forHTTPHeaderField:@"Content-Type"];
    [headers enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
        [urlRequest addValue:obj forHTTPHeaderField:key];
    }];
    
    NSString *aa = [headers objectForKey:@"Authorization"];
    
    NSData *postData = [NSJSONSerialization dataWithJSONObject:data options:NSJSONWritingPrettyPrinted error:nil];
    [urlRequest setHTTPBody:postData];

    [NSURLConnection sendAsynchronousRequest:urlRequest queue:[RNAiaHealthKit requrestQueue] completionHandler:^(NSURLResponse * _Nullable response, NSData * _Nullable data, NSError * _Nullable connectionError) {
        NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *) response;
        NSString *home = NSHomeDirectory();//获取沙盒路径
        NSString *docPath = [home stringByAppendingPathComponent:@"Documents"];
        NSString *filePath = [docPath stringByAppendingPathComponent:@"LogFile.txt"];
        //NSString *message2 = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        NSString *message2 = [NSString stringWithFormat:@"Return with code %ld, %@", (long)[httpResponse statusCode], [[httpResponse URL] absoluteString]];
        NSFileManager *fileManager = [NSFileManager defaultManager];

        
        if(![fileManager fileExistsAtPath:filePath]) //如果不存在
        {
            [message2 writeToFile:filePath atomically:YES encoding:NSUTF8StringEncoding error:nil];
        }
        NSFileHandle *fileHandle = [NSFileHandle fileHandleForUpdatingAtPath:filePath];
        [fileHandle seekToEndOfFile];
        
        NSData* stringData  = [message2 dataUsingEncoding:NSUTF8StringEncoding];
        
        [fileHandle writeData:stringData];
        [fileHandle writeData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
        [fileHandle writeData:[[NSString stringWithFormat:@"Token : %@", aa] dataUsingEncoding:NSUTF8StringEncoding]];
        [fileHandle writeData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
        [fileHandle writeData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
        [fileHandle closeFile];
        
        if (connectionError) {
            NSString *message = @"";
            if (data) {
                message = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            }
            reject(@"NSURLConnection error", message, connectionError);
        }else if ( [httpResponse statusCode] != 200) {
            reject(@"Error", [NSString stringWithFormat:@"Error with code %ld", (long)[httpResponse statusCode]], [NSError errorWithDomain:url code:[httpResponse statusCode] userInfo:@{}]);
            
        }else {
            resolve(data);
        }
    }];
}

@end
  
