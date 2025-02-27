#include <jni.h>
#include <string>
#include <vector>
#include "base64.h"
#include <string.h>
#include <stdlib.h>

const char* STR_AUTHORIZATION = "Authorization";
const char* STR_REFERER = "Referer";
const std::string &refDomainWithoutEq = "Y2hhdGVuZ2luZS54eXo";
const std::string &refPathWithout2Eq = "Y2hhdA";
const std::string &equals = "=";

inline void xorCrypt(const std::string &key, std::string &data) {
    for (size_t i = 0; i != data.size(); i++)
        data[i] ^= key[ i % key.size() ];
}

inline const std::string b64decode(const std::string &base64) {
    std::string output;
    Base64::Decode(base64, &output);
    return output;
}

inline const char* getReferer() {
    std::string protocol = "https";
    std::string domain = b64decode(refDomainWithoutEq + equals);
    std::string path = b64decode(refPathWithout2Eq + equals + equals);
    // TODO: base64, IV/decrypt from substratum, obfuscated chars

    std::string final = protocol + "://" + domain + "/" + path;

    char* result = (char*) malloc((final.length() + 1) * sizeof(char));
    strcpy(result, final.c_str());
    return result;
}

inline const char* getKey() {
    // this is XORed and base64ed after
    std::string baseXor64 = "FmYlWCsFZGt/PD0eHRIZM3xeMGoeT1NVFQUmRzkSDEMkLigNJjsJD3YdEAEnLyk4Zw5rEScwLC8saGAAOB0GTHshHV4OMjlbNSUk";
  //std::string base64 = "OTM0ODY2MTUzZDliN2E0Mzg0Mjg2Mzc1MTIyOTg5Y2I3OGMyZWYyOTkyY2RlMGUyODE1OGM3ZWM";
  //std::string base64 = "OTMyODY2MTUzZDliN2E0Mzg0Mjg2Mzc1MTIyOTg5g2I3OGMyZWYyOTkyY2RlMGUyODE1OGM3ZWM";
    std::string type = "W";//^                                    ^
    std::string prefix = "abb8";
    std::string suffix = "edd";

    // this is XORed
    std::string base64 = b64decode(baseXor64);

    std::string xorKeySuffix = "844";
    for (size_t i = 0; i != xorKeySuffix.size(); i++) {
        if (xorKeySuffix[i] == '8')
            xorKeySuffix[i] += 2;
        else if (xorKeySuffix[i] == '4')
            xorKeySuffix[i] -= 5;
    }

    std::string xorKey = refPathWithout2Eq + equals + refDomainWithoutEq + STR_AUTHORIZATION + xorKeySuffix;

    xorCrypt(xorKey, base64);

    /*base64[3] = '0'; // OTM0O...
    //                     ^

    base64[41] = 'Y'; // ...OTg5g2...
    //                          ^*/

    base64 += 'x';

    std::string final = type + prefix + b64decode(base64);
    final.append(suffix);

    char* result = (char*) malloc((final.length() + 1) * sizeof(char));
    strcpy(result, final.c_str());
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_kdrag0n_chathive_MainActivity_authenticate(
        JNIEnv *env, jobject, jobject request) {
    jclass clazz = env->GetObjectClass(request);

    // method: header(String, String): Request.Builder
    jmethodID header = env->GetMethodID(clazz, "header", "(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Request$Builder;");

    // header: Authorization
    env->CallObjectMethod(request, header, env->NewStringUTF(STR_AUTHORIZATION), env->NewStringUTF(getKey()));

    // header: Referer
    env->CallObjectMethod(request, header, env->NewStringUTF(STR_REFERER), env->NewStringUTF(getReferer()));
}