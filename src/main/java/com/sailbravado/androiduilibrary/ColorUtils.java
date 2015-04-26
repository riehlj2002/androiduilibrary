package com.sailbravado.androiduilibrary;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.TypedValue;

/**
 * Static helper functions for working with colors.
 * Created by John on 3/7/2015.
 */
public class ColorUtils {
    /**
     * Returns the background color for a given theme.
     * @param theme The theme for which to get the background color (must not be null).
     * @return the background color in AARRGGBB format
     * @throws RuntimeException if the theme doesn't specify the <code>colorBackground</code>
     * attribute
     */
    public static int themeBackgroundColor(@NonNull Resources.Theme theme) throws RuntimeException {
        // get the background color of the current theme amd the current screen orientation in order
        // to determine what to put on the buttons
        TypedValue themeBackgroundColor = new TypedValue();
        int backgroundColor;

        if (theme.resolveAttribute(android.R.attr.colorBackground, themeBackgroundColor, true)) {
            switch (themeBackgroundColor.type) {
                case TypedValue.TYPE_INT_COLOR_ARGB4:
                    backgroundColor = Color.argb(
                            (themeBackgroundColor.data & 0xf000) >> 8,
                            (themeBackgroundColor.data & 0xf00) >> 4,
                            themeBackgroundColor.data & 0xf0,
                            (themeBackgroundColor.data & 0xf) << 4);
                    break;

                case TypedValue.TYPE_INT_COLOR_RGB4:
                    backgroundColor = Color.rgb(
                            (themeBackgroundColor.data & 0xf00) >> 4,
                            themeBackgroundColor.data & 0xf0,
                            (themeBackgroundColor.data & 0xf) << 4);
                    break;

                case TypedValue.TYPE_INT_COLOR_ARGB8:
                    backgroundColor = themeBackgroundColor.data;
                    break;

                case TypedValue.TYPE_INT_COLOR_RGB8:
                    backgroundColor = Color.rgb(
                            (themeBackgroundColor.data & 0xff0000) >> 16,
                            (themeBackgroundColor.data & 0xff00) >> 8,
                            themeBackgroundColor.data & 0xff);
                    break;

                default:
                    throw new RuntimeException("ColorUtils.themeBackgroundColor: couldn't parse " +
                            "theme background color attribute " + themeBackgroundColor.toString());
            }
        } else {
            throw new RuntimeException("ColorUtils.themeBackgroundColor: couldn't find " +
                    "background color in theme " + theme.toString());
        }

        return backgroundColor;
    }

    /**
     * Determines whether a color is "dark"
     * @param color the color to test
     * @return if <code>true</code>, the given color is dark.
     */
    public static boolean isDark(int color) {
        // formula taken from http://stackoverflow.com/questions/946544/good-text-foreground-color-for-a-given-background-color
        return (Color.red(color) * 0.299 +
                Color.green(color) * 0.587 +
                Color.blue(color) * 0.114) < 186.0;
    }
}
