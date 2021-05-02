package org.apereo.cas.gauth.credential;

import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.util.CompressionUtils;
import org.apereo.cas.util.serialization.AbstractJacksonBackedStringSerializer;
import org.apereo.cas.web.BaseCasActuatorEndpoint;

import lombok.val;
import org.jooq.lambda.Unchecked;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

/**
 * This is {@link GoogleAuthenticatorTokenCredentialRepositoryEndpoint}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@RestControllerEndpoint(id = "gauthCredentialRepository", enableByDefault = false)
public class GoogleAuthenticatorTokenCredentialRepositoryEndpoint extends BaseCasActuatorEndpoint {
    private final OneTimeTokenCredentialRepository repository;

    public GoogleAuthenticatorTokenCredentialRepositoryEndpoint(final CasConfigurationProperties casProperties,
                                                                final OneTimeTokenCredentialRepository repository) {
        super(casProperties);
        this.repository = repository;
    }

    /**
     * Get one time token account.
     *
     * @param username the username
     * @return the one time token account
     */
    @GetMapping(path = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<? extends OneTimeTokenAccount> get(@PathVariable final String username) {
        return repository.get(username);
    }

    /**
     * Load collection.
     *
     * @return the collection
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<? extends OneTimeTokenAccount> load() {
        return repository.load();
    }

    /**
     * Delete.
     *
     * @param username the username
     */
    @DeleteMapping(path = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@PathVariable final String username) {
        repository.delete(username);
    }

    /**
     * Delete all.
     */
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public void deleteAll() {
        repository.deleteAll();
    }

    /**
     * Export accounts.
     *
     * @return the web endpoint response
     */
    @GetMapping(path = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> exportAccounts() {
        val accounts = repository.load();
        val serializer = new GoogleAuthenticatorAccountSerializer();
        val resource = CompressionUtils.toZipFile(accounts.stream(),
            Unchecked.function(entry -> {
                val acct = (GoogleAuthenticatorAccount) entry;
                val fileName = String.format("%s-%s", acct.getName(), acct.getId());
                val sourceFile = File.createTempFile(fileName, ".json");
                serializer.to(sourceFile, acct);
                return sourceFile;
            }), "gauthaccts");
        val headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(Objects.requireNonNull(resource.getFilename())).build());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    private static class GoogleAuthenticatorAccountSerializer extends AbstractJacksonBackedStringSerializer<GoogleAuthenticatorAccount> {
        private static final long serialVersionUID = 1466569521275630254L;

        @Override
        public Class<GoogleAuthenticatorAccount> getTypeToSerialize() {
            return GoogleAuthenticatorAccount.class;
        }
    }
}
