package com.cometkaizo.util;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import static com.cometkaizo.util.CollectionUtils.anyMatch;
import static com.cometkaizo.util.CollectionUtils.contains;

public class FieldDisplayer {
    protected final Object obj;
    protected Predicate<Class<?>> displayPredicate;

    public FieldDisplayer(Object obj) {
        this.obj = obj;
        displayPredicate = c -> false;
    }

    @Override
    public String toString() {
        var result = new StringBuilder();
        toString(obj, result);
        return result.toString();
    }

    protected void toString(Object o, StringBuilder result) {
        if (o == null) result.append("null");
        else if (displayPredicate.test(o.getClass()) || o == obj) {
            displayFields(o, result);
        } else {
            result.append(o);
        }
    }

    private void displayFields(Object o, StringBuilder result) {
        result.append(o.getClass().getSimpleName()).append('{');

        Field[] fields = o.getClass().getFields();
        boolean first = true;
        for (Field f : fields) {
            if (f == null) continue;
            if (!first) result.append(", ");
            displayField(f, o, result);
            first = false;
        }

        result.append('}');
    }

    protected void displayField(Field f, Object o, StringBuilder result) {
        result.append(f.getName()).append('=');
        try {
            Object value = f.get(o);
            toString(value, result);
        } catch (Exception e) {
            result.append("Unable to evaluate!!! ").append(e.getMessage());
        }
    }

    public FieldDisplayer display(Class<?>... superClasses) {
        return display(c -> {
            return anyMatch(superClasses, superClass -> superClass.isAssignableFrom(c));
        });
    }
    public FieldDisplayer displayExact(Class<?>... classes) {
        return display(c -> contains(classes, c));
    }
    public FieldDisplayer display(Predicate<Class<?>> predicate) {
        return setDisplay(displayPredicate.or(predicate));
    }
    public FieldDisplayer setDisplay(Predicate<Class<?>> predicate) {
        displayPredicate = predicate;
        return this;
    }
}
