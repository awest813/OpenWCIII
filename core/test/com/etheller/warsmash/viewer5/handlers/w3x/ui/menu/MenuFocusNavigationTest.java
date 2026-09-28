package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.FocusableFrame;

class MenuFocusNavigationTest {
    @Test void tabWrapsAndShiftTabReversesSkippingUnavailableFields() {
        final FocusableFrame first = frame(true, true);
        final FocusableFrame hidden = frame(false, true);
        final FocusableFrame disabled = frame(true, false);
        final FocusableFrame last = frame(true, true);
        final List<FocusableFrame> frames = List.of(first, hidden, disabled, last);
        assertSame(last, MenuFocusNavigation.next(frames, first, false));
        assertSame(first, MenuFocusNavigation.next(frames, last, false));
        assertSame(last, MenuFocusNavigation.next(frames, first, true));
        assertSame(first, MenuFocusNavigation.next(frames, last, true));
        assertSame(first, MenuFocusNavigation.next(frames, null, false));
        assertSame(last, MenuFocusNavigation.next(frames, null, true));
    }

    @Test void emptyOrUnavailableScreensHaveNoFocusTarget() {
        assertNull(MenuFocusNavigation.next(List.of(), null, false));
        assertNull(MenuFocusNavigation.next(List.of(frame(false, true), frame(true, false)), null, true));
        final FocusableFrame only = frame(true, true);
        assertSame(only, MenuFocusNavigation.next(List.of(only), only, false));
    }

    private static FocusableFrame frame(boolean visible, boolean focusable) {
        return (FocusableFrame) Proxy.newProxyInstance(FocusableFrame.class.getClassLoader(),
                new Class<?>[] {FocusableFrame.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "isVisibleOnScreen": return visible;
                    case "isFocusable": return focusable;
                    case "equals": return proxy == args[0];
                    case "toString": return "focus fixture";
                    default: throw new AssertionError(method.getName());
                    }
                });
    }
}
