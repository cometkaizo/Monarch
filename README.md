# Monarch
## Overview
Monarch is a bytecode-based, proof-of-concept programming language that aims to explore new programming concepts through a modular and reflective design. Monarch will support and/or already supports features such as the definition of custom syntax, interactions with the compiler during compilation, custom AST rules, and possibly even extensions to the VM.

Currently, Monarch's compiler is written in Java. It will be rewritten in Monarch in the future to allow for interactions with the compiler. The compiler outputs platform-independent compiled files in the form of custom bytecode instructions.

The Monarch Virtual Machine is written in C, and executes the bytecode instructions outputted by the compiler. Monarch defines its own bytecode instruction set. The VM is stack-based.

## Key Features
### Custom Syntax
A source code file specifies exactly what syntax structures it uses within it, kind of like importing classes in Java. Using the `compile with ...` and `compile without ...` syntaxes, which are the only syntaxes imported by default, a syntax can be added or removed by name.

In this system, programming structures found commonly within programming languages like `if` statements or `for` loops have no special status when compared to user-defined structures. In principle, they are more like an API, rather than strict language rules. They can be used or ignored by the compiler at will.

For instance, if you find yourself using the singleton design pattern a lot, you might find it helpful to define a `singleton` keyword which can be applied as a modifier to a class, just as `final` or `abstract` can. Or maybe you define an entirely new type of class entirely. Maybe you reject the existing OOP syntaxes and design your own functional paradigm language rules from scratch.

Syntaxes can take in per-usage parameters, which might include things like what other syntaxes can be used inside this one.

### Macros
Syntaxes don't need to be a new keyword or structure, they can produce the same effect as code that can already be written, just written differently. These are called macros, and are a subcategory of syntaxes that produce existing AST nodes. Macros exist in C, for example, in its pre-processor. 

For example, a macro that prints "Hello, World!" might be used like this:
```
compile with hello_world_macro
hw
```
where `hw` is the syntax defined by `hello_world_macro`, and this might produce AST equivalent to:
```
print("Hello, World!");
```

An example of a macro that "takes" parameters might look like:
```
compile with add_macro
print(add 3 5);
```
and might produce AST equivalent to:
```
print(8);
```

Macros used with syntaxes can create shorthands:
```
compile with compile_with_shorthand

cw hello_world_macro
cw add_macro

hw
print(add 3 5);
```
The word `cw` in this case might be defined by `compile_with_shorthand` as equivalent to `compile with`.

You can even make a macro that imports all the most commonly used syntaxes for you.

## Current Features

- Functions
- Local variables (stack)
- Parameters (stack)
- Return values
- Operators `+, -, >, !, or, and, xor, *`
- `if` statements
- `while` loops
- User input
- Time tracking
- Support for jumping execution between multiple bytecode files 