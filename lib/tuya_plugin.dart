
import 'dart:async';

import 'package:flutter/services.dart';

class TuyaPlugin {
  static const MethodChannel _channel = const MethodChannel('tuya_plugin');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /*
  * */
  Future<TuYaResult> handleResult(info) async {
    print ("Code info 。。。。。。 ${info["code"]}");
    print (info["msg"]);
    final TuYaResult result = new TuYaResult();
    result.setRest(int.parse(info["code"]), info["msg"], info["data"]);

    return result;
  }

  Future<TuYaResult> setECNet(String ssid, String password,String token) async {
    var info = await _channel.invokeMethod("set_ec_net", {
      'ssid': ssid,
      'password': password,
      'token': token,
    });

    return this.handleResult(info);
  }
  // set điểm phát sóng
  Future<TuYaResult> setApNet(String ssid, String password,String token) async {
    var info = await _channel.invokeMethod("set_ap_net", {
      'ssid': ssid,
      'password': password,
      'token': token,
    });

    return this.handleResult(info);
  }

  Future<TuYaResult> uidLogin(String countryCode, String uid, String passwd) async {
    var info = await _channel.invokeMethod("uid_login", {
      'countryCode': countryCode,
      'uid': uid,
      'passwd': passwd,
    });

    return this.handleResult(info);
  }
}

/// data result
class TuYaResult {
  int code;

  String msg;

  String data;

  void setRest(int code, String msg, String data) {
    this.code = code;
    this.msg = msg;
    this.data = data;
  }

  @override
  String toString() {
    String result = '{retCode:$code,retMsg:$msg,data:$data}';
    return result;
  }
}
