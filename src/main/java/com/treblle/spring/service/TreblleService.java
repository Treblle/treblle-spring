package com.treblle.spring.service;

import com.treblle.spring.dto.TrebllePayload;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface TreblleService {

  TrebllePayload createPayload(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      Exception exception,
      long responseTimeInMillis);

  void maskAndSendPayload(
      TrebllePayload payload, byte[] requestBody, byte[] responseBody, Exception chainException);
}
