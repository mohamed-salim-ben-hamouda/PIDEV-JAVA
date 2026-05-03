package com.pidev.utils;

import javafx.scene.image.Image;

public final class ImageAssets {
    private static final String TROPHY_PATH = "/images/trophy.jpg";

    public static final Image TROPHY_ICON_60 = load(TROPHY_PATH, 60, 60);
    public static final Image TROPHY_ICON_80 = load(TROPHY_PATH, 80, 80);

    private ImageAssets() {
    }

    private static Image load(String path, double requestedWidth, double requestedHeight) {
        return new Image(
                ImageAssets.class.getResource(path).toExternalForm(),
                requestedWidth,
                requestedHeight,
                true,
                true,
                true
        );
    }
}
