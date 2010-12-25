LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
	com.tmobile.themes

LOCAL_PACKAGE_NAME := ThemeChooser

include $(BUILD_PACKAGE)

# including the test apk
include $(call all-makefiles-under,$(LOCAL_PATH))
