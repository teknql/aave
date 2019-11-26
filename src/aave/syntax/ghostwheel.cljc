(ns aave.syntax.ghostwheel
  "Namespace for parsing ghosthweel style syntax"
  (:require [aave.code :as code]))

(defn param-vector?
  "Returns whether the vector is valid as parameter bindings"
  [x]
  (vector? x))

(defn schema-vector?
  "Returns whether the vector is valid as malli schema vector"
  [x]
  (vector? x))

(defn extract-schema+params+body
  "Extracts schema + params + body from args."
  [args]
  (let [[params args]                     (code/extract-arg args param-vector? "Must provide parameters")
        [schema-vector args]              (code/extract-arg args schema-vector?)
        split-idx                         (.indexOf (or schema-vector []) '=>)
        [param-schema-vec ret-schema-vec] (split-at split-idx schema-vector)
        _                                 (when schema-vector
                                            (assert (= (count param-schema-vec)
                                                       (count params))
                                                    "Number of parameters in schema do not match.")
                                            (assert (not= split-idx -1)
                                                    "Missing => in schema vector")
                                            (assert (= 2 (count ret-schema-vec))
                                                    "Invalid return schema provided"))
        param-schema                      (when (not-empty param-schema-vec)
                                            (reduce conj [:tuple] param-schema-vec))
        ret-schema                        (last ret-schema-vec)
        body                              args]
    {:params+body  (cons params body)
     :ret-schema   ret-schema
     :param-schema param-schema}))

(defn merge-param-schemas
  "Merges a collection of param schemas into a single multi-dispatch schema"
  [schemas]
  (into [:multi {:dispatch count}]
        (map (fn [x] [(dec (count x)) x]))
        schemas))

(defn parse
  "Parses a collection of quoted expressions as ghostwheel style function definition.

  Retuerns a map to be used by `cg/generate`"
  [args]
  (let [[name-sym args] (code/extract-arg args symbol? "First argument must be a symbol")
        [doc-str args]  (code/extract-arg args string?)
        [meta-map args] (code/extract-arg args map?)
        [params+bodies param-schema ret-schema]
        (if-not (code/overloaded? args)
          ((juxt :params+body :param-schema :ret-schema) (extract-schema+params+body args))
          (loop [params+bodies '()
                 param-schemas []
                 ret-schemas   []
                 items         args]
            (if-let [item (first items)]
              (let [{:keys [params+body param-schema ret-schema]} (extract-schema+params+body item)]
                (recur (cons params+body params+bodies)
                       (conj param-schemas param-schema)
                       (conj ret-schemas ret-schema)
                       (rest items)))
              (let [param-schema  (merge-param-schemas param-schemas)
                    _             (doseq [[a b] (partition 2 1 ret-schemas ret-schemas)]
                                    (assert (= a b) (str "Different return schemas not supported ("
                                                         a " vs. " b ")")))
                    ret-schema    (first ret-schemas)
                    params+bodies (reverse params+bodies)]
                [params+bodies param-schema ret-schema]))))]
    `{:name         ~name-sym
      :doc          ~doc-str
      :meta-map     ~meta-map
      :params+body  ~params+bodies
      :param-schema ~param-schema
      :ret-schema   ~ret-schema}))
