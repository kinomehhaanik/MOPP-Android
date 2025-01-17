package ee.ria.DigiDoc.android.signature.detail;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.TextUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.ViewSavedState;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.libdigidocpp.Container;
import timber.log.Timber;

@SuppressLint("ViewConstructor")
public final class SignatureDetailView extends CoordinatorLayout {

    private final Navigator navigator;
    private final Toolbar toolbarView;

    private final ViewDisposables disposables = new ViewDisposables();

    public SignatureDetailView(Context context, Signature signature, SignedContainer signedContainer) {
        super(context);

        inflate(context, R.layout.signature_detail_screen, this);
        AccessibilityUtils.setViewAccessibilityPaneTitle(this, R.string.signature_details_title);

        navigator = Application.component(context).navigator();
        toolbarView = findViewById(R.id.toolbar);

        toolbarView.setTitle(R.string.signature_details_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        if (isCertificateInExistence(signature.signingCertificate())) {
            findViewById(R.id.signersCertificateButton).setOnClickListener(view -> navigator.execute(Transaction.push(CertificateDetailScreen.create(signature.signingCertificate()))));
        }

        if (isCertificateInExistence(signature.tsCertificate())) {
            findViewById(R.id.signatureDetailTimestampCertificateButton).setOnClickListener(view -> navigator.execute(Transaction.push(CertificateDetailScreen.create(signature.tsCertificate()))));
        }

        if (isCertificateInExistence(signature.ocspCertificate())) {
            findViewById(R.id.signatureDetailOCSPCertificateButton).setOnClickListener(view -> navigator.execute(Transaction.push(CertificateDetailScreen.create(signature.ocspCertificate()))));
        }

        setData(signature, signedContainer);
    }

    private String getContainerMimeType(SignedContainer signedContainer) {
        Container container = Container.open(signedContainer.file().getAbsolutePath());
        if (container == null) {
            return "";
        }
        return container.mediaType();
    }

    private int getNumberOfFilesInContainer(SignedContainer signedContainer) {
        return signedContainer.dataFiles().size();
    }

    private void setData(Signature signature, SignedContainer signedContainer) {
        TextUtil.handleDetailText((signature.signersCertificateIssuer()), findViewById(R.id.signatureDetailSignersCertificateIssuer));
        TextUtil.handleDetailText((signature.name()).replace(",", ", "), findViewById(R.id.signersCertificateButton));
        TextUtil.handleDetailText((signature.signatureMethod()), findViewById(R.id.signatureDetailMethod));
        TextUtil.handleDetailText((getContainerMimeType(signedContainer)), findViewById(R.id.containerDetailFormat));
        TextUtil.handleDetailText((signature.signatureFormat()), findViewById(R.id.signatureDetailFormat));
        TextUtil.handleDetailText((String.valueOf(getNumberOfFilesInContainer(signedContainer))), findViewById(R.id.containerDetailSignedFileCount));
        TextUtil.handleDetailText((signature.signatureTimestamp()), findViewById(R.id.signatureDetailTimestamp));
        TextUtil.handleDetailText((signature.signatureTimestampUTC()), findViewById(R.id.signatureDetailTimestampUTC));
        TextUtil.handleDetailText((signature.hashValueOfSignature()), findViewById(R.id.signatureDetailHashValue));

        TextUtil.handleDetailText(signature.tsCertificateIssuer(), findViewById(R.id.signatureDetailTimestampCertificateIssuer));
        TextUtil.handleDetailText(signature.tsCertificate() != null ? getX509CertificateSubject(signature.tsCertificate()) : "", findViewById(R.id.signatureDetailTimestampCertificateButton));

        TextUtil.handleDetailText((signature.ocspCertificateIssuer()), findViewById(R.id.signatureDetailOCSPCertificateIssuer));
        TextUtil.handleDetailText(signature.ocspCertificate() != null ? getX509CertificateSubject(signature.ocspCertificate()) : "", findViewById(R.id.signatureDetailOCSPCertificateButton));
        TextUtil.handleDetailText((signature.ocspTime()), findViewById(R.id.signatureDetailOCSPTime));
        TextUtil.handleDetailText((signature.ocspTimeUTC()), findViewById(R.id.signatureDetailOCSPTimeUTC));
        TextUtil.handleDetailText((signature.signersMobileTimeUTC()), findViewById(R.id.signatureDetailSignersMobileTimeUTC));
    }

    private static String getX509CertificateSubject(X509Certificate x509Certificate) {
        try {
            X500Name x500name = new JcaX509CertificateHolder(x509Certificate).getSubject();
            RDN cn = x500name.getRDNs(BCStyle.CN)[0];
            return IETFUtils.valueToString(cn.getFirst().getValue());
        } catch (CertificateEncodingException e) {
            Timber.log(Log.ERROR, e, "Unable to get certificate subject");
            return "";
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    private boolean isCertificateInExistence(X509Certificate certificate) {
        return certificate != null;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return ViewSavedState.onSaveInstanceState(super.onSaveInstanceState(), parcel -> {});
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(ViewSavedState.onRestoreInstanceState(state, parcel -> {}));
    }
}
