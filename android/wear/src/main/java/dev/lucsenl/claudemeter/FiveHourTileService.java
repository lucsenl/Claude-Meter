package dev.lucsenl.claudemeter;

import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ProtoLayoutScope;

public final class FiveHourTileService extends ClaudeTileService {
    @Override
    protected LayoutElement tileLayout(DeviceParameters deviceParameters, ProtoLayoutScope scope) {
        return ClaudeTileLayouts.progress(this, deviceParameters, "5-hour",
                ClaudeTileLayouts.fiveHour(this), scope);
    }
}
