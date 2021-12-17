package com.sps.tuya_plugin;

import androidx.annotation.NonNull;

import android.app.Application;
import android.os.Handler;
import android.os.Message;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.alibaba.fastjson.JSON;
import com.tuya.smart.android.user.api.ILoginCallback;
import com.tuya.smart.android.user.bean.User;
import com.tuya.smart.sdk.api.ITuyaActivatorGetToken;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.home.sdk.builder.ActivatorBuilder;
import com.tuya.smart.sdk.api.ITuyaActivator;
import com.tuya.smart.sdk.api.ITuyaSmartActivatorListener;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuya.smart.sdk.enums.ActivatorAPStepCode;
import com.tuya.smart.sdk.enums.ActivatorEZStepCode;
import com.tuya.smart.sdk.enums.ActivatorModelEnum;

import static com.tuya.smart.sdk.enums.ActivatorModelEnum.TY_AP;
import static com.tuya.smart.sdk.enums.ActivatorModelEnum.TY_EZ;

/** TuyaPlugin */
public class TuyaPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  public static final String STATUS_FAILURE_WITH_NETWORK_ERROR = "1001";
  public static final String STATUS_FAILURE_WITH_BIND_GWIDS = "1002";
  public static final String STATUS_FAILURE_WITH_BIND_GWIDS_1 = "1003";
  public static final String STATUS_FAILURE_WITH_GET_TOKEN = "1004";
  public static final String STATUS_FAILURE_WITH_CHECK_ONLINE_FAILURE = "1005";
  public static final String STATUS_FAILURE_WITH_OUT_OF_TIME = "1006";
  public static final String STATUS_DEV_CONFIG_ERROR_LIST = "1007";
  public static final int WHAT_EC_ACTIVE_ERROR = 0x02;
  public static final int WHAT_EC_ACTIVE_SUCCESS = 0x03;
  public static final int WHAT_AP_ACTIVE_ERROR = 0x04;
  public static final int WHAT_AP_ACTIVE_SUCCESS = 0x05;
  public static final int WHAT_EC_GET_TOKEN_ERROR = 0x06;
  public static final int WHAT_DEVICE_FIND = 0x07;
  public static final int WHAT_BIND_DEVICE_SUCCESS = 0x08;
  private static final long CONFIG_TIME_OUT = 100;

  private ITuyaActivator mTuyaActivator;
  private ActivatorModelEnum mModelEnum;
  private MethodChannel channel;

  static int ERROR_CODE_TYPE_ERROR = -100; // Data type type error
  static int ERROR_CODE_NOT_LOGIN = -101; // Not logged in yet
  static int ERROR_CODE_PARAMS_ERROR = -502; // Parameter error
  static int SUCCESS_CODE = 200; // execution succeed

  protected android.content.Context mContext;
  private MethodChannel.Result result;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    this.mContext = flutterPluginBinding.getApplicationContext();
    TuyaHomeSdk.init((Application) flutterPluginBinding.getApplicationContext(),"4m7nm8j79ar8xxvjqu89","5h59ed4dsxw3rgtgjwpsfnhey5nggapt");
    TuyaHomeSdk.setDebugMode(true);
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "tuya_plugin");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    this.result = result;

    switch (call.method) {
      case "set_ec_net":
        this.setEC(call.argument("ssid").toString(), call.argument("password").toString(),call.argument("token").toString());
        break;
      case "set_ap_net":
        this.setAP(call.argument("ssid").toString(), call.argument("password").toString(),call.argument("token").toString());
        break;
        case "uid_login":
        this.uidLogin(call.argument("countryCode").toString(), call.argument("uid").toString(),call.argument("passwd").toString());
        break;
      default:
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  // Chế độ kết nối nhanh
  public void setEC(String ssid, String password, String token) {
    System.out.println("Bắt đầu mở mạng kết nối nhanh");
    mModelEnum = TY_EZ;
    ITuyaActivator ECActivator =  TuyaHomeSdk.getActivatorInstance().newMultiActivator(new ActivatorBuilder()
            .setSsid(ssid)
            .setContext(mContext)
            .setPassword(password)
            .setActivatorModel(TY_EZ)
            .setTimeOut(CONFIG_TIME_OUT)
            .setToken(token).setListener(new ITuyaSmartActivatorListener() {
              @Override
              public void onError(String s, String s1) {
                System.out.println("Cấu hình mạng EZ không thành công");
                callResult(ERROR_CODE_PARAMS_ERROR, "Lỗi cấu hình kết nối");
              }

              @Override
              public void onActiveSuccess(DeviceBean deviceBean) {
                System.out.println("Cấu hình kết nối thành công");
                callResult(SUCCESS_CODE, "Cấu hình thành công");
              }

              @Override
              public void onStep(String s, Object o) {
                System.out.println("-------------------");
              }
            })
    );
    ECActivator.start();
  }

  public void setAP(String ssid, String password, String token) {
    System.out.println("Chế độ điểm phát sóng");
    mModelEnum = TY_AP;
    mTuyaActivator = TuyaHomeSdk.getActivatorInstance().newActivator(new ActivatorBuilder()
            .setSsid(ssid)
            .setContext(mContext)
            .setPassword(password)
            .setActivatorModel(TY_AP)
            .setTimeOut(CONFIG_TIME_OUT)
            .setToken(token).setListener(new ITuyaSmartActivatorListener() {
              @Override
              public void onError(String error, String s1) {
                callResult(WHAT_AP_ACTIVE_ERROR, error);
              }
              @Override
              public void onActiveSuccess(DeviceBean gwDevResp) {
                callResult(WHAT_AP_ACTIVE_SUCCESS, gwDevResp.getDevId());
              }

              @Override
              public void onStep(String step, Object o) {
                switch (step) {
                  case ActivatorAPStepCode.DEVICE_BIND_SUCCESS:
                    callResult(WHAT_AP_ACTIVE_SUCCESS,"WHAT_BIND_DEVICE_SUCCESS");
                    break;
                  case ActivatorAPStepCode.DEVICE_FIND:
                    callResult(WHAT_DEVICE_FIND,"WHAT_DEVICE_FIND");
                    break;
                }
              }
            }));
    mTuyaActivator.start();
  }


  public void uidLogin(String countryCode, String uid, String passwd) {
    System.out.println("Bắt đầu đăng nhập Login");
    TuyaHomeSdk.getUserInstance().loginOrRegisterWithUid(countryCode,uid,passwd, new ILoginCallback() {
      @Override
      public void onSuccess(User user) {
        System.out.println("Đăng nhập thành công"+JSON.toJSONString(user));
        callResult(200, JSON.toJSONString(user));
      }

      @Override
      public void onError(String code, String error) {
        System.out.println("Đăng nhập thất bại");
        callResult(400, error);
      }
    });
  }
  public void callResult(int code, String msg) {
    HashMap<String, String> map = new HashMap<>();
    map.put("code", String.format("%d", code));
    map.put("msg", msg);

    Message message = Message.obtain();
    message.obj = map;
    result.success(map);
  }
  public void callResult(int code, String msg, String data) {
    HashMap<String, String> map = new HashMap<>();
    map.put("code", String.format("%d", code));
    map.put("msg", msg);
    map.put("data", data);

    Message message = Message.obtain();
    message.obj = map;
    result.success(map);
  }
  
  
    @Nonnull
    @Override
    public String getName() {
        return "TuyaHomeModule";
    }

    public void getHomeDetail(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).getHomeDetail(new ITuyaHomeResultCallback() {
                @Override
                public void onSuccess(HomeBean bean) {
                    promise.resolve(TuyaReactUtils.parseToWritableMap(TuyaHomeSdk.newHomeInstance(bean.getHomeId())));
                }

                @Override
                public void onError(String errorCode, String errorMsg) {

                }
            });
        }
    }


    public void getHomeLocalCache(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).getHomeLocalCache(getITuyaHomeResultCallback(promise));
        }
    }


    public void updateHome(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, NAME, LON, LAT, GEONAME), params)) {
            getHomeInstance(params.getDouble(HOMEID)).updateHome(
                    params.getString(NAME),
                    params.getDouble(LON),
                    params.getDouble(LAT),
                    params.getString(GEONAME),
                    getIResultCallback(promise));
        }
    }

    public void dismissHome(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).dismissHome(getIResultCallback(promise));
        }
    }

    public void sortHome(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, IDLIST), params)) {
            ArrayList<Long> list = new ArrayList<>();
            for (int i = 0; i < params.getArray(IDLIST).size(); i++) {
                list.add(coverDTL(params.getArray(IDLIST).getDouble(i)));
            }
            getHomeInstance(params.getDouble(HOMEID)).sortHome(list, getIResultCallback(promise));
        }
    }

    public void addRoom(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, NAME), params)) {
            getHomeInstance(params.getDouble(HOMEID)).addRoom(params.getString(NAME), getITuyaRoomResultCallback(promise));
        }
    }


    public void removeRoom(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, ROOMID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).removeRoom(coverDTL(params.getDouble(ROOMID)), getIResultCallback(promise));
        }
    }


    public void sortRoom(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, IDLIST), params)) {
            ArrayList<Long> list = new ArrayList<>();
            for (int i = 0; i < params.getArray(IDLIST).size(); i++) {
                list.add(coverDTL(params.getArray(IDLIST).getDouble(i)));
            }
            getHomeInstance(params.getDouble(HOMEID)).sortRoom(list, getIResultCallback(promise));
        }
    }


    public void queryRoomList(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).queryRoomList(getITuyaGetRoomListCallback(promise));
        }
    }


    public void getHomeBean(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            promise.resolve(TuyaReactUtils.parseToWritableMap(getHomeInstance(params.getDouble(HOMEID)).getHomeBean()));
        }
    }

    public void createGroup(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, DEVIDLIST, PRODUCTID, NAME), params)) {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < params.getArray(DEVIDLIST).size(); i++) {
                list.add(params.getArray(DEVIDLIST).getString(i));
            }
            getHomeInstance(params.getDouble(HOMEID)).createGroup(
                    params.getString(PRODUCTID),
                    params.getString(NAME),
                    list,
                    getITuyaResultCallback(promise)
            );
        }
    }

    public void queryRoomInfoByDevice(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, DEVICES), params)) {
            List deviceBeans = JsonUtils.parseArray(JsonUtils.toString(TuyaReactUtils.parseToList(params.getArray(DEVICES))), DeviceBean.class);
            List<RoomBean> roomBeans = getHomeInstance(params.getDouble(HOMEID)).queryRoomInfoByDevice(deviceBeans);
            promise.resolve(TuyaReactUtils.parseToWritableArray(JsonUtils.toJsonArray(roomBeans)));
        }
    }


    public void registerHomeDeviceStatusListener(final ReadableMap params) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            if (mITuyaHomeDeviceStatusListener != null) {
                getHomeInstance(params.getDouble(HOMEID)).unRegisterHomeDeviceStatusListener(mITuyaHomeDeviceStatusListener);
                mITuyaHomeDeviceStatusListener = null;

            }
            mITuyaHomeDeviceStatusListener = new ITuyaHomeDeviceStatusListener() {
                @Override
                public void onDeviceDpUpdate(String devId, String dpStr) {
                    WritableMap map = Arguments.createMap();
                    map.putString("devId", devId);
                    map.putString("dpStr", dpStr);
                    map.putString("type", "onDeviceDpUpdate");
                    BridgeUtils.homeDeviceStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }

                @Override
                public void onDeviceStatusChanged(String devId, boolean online) {
                    WritableMap map = Arguments.createMap();
                    map.putString("devId", devId);
                    map.putBoolean("online", online);
                    map.putString("type", "onDeviceStatusChanged");
                    BridgeUtils.homeDeviceStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }

                @Override
                public void onDeviceInfoUpdate(String devId) {
                    WritableMap map = Arguments.createMap();
                    map.putString("devId", devId);
                    map.putString("type", "onDeviceInfoUpdate");
                    BridgeUtils.homeDeviceStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }
            };
            getHomeInstance(params.getDouble(HOMEID)).registerHomeDeviceStatusListener(mITuyaHomeDeviceStatusListener);
        }
    }

    @ReactMethod
    public void unRegisterHomeDeviceStatusListener(ReadableMap params) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            if (mITuyaHomeDeviceStatusListener != null) {
                getHomeInstance(params.getDouble(HOMEID)).unRegisterHomeDeviceStatusListener(mITuyaHomeDeviceStatusListener);
                mITuyaHomeDeviceStatusListener = null;
            }
        }
    }


   
    public void registerHomeStatusListener(final ReadableMap params) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            if (mITuyaHomeStatusListener != null) {
                getHomeInstance(params.getDouble(HOMEID)).unRegisterHomeStatusListener(mITuyaHomeStatusListener);
                mITuyaHomeStatusListener = null;

            }
            mITuyaHomeStatusListener = new ITuyaHomeStatusListener() {

                @Override
                public void onDeviceAdded(String devId) {
                    WritableMap map = Arguments.createMap();
                    map.putString("devId", devId);
                    map.putString("type", "onDeviceAdded");
                    BridgeUtils.homeStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }

                @Override
                public void onDeviceRemoved(String devId) {
                    WritableMap map = Arguments.createMap();
                    map.putString("devId", devId);
                    map.putString("type", "onDeviceRemoved");
                    BridgeUtils.homeStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }

                @Override
                public void onGroupAdded(long groupId) {
                    WritableMap map = Arguments.createMap();
                    map.putDouble("groupId", groupId);
                    map.putString("type", "onGroupAdded");
                    BridgeUtils.homeStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }

                @Override
                public void onGroupRemoved(long groupId) {
                    WritableMap map = Arguments.createMap();
                    map.putDouble("groupId", groupId);
                    map.putString("type", "onGroupRemoved");
                    BridgeUtils.homeStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }

                @Override
                public void onMeshAdded(String meshId) {
                    WritableMap map = Arguments.createMap();
                    map.putString("meshId", meshId);
                    map.putString("type", "onMeshAdded");
                    BridgeUtils.homeStatus(getReactApplicationContext(), map, Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }
            };
            getHomeInstance(params.getDouble(HOMEID)).registerHomeStatusListener(mITuyaHomeStatusListener);
        }
    }

    public void unRegisterHomeStatusListener(ReadableMap params) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            if (mITuyaHomeStatusListener != null) {
                getHomeInstance(params.getDouble(HOMEID)).unRegisterHomeStatusListener(mITuyaHomeStatusListener);
                mITuyaHomeStatusListener = null;
            }
        }
    }


    public void createBlueMesh(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, MESHID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).createBlueMesh(params.getString(MESHID), getITuyaResultCallback(promise));
        }
    }

    public void createSigMesh(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).createSigMesh(getITuyaResultCallback(promise));
        }
    }

    public void queryDeviceListToAddGroup(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, GROUPID, PRODUCTID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).queryDeviceListToAddGroup(coverDTL(params.getDouble(GROUPID)), params.getString(PRODUCTID), getITuyaResultCallback(promise));
        }
    }


    public void queryZigbeeDeviceListToAddGroup(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, PRODUCTID, GROUPID, PARENTID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).queryZigbeeDeviceListToAddGroup(coverDTL(params.getDouble(GROUPID)),
                    params.getString(PRODUCTID),
                    params.getString(PARENTID), getITuyaResultCallback(promise));
        }
    }

    public void onDestroy(ReadableMap params) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            getHomeInstance(params.getDouble(HOMEID)).onDestroy();
        }
    }

    public void createZigbeeGroup(ReadableMap params, Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, PRODUCTID, PARENTID, PARENTTYPE, NAME), params)) {
            getHomeInstance(params.getDouble(HOMEID)).createZigbeeGroup(
                    params.getString(PRODUCTID),
                    params.getString(PARENTID),
                    params.getInt(PARENTTYPE),
                    params.getString(NAME),
                    getITuyaResultCallback(promise)
            );
        }
    }

    public void queryRoomInfoByGroup(ReadableMap params, final Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, LIST), params)) {
            List deviceBeans = JsonUtils.parseArray(JsonUtils.toString(TuyaReactUtils.parseToList(params.getArray(LIST))), GroupBean.class);
            List<RoomBean> roomBeans = getHomeInstance(params.getDouble(HOMEID)).queryRoomInfoByGroup(deviceBeans);
            promise.resolve(TuyaReactUtils.parseToWritableArray(JsonUtils.toJsonArray(roomBeans)));
        }
    }

    public void bindNewConfigDevs(ReadableMap params, Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, DEVIDLIST), params)) {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < params.getArray(DEVIDLIST).size(); i++) {
                list.add(params.getArray(DEVIDLIST).getString(i));
            }
            getHomeInstance(params.getDouble(HOMEID)).bindNewConfigDevs(
                    list,
                    getIResultCallback(promise));
        }
    }


    public void registerProductWarnListener(final ReadableMap params) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            if (mIWarningMsgListener != null) {
                getHomeInstance(params.getDouble(HOMEID)).unRegisterProductWarnListener(mIWarningMsgListener);
                mIWarningMsgListener = null;
            }
            mIWarningMsgListener = new IWarningMsgListener() {
                @Override
                public void onWarnMessageArrived(WarnMessageBean warnMessageBean) {
                    BridgeUtils.warnMessageArrived(getReactApplicationContext(), Arguments.createMap(), Double.valueOf(params.getDouble(HOMEID)).longValue()+"");
                }
            };
            getHomeInstance(params.getDouble(HOMEID)).registerProductWarnListener(mIWarningMsgListener);
        }
    }

    public void unRegisterProductWarnListener(ReadableMap params) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID), params)) {
            if (mIWarningMsgListener != null) {
                getHomeInstance(params.getDouble(HOMEID)).unRegisterProductWarnListener(mIWarningMsgListener);
                mIWarningMsgListener = null;
            }
        }
    }

    public void sortDevInHome(ReadableMap params, Promise promise) {
        if (ReactParamsCheck.checkParams(Arrays.asList(HOMEID, LIST), params)) {
            getHomeInstance(params.getDouble(HOMEID)).sortDevInHome(
                    Double.valueOf(params.getDouble(HOMEID)).toString(),
                    JsonUtils.parseArray(JsonUtils.toString(TuyaReactUtils.parseToList(params.getArray(LIST))), DeviceAndGroupInHomeBean.class),
                    getIResultCallback(promise));
        }
    }


    private ITuyaHome getHomeInstance(double homeId) {
        return TuyaHomeSdk.newHomeInstance(coverDTL(homeId));
    }


    public static ITuyaRoomResultCallback getITuyaRoomResultCallback(final Promise promise) {
        return new ITuyaRoomResultCallback() {
            @Override
            public void onSuccess(RoomBean bean) {
                promise.resolve(TuyaReactUtils.parseToWritableMap(bean));
            }

            @Override
            public void onError(String code, String error) {
                promise.reject(code, error);
            }

        };
    }

    public static ITuyaGetRoomListCallback getITuyaGetRoomListCallback(final Promise promise) {
        return new ITuyaGetRoomListCallback() {
            @Override
            public void onSuccess(List<RoomBean> romeBeans) {
                promise.resolve(TuyaReactUtils.parseToWritableArray(JsonUtils.toJsonArray(romeBeans)));
            }

            @Override
            public void onError(String code, String error) {
                promise.reject(code, error);
            }

        };
    }

    public static ITuyaHomeResultCallback getITuyaHomeResultCallback(final Promise promise) {
        return new ITuyaHomeResultCallback() {
            @Override
            public void onSuccess(HomeBean bean) {
                promise.resolve(TuyaReactUtils.parseToWritableMap(bean));
            }

            @Override
            public void onError(String code, String error) {
                promise.reject(code, error);
            }

        };
    }

  
  
}
