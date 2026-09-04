package com.multitenanterp.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseDocumentStorageTest {
    private MockRestServiceServer server;
    private SupabaseDocumentStorage storage;

    @BeforeEach void setUp() {
        RestClient.Builder builder=RestClient.builder();
        server=MockRestServiceServer.bindTo(builder).build();
        storage=new SupabaseDocumentStorage(builder,"https://project.supabase.co","server-secret","employee-documents");
    }

    @Test void uploadsDownloadsAndDeletesThroughAuthenticatedStorageApi() throws Exception {
        String key="tenant-id/employee-id/document-id";
        server.expect(once(),requestTo("https://project.supabase.co/storage/v1/object/employee-documents/"+key))
                .andExpect(method(HttpMethod.POST)).andExpect(header("apikey","server-secret"))
                .andExpect(header("Authorization","Bearer server-secret")).andExpect(content().bytes("content".getBytes()))
                .andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        server.expect(once(),requestTo("https://project.supabase.co/storage/v1/object/authenticated/employee-documents/"+key))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization","Bearer server-secret"))
                .andRespond(withSuccess("content",MediaType.APPLICATION_OCTET_STREAM));
        server.expect(once(),requestTo("https://project.supabase.co/storage/v1/object/employee-documents"))
                .andExpect(method(HttpMethod.DELETE)).andExpect(content().json("{\"prefixes\":[\""+key+"\"]}"))
                .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));

        storage.store(key,new ByteArrayInputStream("content".getBytes()));
        assertThat(storage.load(key).getContentAsByteArray()).isEqualTo("content".getBytes());
        storage.delete(key);
        server.verify();
    }
}
