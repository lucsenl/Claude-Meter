package dev.lucsenl.claudemeter;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

public final class WearDataLayerService extends WearableListenerService {
    @Override public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem() != null) {
                WearPreferences.apply(event.getDataItem(), getApplicationContext());
            }
        }
    }
}
