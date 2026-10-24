package ru.tihiy.dom;

final class SceneInfo {
    final String id;
    final String en;
    final String ru;

    SceneInfo(String id, String en, String ru) {
        this.id = id;
        this.en = en;
        this.ru = ru;
    }

    String label() {
        return L.en ? en : ru;
    }
}
