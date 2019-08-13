package ee.ria.DigiDoc.configuration.loader;

import ee.ria.DigiDoc.configuration.SignatureVerifier;

public abstract class ConfigurationLoader {

    private String configurationJson;
    private String configurationSignature;
    private String configurationSignaturePublicKey;

    public void load() {
        this.configurationJson = loadConfigurationJson().trim();
        this.configurationSignature = loadConfigurationSignature().trim();
        this.configurationSignaturePublicKey = loadConfigurationSignaturePublicKey().trim();

        verifyConfigurationSignature();
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public String getConfigurationSignature() {
        return configurationSignature;
    }

    public String getConfigurationSignaturePublicKey() {
        return configurationSignaturePublicKey;
    }

    abstract String loadConfigurationJson();

    abstract String loadConfigurationSignature();

    abstract String loadConfigurationSignaturePublicKey();

    private void verifyConfigurationSignature() {
        boolean signatureValid = SignatureVerifier.verify(configurationSignature, configurationSignaturePublicKey, configurationJson);
        if (!signatureValid) {
            throw new IllegalStateException("Configuration signature validation failed!");
        }
    }
}
