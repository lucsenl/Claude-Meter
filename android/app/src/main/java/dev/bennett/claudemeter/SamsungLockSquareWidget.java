package dev.bennett.claudemeter;

import dev.bennett.claudemeter.SamsungLockWidgetSupport;

/* JADX INFO: loaded from: classes.dex */
public final class SamsungLockSquareWidget extends SamsungLockWidgetProvider {
    @Override // dev.bennett.claudemeter.SamsungLockWidgetProvider
    protected SamsungLockWidgetSupport.Shape shape() {
        return SamsungLockWidgetSupport.Shape.SQUARE;
    }

    @Override // dev.bennett.claudemeter.SamsungLockWidgetProvider
    protected SamsungLockWidgetSupport.Style style() {
        return SamsungLockWidgetSupport.Style.NUMBERS;
    }
}
