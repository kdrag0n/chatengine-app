#include <jni.h>
#include <string>

char APK_SIGNATURE_PRODUCTION[] = "";
char BASE64_LICENSE_KEY[] = "";

jboolean ENABLE_APP_BLACKLIST_CHECK = JNI_FALSE;
jboolean ENABLE_INTERNET_CHECK = JNI_FALSE;
jboolean REQUIRE_INSTALL_FROM_PLAY_STORE = JNI_FALSE;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_khronodragon_android_chatengine_ChatActivity_getReferrer(
        JNIEnv *env,
        jobject) {
    std::string protocol = "http";
    std::string domain = "chatengine.xyz";
    std::string path = "chat";
    // TODO: base64, IV/decrypt from substratum, obfuscated chars

    std::string final = protocol + "://" + domain + "/" + path;
    return env->NewStringUTF(final.c_str());
}
