/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.rmen.android.networkmonitor.app.service.datasources;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;


import ca.rmen.android.networkmonitor.util.Log;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.PermissionUtil;

/**
 * Retrieves information from the current cell we are connected to.
 */
public class CellLocationDataSource implements NetMonDataSource {

    private static final String TAG = Constants.TAG + CellLocationDataSource.class.getSimpleName();
    /**
     * @see {@link CdmaCellLocation#getBaseStationLatitude()}
     */
    private static final int CDMA_COORDINATE_DIVISOR = 3600 * 4;
    private TelephonyManager mTelephonyManager;
    private Context mContext;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mContext = context;
    }

    @Override
    public void onDestroy() {}

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues();
        if (!PermissionUtil.hasLocationPermission(mContext)) {
            Log.d(TAG, "No location permissions");
            return values;
        }
        CellLocation cellLocation = mTelephonyManager.getCellLocation();
        if (cellLocation instanceof GsmCellLocation) {
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            int cid = gsmCellLocation.getCid();
            // The javadoc says the cell id should be less than FFFF, but this
            // isn't always so. We'll report both the full cell id returned by
            // Android, and the truncated one (taking only the last 2 bytes).
            int shortCid = cid > 0 ? cid & 0xFFFF : cid;
            int rnc = cid > 0 ? cid >> 16 & 0xFFFF : 0;
            values.put(NetMonColumns.GSM_FULL_CELL_ID, cid);
            if (rnc > 0) values.put(NetMonColumns.GSM_RNC, rnc);
            values.put(NetMonColumns.GSM_SHORT_CELL_ID, shortCid);
            values.put(NetMonColumns.GSM_CELL_LAC, gsmCellLocation.getLac());
            if (Build.VERSION.SDK_INT >= 9) values.put(NetMonColumns.GSM_CELL_PSC, getPsc(gsmCellLocation));
        } else if (cellLocation instanceof CdmaCellLocation) {
            CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
            values.put(NetMonColumns.CDMA_CELL_BASE_STATION_ID, cdmaCellLocation.getBaseStationId());
            int latitude = cdmaCellLocation.getBaseStationLatitude();
            if (latitude < Integer.MAX_VALUE) {
                values.put(NetMonColumns.CDMA_CELL_LATITUDE, (double) latitude / CDMA_COORDINATE_DIVISOR);
            }
            int longitude = cdmaCellLocation.getBaseStationLongitude();
            if (longitude < Integer.MAX_VALUE) {
                values.put(NetMonColumns.CDMA_CELL_LONGITUDE, (double) longitude / CDMA_COORDINATE_DIVISOR);
            }
            values.put(NetMonColumns.CDMA_CELL_NETWORK_ID, cdmaCellLocation.getNetworkId());
            values.put(NetMonColumns.CDMA_CELL_SYSTEM_ID, cdmaCellLocation.getSystemId());
        }
        return values;
    }

    @TargetApi(9)
    private int getPsc(GsmCellLocation gsmCellLocation) {
        return gsmCellLocation.getPsc();
    }
}
