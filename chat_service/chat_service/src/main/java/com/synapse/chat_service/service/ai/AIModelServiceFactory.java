package com.synapse.chat_service.service.ai;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AIModelServiceFactory {

    private final Map<AIModelType, AIModelService> services;

    public AIModelServiceFactory(List<AIModelService> serviceList) {
        this.services = serviceList.stream()
                .collect(Collectors.toUnmodifiableMap(AIModelService::getModelType, Function.identity()));
    }

    public AIModelService getService(AIModelType modelType) {
        AIModelService service = services.get(modelType);
        if (service == null) {
            throw new IllegalArgumentException("지원하지 않는 AI 모델 타입입니다: " + modelType);
        }
        return service;
    }
}
