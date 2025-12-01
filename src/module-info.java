module com.cursorflow {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires jna;
    requires jna.platform;
    requires slf4j.api;
    requires slf4j.simple;

    exports com.cursorflow;
    exports com.cursorflow.core;
    exports com.cursorflow.effect;
}