package com.cometkaizo.command.arguments;

import java.util.function.Predicate;

/**
 * Arguments are responsible for translating user inputted strings into their corresponding objects.
 * (e.g. "true" -> {@code true}, "0.34" -> {@code 0.34D}, etc)
 */
public abstract class Argument {

    protected final String name;
    protected final Predicate<Object> requirement;

    protected Argument(String name) {
        this.name = name;
        this.requirement = o -> true;
    }
    protected Argument(String name, Predicate<Object> requirement) {
        this.name = name;
        this.requirement = requirement;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns true if the predicate is met and the given string can be translated.
     * @param string the string to test
     * @return whether the string can be translated
     */
    public abstract boolean accepts(String string);

    /**
     * Translates the given string if it can, otherwise throws IllegalArgumentException
     * @param string the string to translate
     * @return the translated object
     * @throws IllegalArgumentException Thrown if the given string cannot be translated. More formally,
     * for a given string {@code s}, this method will throw an exception if {@code is(s)} returns false.
     */
    public abstract Object translate(String string) throws IllegalArgumentException;

    public String toPrettyString() {
        return name + " : " + getClass().getSimpleName().replaceAll("(?<=.)Argument$", "").toUpperCase();
    }

}
