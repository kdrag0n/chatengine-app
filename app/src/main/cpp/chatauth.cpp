#include <jni.h>
#include <string>
#include <vector>
#include "base64.h"
#include <syslog.h>
#include <string.h>
#include <stdlib.h>

char APK_SIGNATURE_PRODUCTION[] = "";
char BASE64_LICENSE_KEY[] = "";
const char* STR_AUTHORIZATION = "Authorization";
const char* STR_REFERER = "Referer";

jboolean ENABLE_APP_BLACKLIST_CHECK = JNI_FALSE;
jboolean ENABLE_INTERNET_CHECK = JNI_FALSE;
jboolean REQUIRE_INSTALL_FROM_PLAY_STORE = JNI_FALSE;

static void debug(const std::string &message) {
    syslog(LOG_DEBUG, "%s", message.c_str());
}

static const std::string b64decode(const std::string &base64) {
    std::string output;
    Base64::Decode(base64, &output);
    return output;
}

static const char* getReferer() {
    std::string protocol = "https";
    std::string domain = b64decode("Y2hhdGVuZ2luZS54eXo=");
    std::string path = b64decode("Y2hhdA==");
    // TODO: base64, IV/decrypt from substratum, obfuscated chars

    std::string final = protocol + "://" + domain + "/" + path;

    char* result = (char*) malloc((final.length() + 1) * sizeof(char));
    strcpy(result, final.c_str());
    return result;
}

static const char* getKey() {
    std::string base64 = "OTM0ODY2MTUzZDliN2E0Mzg0Mjg2Mzc1MTIyOTg5Y2I3OGMyZWYyOTkyY2RlMGUyODE1OGM3ZWM";
    std::string type = "W";
    std::string prefix = "abb8";
    std::string suffix = "edd";

    char base64suffix = 92;
    char twoEight = 28;
    base64suffix = base64suffix + twoEight; // 'x'
    base64.append(&base64suffix);

    std::string final = type + prefix + b64decode(base64);
    final.append(suffix);

    char* result = (char*) malloc((final.length() + 1) * sizeof(char));
    strcpy(result, final.c_str());
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_khronodragon_android_chatengine_ChatActivity_authenticate(
        JNIEnv *env, jobject, jobject request) {
    jclass clazz = env->GetObjectClass(request);

    // method: header(String, String): Request.Builder
    jmethodID header = env->GetMethodID(clazz, "header", "(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Request$Builder;");

    // header: Authorization
    env->CallObjectMethod(request, header, env->NewStringUTF(STR_AUTHORIZATION), env->NewStringUTF(getKey()));

    // header: Referer
    env->CallObjectMethod(request, header, env->NewStringUTF(STR_REFERER), env->NewStringUTF(getReferer()));
}