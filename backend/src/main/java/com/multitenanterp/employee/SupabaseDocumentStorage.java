package com.multitenanterp.employee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

@Component
@ConditionalOnProperty(name="app.storage.provider",havingValue="supabase")
public class SupabaseDocumentStorage implements DocumentStorage {
    private final RestClient client;
    private final String storageUrl;
    private final String bucket;

    public SupabaseDocumentStorage(RestClient.Builder builder,
                                   @Value("${app.storage.supabase.url}") String projectUrl,
                                   @Value("${app.storage.supabase.service-key}") String serviceKey,
                                   @Value("${app.storage.supabase.bucket:employee-documents}") String bucket) {
        if (!bucket.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid Supabase storage bucket");
        this.bucket = bucket;
        this.storageUrl = projectUrl.replaceAll("/+$", "") + "/storage/v1";
        this.client = builder
                .defaultHeader("apikey",serviceKey).defaultHeader("Authorization","Bearer " + serviceKey).build();
    }

    @Override public void store(String key, InputStream content) throws IOException {
        try {
            client.post().uri(objectUri("/object/",key)).contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("x-upsert","false").body(content.readAllBytes()).retrieve().toBodilessEntity();
        } catch (RestClientException exception) { throw new IOException("Supabase Storage upload failed",exception); }
    }

    @Override public Resource load(String key) {
        try {
            byte[] content=client.get().uri(objectUri("/object/authenticated/",key)).retrieve().body(byte[].class);
            return new ByteArrayResource(content==null?new byte[0]:content);
        } catch (RestClientException exception) { return new MissingResource(); }
    }

    @Override public void delete(String key) throws IOException {
        try {
            client.method(org.springframework.http.HttpMethod.DELETE).uri(URI.create(storageUrl+"/object/"+bucket))
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("prefixes",new String[]{key})).retrieve().toBodilessEntity();
        } catch (RestClientException exception) { throw new IOException("Supabase Storage delete failed",exception); }
    }

    private URI objectUri(String operation,String key){
        if(!key.matches("[A-Za-z0-9_-]+/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid storage key");
        return URI.create(storageUrl+operation+bucket+"/"+key);
    }

    private static final class MissingResource extends ByteArrayResource {
        private MissingResource(){super(new byte[0]);}
        @Override public boolean exists(){return false;}
    }
}
