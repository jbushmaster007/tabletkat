package org.exalm.tabletkat.quicksettings;

import android.content.Context;

import java.util.HashMap;

import de.robv.android.xposed.XposedHelpers;

public class RowFactory {
    private static HashMap<String, Class> classMapping;

    public static Row createRowFromString(Context c, String id){
        Class cl = classMapping.get(id);
        if (cl == null){
            return null;
        }
        return (Row) XposedHelpers.newInstance(cl, c);
    }

    static {
        classMapping = new HashMap<String, Class>();
        classMapping.put("airplane", RowAirplane.class);
        classMapping.put("bluetooth", RowBluetooth.class);
        classMapping.put("brightness", RowBrightness.class);
        classMapping.put("dnd", RowDnd.class);
        classMapping.put("location", RowLocation.class);
        classMapping.put("rotate", RowRotate.class);
        classMapping.put("settings", RowSettings.class);
        classMapping.put("wifi", RowWifi.class);
        classMapping.put("wifi-switch", RowWifiSwitch.class);

        //Android-x86
        classMapping.put("poweroff", RowPowerOff.class);
    }
}
