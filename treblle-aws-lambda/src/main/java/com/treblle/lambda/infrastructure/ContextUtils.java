package com.treblle.lambda.infrastructure;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ContextUtils {

    public static <K, V> Map<K, V> iteratorToMap(Iterator<Map.Entry<K, V>> iterator) {
        Spliterator<Map.Entry<K, V>> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
