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
                  "passwordConfirmation",
                  "cc",
                  "card_number",
                  "cardNumber",
                  "ccv",
                  "ssn",
                  "credit_score",
                  "creditScore",
                  "api_key"
          );

  private Pattern pattern;
  private Pattern catchAllPattern;

  public DataMaskerImpl(TreblleProperties properties) {
    Set<String> keywords = new HashSet<>(9);
    keywords.addAll(DEFAULT_KEYWORDS);
    keywords.addAll(properties.getMaskingKeywords());

    String mergedPattern = keywords.stream()
            .filter(it -> !it.endsWith(".*"))
            .collect(Collectors.joining("|"));

    try {
      pattern = Pattern.compile("^(" + mergedPattern + ")$", Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException exception) {
      log.error("Error while compiling regex with custom keywords. Continuing with default pattern.", exception);
      String defaultRegex = DEFAULT_KEYWORDS.stream().map(it -> "\\b" + it + "\\b").collect(Collectors.joining("|"));
      pattern = Pattern.compile(defaultRegex, Pattern.CASE_INSENSITIVE);
    }

    String mergedCatchAllPattern = keywords.stream()
            .filter(it -> it.endsWith(".*"))
            .map(this::removeCatchAllSuffix)
            .collect(Collectors.joining("|"));

    try {
      catchAllPattern = Pattern.compile("^(" + mergedCatchAllPattern + ")$", Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException exception) {
      log.error("Error while compiling catch all regex with custom keywords. Continuing with empty pattern.", exception);
      catchAllPattern = null;
    }
  }

  private String removeCatchAllSuffix(String input) {
    return input.substring(0, input.length() - ".*".length());
  }

  private boolean matchesMaskingKeywords(String key) {
    return pattern.matcher(key).matches();
  }

  private boolean matchesCatchAllMaskingKeywords(String key) {
    if (catchAllPattern == null) {
      return false;
    }
    return catchAllPattern.matcher(key).matches();
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
              if (matchesMaskingKeywords(entry.getKey()) && Objects.nonNull(entry.getValue())) {
                return MASKED_VALUE;
              } else {
                return entry.getValue() != null ? entry.getValue() : "";
              }
            }
    ));
  }

  private JsonNode maskInternal(String key, JsonNode target) {
    if (target.isValueNode() && key != null && matchesMaskingKeywords(key)) {
      return new TextNode(MASKED_VALUE);
    } else if (key != null && matchesCatchAllMaskingKeywords(key)) {
      return maskAllInternal(target);
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

  private JsonNode maskAllInternal(JsonNode target) {
    if (target.isValueNode()) {
      return new TextNode(MASKED_VALUE);
    } else if (target.isArray()) {
      for (int index = 0; index < target.size(); index++) {
        ((ArrayNode) target).set(index, maskAllInternal(target.get(index)));
      }
    } else if (target.isObject()) {
      Iterator<Entry<String, JsonNode>> fields = target.fields();
      while (fields.hasNext()) {
        Entry<String, JsonNode> field = fields.next();
        ((ObjectNode) target).replace(field.getKey(), maskAllInternal(field.getValue()));
      }
    }
    return target;
  }

}
