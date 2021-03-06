(ns aave.core
  "Main namespace of aave"
  (:require [aave.code :as code]
            [aave.config :as config]
            [aave.syntax.ghostwheel :as syntax.gw]))

(defn fns
  "Returns a sequence of vars of all functions generated by aave in the provided ns"
  [ns]
  (filter (comp ::generated meta) (vals (ns-interns ns))))

(def config
  "Configuration atom for aave"
  config/config)

(defn set-config!
  "Sets aave's config to the new config"
  [new-config]
  (config/set! new-config))


(defmacro >defn
  "Defines a function with optional schemas in the style of ghostwheel"
  {:arglists '([name doc-string? attr-map? [params*] [schemas*]? body])}
  [& args]
  (let [cfg (merge (syntax.gw/parse args)
                   {:private false})]
    `(code/generate ~cfg)))

(defmacro >defn-
  "Defines a private function with optional schemas in the style of ghostwheel"
  {:arglists '([name doc-string? attr-map? [params*] [schemas*]? body])}
  [& args]
  (let [cfg (merge (syntax.gw/parse args)
                   {:private true})]
    `(code/generate ~cfg)))
