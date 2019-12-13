# Aave

[malli](https://github.com/metosin/malli) powered code checking for Clojure.

Pre-alpha - highly subject to change.

> “Wherever we find orderly, stable systems in Nature, we find that they are
> hierarchically structured, for the simple reason that without such structuring
> of complex systems into sub-assemblies, there could be no order and
> stability-except the order of a dead universr filled with a uniformly distributed gas.”

-- Arthur Koestler, The Ghost in the Machine


## About

aave enables you to write inline schema definitions for your functions. It is
highly configurable enabling you to choose what happens when various checks fail.
It also provides utilities for common code-quality checks, such as naming
conventions.

## Examples / Features

### In/Outstrumentation

Checks that function inputs and outputs adhere to their schemas before/after
invoking function bodies.

#### Flags:

- `:aave.core/instrument`, default `true`
- `:aave.core/outstrument`, default `true`

#### Hooks:

- `:aave.core/on-instrument-fail`, default: throw exception
- `:aave.core/on-outstrument-fail`, default: throw exception

```clj
(ns example
  (:require [aave.core :refer [>defn] :as a]))

(>defn bad-return-val
  [x y]
  [int? int? => string?]
  (+ x y))

;; (bad-return-val 1 "foo") will raise `on-instrument-fail`
;; (bad-return-val 1 2) will raise `on-outstrument-fail`
```

### Stub Generation

Causes function definitions with empty bodies to
return randomly generated data instead of `nil`.

#### Flags:

- `:aave.core/generate-stubs`, Default: `true`


```clj
(ns example
  (:require [aave.core :refer [>defn] :as a]))

(>defn add
  {::a/generate-stubs true}
  [x y]
  [int? int? => int?])

;; (add 1 2) => -1
```

### Purity Enforcement

Checks that functions that call functions with `!` are also named with a `!`.

#### Flags:

- `:aave.core/enforce-purity`, default: `true`

#### Hooks:

- `:aave.core/on-compilation-purity-fail`, default: throw compile-time exception


```clj
(ns example
  (:require [aave.core :refer [>defn] :as a]))

(def counter (atom nil))

(>defn increment-counter
  "Increments the coutner and returns the old value"
  {::a/enforce-purity true
   ::a/on-purity-fail (fn [] (throw (ex-info "Purity mismatch")))}
  []
  [=> int?]
  (swap! some-atom inc)))
;; Will raise an exception. Will pass if function is named `increment-counter!`
```


## Development

aave is an open-source project and contributions are more than welcome!

### Roadmap

- [ ] Compile-time exercising of (pure) functions
- [ ] Test assertions for a namespace
- [ ] Benchmarking via `::a/benchmark true`
- [ ] Consider custom exception types instead of `ex-info`
- [ ] Additional syntax support (eg. schema style)
- [ ] Clojurescript testing
- [ ] Config groups to save groups of settings as named groups.
- [ ] Configuration at compile time and loading via file at run time
- [ ] nrepl middleware - add ops for getting sample input and querying function metadata

### Testing

```
clojure -Atest
```

### Important Namespaces

- `aave.core` - The main "user-level" namespace. Should remain concise and
  focused on things that a developer would want to do from the REPL or imports
  that should be readily available.

- `aave.code` - Responsible for code generation and analysis. Provides utilities
  and macros for analysing and modifying code. The most important function in
  here is the `generate` macro, which is used to generate function definitions
  from a config map.

- `aave.config` - Provides the implementation of the configuration management
  for aave. For users, the majority of functionality should be exposed via
  `aave.core`.

- `aave.syntax.*` - Code parsing constructs for specific syntaxes of
  function declaration. Eg. `ghostwheel`, `def-spec`, etc. If you want to extend
  aave with support for your favorite way of declaring functions, this is the
  place to do it.

## Inspiration

Aave borrows and builds upon ideas of many fantastic projects. Most directly, it
is largely inspired by tools like
[ghostwheel](https://github.com/gnl/ghostwheel) and
[orchestra](https://github.com/jeaye/orchestra). Thanks to the authors and many
others that have provided an amazing foundation of ideas to build upon.

