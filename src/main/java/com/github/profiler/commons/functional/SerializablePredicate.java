package com.github.profiler.commons.functional;

import org.pitest.functional.predicate.Predicate;

import java.io.Serializable;

public interface SerializablePredicate<T extends Serializable> extends Predicate<T>, Serializable {

}
