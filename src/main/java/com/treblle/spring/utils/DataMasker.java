package com.treblle.spring.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface DataMasker {

  JsonNode mask(JsonNode node);

  Map<String, String> mask(Map<String, String> headers);

}
