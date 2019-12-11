(ns aave.config
  "Namespace for configuring aave")

(def default
  "The default settings used by aave"
  {:aave.core/generate-stubs             true
   :aave.core/outstrument                true
   :aave.core/instrument                 true
   :aave.core/enforce-purity             true
   :aave.core/malli-opts                 {}
   :aave.core/on-compilation-purity-fail (fn [] (throw (ex-info "Function name implies purity, but it calls impure code" {})))
   :aave.core/on-instrument-fail         #(println "Instrument failed: " %)
   :aave.core/on-outstrument-fail        #(println "Outstrument failed: " %)})

(def config
  "Configuration atom for aave"
  (atom default))

(defn set!
  "Sets aave's config to the new config"
  [new-config]
  (reset! config new-config))
