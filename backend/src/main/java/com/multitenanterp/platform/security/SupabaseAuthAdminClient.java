package com.multitenanterp.platform.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class SupabaseAuthAdminClient {
    private final RestClient.Builder builder;
    private final String projectUrl;
    private final String secretKey;

    public SupabaseAuthAdminClient(RestClient.Builder builder,
                                   @Value("${app.auth.supabase.url:}") String projectUrl,
                                   @Value("${app.auth.supabase.secret-key:}") String secretKey) {
        this.builder=builder;
        this.projectUrl=projectUrl.replaceAll("/+$","");
        this.secretKey=secretKey;
    }

    public UUID invite(String email,String displayName){
        requireConfigured();
        try {
            AuthUser response=client().post().uri(URI.create(projectUrl+"/auth/v1/invite"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email",email,"data",Map.of("display_name",displayName)))
                    .retrieve().body(AuthUser.class);
            if(response==null||response.id()==null)throw new ResponseStatusException(BAD_GATEWAY,"Supabase returned no user identifier");
            return response.id();
        } catch(RestClientException exception){
            throw new ResponseStatusException(BAD_GATEWAY,"Supabase could not invite the user",exception);
        }
    }

    public void delete(UUID authUserId){
        if(projectUrl.isBlank()||secretKey.isBlank())return;
        try {client().delete().uri(URI.create(projectUrl+"/auth/v1/admin/users/"+authUserId)).retrieve().toBodilessEntity();}
        catch(RestClientException ignored){/* Best-effort compensation; the original provisioning failure is preserved. */}
    }

    private RestClient client(){return builder.defaultHeader("apikey",secretKey)
            .defaultHeader("Authorization","Bearer "+secretKey).build();}
    private void requireConfigured(){if(projectUrl.isBlank()||secretKey.isBlank())
        throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Supabase account provisioning is not configured");}

    @JsonIgnoreProperties(ignoreUnknown=true)
    private record AuthUser(UUID id){}
}
