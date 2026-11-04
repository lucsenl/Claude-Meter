package dev.lucsenl.claudemeter;

import dev.lucsenl.claudemeter.SamsungLockWidgetSupport;

/* JADX INFO: loaded from: classes.dex */
public final class SamsungLockSquareWidget extends SamsungLockWidgetProvider {
    @Override // dev.lucsenl.claudemeter.SamsungLockWidgetProvider
    protected SamsungLockWidgetSupport.Shape shape() {
        return SamsungLockWidgetSupport.Shape.SQUARE;
    }

    @Override // dev.lucsenl.claudemeter.SamsungLockWidgetProvider
    protected SamsungLockWidgetSupport.Style style() {
        return SamsungLockWidgetSupport.Style.NUMBERS;
    }
}
