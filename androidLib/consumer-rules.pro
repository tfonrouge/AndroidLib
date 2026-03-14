# androidLib consumer ProGuard rules
# These rules are applied to consumers of this library when R8/ProGuard is enabled.

# Keep @Serializable classes used in JSON-RPC wire protocol
-keep class com.fonrouge.androidLib.commonServices.JsonRpcRequest { *; }
-keep class com.fonrouge.androidLib.commonServices.JsonRpcResponse { *; }
-keep class com.fonrouge.androidLib.commonServices.RpcException { *; }
-keep class com.fonrouge.androidLib.commonServices.ApiRouteEntry { *; }
-keep class com.fonrouge.androidLib.commonServices.ServiceContractEntry { *; }
-keep class com.fonrouge.androidLib.commonServices.ProtocolInfo { *; }
-keep class com.fonrouge.androidLib.commonServices.ApiContract { *; }

# Keep IServiceProxy implementors' method names for stack trace resolution
# (callerMethodName() relies on unobfuscated method names)
-keepnames class * implements com.fonrouge.androidLib.commonServices.IServiceProxy { *; }

# Keep kotlinx.serialization generated serializers
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
