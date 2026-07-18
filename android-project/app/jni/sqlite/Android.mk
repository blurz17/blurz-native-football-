LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := sqlite3
LOCAL_SRC_FILES := sqlite-amalgamation-3450300/sqlite3.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/sqlite-amalgamation-3450300
LOCAL_CFLAGS    := -O3 -fPIC
include $(BUILD_STATIC_LIBRARY)
