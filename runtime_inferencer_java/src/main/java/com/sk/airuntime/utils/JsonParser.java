package com.sk.airuntime.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class JsonParser {

    private ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> parse(String json) throws JsonProcessingException {
        Map<String, Object> resultMap =  mapper.readValue(json, DataMap.class);
        return resultMap;
    }

     public String mapToJsonStr(Map<String, ?> map) throws JsonProcessingException {
         return mapper.writeValueAsString(map);
    }

}

class DataMap extends HashMap<String, Object> {
}
