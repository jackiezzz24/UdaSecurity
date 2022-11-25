module Security {
    requires Image;
    requires com.miglayout.swing;
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    opens com.udacity.security.data to com.google.gson;
}