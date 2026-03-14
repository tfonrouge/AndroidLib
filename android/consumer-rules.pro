# fslib-android consumer ProGuard rules
# These rules are applied to consumers of this library when R8/ProGuard is enabled.

# Keep @Serializable classes used in JSON-RPC wire protocol
-keep class com.fonrouge.fslib.android.commonServices.JsonRpcRequest { *; }
-keep class com.fonrouge.fslib.android.commonServices.JsonRpcResponse { *; }
-keep class com.fonrouge.fslib.android.commonServices.RpcException { *; }
-keep class com.fonrouge.fslib.android.commonServices.ApiRouteEntry { *; }
-keep class com.fonrouge.fslib.android.commonServices.ServiceContractEntry { *; }
-keep class com.fonrouge.fslib.android.commonServices.ProtocolInfo { *; }
-keep class com.fonrouge.fslib.android.commonServices.ApiContract { *; }

# Keep IServiceProxy implementors' method names for stack trace resolution
# (callerMethodName() relies on unobfuscated method names)
-keepnames class * implements com.fonrouge.fslib.android.commonServices.IServiceProxy { *; }

# Keep kotlinx.serialization generated serializers
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
