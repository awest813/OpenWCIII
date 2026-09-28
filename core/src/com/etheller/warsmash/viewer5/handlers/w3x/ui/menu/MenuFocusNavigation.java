package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import java.util.List;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.FocusableFrame;

public final class MenuFocusNavigation {
    private MenuFocusNavigation() { }

    public static FocusableFrame next(List<FocusableFrame> frames, FocusableFrame current, boolean backwards) {
        if (frames.isEmpty()) return null;
        int index = current == null ? -1 : frames.indexOf(current);
        if (index < 0) index = backwards ? 0 : -1;
        for (int i = 0; i < frames.size(); i++) {
            index = Math.floorMod(index + (backwards ? -1 : 1), frames.size());
            final FocusableFrame candidate = frames.get(index);
            if (candidate.isVisibleOnScreen() && candidate.isFocusable()) return candidate;
        }
        return null;
    }
}
