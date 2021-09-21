package com.treblle.spring.utils;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonMasker {

  JsonNode mask(JsonNode node);
}
