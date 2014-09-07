TEMPLATE = app
CONFIG += console
CONFIG -= app_bundle
CONFIG -= qt

SOURCES += \
    udpee.cpp

CCFLAG = -g3

DESTDIR = .
OBJECTS_DIR = ./build/local

OTHER_FILES=.astylerc

