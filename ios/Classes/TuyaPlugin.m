#import "TuyaPlugin.h"
#import <TuyaSmartHomeKit/TuyaSmartKit.h>
#import <Foundation/Foundation.h>

@implementation TuyaPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"tuya_plugin"
            binaryMessenger:[registrar messenger]];
  TuyaPlugin* instance = [[TuyaPlugin alloc] init];
  [[TuyaSmartSDK sharedInstance] startWithAppKey:@"dfn7nrd4yy47p89cea3a" secretKey:@"yeetfswhrjm4fvjcad7mwnsm754yuhmw"];
  #ifdef DEBUG
      [[TuyaSmartSDK sharedInstance] setDebugMode:YES];
  #else
  #endif
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
       result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  }else if ([@"set_ec_net" isEqualToString:call.method]) {
       [self startConfigWiFi:call.arguments[@"ssid"] password:call.arguments[@"password"] token:call.arguments[@"token"]];
  }else if ([@"set_ap_net" isEqualToString:call.method]) {
       [self startApConfigWiFi:call.arguments[@"ssid"] password:call.arguments[@"password"] token:call.arguments[@"token"]];
  }else if ([@"uid_login" isEqualToString:call.method]) {
      [[TuyaSmartUser sharedInstance] loginOrRegisterWithCountryCode:call.arguments[@"countryCode"] uid:call.arguments[@"uid"]  password:call.arguments[@"passwd"] createHome:YES success:^(id result1) {
              result(@"Data");
      } failure:^(NSError *error) {
             result(@"Login fail");
      }];
  }else {
    result(FlutterMethodNotImplemented);
  }
}

- (void)startConfigWiFi:(NSString *)ssid password:(NSString *)password token:(NSString *)token {
    [TuyaSmartActivator sharedInstance].delegate = self;

    [[TuyaSmartActivator sharedInstance] startConfigWiFi:TYActivatorModeEZ ssid:ssid password:password token:token timeout:100];
}

#pragma mark - TuyaSmartActivatorDelegate
- (void)activator:(TuyaSmartActivator *)activator didReceiveDevice:(TuyaSmartDeviceModel *)deviceModel error:(NSError *)error {

    if (!error && deviceModel) {
        NSLog(@"NSLogprintf");
    }

    if (error) {
        NSLog(@"NSLogprintf");

    }
}

- (void)startApConfigWiFi:(NSString *)ssid password:(NSString *)password token:(NSString *)token {
    [TuyaSmartActivator sharedInstance].delegate = self;

    [[TuyaSmartActivator sharedInstance] startConfigWiFi:TYActivatorModeAP ssid:ssid password:password token:token timeout:100];
}


@end


