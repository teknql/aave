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
        settings        (merge config/config meta-map)
        malli-opts      (:aave.core/malli-opts meta-map)
        param-explainer (when param-schema
                          (m/explainer param-schema malli-opts))
        ret-explainer   (when ret-schema
                          (m/explainer ret-schema malli-opts))
        new-meta        (-> (meta name)
                            (assoc :doc doc
                                   :aave.core/generated true
                                   :aave.core/param-schema param-schema
                                   :aave.core/param-explainer param-explainer
                                   :aave.core/ret-schema ret-schema
                                   :aave.core/ret-explainer ret-explainer)
                            (merge meta-map))
        params+body     (cond->> params+body
                          (::generate-stubs settings)
                          (map-body (fn [_ body]
                                      (if (empty? body)
                                        `((mg/generate ~ret-schema))
                                        body)))
                          (and (::instrument settings) param-explainer)
                          (map-body (fn [param-syms body]
                                      `((when-some [failure# ((-> #'~name meta ::param-explainer) [~@param-syms])]
                                          (~(::on-instrument-fail settings) failure#))
                                        (do ~@body))))

                          (and (::outstrument settings) ret-explainer)
                          (map-body (fn [_ body]
                                      `((let [result# (do ~@body)]
                                          (if-some [failure# ((-> #'~name meta ::ret-explainer) result#)]
                                            (~(::on-outstrument-fail settings) failure#)
                                            result#))))))
        fn-def (concat (keep identity
                             [def-sym
                              name
                              new-meta])
                       params+body)]
    `~fn-def))
