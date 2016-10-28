LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, java)

LOCAL_JAVA_LIBRARIES := droidlogic

LOCAL_PACKAGE_NAME := Utility

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res 

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
