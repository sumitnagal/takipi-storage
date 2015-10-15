package com.takipi.oss.storage.resources;

import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.takipi.oss.storage.data.EncodingType;
import com.takipi.oss.storage.data.MultiResponse;
import com.takipi.oss.storage.data.MutliRequest;
import com.takipi.oss.storage.fs.Record;
import com.takipi.oss.storage.fs.api.Filesystem;

@Path("/storage/v1/json/multi")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JsonMultiStorageResource {
    private static final Logger logger = LoggerFactory.getLogger(JsonMultiStorageResource.class);

    protected Filesystem fs;

    public JsonMultiStorageResource(Filesystem fs) {
        this.fs = fs;
    }

    @POST
    @Timed
    public Response post(MutliRequest keys) {
        try {
            MultiResponse response = handleResponse(keys);

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError().entity("Problem getting keys").build();
        }
    }

    private MultiResponse handleResponse(MutliRequest request) {
        Map<Record, String> records = Maps.newHashMap();

        for (Record record : request.records) {
            try {
                InputStream is = fs.get(record);
                records.put(record, encode(request.encodingType, is));
            } catch (Exception e) {
                logger.error("Problem with record " + record, e);
            }
        }

        MultiResponse response = new MultiResponse();
        ObjectMapper mapper = new ObjectMapper(); // Jackson doesn't like Map<Object, ..> keys and it does toString rather then serialization

        for (Map.Entry<Record, String> entry : records.entrySet()) {
            Record record = entry.getKey();
            String value = entry.getValue();

            try {
                response.records.put(mapper.writeValueAsString(record), value);
            } catch (Exception e) {
                logger.error("Problem with json for record " + record, e);
            }
        }

        return response;
    }

    private String encode(EncodingType type, InputStream is) throws Exception {
        switch (type) {
            case PLAIN:
            case JSON: {
                return IOUtils.toString(is);
            }
            case BINARY: {
                throw new UnsupportedOperationException("not yet implemented");
                // byte[] bytes = IOUtils.toByteArray(is);
                // Base64Coder.encode(bytes);
            }
        }

        throw new Exception("problem encoding");
    }
}