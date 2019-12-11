(ns aave.code
  "Namespace providing utilities to parse and generate code"
  (:require [aave.config :as config]
            [malli.core :as m]
            [malli.generator :as mg]))

(defn overloaded?
  "Returns whether the provided `body+params` list is overloaded in arities"
  [body+params]
  (not (vector? (first body+params))))

(defn map-body
  "Maps over the `body` portion of a `body+params` list with `f`. Calls `f` with both the param
  symbols and the body."
  [f body+params]
  (if (overloaded? body+params)
    (map #(map-body f %) body+params)
    (cons (first body+params) (f (first body+params) (rest body+params)))))

(defn pure?
  "Naively iterates over symbols in the provided `form` looking for whether any
  of them end with a literal `!`. If so, returns `false`.

  Does not count `persistent!`."
  [form]
  (let [form (if-not (seqable? form)
               (list form)
               form)]
    (->> (flatten form)
         (filter symbol?)
         (keep #(re-matches #".*!$" (str %)))
         (remove #{"persistent!"})
         (empty?))))

(def impure?
  "The complement of `pure?`."
  (complement pure?))

(defn extract-arg
  "Utility function for extracting arguments from a list.

  Returns a tuple of the value matching the `pred` if it returns logical true and the
  rest of the arg list. Otherwise returns the original arg list.

  Takes an optional `error` which will assert that the `pred` returns a truthy
  value.."
  ([args pred] (extract-arg args pred nil))
  ([args pred error]
   (let [[arg new-args] [(first args) (rest args)]]
     (if error
       (do (assert (pred arg) error)
           [arg new-args])
       (if (pred arg)
         [arg new-args]
         [nil args])))))

(defmacro generate
  "Generates code using the provided `generate-map`"
  {:arglists '([generate-map])}
  [{:keys [name doc params+body private param-schema ret-schema meta-map]}]
  (let [def-sym         (if private
                          'defn-
                          'defn)
        settings        (merge @config/config meta-map)
        malli-opts      (:aave.core/malli-opts meta-map)
        param-explainer (when param-schema
                          `(m/explainer ~param-schema ~malli-opts))
        ret-explainer   (when ret-schema
                          `(m/explainer ~ret-schema ~malli-opts))
        new-meta        (-> (meta name)
                            (assoc :doc doc
                                   :aave.core/generated true
                                   :aave.core/param-schema param-schema
                                   :aave.core/param-explainer param-explainer
                                   :aave.core/ret-schema ret-schema
                                   :aave.core/ret-explainer ret-explainer)
                            (merge meta-map))
        enforce-purity? (:aave.core/enforce-purity settings)
        on-purity-fail  (:aave.core/on-compilation-purity-fail settings)
        params+body     (cond->> params+body
                          (:aave.core/generate-stubs settings)
                          (map-body (fn [_ body]
                                      (if (empty? body)
                                        `((mg/generate ~ret-schema))
                                        body)))
                          (and (:aave.core/instrument settings) param-explainer)
                          (map-body (fn [param-syms body]
                                      `((when-some [failure# ((-> #'~name meta :aave.core/param-explainer) [~@param-syms])]
                                          (~(:aave.core/on-instrument-fail settings) failure#))
                                        (do ~@body))))

                          (and (:aave.core/outstrument settings) ret-explainer)
                          (map-body (fn [_ body]
                                      `((let [result# (do ~@body)]
                                          (if-some [failure# ((-> #'~name meta :aave.core/ret-explainer) result#)]
                                            (~(:aave.core/on-outstrument-fail settings) failure#)
                                            result#))))))
        fn-def (concat (keep identity
                             [def-sym
                              name
                              new-meta])
                       params+body)]
    `(cond
      (and ~enforce-purity? ~(pure? name) ~(impure? params+body))
      (~on-purity-fail)
      :else ~fn-def)))
