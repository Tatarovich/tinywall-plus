package ru.tihiy.dom;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

final class AppInfo {
    final String label;
    final String pkg;
    final ComponentName component;
    final Drawable icon;
    final String key;

    AppInfo(String label, String pkg, ComponentName component, Drawable icon) {
        this.label = label;
        this.pkg = pkg;
        this.component = component;
        this.icon = icon;
        this.key = component.flattenToString();
    }
}
