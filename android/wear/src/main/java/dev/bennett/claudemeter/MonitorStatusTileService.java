package dev.bennett.claudemeter;

import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ProtoLayoutScope;

public final class MonitorStatusTileService extends ClaudeTileService {
    @Override
    protected LayoutElement tileLayout(DeviceParameters deviceParameters, ProtoLayoutScope scope) {
        return ClaudeTileLayouts.monitor(this, deviceParameters, scope);
    }
}
