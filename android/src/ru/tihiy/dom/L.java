package ru.tihiy.dom;

final class L {
    static boolean en = true;

    private L() {}

    static String s(String english, String russian) {
        return en ? english : russian;
    }
}
