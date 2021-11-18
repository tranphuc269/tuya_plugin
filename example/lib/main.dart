import 'package:app_settings/app_settings.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:tuya_plugin/tuya_plugin.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  TuyaPlugin _controller = new TuyaPlugin();
  Dio dio = new Dio();
  final GlobalKey<FormState> _formKey = new GlobalKey<FormState>();
  String ssid;
  String password;
  String token="97bf246cf992b8e4d25b90f97c10374b";



  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: SafeArea(
        child: Scaffold(
          body: Form(
            key: _formKey,
            child: Column(
              children: [
                TextFormField(
                  decoration: InputDecoration(
                      hintText: "ssid"
                  ),
                  onSaved: (v){
                    setState(() {
                      ssid=v;
                    });
                  },
                ),
                TextFormField(
                  decoration: InputDecoration(
                      hintText: "mật khẩu"
                  ),
                  onSaved: (v){
                    setState(() {
                      password=v;
                    });
                  },
                ),

                RaisedButton(
                  child: Text('Kết nối nhanh'),
                  onPressed: () {
                    this._controller.setECNet(ssid, password,token).then((value) {
                      Fluttertoast.showToast(msg: value.msg.toString());
                    });
                    _formKey.currentState.save();
                    dio.post("https://us-central1-my-first-action-project-96da6.cloudfunctions.net/get_tuya_pairing_token", data: {"tuya_user_id": "ay15956109400526lf4b"}).then((response) {
                      if(response.data["code"]==200){
                        print(response.data['data']['region']+response.data['data']['token']+response.data['data']['secret']);
                        this._controller.setECNet(ssid, password,response.data['data']['region']+response.data['data']['token']+response.data['data']['secret']).then((value) {
                          Fluttertoast.showToast(msg: value.msg.toString());
                        });
                      }
                    });
                  },
                ),
                RaisedButton(
                  child: Text('Điểm phát sóng'),
                  onPressed: () {
                    this._controller.setApNet(ssid, password,token).then((value) {
                      Fluttertoast.showToast(msg: value.msg.toString());
                    });
                    _formKey.currentState.save();
                    dio.post("https://us-central1-my-first-action-project-96da6.cloudfunctions.net/get_tuya_pairing_token", data: {"tuya_user_id": "ay15956109400526lf4b"}).then((response) {
                      if(response.data["code"]==200){
                        print(response.data['data']['region']+response.data['data']['token']+response.data['data']['secret']);
                        this._controller.setApNet(ssid, password,response.data['data']['region']+response.data['data']['token']+response.data['data']['secret']).then((value) {
                          Fluttertoast.showToast(msg: value.msg.toString());
                        });
                      }
                    });
                  },
                ),
                RaisedButton(
                    child: Text('Đăng nhập với uid'),
                    onPressed: () {
                      this._controller.uidLogin("84", "userName","password").then((value) {
                        Fluttertoast.showToast(msg: value.msg.toString());
                        print("Data response"+value.toString());
                      });
                    }
                ),
                RaisedButton(
                  child: Text('Cài đặt wifi'),
                  onPressed: () {
                    AppSettings.openWIFISettings();
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
