package ee.ria.DigiDoc.android.signature.update.mobileid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Locale;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.mobileid.MobileIdMessageException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.DigiDoc.mobileid.dto.response.MobileIdServiceResponse;
import ee.ria.DigiDoc.mobileid.dto.response.RESTServiceFault;
import ee.ria.DigiDoc.sign.SignLib;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import timber.log.Timber;

import static ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest.toJson;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CERTIFICATE_CERT_BUNDLE;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MID_BROADCAST_ACTION;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MID_BROADCAST_TYPE_KEY;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.SERVICE_FAULT;

public final class MobileIdOnSubscribe implements ObservableOnSubscribe<MobileIdResponse> {

    private final Navigator navigator;
    private final SignedContainer container;
    private final Locale locale;
    private final LocalBroadcastManager broadcastManager;
    private final String uuid;
    private final String personalCode;
    private final String phoneNo;

    private final Intent intent;

    public MobileIdOnSubscribe(Navigator navigator, Intent intent, SignedContainer container, Locale locale,
                               String uuid, String personalCode, String phoneNo) {
        this.navigator = navigator;
        this.intent = intent;
        this.container = container;
        this.locale = locale;
        this.broadcastManager = LocalBroadcastManager.getInstance(navigator.activity());
        this.uuid = uuid;
        this.personalCode = personalCode;
        this.phoneNo = phoneNo;
    }

    @Override
    public void subscribe(ObservableEmitter<MobileIdResponse> emitter) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (navigator.activity() == null) {
                    Timber.log(Log.ERROR,"Activity is null");
                    IllegalStateException ise = new IllegalStateException("Activity not found. Please try again after restarting application");
                    emitter.onError(ise);
                }
                switch (intent.getStringExtra(MID_BROADCAST_TYPE_KEY)) {
                    case SERVICE_FAULT:
                        RESTServiceFault fault = RESTServiceFault
                                .fromJson(intent.getStringExtra(SERVICE_FAULT));
                        if (fault.getStatus() != null) {
                            emitter.onError(MobileIdMessageException
                                    .create(navigator.activity(), fault.getStatus(), fault.getDetailMessage()));
                        } else {
                            emitter.onError(MobileIdMessageException
                                    .create(navigator.activity(), fault.getResult(), fault.getDetailMessage()));
                        }
                        break;
                    case CREATE_SIGNATURE_CHALLENGE:
                        String challenge =
                                intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE);
                        emitter.onNext(MobileIdResponse.challenge(challenge));
                        break;
                    case CREATE_SIGNATURE_STATUS:
                        MobileIdServiceResponse status =
                                MobileIdServiceResponse.fromJson(
                                        intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                        switch (status.getStatus()) {
                            case USER_CANCELLED:
                                emitter.onNext(MobileIdResponse.status(status.getStatus()));
                                break;
                            case OK:
                                emitter.onNext(MobileIdResponse.signature(status.getSignature()));
                                emitter.onNext(MobileIdResponse.success(container));
                                emitter.onComplete();
                                break;
                            default:
                                emitter.onError(MobileIdMessageException
                                        .create(navigator.activity(), status.getStatus(), null));
                                break;
                        }
                        break;
                }
            }
        };

        broadcastManager.registerReceiver(receiver, new IntentFilter(MID_BROADCAST_ACTION));
        emitter.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

        ConfigurationProvider configurationProvider =
                ((Application) navigator.activity().getApplication()).getConfigurationProvider();
        String displayMessage = navigator.activity()
                .getString(R.string.signature_update_mobile_id_display_message);
        MobileCreateSignatureRequest request = MobileCreateSignatureRequestHelper
                .create(container, uuid, configurationProvider.getMidRestUrl(),
                        configurationProvider.getMidSkRestUrl(), locale, personalCode, phoneNo, displayMessage);

        intent.putExtra(CREATE_SIGNATURE_REQUEST, toJson(request));
        intent.putExtra(ACCESS_TOKEN_PASS, SignLib.accessTokenPass());
        intent.putExtra(ACCESS_TOKEN_PATH, SignLib.accessTokenPath());
        intent.putStringArrayListExtra(CERTIFICATE_CERT_BUNDLE,
                new ArrayList<>(configurationProvider.getCertBundle()));
        navigator.activity().startService(intent);
    }
}
