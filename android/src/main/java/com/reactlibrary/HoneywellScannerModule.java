package com.reactlibrary;

import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.honeywell.aidc.*;
import com.honeywell.aidc.AidcManager.CreatedCallback;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.reactlibrary.HoneywellScannerPackage.HoneyWellTAG;

public class HoneywellScannerModule extends ReactContextBaseJavaModule implements BarcodeReader.BarcodeListener {

    // Debugging
    private static final boolean D = true;

    private final ReactApplicationContext reactContext;
    private AidcManager manager;
    private BarcodeReader reader;

    private static final String BARCODE_READ_SUCCESS = "barcodeReadSuccess";
    private static final String BARCODE_READ_FAIL = "barcodeReadFail";

    public HoneywellScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "HoneywellScanner";
    }

    /**
     * Send event to javascript
     *
     * @param eventName Name of the event
     * @param params    Additional params
     */
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        if (reactContext.hasActiveCatalystInstance()) {
            if (D) Log.d(HoneyWellTAG, "Sending event: " + eventName);
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent) {
        if (D) Log.d(HoneyWellTAG, "HoneywellBarcodeReader - Barcode scan read");
        WritableMap params = Arguments.createMap();
        params.putString("data", barcodeReadEvent.getBarcodeData());
        sendEvent(BARCODE_READ_SUCCESS, params);
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {
        if (D) Log.d(HoneyWellTAG, "HoneywellBarcodeReader - Barcode scan failed");
        sendEvent(BARCODE_READ_FAIL, null);
    }

    /*******************************/
    /** Methods Available from JS **/
    /*******************************/

    @ReactMethod
    public void startReader(final Promise promise) {
        AidcManager.create(reactContext, new CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                try {
                    reader = manager.createBarcodeReader();
                } catch (InvalidScannerNameException e) {
                    e.printStackTrace();
                }
                if (reader != null) {
                    reader.addBarcodeListener(HoneywellScannerModule.this);
                    try {
                        reader.claim();
                        reader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);

                        reader.setProperty(BarcodeReader.PROPERTY_EAN_8_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                        reader.setProperty(BarcodeReader.PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                        promise.resolve(true);
                        Map<String, Object> properties = new HashMap<>();
                        properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
                        properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
                        properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
                        properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
                        properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
                        properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
                        properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, false);
                        properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, false);
                        properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, false);
                        properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, false);
                        properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, false);
                        // Set Max Code 39 barcode length
                        properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
                        // Turn on center decoding
                        properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
                        // Enable bad read response
                        properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
                        // Apply the settings
                        reader.setProperties(properties);
                    } catch (ScannerUnavailableException | UnsupportedPropertyException e) {
                        promise.resolve(false);
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @ReactMethod
    public void stopReader(Promise promise) {
        if (reader != null) {
            reader.close();
        }
        if (manager != null) {
            manager.close();
        }
        promise.resolve(null);
    }

    private boolean isCompatible() {
        // This... is not optimal. Need to find a better way to performantly check whether device has a Honeywell scanner
        return Build.BRAND.toLowerCase().contains("honeywell");
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("BARCODE_READ_SUCCESS", BARCODE_READ_SUCCESS);
        constants.put("BARCODE_READ_FAIL", BARCODE_READ_FAIL);
        constants.put("isCompatible", isCompatible());
        return constants;
    }


}
