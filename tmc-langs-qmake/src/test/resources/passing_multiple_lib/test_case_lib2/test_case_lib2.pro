QT -= gui

TARGET = test_case_lib2
TEMPLATE = lib
CONFIG += staticlib

win32 {
    CONFIG -= debug_and_release debug_and_release_target
}

DEFINES += QT_DEPRECATED_WARNINGS

SOURCES += \
        test_case_lib2.cpp

HEADERS += \
        test_case_lib2.h

unix {
	target.path = /usr/lib
	INSTALLS += target
}
