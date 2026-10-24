package ru.tihiy.dom;

final class Palette {
    final boolean light;
    final int bgTop, bgBottom, text, muted, muted2;
    final int card, cardFocus, cardBorder;
    final int focus, accent;
    final int dock, dockBorder, panel, panelBorder, overlay;
    final int scrimBase;

    private Palette(boolean light, int bgTop, int bgBottom, int text, int muted, int muted2,
                    int card, int cardFocus, int cardBorder, int focus, int accent,
                    int dock, int dockBorder, int panel, int panelBorder, int overlay, int scrimBase) {
        this.light = light;
        this.bgTop = bgTop;
        this.bgBottom = bgBottom;
        this.text = text;
        this.muted = muted;
        this.muted2 = muted2;
        this.card = card;
        this.cardFocus = cardFocus;
        this.cardBorder = cardBorder;
        this.focus = focus;
        this.accent = accent;
        this.dock = dock;
        this.dockBorder = dockBorder;
        this.panel = panel;
        this.panelBorder = panelBorder;
        this.overlay = overlay;
        this.scrimBase = scrimBase;
    }

    static Palette of(boolean light) {
        if (light) {
            return new Palette(true,
                    0xFFF3F6FB, 0xFFE4EAF4,
                    0xFF10141C, 0xFF5C6678, 0xFF8B94A6,
                    0xF2FFFFFF, 0xFFFFFFFF, 0x2210141C,
                    0xFF1E4FD6, 0xFF3568FF,
                    0xF2FFFFFF, 0x1410141C,
                    0xFFF7F9FD, 0x1410141C,
                    0x66000000, 0xFFEEF1F6);
        }
        return new Palette(false,
                0xFF0F1724, 0xFF05070C,
                0xFFF3F6FB, 0xFF8B95A8, 0xFF5D6678,
                0xB80B0F14, 0xD9121A26, 0x26FFFFFF,
                0xFFDCE7FF, 0xFF7AA2FF,
                0xCC0E1219, 0x14FFFFFF,
                0xFF0E1117, 0x14FFFFFF,
                0x99000000, 0xFF000000);
    }
}
