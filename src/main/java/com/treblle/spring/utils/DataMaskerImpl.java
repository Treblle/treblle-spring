package com.treblle.spring.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.treblle.spring.configuration.TreblleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class DataMaskerImpl implements DataMasker {

  private static final Logger log = LoggerFactory.getLogger(DataMaskerImpl.class);

  private static final String MASKED_VALUE = "******";

  private static final List<String> DEFAULT_KEYWORDS =
          Arrays.asList(
                  "password",
                  "pwd",
                  "secret",
                  "password_confirmation",
                  "cc",
                  "card_number",
                  "ccv",
                  "ssn",
                  "credit_score");

  private Pattern pattern;

  public DataMaskerImpl(TreblleProperties properties) {
    Set<String> keywords = new HashSet<>(9);
    keywords.addAll(DEFAULT_KEYWORDS);
    keywords.addAll(properties.getMaskingKeywords());

    String regex = keywords.stream().map(it -> "\\b" + it + "\\b").collect(Collectors.joining("|"));

    try {
      pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException exception) {
      log.error("Error while compiling regex with custom keywords. Continuing with default.");
      String defaultRegex = DEFAULT_KEYWORDS.stream().map(it -> "\\b" + it + "\\b").collect(Collectors.joining("|"));
      pattern = Pattern.compile(defaultRegex, Pattern.CASE_INSENSITIVE);
    }
  }

  @Override
  public JsonNode mask(JsonNode node) {
    return maskInternal(null, node);
  }

  @Override
  public Map<String, String> mask(Map<String, String> headers) {
    return headers.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              if (pattern.matcher(entry.getKey()).matches() && Objects.nonNull(entry.getValue())) {
                return MASKED_VALUE;
              } else {
                return entry.getValue();
              }
            }
    ));
  }

  private JsonNode maskInternal(String key, JsonNode target) {
    if (target.isTextual() && key != null && pattern.matcher(key).matches()) {
      return new TextNode(MASKED_VALUE);
    }
    if (target.isObject()) {
      Iterator<Entry<String, JsonNode>> fields = target.fields();
      while (fields.hasNext()) {
        Entry<String, JsonNode> field = fields.next();
        ((ObjectNode) target).replace(field.getKey(), maskInternal(field.getKey(), field.getValue()));
      }
    }
    if (target.isArray()) {
      for (int index = 0; index < target.size(); index++) {
        ((ArrayNode) target).set(index, maskInternal(key, target.get(index)));
      }
    }
    return target;
  }

}
