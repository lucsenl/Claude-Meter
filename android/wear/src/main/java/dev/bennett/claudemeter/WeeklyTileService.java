package dev.bennett.claudemeter;

import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ProtoLayoutScope;

public final class WeeklyTileService extends ClaudeTileService {
    @Override
    protected LayoutElement tileLayout(DeviceParameters deviceParameters, ProtoLayoutScope scope) {
        // Free-tier accounts report a monthly window instead of a weekly one; the tile
        // follows whichever long-cadence window the subscription currently has.
        return ClaudeTileLayouts.progress(this, deviceParameters,
                ClaudeTileLayouts.longWindowLabel(this),
                ClaudeTileLayouts.longWindow(this), scope);
    }
}
